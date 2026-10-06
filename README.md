# HttpServer

一个基于 Java 原生 Socket 实现的轻量级 HTTP 服务器，支持 HTTP/HTTPS 协议、WebSocket 通信、文件上传下载等功能，无需依赖任何第三方 Web 框架。

## 功能特性

- **HTTP 协议支持**：支持 GET、POST、PUT 等 HTTP 方法
- **HTTPS 支持**：内置 SSL/TLS 加密通信支持
- **WebSocket 通信**：提供完整的 WebSocket 服务端和客户端实现
- **灵活的路由系统**：支持通配符 `*`、`**` 和路径参数 `:paramName` 的路由匹配
- **文件上传**：支持单文件和多文件上传（`multipart/form-data`）
- **文件下载**：支持文件下载和断点续传（Range 请求）
- **请求过滤器**：支持全局请求过滤/拦截机制
- **线程池**：使用线程池处理并发请求
- **自定义异常**：支持 302 重定向、404、500 等 HTTP 状态码异常

## 技术栈

- **开发语言**：Java 8
- **构建工具**：Maven
- **网络通信**：原生 Java Socket 编程
- **日志框架**：SLF4J
- **无第三方 Web 框架依赖**

## 项目结构

```
src/main/java/com/fgsqw/
├── Main.java                    # 入口示例
├── HttpServer.java              # HTTP 服务器核心类
├── Request.java                 # HTTP 请求封装
├── Response.java                # HTTP 响应封装
├── HttpBase.java                # HTTP 基础类
├── HttpConstant.java            # HTTP 常量定义
├── HttpHandler.java             # 请求处理器接口
├── RequestFilter.java           # 请求过滤器接口
├── ContentTypes.java            # 内容类型映射
├── StatusEnum.java              # HTTP 状态码枚举
├── exception/                   # 异常类
│   ├── LCodeException.java      # 自定义状态码异常基类
│   ├── L302Exception.java       # 302 重定向异常
│   ├── L404Exception.java       # 404 未找到异常
│   └── L500Exception.java       # 500 服务器错误异常
├── stream/                      # 输入流处理
│   ├── BodyInputStream.java     # 请求体输入流
│   ├── SingleUploadInputStream.java  # 单文件上传流
│   └── MultUploadInputStream.java    # 多文件上传流
├── utils/                       # 工具类
│   ├── ThreadUtils.java         # 线程工具
│   ├── IOUtil.java              # IO 工具
│   ├── HttpUtil.java            # HTTP 工具
│   ├── FileUtil.java            # 文件工具
│   ├── StringUtils.java         # 字符串工具
│   ├── ByteUtil.java            # 字节工具
│   └── BufferInputStream.java   # 缓冲输入流
└── websocket/                   # WebSocket 模块
    ├── WebSocketServer.java     # WebSocket 服务端
    ├── WebSocketClient.java     # WebSocket 客户端
    └── WebSocketUtil.java       # WebSocket 工具类
```

## 快速开始

### 环境要求

- JDK 8+
- Maven 3.x

### 构建项目

```bash
mvn clean package
```

### 基本用法

```java
import com.fgsqw.httpserver.HttpServer;

public class Main {
    public static void main(String[] args) throws Exception {
        // 创建服务器，指定端口
        HttpServer server = new HttpServer(8080);

        // 注册路由处理器
        server.addPath("/", (request, response) -> {
            response.writeString("Hello, World!");
        });

        // 启动服务
        server.start();
    }
}
```

### 路由示例

```java
HttpServer server = new HttpServer(8080);

// 精确匹配
server.addPath("/api/users", (request, response) -> {
    response.writeString("用户列表");
});

// 通配符匹配（单级路径）
server.addPath("/api/users/*", (request, response) -> {
    response.writeString("用户详情");
});

// 通配符匹配（多级路径）
server.addPath("/api/**", (request, response) -> {
    response.writeString("API 接口");
});

// 指定 HTTP 方法
server.addPath("/api/data", "POST", (request, response) -> {
    String body = request.getRequestBody();
    response.writeString("收到数据: " + body);
});

server.start();
```

### 请求参数

```java
server.addPath("/api/search", (request, response) -> {
    // 获取 URL 查询参数
    String keyword = request.getQueryParam("keyword");
    String page = request.getQueryParam("page");
    
    response.writeString("搜索: " + keyword + ", 页码: " + page);
});
```

### 响应数据

```java
server.addPath("/api/data", (request, response) -> {
    // 返回 JSON
    response.writeString("{\"status\":\"ok\"}");
    
    // 返回字节数组
    byte[] data = ...;
    response.writeBytes(data, "application/octet-stream");
    
    // 返回文件
    File file = new File("/path/to/file.pdf");
    response.writeFile(file);
    
    // 返回流
    InputStream is = ...;
    response.writeStream(is, "application/pdf");
});
```

### 文件上传

```java
server.addPath("/upload", "POST", (request, response) -> {
    // 单文件上传
    Request.UploadResult result = request.transferUploadFile("/upload/dir");
    System.out.println("文件名: " + result.getFileName());
    System.out.println("大小: " + result.getFileSize());
    
    // 多文件上传
    MultUploadInputStream multStream = request.getMultUploadInputStream();
    while (multStream != null) {
        String fileName = multStream.getFileName();
        // 读取文件内容...
        if (multStream.isHasNext()) {
            multStream = multStream.next();
        } else {
            break;
        }
    }
    
    response.writeString("上传成功");
});
```

### HTTPS 配置

```java
HttpServer server = new HttpServer(443);

// 启用 SSL（需要 PKCS12 格式的证书）
server.setOpenSSL("/path/to/cert.p12", "password");

server.addPath("/", (request, response) -> {
    response.writeString("HTTPS 安全连接");
});

server.start();
```

### 请求过滤器

```java
HttpServer server = new HttpServer(8080);

// 设置全局请求过滤器
server.setRequestFilter((request, response, handler) -> {
    // 前置处理：如鉴权、日志记录等
    String token = request.getHeaderValue("Authorization");
    if (token == null) {
        response.writeErrorCode(401, "Unauthorized");
        return;
    }
    // 继续执行处理器
    handler.handle(request, response);
});

server.start();
```

### WebSocket 服务端

```java
// WebSocket 握手
WebSocketServer wsServer = new WebSocketServer(secWebSocketKey, socket);

// 发送消息
wsServer.sendString("Hello WebSocket");

// 接收消息
String message = wsServer.readString();

// 关闭连接
wsServer.close();
```

### WebSocket 客户端

```java
// 连接 WebSocket 服务器
WebSocketClient client = new WebSocketClient("ws://localhost:8080/ws");

// 发送消息
client.sendTextMessage("Hello Server");

// 接收消息
String response = client.readString();

// 关闭连接
client.close();
```

## 自定义异常

```java
// 302 重定向
throw new L302Exception("重定向到登录页", "/login");

// 404 未找到
throw new L404Exception("页面不存在");

// 500 服务器错误
throw new L500Exception("数据库连接失败");

// 自定义状态码
throw new LCodeException(403, "禁止访问");
```

## 许可证

Copyright 2026 fgsqme

Licensed under the Apache License, Version 2.0
