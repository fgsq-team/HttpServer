package com.fgsqw.httpserver.stream;

import java.io.IOException;
import java.io.InputStream;

/**
 * 请求体输入流
 * 限制读取指定大小的请求体数据
 *
 * @author fgsq
 */
public class BodyInputStream extends InputStream {
    /** 剩余大小 */
    private long size;
    /** 输入流 */
    private final InputStream in;

    /**
     * 构造函数
     *
     * @param in   输入流
     * @param size 限制大小
     */
    public BodyInputStream(InputStream in, long size) {
        this.in = in;
        this.size = size;
    }

    @Override
    public int read() throws IOException {
        if (size <= 0) {
            return -1;
        }
        int read = in.read();
        if (read != -1) {
            size--;
        }
        return read;
    }

    @Override
    public int read(byte[] b) throws IOException {
        if (size <= 0) {
            return -1;
        }
        int read = in.read(b, 0, (int) Math.min(size, b.length));
        if (read != -1) {
            size -= read;
        }
        return read;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (size <= 0) {
            return -1;
        }
        int read = in.read(b, off, (int) Math.min(size, len));
        if (read != -1) {
            size -= read;
        }
        return read;
    }
}
