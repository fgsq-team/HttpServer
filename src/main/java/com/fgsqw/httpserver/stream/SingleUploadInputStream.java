package com.fgsqw.httpserver.stream;

import com.fgsqw.httpserver.HttpConstant;
import com.fgsqw.httpserver.Request;
import com.fgsqw.httpserver.utils.HttpUtil;
import com.fgsqw.httpserver.utils.StringUtils;

import java.io.IOException;
import java.io.InputStream;


/**
 * 单文件上传输入流
 * 支持解析单文件上传，速度较快
 *
 * @author fgsq
 */
public class SingleUploadInputStream extends InputStream {

    /** 文件名 */
    private String fileName;
    /** 文件大小 */
    private long fileSize;
    /** 已读大小 */
    private long readSize;
    /** 输入流 */
    private final InputStream is;

    /**
     * 构造函数
     *
     * @param request 请求对象
     * @throws IOException IO异常
     */
    public SingleUploadInputStream(Request request) throws IOException {
        this.is = request.getInputStream();
        // 第一行(分割行长度)
        long firstLineLength = -1;
        // 已读请求体总长度
        long readHeadLength = 0;
        // 文件名
        String filename = null;
        boolean ready = false;
        while (true) {
            // 读取一行字节
            byte[] startFlagBytes = HttpUtil.readHttpLineByte(is);
            if (startFlagBytes == null) {
                break;
            }
            if (firstLineLength == -1) {
                // 减去尾部分割数据
                firstLineLength = startFlagBytes.length + 4;
            }
            readHeadLength += startFlagBytes.length;
            String line = new String(startFlagBytes);
            String lineUpperCase = line.toUpperCase();
            if (lineUpperCase.startsWith(HttpConstant.CONTENT_DISPOSITION.toUpperCase())) {
                try {
                    int filenameIndex = lineUpperCase.indexOf("FILENAME=\"");
                    if (filenameIndex >= 0) {
                        filename = line.substring(filenameIndex + 10);
                        filename = filename.substring(0, filename.indexOf("\""));
                    }
                } catch (Exception e) {
                    // filename 解析失败，后续会使用 UUID 作为文件名
                    filename = null;
                }
            }
            if (lineUpperCase.startsWith(HttpConstant.CONTENT_TYPE.toUpperCase())) {
                if (filename == null) {
                    filename = StringUtils.getUUID();
                }
                this.fileName = filename;
                // 精确跳过 \r\n
                is.read();
                is.read();
                readHeadLength += 2;
                // 数据长度 = 总请求体长度 -  (尾部分割行长度 + 已读请求体长度)
                fileSize = request.getContentLength() - (firstLineLength + readHeadLength);
                ready = true;
                break;
            }
        }
        if (!ready) {
            throw new RuntimeException("文件上传失败");
        }
    }

    @Override
    public int read() throws IOException {
        if (readSize >= fileSize) {
            return -1;
        }
        int read = is.read();
        if (read != -1) {
            readSize++;
        }
        return read;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (readSize >= fileSize) {
            return -1;
        }
        long remainingSize = fileSize - readSize;
        int min = (int) Math.min(len, remainingSize);
        int read = is.read(b, off, min);
        if (read != -1) {
            readSize += read;
        }
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

    @Override
    public void close() throws IOException {
        is.close();
    }
}
