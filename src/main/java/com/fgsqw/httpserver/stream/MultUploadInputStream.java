package com.fgsqw.httpserver.stream;

import com.fgsqw.httpserver.HttpConstant;
import com.fgsqw.httpserver.Request;
import com.fgsqw.httpserver.exception.L500Exception;
import com.fgsqw.httpserver.utils.BufferInputStream;
import com.fgsqw.httpserver.utils.HttpUtil;
import com.fgsqw.httpserver.utils.StringUtils;

import java.io.IOException;
import java.io.InputStream;

/**
 * 多文件上传输入流
 * 支持解析multipart/form-data格式的多文件上传
 *
 * @author fgsq
 */
public class MultUploadInputStream extends InputStream {

    /** 文件名 */
    private String fileName;
    /** 文件大小 */
    private long fileSize;
    /** 已读大小 */
    private long readSize;
    /** 分隔符 */
    private String boundary;
    /** 分隔符字节数组 */
    private byte[] boundaryByte;
    /** 是否还有下一个文件 */
    private boolean hasNext = false;
    /** 请求对象 */
    private final Request request;
    /** 输入流 */
    private final InputStream is;
    /** 缓存流 */
    private final BufferInputStream bufferInputStream;

    /**
     * 获取下一个文件输入流
     *
     * @return 下一个文件的输入流
     * @throws IOException IO异常
     */
    public MultUploadInputStream next() throws IOException {
        if (!hasNext) {
            return null;
        }
        MultUploadInputStream multUploadInputStream = new MultUploadInputStream(request, bufferInputStream, boundary, boundaryByte);
        boolean ready = multUploadInputStream.readFileContentType();
        if (!ready) {
            return null;
        }
        return multUploadInputStream;
    }

    /**
     * 读取文件内容类型
     *
     * @return 是否成功读取
     * @throws IOException IO异常
     */
    private boolean readFileContentType() throws IOException {
        while (true) {
            String line = HttpUtil.readHttpLine(bufferInputStream);
            String lineUpperCase = line.toUpperCase();
            if (StringUtils.isEmpty(line)) {
                throw new L500Exception("文件上传失败");
            }
            if (lineUpperCase.startsWith(HttpConstant.CONTENT_DISPOSITION.toUpperCase())) {
                try {
                    fileName = line.substring(lineUpperCase.indexOf("filename=\"".toUpperCase()) + 10);
                    fileName = fileName.substring(0, fileName.indexOf("\""));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (lineUpperCase.startsWith(HttpConstant.CONTENT_TYPE.toUpperCase())) {
                if (fileName == null) {
                    fileName = StringUtils.getUUID();
                }
                bufferInputStream.skip(2); // \r\n
                break;
            }
        }
        return true;
    }

    /**
     * 读取分隔符
     *
     * @throws IOException IO异常
     */
    private void readBoundary() throws IOException {
        String headerValue = request.getHeaderValue(HttpConstant.CONTENT_TYPE);
        int i = headerValue.lastIndexOf("boundary=");
        boundary = headerValue.substring(i + 9);
        i = boundary.indexOf(";");
        if (i > 0) {
            boundary = boundary.substring(0, i);
        }
        boundary = "--" + boundary;
        boundaryByte = boundary.getBytes();
        while (true) {
            String line = HttpUtil.readHttpLine(bufferInputStream);
            if (StringUtils.isEmpty(line)) {
                throw new L500Exception("文件上传失败");
            }
            if (line.startsWith(boundary)) {
                break;
            }
        }
    }

    /**
     * 构造函数
     *
     * @param request         请求对象
     * @param bufferInputStream 缓存输入流
     * @param boundary        分隔符
     * @param boundaryByte    分隔符字节数组
     * @throws IOException IO异常
     */
    public MultUploadInputStream(Request request, BufferInputStream bufferInputStream, String boundary, byte[] boundaryByte) throws IOException {
        this.request = request;
        this.is = request.getInputStream();
        this.boundary = boundary;
        this.boundaryByte = boundaryByte;
        this.bufferInputStream = bufferInputStream;
    }

    /**
     * 构造函数
     *
     * @param request 请求对象
     * @throws IOException IO异常
     */
    public MultUploadInputStream(Request request) throws IOException {
        this.request = request;
        is = request.getInputStream();
        bufferInputStream = new BufferInputStream(request.getInputStream());
        readBoundary();
        boolean ready = readFileContentType();
        if (!ready) {
            throw new L500Exception("文件上传失败");
        }
    }

    /** 缓存 */
    byte[] cache = new byte[100];
    /** 读取计数 */
    int readCount = 0;
    /** 读取索引 */
    int readIndex = 0;
    /** 是否读取完成 */
    boolean readCompleted = false;

    @Override
    public int read() throws IOException {
        if (readCompleted) {
            return -1;
        }
        if (readCount > 0) {
            readCount--;
            fileSize++;
            return cache[readIndex++] & 0xFF;
        } else {
            readIndex = 0;
        }
        int read = bufferInputStream.read();
        if (read != -1) {
            readSize += read;
            if (read == '\r') {
                int r = bufferInputStream.read();
                cache[readCount++] = (byte) r;
                if (r == '\n') {
                    // 读取数据判断是不是 boundary结束标识
                    boolean flag = true;
                    for (byte b : boundaryByte) {
                        r = bufferInputStream.read();
                        if (r == -1) {
                            return r;
                        }
                        // 先缓存数据,如果不是boundary结束标识,那下次读取就可以读取缓存的数据
                        cache[readCount++] = (byte) r;
                        if (r != b) {
                            flag = false;
                            break;
                        }
                    }
                    // 读取结束标识是 -- 的话就还有文件
                    if (flag) {
                        r = bufferInputStream.read(cache, 0, 2);
                        if (r == 2) {
                            hasNext = !(cache[0] == '-' && cache[1] == '-');
                        }
                        readCompleted = true;
                        return -1;
                    }
                }
            }
        }
        fileSize++;
        return read;
    }

    /**
     * 获取文件名
     *
     * @return 文件名
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * 获取文件大小
     *
     * @return 文件大小
     */
    public long getFileSize() {
        return fileSize;
    }

    /**
     * 获取已读大小
     *
     * @return 已读大小
     */
    public long getReadSize() {
        return readSize;
    }

    /**
     * 是否还有下一个文件
     *
     * @return 是否还有下一个文件
     */
    public boolean isHasNext() {
        return hasNext;
    }

    @Override
    public void close() throws IOException {
        request.getInputStream().close();
    }

}
