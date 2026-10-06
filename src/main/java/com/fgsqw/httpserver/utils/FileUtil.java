package com.fgsqw.httpserver.utils;


import java.io.*;
import java.nio.charset.Charset;
import java.text.DecimalFormat;

/**
 * 文件工具类
 * 提供文件操作、文件大小转换、文件复制等功能
 *
 * @author fgsq
 */
public class FileUtil {

    /** US-ASCII 字符集 */
    public static final Charset US_ASCII = Charset.forName("US-ASCII");
    /**
     * ISO Latin Alphabet No. 1, a.k.a. ISO-LATIN-1
     */
    public static final Charset ISO_8859_1 = Charset.forName("ISO-8859-1");
    /**
     * Eight-bit UCS Transformation Format
     */
    public static final Charset UTF_8 = Charset.forName("UTF-8");
    /**
     * Sixteen-bit UCS Transformation Format, big-endian byte order
     */
    public static final Charset UTF_16BE = Charset.forName("UTF-16BE");
    /**
     * Sixteen-bit UCS Transformation Format, little-endian byte order
     */
    public static final Charset UTF_16LE = Charset.forName("UTF-16LE");
    /**
     * Sixteen-bit UCS Transformation Format, byte order identified by an
     * optional byte-order mark
     */
    public static final Charset UTF_16 = Charset.forName("UTF-16");

    /** 默认文件类型 */
    public static final String DEFAULT_TYPE = "*/*";

    /**
     * long格式转换文件大小
     *
     * @param size 文件大小（字节）
     * @return 格式化后的文件大小字符串
     */
    public static String computeSize(long size) {
        StringBuffer bytes = new StringBuffer();
        DecimalFormat format = new DecimalFormat("###.0");
        if (size >= 1024 * 1024 * 1024) {
            double i = (size / (1024.0 * 1024.0 * 1024.0));
            bytes.append(format.format(i)).append("GB");
        } else if (size >= 1024 * 1024) {
            double i = (size / (1024.0 * 1024.0));
            bytes.append(format.format(i)).append("MB");
        } else if (size >= 1024) {
            double i = (size / (1024.0));
            bytes.append(format.format(i)).append("KB");
        } else if (size < 1024) {
            if (size <= 0) {
                bytes.append("0B");
            } else {
                bytes.append((int) size).append("B");
            }
        }
        return bytes.toString();
    }


    /**
     * 创建空文件
     *
     * @param path 文件路径
     */
    public static void createNullFile(String path) {
        try {
            OutputStream fileOut = new FileOutputStream(path);
            fileOut.write(0);
            fileOut.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 复制文件
     *
     * @param srcPath 源文件路径
     * @param outPath 目标文件路径
     * @throws IOException IO异常
     */
    public static void copyFile(String srcPath, String outPath) throws IOException {
        File outFile = new File(outPath);
        File parentFile = outFile.getParentFile();
        if (!parentFile.exists()) {
            parentFile.mkdirs();
        }
        InputStream input = new FileInputStream(srcPath);
        OutputStream out = new FileOutputStream(outFile);
        byte[] buffer = new byte[2048];
        int ten = 0;
        while ((ten = input.read(buffer)) != -1) {
            out.write(buffer, 0, ten);
        }
        IOUtil.closeIO(out, input);
    }

    /**
     * 写入字符串到文件（追加模式）
     *
     * @param str  字符串内容
     * @param path 文件路径
     * @return 是否写入成功
     */
    public static boolean writeString(String str, String path) {
        return writeString(str, path, true);
    }

    /**
     * 写入字符串到文件
     *
     * @param str    字符串内容
     * @param path   文件路径
     * @param append 是否追加
     * @return 是否写入成功
     */
    public static boolean writeString(String str, String path, boolean append) {
        try {
            OutputStream out = new FileOutputStream(path, append);
            out.write(str.getBytes(FileUtil.UTF_8));
            out.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 防止重名文件被覆盖
     *
     * @param outFile 输出文件
     * @return 处理后的文件
     */
    public static File avoidDuplication(File outFile) {
        String name = outFile.getName();
        if (outFile.exists()) {
            for (int s = 1; s < 65535; s++) {
                String str;
                if (name.contains(".")) {
                    String prefix = name.substring(0, name.lastIndexOf(".")) + "(" + s + ")";
                    String suffix = name.substring(name.lastIndexOf("."));
                    str = prefix + suffix;
                } else {
                    str = name + "(" + s + ")";
                }
                outFile = new File(outFile.getParentFile(), str);
                if (!outFile.exists()) {
                    return outFile;
                }
            }
        }
        return outFile;
    }

    /**
     * 转换为Android文档URI
     *
     * @param path 文件路径
     * @return URI字符串
     */
    public static String changeToUri(String path) {
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        String path2 = path.replace("/storage/emulated/0/", "").replace("/", "%2F");
        return "content://com.android.externalstorage.documents/tree/primary%3AAndroid%2Fdata/document/primary%3A" + path2;
    }

    /**
     * 删除文件（支持递归删除目录）
     *
     * @param file 要删除的文件
     * @return 是否删除成功
     */
    public static boolean deleteFile(File file) {
        if (file.exists()) {
            if (file.isFile()) {
                if (!file.delete()) {
                    return false;
                }
            } else if (file.isDirectory()) {
                File files[] = file.listFiles();
                for (File f : files) {
                    if (!deleteFile(f)) {
                        return false;
                    }
                }
                if (!file.delete()) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 获取父路径
     *
     * @param path 路径
     * @return 父路径
     */
    public static String getParentPath(String path) {
        if (!StringUtils.isEmpty(path)) {
            String s = path;
            int i = s.lastIndexOf("/");
            if (i > -1) {
                int length = s.length() - 1;
                if (i == length) {
                    s = s.substring(0, length);
                    i = s.lastIndexOf("/");
                }
                return s.substring(0, i);

            } else {
                return path;
            }
        }
        return null;
    }
}
