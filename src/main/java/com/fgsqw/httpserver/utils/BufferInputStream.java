package com.fgsqw.httpserver.utils;

import java.io.IOException;
import java.io.InputStream;

/**
 * 缓存输入流
 * 可以加快流动态操作速度，通过内部缓冲区减少实际读取次数
 *
 * @author fgsq
 */
public class BufferInputStream extends InputStream {

    /** 原始输入流 */
    private final InputStream is;
    /** 缓冲区总大小 */
    private int total = 0;
    /** 当前读取索引 */
    private int index = 0;
    /** 缓冲区大小 */
    private int bufferSize = 1024 * 1024;
    /** 缓冲区 */
    private final byte[] buffer;

    /**
     * 构造函数
     *
     * @param is 输入流
     */
    public BufferInputStream(InputStream is) {
        this.is = is;
        buffer = new byte[bufferSize];
    }

    /**
     * 构造函数
     *
     * @param is         输入流
     * @param bufferSize 缓冲区大小
     */
    public BufferInputStream(InputStream is, int bufferSize) {
        this(is, new byte[bufferSize], bufferSize);
    }

    /**
     * 构造函数
     *
     * @param is         输入流
     * @param buffer     缓冲区
     * @param bufferSize 缓冲区大小
     */
    public BufferInputStream(InputStream is, byte[] buffer, int bufferSize) {
        this.is = is;
        this.bufferSize = bufferSize;
        this.buffer = buffer;
    }

    /**
     * 构造函数
     *
     * @param is     输入流
     * @param buffer 缓冲区
     */
    public BufferInputStream(InputStream is, byte[] buffer) {
        this(is, buffer, buffer.length);
    }

    @Override
    public int read() throws IOException {
        if (total <= 0) {
            total = is.read(buffer, 0, bufferSize);
            if (total == -1) {
                return -1;
            }
            index = 0;
        }
        total--;
        return buffer[index++] & 0xFF;
    }

    @Override
    public void close() throws IOException {
        is.close();
    }
}
