package com.fgsqw.httpserver.exception;

/**
 * 404未找到异常类
 * 用于表示请求的资源未找到
 *
 * @author fgsq
 */
public class L404Exception extends RuntimeException {

    /**
     * 默认构造函数
     */
    public L404Exception(){
        super("404 Not Found");
    }

    /**
     * 构造函数
     *
     * @param message 错误消息
     */
    public L404Exception(String message) {
        super(message);
    }
}
