package com.fgsqw.httpserver;

/**
 * 请求过滤器接口
 * 用于在请求到达处理器之前进行拦截和预处理
 *
 * @author fgsq
 */
public interface RequestFilter {
    /**
     * 过滤请求
     *
     * @param request     请求对象
     * @param response    响应对象
     * @param httpHandler 请求处理器
     * @throws Exception 过滤异常
     */
    void filter(Request request, Response response, HttpHandler httpHandler) throws Exception;
}
