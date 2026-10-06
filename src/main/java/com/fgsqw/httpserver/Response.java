// Response.java
package com.fgsqw.httpserver;

import com.fgsqw.httpserver.utils.IOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.util.Map;
import java.util.Set;

/**
 * HTTP响应类
 * 封装HTTP响应的所有信息，包括状态码、头部、内容等
 *
 * @author fgsq
 */
public class Response extends HttpBase {

    /**
     * 日志
     */
    private static final Logger logger = LoggerFactory.getLogger(Response.class);

    /** 范围长度 */
    private long[] rangeLength = null;

    /** 状态码 */
    private int status = 200;
    /** 状态消息 */
    private String statusMessage = "";
    /** 是否已响应标志 */
    private boolean responded = false;

    /**
     * 构造函数
     *
     * @param socket 客户端Socket
     * @throws IOException IO异常
     */
    public Response(Socket socket) throws IOException {
        super(socket);
        setStatus(200, "OK");
        addHeader(HttpConstant.CONTENT_TYPE, HttpConstant.HTML_CONTEXT_TYPE);
        addHeader(HttpConstant.CONNECTION, HttpConstant.CLOSE);
    }

    /**
     * 是否已响应
     *
     * @return 是否已响应
     */
    public boolean isResponded() {
        return responded;
    }

    /**
     * 设置状态码
     *
     * @param status        状态码
     * @param statusMessage 状态消息
     */
    public void setStatus(int status, String statusMessage) {
        this.status = status;
        this.statusMessage = statusMessage;
    }

    /**
     * 获取范围长度
     *
     * @return 范围长度数组
     */
    public long[] getRangeLength() {
        return rangeLength;
    }

    /**
     * 设置范围长度
     *
     * @param rangeLength 范围长度数组
     */
    public void setRangeLength(long[] rangeLength) {
        this.rangeLength = rangeLength;
    }

    /**
     * 设置内容范围
     *
     * @param start  起始位置
     * @param end    结束位置
     * @param length 内容长度
     */
    public void setContentRange(long start, long end, long length) {
        setStatus(206, "Partial Content");
        addHeader(HttpConstant.ACCEPT_RANGES, "bytes");
        addHeader(HttpConstant.CONTENT_RANGES, "bytes " + start + "-" + (end - 1) + "/" + (length + start));
        setContentLength(length);
    }

    /**
     * 设置内容类型
     *
     * @param contentType 内容类型
     */
    public void setContentType(String contentType) {
        addHeader(HttpConstant.CONTENT_TYPE, contentType);
    }

    /**
     * 获取状态码
     *
     * @return 状态码
     */
    public int getStatus() {
        return status;
    }

    /**
     * 创建响应头
     *
     * @return 响应头字符串
     */
    private StringBuilder createHeader() {
        StringBuilder sb = new StringBuilder();
        sb.append(HttpConstant.HTTP_VERSION).append(" ")
                .append(status).append(" ")
                .append(statusMessage)
                .append(HttpConstant.CRLF);
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
     * 写入204无内容
     *
     * @throws IOException IO异常
     */
    public void write204() throws IOException {
        String responseBody = "204 No Content";
        writeErrorCode(StatusEnum.STATUS_204_NOT_CONTENT, responseBody);
    }

    /**
     * 写入404未找到
     *
     * @throws IOException IO异常
     */
    public void write404() throws IOException {
        String responseBody = "404 Not Found";
        write404(responseBody);
    }

    /**
     * 写入405方法不允许
     *
     * @throws IOException IO异常
     */
    public void write405() throws IOException {
        String responseBody = "405 Method Not Allowed";
        write405(responseBody);
    }

    /**
     * 写入302重定向
     *
     * @param message 消息
     * @param url     重定向URL
     * @throws IOException IO异常
     */
    public void write302(String message, String url) throws IOException {
        // 重定向
        addHeader("Location", url);
        writeErrorCode(StatusEnum.STATUS_302_FOUND, message);
    }

    /**
     * 写入404未找到
     *
     * @param message 消息
     * @throws IOException IO异常
     */
    public void write404(String message) throws IOException {
        writeErrorCode(StatusEnum.STATUS_404_NOT_FOUND, message);
    }

    /**
     * 写入405方法不允许
     *
     * @param message 消息
     * @throws IOException IO异常
     */
    public void write405(String message) throws IOException {
        writeErrorCode(StatusEnum.STATUS_405_METHOD_NOT_ALLOWED, message);
    }

    /**
     * 写入500内部错误
     *
     * @throws IOException IO异常
     */
    public void write500() throws IOException {
        String responseBody = "500 error";
        writeErrorCode(StatusEnum.STATUS_500_INTERNAL_SERVER_ERROR, responseBody);
    }

    /**
     * 写入500内部错误
     *
     * @param message 错误消息
     * @throws IOException IO异常
     */
    public void write500(String message) throws IOException {
        writeErrorCode(StatusEnum.STATUS_500_INTERNAL_SERVER_ERROR, message);
    }

    /**
     * 写入错误码
     *
     * @param statusEnum 状态枚举
     * @throws IOException IO异常
     */
    public void writeErrorCode(StatusEnum statusEnum) throws IOException {
        writeErrorCode(statusEnum, statusEnum.getStatusMessage());
    }

    /**
     * 写入错误码
     *
     * @param statusEnum 状态枚举
     * @param message    消息
     * @throws IOException IO异常
     */
    public void writeErrorCode(StatusEnum statusEnum, String message) throws IOException {
        writeErrorCode(statusEnum.getStatus(), message);
    }

    /**
     * 写入错误码
     *
     * @param status  状态码
     * @param message 消息
     * @throws IOException IO异常
     */
    public void writeErrorCode(int status, String message) throws IOException {
        setStatus(status, message);
        writeBytes(message.getBytes(), HttpConstant.TEXT_CONTEXT_TYPE);
    }

    /**
     * 写入字符串（JSON格式）
     *
     * @param responseBody 响应体
     * @throws IOException IO异常
     */
    public void writeString(String responseBody) throws IOException {
        writeString(responseBody, HttpConstant.STREAM_CONTEXT_JSON);
    }

    /**
     * 写入空响应
     *
     * @throws IOException IO异常
     */
    public void writeEmpty() throws IOException {
        writeString("", HttpConstant.STREAM_CONTEXT_JSON);
    }

    /**
     * 写入字符串
     *
     * @param responseBody 响应体
     * @param contentType  内容类型
     * @throws IOException IO异常
     */
    public void writeString(String responseBody, String contentType) throws IOException {
        writeBytes(responseBody.getBytes(), contentType);
    }

    /**
     * 写入字节数组
     *
     * @param bytes 字节数组
     * @throws IOException IO异常
     */
    public void writeBytes(byte[] bytes) throws IOException {
        writeBytes(bytes, HttpConstant.STREAM_CONTEXT_TYPE);
    }

    /**
     * 返回字节数组
     *
     * @param bytes       字节数组
     * @param contentType 响应体contentType
     */
    public void writeBytes(byte[] bytes, String contentType) throws IOException {
        if (isClosed()) {
            return;
        }
        setContentLength(bytes.length);
        setContentType(contentType);
        StringBuilder sb = createHeader();
        out.write(sb.toString().getBytes());
        out.write(bytes);
        out.flush();
        responded = true;
    }

    /**
     * 根据文件名获取内容类型
     *
     * @param name 文件名
     * @return 内容类型
     */
    public static String getContentTypeByName(String name) {
        int i = name.lastIndexOf(".");
        if (i > 0) {
            String suffix = name.substring(i + 1);
            return ContentTypes.contentTypeMap.get(suffix);
        }
        return null;
    }

    /**
     * 返回文件，根据文件名判断响应体的contentType类型
     *
     * @param file 文件
     */
    public void writeFile(File file) throws IOException {
        String name = file.getName();
        String contentTypeByName = getContentTypeByName(name);
        if (contentTypeByName != null) {
            writeFile(file, contentTypeByName);
        } else {
            writeFile(file, HttpConstant.STREAM_CONTEXT_TYPE);
        }
    }

    /**
     * 写入文件
     *
     * @param file        文件
     * @param contentType 内容类型
     * @throws IOException IO异常
     */
    public void writeFile(File file, String contentType) throws IOException {
        if (isClosed()) {
            return;
        }
        if (!file.exists()) {
            write404();
            return;
        }
        setContentLength(file.length());
        setContentType(contentType);
        InputStream is = new FileInputStream(file);
        long contentLength;
        if (rangeLength == null) {
            setStatus(200, "OK");
            StringBuilder sb = createHeader();
            out.write(sb.toString().getBytes());
            contentLength = file.length();
            setContentLength(contentLength);
            IOUtil.transfer(is, out);
        } else {
            long start = rangeLength[0];
            long end = rangeLength[1];
            if (end == -1) {
                end = file.length();
            }
            contentLength = file.length() - start;
            setContentRange(start, end, contentLength);
            StringBuilder sb = createHeader();
            out.write(sb.toString().getBytes());
            // 跳过指定数据包
            is.skip(start);
            IOUtil.transfer(is, out, contentLength);
        }
        out.flush();
        responded = true;
    }

    /**
     * 写入流
     *
     * @param is 输入流
     * @throws IOException IO异常
     */
    public void writeStream(InputStream is) throws IOException {
        writeStream(is, HttpConstant.STREAM_CONTEXT_TYPE);
    }

    /**
     * 返回数据流
     *
     * @param is          数据流
     * @param contentType 响应体contentType类型
     */
    public void writeStream(InputStream is, String contentType) throws IOException {
        if (isClosed()) {
            return;
        }
        setContentType(contentType);
        writeDirectStream(is);
    }

    /**
     * 直接写入流
     *
     * @param is 输入流
     * @throws IOException IO异常
     */
    public void writeDirectStream(InputStream is) throws IOException {
        if (isClosed()) {
            return;
        }
        StringBuilder sb = createHeader();
        out.write(sb.toString().getBytes());
        IOUtil.transfer(is, out);
        out.flush();
        close();
        responded = true;
    }

    /**
     * 写入流（自动识别内容类型）
     *
     * @param is       输入流
     * @param fileName 文件名
     * @throws IOException IO异常
     */
    public void writeStreamAutoContentType(InputStream is, String fileName) throws IOException {
        if (isClosed()) {
            return;
        }
        String contentTypeByName = getContentTypeByName(fileName);
        writeStream(is, contentTypeByName);
    }

    /**
     * 获取输出流
     *
     * @return 输出流
     */
    public OutputStream getOutputStream() {
        responded = true;
        return out;
    }

    /**
     * 获取带内容类型的输出流
     *
     * @param contentType 内容类型
     * @return 输出流
     * @throws IOException IO异常
     */
    public OutputStream getBodyOutputStream(String contentType) throws IOException {
        setContentType(contentType);
        StringBuilder sb = createHeader();
        out.write(sb.toString().getBytes());
        out.flush();
        responded = true;
        return out;
    }

    /**
     * 关闭响应
     */
    public void close() {
        try {
            out.close();
        } catch (IOException ignored) {
        }
    }

}
