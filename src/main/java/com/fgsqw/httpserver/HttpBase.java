// HttpBase.java
package com.fgsqw.httpserver;

import com.fgsqw.httpserver.utils.HttpUtil;
import com.fgsqw.httpserver.utils.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Map;
import java.util.TreeMap;

/**
 * HTTP基类
 * 封装HTTP请求和响应的公共属性和方法
 *
 * @author fgsq
 */
public class HttpBase {

    /** 内容长度 */
    private long contentLength = 0;
    /** 客户端IP */
    private String clientIp;
    /** 请求URL */
    private final Url url;
    /** 请求来源URL */
    private Url refererUrl;
    /** 请求Socket */
    protected final Socket socket;
    /** 输入流 */
    protected final InputStream in;
    /** 输出流 */
    protected final OutputStream out;

    /** 请求头映射（不区分大小写） */
    private final Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    /**
     * 构造函数（通过URL创建连接）
     *
     * @param url URL地址
     * @throws IOException IO异常
     */
    public HttpBase(String url) throws IOException {
        this.url = HttpUtil.parseHttpUrl(url);
        this.socket = new Socket(this.url.getHost(), this.url.getPort());
        in = socket.getInputStream();
        out = socket.getOutputStream();
        InetAddress localHost = socket.getInetAddress();
        clientIp = localHost.getHostAddress();
    }

    /**
     * 构造函数（通过Socket创建）
     *
     * @param socket 客户端Socket
     * @throws IOException IO异常
     */
    public HttpBase(Socket socket) throws IOException {
        this.socket = socket;
        in = socket.getInputStream();
        out = socket.getOutputStream();
        url = new Url();
        url.setHost(socket.getInetAddress().getHostName());
        url.setPort(socket.getPort());
        InetAddress localHost = socket.getInetAddress();
        clientIp = localHost.getHostAddress();
    }

    /**
     * 获取真实ip
     */
    public String getRealClientIP() {
        // nginx转发时获取真实ip
        String ip = headers.get(HttpConstant.X_REAL_I_P);
        if (!StringUtils.isEmpty(ip) && !HttpConstant.UNKNOWN.equals(ip)) {
            return ip;
        }
        // 获取转发真实ip
        ip = headers.get(HttpConstant.X_FORWARDED_FOR);
        if (!StringUtils.isEmpty(ip) && !HttpConstant.UNKNOWN.equals(ip)) {
            return ip;
        }
        return clientIp;
    }

    /**
     * 获取客户端ip
     */
    public String getClientIP() {
        return clientIp;
    }

    /**
     * 添加请求头
     *
     * @param key   头部名称
     * @param value 头部值
     */
    public void addHeader(String key, String value) {
        headers.put(key, value);
        if (HttpConstant.CONTENT_LENGTH.equalsIgnoreCase(key)) {
            // 设置 contentLength
            contentLength = Long.parseLong(value.replaceAll("[\\s+\\t\\n\\r]", ""));
        } else if (HttpConstant.REFERER.equalsIgnoreCase(key)) {
            refererUrl = HttpUtil.parseHttpUrl(value);
        }
    }

    /**
     * 设置客户端IP
     *
     * @param clientIp 客户端IP
     */
    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    /**
     * 获取Socket
     *
     * @return Socket
     */
    public Socket getSocket() {
        return socket;
    }

    /**
     * 获取所有请求头
     *
     * @return 请求头映射
     */
    public Map<String, String> getHeaders() {
        return headers;
    }

    /**
     * 获取请求头值
     *
     * @param key 头部名称
     * @return 头部值
     */
    public String getHeaderValue(String key) {
        return headers.get(key);
    }

    /**
     * 获取内容长度
     *
     * @return 内容长度
     */
    public long getContentLength() {
        return contentLength;
    }

    /**
     * 设置内容长度
     *
     * @param contentLength 内容长度
     */
    public void setContentLength(long contentLength) {
        this.contentLength = contentLength;
        headers.put(HttpConstant.CONTENT_LENGTH, contentLength + "");
    }

    /**
     * 获取URL
     *
     * @return URL对象
     */
    public Url getUrl() {
        return url;
    }

    /**
     * 获取来源URL
     *
     * @return 来源URL对象
     */
    public Url getRefererUrl() {
        return refererUrl;
    }

    /**
     * 是否已关闭
     *
     * @return 是否已关闭
     */
    public boolean isClosed() {
        return socket.isClosed();
    }

    /**
     * 获取输入流
     *
     * @return 输入流
     */
    public InputStream getIn() {
        return in;
    }

    /**
     * 获取输出流
     *
     * @return 输出流
     */
    public OutputStream getOut() {
        return out;
    }
}
