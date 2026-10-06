package com.fgsqw.httpserver.exception;

/**
 * 500内部服务器错误异常类
 * 用于表示服务器内部错误
 *
 * @author fgsq
 */
public class L500Exception extends RuntimeException {
    /**
     * 构造函数
     *
     * @param message 错误消息
     */
    public L500Exception(String message) {
        super(message);
    }
}
