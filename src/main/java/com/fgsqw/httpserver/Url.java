package com.fgsqw.httpserver;

/**
 * URL类
 * 封装URL的各个组成部分，包括协议、主机、端口、路径、查询参数等
 *
 * @author fgsq
 */
public class Url {
    /** 协议（http/https） */
    public String protocol;
    /** 主机名 */
    public String host;
    /** 端口号 */
    public int port;
    /** 路径 */
    public String path;
    /** 完整URL */
    public String url;
    /** 查询参数 */
    public String query;

    /**
     * 默认构造函数
     */
    public Url() {
    }

    /**
     * 构造函数
     *
     * @param url 完整URL
     */
    public Url(String url) {
        this.url = url;
    }

    /**
     * 构造函数
     *
     * @param protocol 协议
     * @param host     主机名
     * @param port     端口号
     * @param path     路径
     * @param query    查询参数
     */
    public Url(String protocol, String host, int port, String path, String query) {
        this.protocol = protocol;
        this.host = host;
        this.port = port;
        this.path = path;
        this.query = query;
    }

    /**
     * 获取协议
     *
     * @return 协议
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * 设置协议
     *
     * @param protocol 协议
     */
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    /**
     * 获取主机名
     *
     * @return 主机名
     */
    public String getHost() {
        return host;
    }

    /**
     * 设置主机名
     *
     * @param host 主机名
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * 获取端口号
     *
     * @return 端口号
     */
    public int getPort() {
        return port;
    }

    /**
     * 设置端口号
     *
     * @param port 端口号
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * 获取路径
     *
     * @return 路径
     */
    public String getPath() {
        return path;
    }

    /**
     * 设置路径
     *
     * @param path 路径
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * 获取完整URL
     *
     * @return 完整URL
     */
    public String getUrl() {
        return url;
    }

    /**
     * 设置完整URL
     *
     * @param url 完整URL
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * 获取查询参数
     *
     * @return 查询参数
     */
    public String getQuery() {
        return query;
    }

    /**
     * 设置查询参数
     *
     * @param query 查询参数
     */
    public void setQuery(String query) {
        this.query = query;
    }

}
