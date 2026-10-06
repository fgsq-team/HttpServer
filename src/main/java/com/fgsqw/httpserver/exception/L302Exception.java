package com.fgsqw.httpserver.exception;

/**
 * 302临时重定向异常类
 * 用于表示资源临时移动到其他位置
 *
 * @author fgsq
 */
public class L302Exception extends RuntimeException {
    /** 重定向URL */
    private String url = "/";

    /**
     * 构造函数
     *
     * @param message 错误消息
     */
    public L302Exception(String message) {
        super(message);
    }

    /**
     * 构造函数
     *
     * @param message 错误消息
     * @param url     重定向URL
     */
    public L302Exception(String message, String url) {
        super(message);
        this.url = url;
    }

    /**
     * 获取重定向URL
     *
     * @return 重定向URL
     */
    public String getUrl() {
        return url;
    }
}
