package com.fgsqw.httpserver.utils;

import java.util.UUID;

/**
 * 字符串工具类
 * 提供字符串判空、UUID生成等功能
 *
 * @author fgsq
 */
public class StringUtils {
    /**
     * 判断字符串是否为空
     *
     * @param str 字符串
     * @return 是否为空
     */
    public static boolean isEmpty(Object str) {
        return str == null || "".equals(str);
    }

    /**
     * 生成UUID（去除横线）
     *
     * @return UUID字符串
     */
    public static String getUUID() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }
}
