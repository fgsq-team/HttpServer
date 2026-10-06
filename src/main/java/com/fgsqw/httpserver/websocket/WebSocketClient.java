
package com.fgsqw.httpserver.websocket;

import com.fgsqw.httpserver.HttpBase;
import com.fgsqw.httpserver.HttpConstant;
import com.fgsqw.httpserver.Request;
import com.fgsqw.httpserver.utils.FileUtil;
import com.fgsqw.httpserver.utils.HttpUtil;
import com.fgsqw.httpserver.utils.IOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Map;
import java.util.Set;

/**
 * WebSocket客户端实现类
 * 处理客户端特有的功能，包括握手、消息发送和接收
 *
 * @author fgsq
 */
public class WebSocketClient extends HttpBase implements Closeable {
    /** 日志 */
    private static final Logger logger = LoggerFactory.getLogger(WebSocketClient.class);
    /** WebSocket密钥 */
    private static final String WEBSOCKET_KEY = "dGhlIHNhbXBsZSBub25jZQ=="; // 示例密钥

    /** 输出流 */
    private OutputStream out;
    /** 输入流 */
    private InputStream in;
    /** 是否已关闭 */
    private boolean isClosed = false;
    /** 请求对象 */
    private Request request;

    /**
     * 构造函数
     *
     * @param url WebSocket服务器URL
     * @throws IOException IO异常
     */
    public WebSocketClient(String url) throws IOException {
        this(url, null);
    }

    /**
     * 构造函数
     *
     * @param socket Socket连接
     * @param path   请求路径
     * @throws IOException IO异常
     */
    public WebSocketClient(Socket socket, String path) throws IOException {
        this(socket, path, null);
    }

    /**
     * 构造函数
     *
     * @param url     WebSocket服务器URL
     * @param headers 请求头
     * @throws IOException IO异常
     */
    public WebSocketClient(String url, Map<String, String> headers) throws IOException {
        super(url);
        if (headers != null) {
            getHeaders().putAll(headers);
        }
        Socket socket = getSocket();
        this.out = socket.getOutputStream();
        this.in = socket.getInputStream();
        initialize();
    }

    /**
     * 构造函数
     *
     * @param socket  Socket连接
     * @param path    请求路径
     * @param headers 请求头
     * @throws IOException IO异常
     */
    public WebSocketClient(Socket socket, String path, Map<String, String> headers) throws IOException {
        super(socket);
        if (headers != null) {
            getHeaders().putAll(headers);
        }
        getUrl().setPath(path);
        this.out = socket.getOutputStream();
        this.in = socket.getInputStream();
        initialize();
    }

    /**
     * 初始化WebSocket握手请求，发送请求头和握手数据
     *
     * @throws IOException IO异常
     */
    public void initialize() throws IOException {
        addHeader("Connection", "Upgrade");
        addHeader("Upgrade", "websocket");
        addHeader("Sec-WebSocket-Key", WEBSOCKET_KEY);
        addHeader("Sec-WebSocket-Version", "13");
        addHeader("Host", getUrl().getHost() + ":" + getUrl().getPort());
        StringBuilder header = createHeader();
        out.write(header.toString().getBytes(FileUtil.UTF_8));
        out.flush();
        request = HttpUtil.parseRequestFromStream(in, getSocket(), null);
    }

    /**
     * 创建WebSocket请求头
     *
     * @return 创建的请求头字符串
     */
    private StringBuilder createHeader() {
        StringBuilder sb = new StringBuilder();
        sb.append("GET ");
        sb.append(getUrl().getPath()).append(" ");
        sb.append(HttpConstant.HTTP_VERSION);
        sb.append(HttpConstant.CRLF);
        Set<Map.Entry<String, String>> entries = getHeaders().entrySet();
        for (Map.Entry<String, String> entry : entries) {
            sb.append(entry.getKey());
            sb.append(": ");
            sb.append(entry.getValue());
            sb.append(HttpConstant.CRLF);
        }
        sb.append(HttpConstant.CRLF);
        return sb;
    }


    /**
     * 读取服务器的响应头，确认WebSocket握手成功
     *
     * @throws IOException 如果发生I/O错误或响应不正确
     */
    private void readServerResponse() throws IOException {
        StringBuilder response = new StringBuilder();
        byte[] buffer = new byte[1024];
        int bytesRead;

        // 读取响应头
        while ((bytesRead = in.read(buffer)) > 0) {
            response.append(new String(buffer, 0, bytesRead, FileUtil.UTF_8));

            // 如果已经读取到完整的响应头（以 "\r\n\r\n" 结尾），则停止读取
            if (response.toString().endsWith("\r\n\r\n")) {
                break;
            }
        }

        // 打印响应头用于调试
        logger.info("Server response: {}", response.toString());

        // 简单验证是否是 WebSocket 握手成功的响应
        if (!response.toString().contains("HTTP/1.1 101 Switching Protocols")) {
            throw new IOException("WebSocket handshake failed: " + response.toString());
        }
    }

    /**
     * 读取消息
     *
     * @return 接收到的消息字符串
     * @throws IOException 如果发生I/O错误
     */
    public String readString() throws IOException {
        WebSocketUtil.FrameData frame = readFrame();
        if (frame == null) return null;
        byte[] data = frame.getData();
        if (data == null) return null;
        return new String(data, FileUtil.UTF_8);
    }

    /**
     * 读取WebSocket帧数据
     *
     * @return 解码后的数据字节数组
     * @throws IOException 如果发生I/O错误
     */
    protected WebSocketUtil.FrameData readFrame() throws IOException {
        if (isClosed) {
            throw new IOException("Socket is closed");
        }
        return WebSocketUtil.readFrame(in, true);  // true 表示作为客户端读取
    }

    /**
     * 发送文本消息给服务器
     *
     * @param msg 要发送的消息
     * @throws IOException 如果发生I/O错误
     */
    public void sendTextMessage(String msg) throws IOException {
        WebSocketUtil.sendClientFrame(out, msg.getBytes(FileUtil.UTF_8), WebSocketUtil.OPCODE_TEXT);
    }

    /**
     * 发送二进制消息给服务器
     *
     * @param data 要发送的二进制数据
     * @throws IOException 如果发生I/O错误
     */
    public void sendBinaryMessage(byte[] data) throws IOException {
        WebSocketUtil.sendClientFrame(out, data, WebSocketUtil.OPCODE_BINARY);
    }

    /**
     * 发送关闭帧
     *
     * @throws IOException 如果发生I/O错误
     */
    public void sendClose() throws IOException {
        WebSocketUtil.sendClientFrame(out, new byte[0], WebSocketUtil.OPCODE_CLOSE);
    }

    /**
     * 发送ping消息
     *
     * @throws IOException 如果发生I/O错误
     */
    public void sendPing() throws IOException {
        WebSocketUtil.sendClientFrame(out, new byte[0], WebSocketUtil.OPCODE_PING);
    }

    /**
     * 关闭连接
     *
     * @throws IOException IO异常
     */
    @Override
    public void close() throws IOException {
        IOUtil.closeIO(in, out, getSocket());
        isClosed = true;
    }

    /**
     * 检查连接是否已关闭
     *
     * @return 如果连接已关闭则返回true，否则false
     */
    public boolean isClosed() {
        return isClosed;
    }


    public static void main(String[] args) throws Exception {
        WebSocketClient webSocketClient = new WebSocketClient("ws://127.0.0.1:8103/xfy_educate_school_leadin_websocket/ws_user10001_6880976411");
        webSocketClient.sendTextMessage("a");

        while (true) {
            String s = webSocketClient.readString();
            System.out.println(s);
            if (s != null) {
                webSocketClient.sendTextMessage(s);

            }
            if ("c".equals(s)) {
                break;
            }
        }
    }
}
