package com.fgsqw.httpserver;

/**
 * HTTP状态码枚举类
 * 定义了常用的HTTP响应状态码及其对应的消息
 *
 * @author fgsq
 */
public enum StatusEnum {
    /** 200 请求成功 */
    STATUS_200_OK(200, "OK"),
    /** 301 资源已永久移动到新位置 */
    STATUS_301_MOVED_PERMANENTLY(301, "Moved Permanently"),
    /** 302 资源临时移动 */
    STATUS_302_FOUND(302, "Found"),
    /** 404 资源未找到 */
    STATUS_404_NOT_FOUND(404, "Not Found"),
    /** 405 请求方法不允许使用 */
    STATUS_405_METHOD_NOT_ALLOWED(405, "Method Not Allowed"),
    /** 500 服务器内部错误 */
    STATUS_500_INTERNAL_SERVER_ERROR(500, "Internal Server Error"),
    /** 204 无内容 */
    STATUS_204_NOT_CONTENT(204, "No Content");
    /** 状态码 */
    private final int status;
    /** 状态消息 */
    private final String statusMessage;

    /**
     * 构造函数
     *
     * @param status        状态码
     * @param statusMessage 状态消息
     */
    StatusEnum(int status, String statusMessage) {
        this.status = status;
        this.statusMessage = statusMessage;
    }

    /**
     * 获取状态码
     *
     * @return 状态码
     */
    public int getStatus() {
        return status;
    }

    /**
     * 获取状态消息
     *
     * @return 状态消息
     */
    public String getStatusMessage() {
        return statusMessage;
    }
}
