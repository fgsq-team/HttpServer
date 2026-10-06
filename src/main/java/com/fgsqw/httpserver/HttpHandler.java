package com.fgsqw.httpserver;

/**
 * HTTP请求处理器接口
 * 用于处理匹配到的HTTP请求
 *
 * @author fgsq
 */
public interface HttpHandler {
    /**
     * 处理HTTP请求
     *
     * @param request  请求对象
     * @param response 响应对象
     * @throws Exception 处理异常
     */
    void handle(Request request, Response response) throws Exception;
}
