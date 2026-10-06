package com.fgsqw.httpserver.utils;


import java.io.*;
import java.net.Socket;

/**
 * IO工具类
 * 提供流关闭、流传输、读写等功能
 *
 * @author fgsq
 */
public class IOUtil {

    /**
     * 关闭IO资源
     *
     * @param io 可关闭的IO对象数组
     */
    public static void closeIO(Object... io) {
        if (io != null) {
            for (Object closeable : io) {
                if (closeable != null) {
                    try {
                        if (closeable instanceof Socket) {
                            ((Socket) closeable).close();
                        } else if (closeable instanceof AutoCloseable) {
                            ((AutoCloseable) closeable).close();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * 流传输（全部）
     *
     * @param input  输入流
     * @param output 输出流
     * @return 传输的总字节数
     * @throws IOException IO异常
     */
    public static long transfer(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[81920];
        int ten = 0;
        long total = 0;
        while ((ten = input.read(buffer)) != -1) {
            output.write(buffer, 0, ten);
            total += ten;
        }
        return total;
    }

    /**
     * 流传输（指定大小）
     *
     * @param input  输入流
     * @param output 输出流
     * @param size   传输大小
     * @return 传输的总字节数
     * @throws IOException IO异常
     */
    public static long transfer(InputStream input, OutputStream output, long size) throws IOException {
        byte[] buffer = new byte[4096];
        int ten = 0;
        long total = 0;
        while ((ten = input.read(buffer, 0, (int) Math.min(4096, (size - total)))) > 0) {
            output.write(buffer, 0, (int) ten);
            total += ten;
        }
        return total;
    }


    /**
     * 读取输入流为文本
     *
     * @param inputStream 输入流
     * @return 文本内容
     */
    public static String readInputTxt(InputStream inputStream) {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * 阻塞获取数据包直到达到len时跳出
     *
     * @param is     流
     * @param buf    缓冲区
     * @param offset 写入buff偏移
     * @param len    读取大小
     * @return 总读取大小
     */
    public static int readFully(InputStream is, byte[] buf, int offset, int len) throws IOException {
        int totalRecv = 0;
        int off = offset;
        int size = len;

        while (size > 0) {
            int i = is.read(buf, off, size);
            if (i <= 0) {
                return i;
            }
            off += i;
            totalRecv += i;
            size -= i;
        }
        return totalRecv;
    }

    /**
     * 写入字节数组到输出流
     *
     * @param out 输出流
     * @param buf 字节数组
     * @throws IOException IO异常
     */
    public static void write(OutputStream out, byte[] buf) throws IOException {
        write(out, buf, buf.length);
    }

    /**
     * 写入字节数组到输出流（指定长度）
     *
     * @param out 输出流
     * @param buf 字节数组
     * @param len 写入长度
     * @throws IOException IO异常
     */
    public static void write(OutputStream out, byte[] buf, int len) throws IOException {
        write(out, buf, 0, len);
    }

    /**
     * 写入字节数组到输出流（指定偏移和长度）
     *
     * @param out    输出流
     * @param buf    字节数组
     * @param offset 偏移量
     * @param len    写入长度
     * @throws IOException IO异常
     */
    public static void write(OutputStream out, byte[] buf, int offset, int len) throws IOException {
        out.write(buf, offset, len);
        out.flush();
    }

    /**
     * 写入单个字节到输出流
     *
     * @param out 输出流
     * @param buf 字节
     * @throws IOException IO异常
     */
    public static void write(OutputStream out, int buf) throws IOException {
        out.write(buf);
        out.flush();
    }

    /**
     * 读取输入流为字节数组
     *
     * @param is 输入流
     * @return 字节数组
     * @throws IOException IO异常
     */
    public static byte[] readBytes(InputStream is) throws IOException {
        byte[] buffer = new byte[1024];
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int ten = 0;
        while ((ten = is.read(buffer)) != -1) {
            out.write(buffer, 0, ten);
        }
        return out.toByteArray();
    }


}
