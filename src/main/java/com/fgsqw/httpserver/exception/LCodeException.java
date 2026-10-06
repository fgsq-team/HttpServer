package com.fgsqw.httpserver.exception;

/**
 * HTTP状态码异常类
 * 用于表示自定义HTTP状态码的错误
 *
 * @author fgsq
 */
public class LCodeException extends RuntimeException {

    /** HTTP状态码 */
    private final int status;

    /**
     * 构造函数
     *
     * @param status  HTTP状态码
     * @param message 错误消息
     */
    public LCodeException(int status, String message) {
        super(message);
        this.status = status;
    }

    /**
     * 获取HTTP状态码
     *
     * @return HTTP状态码
     */
    public int getStatus() {
        return status;
    }

}
