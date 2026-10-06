
package com.fgsqw.httpserver.websocket;

import com.fgsqw.httpserver.utils.ByteUtil;
import com.fgsqw.httpserver.utils.IOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;

/**
 * WebSocket工具类
 * 处理通用的帧编码解码、掩码处理等逻辑
 *
 * @author fgsq
 */
public class WebSocketUtil {
    /** 日志 */
    private static final Logger logger = LoggerFactory.getLogger(WebSocketUtil.class);

    /** WebSocket完成标志 */
    public static final int WS_FIN = 128;
    /** 操作码：续传 */
    public static final int OPCODE_CONTINUATION = 0;
    /** 操作码：文本 */
    public static final int OPCODE_TEXT = 1;
    /** 操作码：二进制 */
    public static final int OPCODE_BINARY = 2;
    /** 操作码：关闭 */
    public static final int OPCODE_CLOSE = 8;
    /** 操作码：ping */
    public static final int OPCODE_PING = 9;
    /** 操作码：pong */
    public static final int OPCODE_PONG = 10;

    /**
     * 读取WebSocket帧数据
     *
     * @param in       输入流
     * @param isClient true表示客户端，false表示服务端
     * @return 解码后的数据字节数组和操作码的组合对象
     * @throws IOException 如果发生I/O错误
     */
    public static FrameData readFrame(InputStream in, boolean isClient) throws IOException {
        if (isClient) {
            return readClientFrame(in);
        } else {
            return readServerFrame(in);
        }
    }

    /**
     * 读取客户端的WebSocket帧数据
     *
     * @param in 输入流
     * @return 解码后的数据字节数组和操作码的组合对象
     * @throws IOException 如果发生I/O错误
     */
    public static FrameData readClientFrame(InputStream in) throws IOException {
        int curByte = in.read();
        if (curByte == -1) {
            throw new IOException("Socket is closed");
        }
        int opcode = curByte & 0xF;
        // 检查是否是关闭帧
        if (opcode == OPCODE_CLOSE) {
            return new FrameData(null, opcode);
        }
        // 检查接收错误
        if ((curByte & 0x70) > 0) {
            return null;
        }
        int mask = in.read();
        long frameLength = getFrameLength(mask, in);
        if (frameLength < 0) {
            return null;
        }
        if (isControlFrame(opcode) && (frameLength > 125)) {
            return null;
        }
        if (opcode == OPCODE_TEXT || opcode == OPCODE_BINARY) {
            byte[] data = new byte[(int) frameLength];
            if (IOUtil.readFully(in, data, 0, (int) frameLength) != frameLength) {
                return null;
            }
            return new FrameData(data, opcode);
        } else if (opcode == OPCODE_PING || opcode == OPCODE_PONG) {
            logger.info("ping/pong received");
        }
        return null;
    }

    /**
     * 读取服务端的WebSocket帧数据
     *
     * @param in 输入流
     * @return 解码后的数据字节数组和操作码的组合对象
     * @throws IOException 如果发生I/O错误
     */
    public static FrameData readServerFrame(InputStream in) throws IOException {
        int curByte = in.read();
        if (curByte == -1) {
            throw new IOException("Socket is closed");
        }
        int opcode = curByte & 0xF;
        // 检查是否是关闭帧
        if (opcode == OPCODE_CLOSE) {
            return new FrameData(null, opcode);
        }
        // 检查接收错误（RSV1-3位不为0）
        if ((curByte & 0x70) > 0) {
            return null;
        }
        // 第二个字节：bit7=mask标志，bit0-6=payload长度
        int maskByte = in.read();
        if (maskByte == -1) {
            throw new IOException("Socket is closed");
        }
        boolean hasMask = (maskByte & 0x80) != 0;  // 从第二个字节读取mask标志
        long frameLength = getFrameLength(maskByte & 0x7F, in);
        if (frameLength < 0) {
            return null;
        }
        if (isControlFrame(opcode) && (frameLength > 125)) {
            return null;
        }
        if (opcode == OPCODE_TEXT || opcode == OPCODE_BINARY) {
            byte[] masks = new byte[4];
            // 服务端必须读取mask key
            if (hasMask) {
                if (IOUtil.readFully(in, masks, 0, 4) != 4) {
                    return null;
                }
            }
            byte[] data = new byte[(int) frameLength];
            if (IOUtil.readFully(in, data, 0, (int) frameLength) != frameLength) {
                return null;
            }
            // 如果有掩码，则进行解掩码
            if (hasMask) {
                for (int i = 0; i < data.length; i++) {
                    data[i] = (byte) (data[i] ^ masks[i & 0x3]);
                }
            }
            return new FrameData(data, opcode);
        } else if (opcode == OPCODE_PING || opcode == OPCODE_PONG) {
            logger.info("ping/pong received");
        }
        return null;
    }

    /**
     * 获取WebSocket帧的数据长度
     *
     * @param mask 初始掩码值
     * @param in   输入流
     * @return 数据长度
     * @throws IOException 如果发生I/O错误
     */
    public static long getFrameLength(int mask, InputStream in) throws IOException {
        long frameLength = mask & 0x7F;
        if (frameLength == 126) {
            byte[] buf = new byte[2];
            if (IOUtil.readFully(in, buf, 0, 2) != 2) {
                return -1;
            }
            frameLength = ByteUtil.bytesToShort(buf);
        } else if (frameLength == 127) {
            byte[] buf = new byte[8];
            if (IOUtil.readFully(in, buf, 0, 8) != 8) {
                return -1;
            }
            frameLength = ByteUtil.bytesToLong(buf);
        }
        return frameLength;
    }

    /**
     * 发送指定操作码的帧数据（服务器端使用）
     *
     * @param out    输出流
     * @param data   要发送的数据
     * @param opCode 操作码
     * @throws IOException 如果发生I/O错误
     */
    public static void sendServerFrame(OutputStream out, byte[] data, int opCode) throws IOException {
        if (data == null) {
            throw new IllegalArgumentException("Data cannot be null");
        }
        int len = data.length;
        try {
            // 写入帧头
            out.write(WS_FIN | opCode);
            // 写入长度和可能的扩展数据
            if (len < 126) {
                out.write(len);
            } else if (len <= 65535) {
                out.write(126);
                out.write(ByteUtil.shortToBytes((short) len));
            } else {
                out.write(127);
                out.write(ByteUtil.longToBytes(len));
            }
            // 直接写入数据（服务器端不需要掩码）
            out.write(data);
            out.flush();
        } catch (IOException e) {
            throw new IOException("Error sending WebSocket frame: " + e.getMessage(), e);
        }
    }

    /**
     * 发送指定操作码的帧数据（客户端使用）
     *
     * @param out    输出流
     * @param data   要发送的数据
     * @param opCode 操作码
     * @throws IOException 如果发生I/O错误
     */
    public static void sendClientFrame(OutputStream out, byte[] data, int opCode) throws IOException {
        if (data == null) {
            throw new IllegalArgumentException("Data cannot be null");
        }
        int len = data.length;
        byte[] mask = generateMask();
        try {
            // 写入帧头
            out.write(WS_FIN | opCode);
            // 写入长度和可能的扩展数据，并设置掩码标志
            if (len < 126) {
                out.write(len | 0x80); // 设置掩码标志
                out.write(mask);
            } else if (len <= 65535) {
                out.write(126 | 0x80); // 设置掩码标志
                out.write(ByteUtil.shortToBytes((short) len));
                out.write(mask);
            } else {
                out.write(127 | 0x80); // 设置掩码标志
                out.write(ByteUtil.longToBytes(len));
                out.write(mask);
            }
            // 对数据进行掩码处理并发送
            byte[] maskedData = new byte[len];
            for (int i = 0; i < len; i++) {
                maskedData[i] = (byte) (data[i] ^ mask[i % 4]);
            }
            out.write(maskedData);
            out.flush();
        } catch (IOException e) {
            throw new IOException("Error sending WebSocket frame: " + e.getMessage(), e);
        }
    }

    /**
     * 生成随机的4字节掩码键
     *
     * @return 4字节的随机掩码
     */
    public static byte[] generateMask() {
        byte[] mask = new byte[4];
        // 使用安全的随机数生成
        new Random().nextBytes(mask);
        return mask;
    }

    /**
     * 检查是否是控制帧
     *
     * @param frameType 帧类型
     * @return 如果是控制帧则返回true，否则false
     */
    public static boolean isControlFrame(int frameType) {
        return frameType == OPCODE_CLOSE || frameType == OPCODE_PING || frameType == OPCODE_PONG;
    }

    /**
     * WebSocket帧数据内部类
     * 表示一个WebSocket帧的数据和元信息
     */
    public static class FrameData {
        /** 帧数据 */
        private final byte[] data;
        /** 操作码 */
        private final int opcode;

        /**
         * 构造函数
         *
         * @param data   帧数据
         * @param opcode 操作码
         */
        public FrameData(byte[] data, int opcode) {
            this.data = data;
            this.opcode = opcode;
        }

        /**
         * 获取帧数据
         *
         * @return 帧数据
         */
        public byte[] getData() {
            return data;
        }

        /**
         * 获取操作码
         *
         * @return 操作码
         */
        public int getOpcode() {
            return opcode;
        }
    }
}
