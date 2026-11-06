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
            if (StringUtils.isEmpty(line)) {
                throw new L500Exception("文件上传失败");
            }
            String lineUpperCase = line.toUpperCase();
            if (lineUpperCase.startsWith(HttpConstant.CONTENT_DISPOSITION.toUpperCase())) {
                try {
                    int filenameIndex = lineUpperCase.indexOf("FILENAME=\"");
                    if (filenameIndex >= 0) {
                        fileName = line.substring(filenameIndex + 10);
                        fileName = fileName.substring(0, fileName.indexOf("\""));
                    }
                } catch (Exception e) {
                    // filename 解析失败，后续会使用 UUID 作为文件名
                    fileName = null;
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

    /** 缓存：用于 boundary 匹配失败时暂存已读字节 */
    byte[] cache = new byte[256];
    /** 缓存中有效字节数 */
    int readCount = 0;
    /** 缓存读取索引 */
    int readIndex = 0;
    /** 是否读取完成（已检测到 boundary） */
    boolean readCompleted = false;

    @Override
    public int read() throws IOException {
        if (readCompleted) {
            return -1;
        }
        // 优先从缓存读取（boundary 匹配失败后回退的数据）
        if (readCount > 0) {
            readCount--;
            readSize++;
            fileSize++;
            return cache[readIndex++] & 0xFF;
        }
        readIndex = 0;

        int read = bufferInputStream.read();
        if (read == -1) {
            readCompleted = true;
            return -1;
        }
        readSize++;

        if (read == '\r') {
            int r = bufferInputStream.read();
            if (r == -1) {
                // 流结束，返回 \r
                readCompleted = true;
                return '\r';
            }
            readSize++;

            if (r != '\n') {
                // \r 后不是 \n，\r 作为数据返回，下一个字节缓存
                cache[0] = (byte) r;
                readCount = 1;
                readIndex = 0;
                fileSize++;
                return '\r';
            }

            // \r\n 匹配成功，尝试匹配 boundary
            cache[0] = (byte) '\r';
            cache[1] = (byte) '\n';
            readCount = 2;

            boolean flag = true;
            for (byte b : boundaryByte) {
                r = bufferInputStream.read();
                if (r == -1) {
                    // 流意外结束，缓存中的数据作为文件数据返回
                    readCompleted = true;
                    return cache[readIndex++] & 0xFF;
                }
                readSize++;
                // 防止缓存溢出，动态扩容
                if (readCount >= cache.length) {
                    byte[] newCache = new byte[cache.length * 2];
                    System.arraycopy(cache, 0, newCache, 0, cache.length);
                    cache = newCache;
                }
                cache[readCount++] = (byte) r;
                if (r != (b & 0xFF)) {
                    flag = false;
                    break;
                }
            }

            if (flag) {
                // boundary 完全匹配，读取结束标识 --
                int b1 = bufferInputStream.read();
                int b2 = bufferInputStream.read();
                if (b1 != -1 && b2 != -1) {
                    hasNext = !(b1 == '-' && b2 == '-');
                }
                readCompleted = true;
                return -1;
            }
            // boundary 匹配失败，cache 中的 \r\n + 已读字节作为文件数据返回
            // 下次调用 read() 时会优先从 cache 读取
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
        bufferInputStream.close();
    }

}
