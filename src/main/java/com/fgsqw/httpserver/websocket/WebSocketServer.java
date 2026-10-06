
package com.fgsqw.httpserver.websocket;

import com.fgsqw.httpserver.HttpConstant;
import com.fgsqw.httpserver.utils.FileUtil;
import com.fgsqw.httpserver.utils.IOUtil;
import com.fgsqw.httpserver.utils.StringUtils;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * WebSocket服务端实现类
 * 处理服务器端特有的功能，包括握手、消息发送和接收
 *
 * @author fgsq
 */
public class WebSocketServer implements Closeable {
    /** 日志 */
    private static final Logger logger = LoggerFactory.getLogger(WebSocketServer.class);

    /** Socket连接 */
    private Socket socket;
    /** 输出流 */
    private OutputStream out;
    /** 输入流 */
    private InputStream in;
    /** 是否已关闭 */
    private boolean isClosed = false;

    /**
     * 构造函数
     *
     * @param secWebSocketKey WebSocket密钥
     * @param socket          Socket连接
     * @throws IOException              IO异常
     * @throws NoSuchAlgorithmException 算法不存在异常
     */
    public WebSocketServer(String secWebSocketKey, Socket socket) throws IOException, NoSuchAlgorithmException {
        this.socket = socket;
        this.out = socket.getOutputStream();
        this.in = socket.getInputStream();
        writeWebSocket(secWebSocketKey); // WebSocket 握手
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
        return WebSocketUtil.readFrame(in, false);  // false 表示作为服务端读取
    }

    /**
     * 发送文本消息给客户端
     *
     * @param msg 要发送的消息
     * @throws IOException 如果发生I/O错误
     */
    public synchronized void sendString(String msg) throws IOException {
        WebSocketUtil.sendServerFrame(out, msg.getBytes(FileUtil.UTF_8), WebSocketUtil.OPCODE_TEXT);
    }

    /**
     * 发送二进制消息给客户端
     *
     * @param data 要发送的二进制数据
     * @throws IOException 如果发生I/O错误
     */
    public synchronized void sendBinary(byte[] data) throws IOException {
        WebSocketUtil.sendServerFrame(out, data, WebSocketUtil.OPCODE_BINARY);
    }

    /**
     * 发送关闭帧
     *
     * @throws IOException 如果发生I/O错误
     */
    public synchronized void sendClose() throws IOException {
        WebSocketUtil.sendServerFrame(out, new byte[0], WebSocketUtil.OPCODE_CLOSE);
    }

    /**
     * 发送ping消息
     *
     * @throws IOException 如果发生I/O错误
     */
    public synchronized void sendPing() throws IOException {
        WebSocketUtil.sendServerFrame(out, new byte[0], WebSocketUtil.OPCODE_PING);
    }

    /**
     * 关闭连接
     *
     * @throws IOException IO异常
     */
    @Override
    public synchronized void close() throws IOException {
        if (isClosed) return;
        isClosed = true;
        IOUtil.closeIO(in, out, socket);
    }

    /**
     * 发送WebSocket握手消息
     *
     * @param webSocketKey WebSocket密钥
     * @throws IOException              IO异常
     * @throws NoSuchAlgorithmException 算法不存在异常
     */
    public void writeWebSocket(String webSocketKey) throws IOException, NoSuchAlgorithmException {
        if (!StringUtils.isEmpty(webSocketKey)) {
            byte[] digest = MessageDigest
                    .getInstance("SHA-1")
                    .digest((webSocketKey + HttpConstant.WS_MAGIC).getBytes(FileUtil.UTF_8));
            String handshakeAccept = Base64.encodeBase64String(digest);
            String s = ("HTTP/1.1 101 Switching Protocols\r\n"
                    + "Upgrade: websocket\r\n"
                    + "Connection: Upgrade\r\n"
                    + "Sec-WebSocket-Accept: "
                    + handshakeAccept
                    + "\r\n\r\n");
            out.write(s.getBytes(FileUtil.UTF_8));
            out.flush();
        }
    }

    /**
     * 检查连接是否已关闭
     *
     * @return 如果连接已关闭则返回true，否则false
     */
    public boolean isClosed() {
        return isClosed;
    }
}
