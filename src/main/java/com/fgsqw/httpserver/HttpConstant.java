package com.fgsqw.httpserver;

/**
 * HTTP常量类
 * 定义了HTTP协议中常用的常量值
 *
 * @author fgsq
 */
public class HttpConstant {
    /** 内容长度 */
    public static final String CONTENT_LENGTH = "Content-Length";
    /** 内容类型 */
    public static final String CONTENT_TYPE = "Content-Type";
    /** 接受范围请求 */
    public static final String ACCEPT_RANGES = "Accept-Ranges";
    /** 内容范围 */
    public static final String CONTENT_RANGES = "Content-Range";
    /** 连接方式 */
    public static final String CONNECTION = "Connection";
    /** 接受编码 */
    public static final String ACCEPT_ENCODING = "accept-encoding";
    /** 传输编码 */
    public static final String TRANSFER_ENCODING = "transfer-encoding";
    /** 保持连接 */
    public static final String KEEP_ALIVE = "keep-alive";
    /** 来源页面 */
    public static final String REFERER = "referer";
    /** 协议分隔符 */
    public static final String PROTOCOL_SPILT = "://";
    /** 关闭连接 */
    public static final String CLOSE = "close";
    /** 内容处置 */
    public static final String CONTENT_DISPOSITION = "Content-Disposition";
    /** 真实IP头 */
    public static final String X_REAL_I_P = "X-Real-IP";
    /** 转发IP头 */
    public static final String X_FORWARDED_FOR = "X-Forwarded-For";
    /** POST方法 */
    public static final String METHOD_POST = "POST";
    /** GET方法 */
    public static final String METHOD_GET = "GET";
    /** PUT方法 */
    public static final String METHOD_PUT = "GET";
    /** DELETE方法 */
    public static final String METHOD_REMOVE = "GET";
    /** 未知 */
    public static final String UNKNOWN = "unknown";
    /** HTTP版本 */
    public static final String HTTP_VERSION = "HTTP/1.1";
    /** HTML内容类型 */
    public static final String HTML_CONTEXT_TYPE = "text/html; charset-utf-8";
    /** 文本内容类型 */
    public static final String TEXT_CONTEXT_TYPE = "text/plain; charset=UTF-8";
    /** 流内容类型 */
    public static final String STREAM_CONTEXT_TYPE = "application/octet-stream";
    /** 图片内容类型 */
    public static final String STREAM_CONTEXT_IMAGE = "image/png";
    /** JSON内容类型 */
    public static final String STREAM_CONTEXT_JSON = "application/json";
    /** WebSocket魔术字符串 */
    public static final String WS_MAGIC = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
    /** 回车换行 */
    public static final String CRLF = "\r\n";

}
