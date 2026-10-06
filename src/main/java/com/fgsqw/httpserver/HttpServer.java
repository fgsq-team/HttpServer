package com.fgsqw.httpserver;

import com.fgsqw.httpserver.exception.L302Exception;
import com.fgsqw.httpserver.exception.L404Exception;
import com.fgsqw.httpserver.exception.L500Exception;
import com.fgsqw.httpserver.exception.LCodeException;
import com.fgsqw.httpserver.utils.HttpUtil;
import com.fgsqw.httpserver.utils.IOUtil;
import com.fgsqw.httpserver.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 轻量 HttpServer 服务
 * 支持HTTP/HTTPS协议，提供路由管理、请求处理等功能
 *
 * @author fgsq
 */
public class HttpServer {

    /**
     * 日志
     */
    private static final Logger logger = LoggerFactory.getLogger(HttpServer.class);

    /** 服务器端口号 */
    private int serverPort = 8080;
    /** 服务器运行状态标志 */
    private boolean isServerRunning = false;
    /** 是否启用SSL */
    private boolean isOpenSSL = false;
    /** SSL密钥文件路径 */
    private String sslKeyPath = null;
    /** SSL密钥密码 */
    private String sslPassword = null;
    /** 服务器Socket */
    private ServerSocket serverSocket;
    /** 请求过滤器 */
    private RequestFilter requestFilter;
    /** 线程池执行器 */
    private final ExecutorService threadPoolExecutor;
    /** 是否为内部创建的线程池 */
    private final boolean isInternalThreadPool;
    /** Socket超时时间（毫秒），默认30秒 */
    private int socketTimeout = 30000;

    // 请求路径列表
    private final ArrayList<PathEntry> requestHandlers = new ArrayList<>();

    /**
     * 默认构造函数，创建20个线程的线程池
     */
    public HttpServer() {
        threadPoolExecutor = Executors.newFixedThreadPool(20);
        isInternalThreadPool = true;
    }

    /**
     * 构造函数
     *
     * @param serverPort 服务器端口号
     */
    public HttpServer(int serverPort) {
        this();
        this.serverPort = serverPort;
    }

    /**
     * 构造函数
     *
     * @param serverPort         服务器端口号
     * @param threadPoolExecutor 线程池执行器
     */
    public HttpServer(int serverPort, ExecutorService threadPoolExecutor) {
        this.serverPort = serverPort;
        this.threadPoolExecutor = threadPoolExecutor;
        this.isInternalThreadPool = false;
    }

    /**
     * 构造函数
     *
     * @param threadPoolExecutor 线程池执行器
     */
    public HttpServer(ExecutorService threadPoolExecutor) {
        this.threadPoolExecutor = threadPoolExecutor;
        this.isInternalThreadPool = false;
    }

    /**
     * 设置Socket超时时间
     *
     * @param socketTimeout 超时时间（毫秒）
     */
    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    /**
     * 设置请求过滤器
     *
     * @param requestFilter 请求过滤器
     */
    public void setRequestFilter(RequestFilter requestFilter) {
        this.requestFilter = requestFilter;
    }

    /**
     * 添加路由处理器
     *
     * @param routePattern   路由模式
     * @param requestHandler 请求处理器
     */
    public void addPath(String routePattern, HttpHandler requestHandler) {
        requestHandlers.add(new PathEntry(routePattern, requestHandler));
    }

    /**
     * 添加路由处理器（指定HTTP方法）
     *
     * @param routePattern   路由模式
     * @param httpMethod     HTTP方法（GET/POST等）
     * @param requestHandler 请求处理器
     */
    public void addPath(String routePattern, String httpMethod, HttpHandler requestHandler) {
        requestHandlers.add(new PathEntry(routePattern, httpMethod, requestHandler));
    }

    /**
     * 启用SSL
     *
     * @param sslKeyPath  SSL密钥文件路径
     * @param sslPassword SSL密钥密码
     */
    public void setOpenSSL(String sslKeyPath, String sslPassword) {
        this.sslKeyPath = sslKeyPath;
        this.sslPassword = sslPassword;
        this.isOpenSSL = true;
    }

    /**
     * @author fgsq
     * @comments 排序路由模式
     * @date 2024/4/27 15:07
     */
    private void sortRoutePatterns() {
//        requestHandlers.sort((o1, o2) -> (o2.getRoutePattern().length() - o1.getRoutePattern().length()));
        Collections.sort(requestHandlers, (o1, o2) -> (o2.getRoutePattern().length() - o1.getRoutePattern().length()));
    }

    /**
     * @author fgsq
     * @comments 新的 Web 客户端连接
     * @date 2024/4/27 15:06
     */
    public void handleWebClient(Socket clientSocket, String initialDataPrefix) {
        Request request = null;
        InputStream clientInputStream = null;
        OutputStream clientOutputStream = null;
        try {
            clientInputStream = clientSocket.getInputStream();
            clientOutputStream = clientSocket.getOutputStream();
            request = new Request(clientSocket);
            Response response = new Response(clientSocket);
            // 解析请求
            request = HttpUtil.parseRequestFromStream(clientInputStream, clientSocket, initialDataPrefix);
            if (request == null) {
                response.write500();
                return;
            }
            // 请求解析完毕
            boolean pathMatched = false;
            response.setRangeLength(request.getRangeLength());
            // 请求路径匹配
            for (PathEntry pathEntry : requestHandlers) {
                String routePattern = pathEntry.getRoutePattern();
                pathMatched = pathMatches(routePattern, request.getRequestPath());
                if (pathMatched) {
                    if (!StringUtils.isEmpty(pathEntry.getHttpMethod()) &&
                            !request.getRequestMethod().equalsIgnoreCase(pathEntry.getHttpMethod())) {
                        response.write405();
                        return;
                    }
                    HttpHandler requestHandler = pathEntry.getRequestHandler();
                    if (requestHandler != null) {
                        try {
                            if (requestFilter != null) {
                                requestFilter.filter(request, response, requestHandler);
                            } else {
                                requestHandler.handle(request, response);
                            }
                            if (!response.isResponded()) {
                                response.write204();
                            }
                        } catch (L500Exception e) {
                            response.write500(e.getMessage());
                            return;
                        } catch (L404Exception e) {
                            response.write404(e.getMessage());
                            return;
                        } catch (L302Exception e) {
                            response.write302(e.getMessage(), e.getUrl());
                            return;
                        } catch (LCodeException e) {
                            response.writeErrorCode(e.getStatus(), e.getMessage());
                            return;
                        } catch (SocketException e) {
                            return;
                        } catch (Exception e) {
                            logger.error("异常 ", e);
                            response.write500();
                            return;
                        }
                    }
                    break;
                }
            }
            // 请求路径找不到直接返回404
            if (!pathMatched) {
                logger.info("404错误，地址为：{} IP为：{}", request.getRequestPath(), request.getRealClientIP());
                response.write404();
            }
        } catch (Exception e) {
            logger.error("异常 ", e);
        } finally {
            gracefulClose(clientSocket);
        }
    }

    /**
     * 创建SSL ServerSocket
     *
     * @param port        端口号
     * @param keyP12Path  P12密钥文件路径
     * @param password    密钥密码
     * @return ServerSocket
     * @throws Exception 异常
     */
    public static ServerSocket createSslSocketServer(int port, String keyP12Path, String password) throws Exception {
        char[] passwordCharArray = password.toCharArray();
        // 加载密钥库
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(new FileInputStream(keyP12Path), passwordCharArray);
        // 初始化密钥管理器
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, passwordCharArray);
        // 初始化信任管理器
        TrustManagerFactory tmf = TrustManagerFactory
                .getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ks);
        // 初始化 SSL 上下文
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        // 创建 SSLServerSocket
        SSLServerSocketFactory sslServerSocketFactory = sslContext.getServerSocketFactory();
        return sslServerSocketFactory.createServerSocket(port);
    }

    /**
     * 创建普通ServerSocket
     *
     * @param port 端口号
     * @return ServerSocket
     * @throws Exception 异常
     */
    public static ServerSocket createSocketServer(int port) throws Exception {
        return new ServerSocket(port);
    }

    /**
     * 匹配类似 Nginx 路径的规则，支持：
     * 1. 通配符 * 匹配任意字符（不跨路径分隔符）
     * 2. 通配符 ** 匹配任意字符（跨路径分隔符）
     * 3. 路径参数 :paramName 匹配单个路径段
     *
     * @param registeredRoutePattern 注册的路由模式，如 /foo/*
     * @param incomingRequestPath    请求路径，如 /foo/abc/bar/123/xyz
     * @return 是否匹配
     */
    public static boolean pathMatches(String registeredRoutePattern, String incomingRequestPath) {
        // 转换注册路由为正则表达式
        String regex = patternToRegex(registeredRoutePattern);
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(incomingRequestPath);
        return matcher.matches();
    }

    /**
     * 将路径模式转换为正则表达式
     *
     * @param pattern 路径模式
     * @return 正则表达式
     */
    private static String patternToRegex(String pattern) {
        StringBuilder regex = new StringBuilder();
        String[] tokens = pattern.split("/");
        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];
            if (i > 0) {
                regex.append("/");
            }
            if (token.equals("**")) {
                // ** 匹配任意字符（跨路径）
                regex.append(".*");
            } else if (token.equals("*")) {
                // * 匹配单个路径段（不含 /）
                regex.append("[^/]+");
            } else if (token.startsWith(":")) {
                // 路径参数匹配单个路径段
                regex.append("[^/]+");
            } else if (!token.isEmpty()) {
                // 普通字符串，转义特殊字符
                regex.append(Pattern.quote(token));
            }
            // token 为空时，说明有连续的斜杠，忽略即可
        }
        // 允许路径结尾有无斜杠都匹配
        regex.append("/?");
        return "^" + regex + "$";
    }


    /**
     * 创建并直接启动线程
     */
    public void runThread(Runnable workerTask) {
        threadPoolExecutor.submit(workerTask);
    }

    /**
     * 启动服务
     */
    public void start() throws Exception {
        sortRoutePatterns();
        isServerRunning = true;
        if (isOpenSSL) {
            serverSocket = createSslSocketServer(serverPort, sslKeyPath, sslPassword);
        } else {
            serverSocket = createSocketServer(serverPort);
        }
        try {
            logger.info("服务启动成功,端口为:{}", serverPort);
            while (isServerRunning) {
                Socket socket = serverSocket.accept();
                socket.setSoTimeout(socketTimeout);
                runThread(() -> {
                    try {
                        handleWebClient(socket, null);
                    } catch (Exception e) {
                        logger.error("异常 ", e);
                    }
                });
            }
        } catch (IOException e) {
            logger.error("异常 ", e);
        }
    }

    /**
     * 混合模式启动（不监听Socket）
     */
    public void startBlendingMode() {
        sortRoutePatterns();
        isServerRunning = true;
    }

    /**
     * 停止服务
     */
    public void stop() {
        if (isServerRunning) {
            isServerRunning = false;
        }
        try {
            if (serverSocket != null) {
                serverSocket.close();
                serverSocket = null;
            }
        } catch (IOException e) {
            logger.error("异常 ", e);
        }
        // 仅关闭内部创建的线程池，外部传入的由调用方自行管理
        if (isInternalThreadPool && threadPoolExecutor != null) {
            threadPoolExecutor.shutdown();
        }
    }


    /**
     * 优雅关闭Socket连接
     * 先半关闭输出流通知客户端数据已发送完毕，等待客户端关闭连接后再彻底关闭Socket
     * 替代固定延时，确保客户端能完整接收响应数据
     *
     * @param socket 客户端Socket
     */
    private void gracefulClose(Socket socket) {
        try {
            if (socket == null || socket.isClosed()) {
                return;
            }
            // 半关闭输出流：发送FIN通知客户端“数据已写完”
            // 此时输入流仍然打开，可以等待客户端的响应
            socket.shutdownOutput();
            // 等待客户端关闭连接（读到EOF返回-1）
            // 设置短超时兜底，防止异常客户端不关闭导致线程卡死
            socket.setSoTimeout(2000);
            InputStream in = socket.getInputStream();
            while (in.read() != -1) {
                // 丢弃客户端可能发送的残余数据
            }
        } catch (Exception e) {
            // SocketTimeoutException或其他IO异常，忽略即可
        } finally {
            IOUtil.closeIO(socket);
        }
    }

    /**
     * 路由条目内部类
     * 封装路由模式、HTTP方法和处理器的映射关系
     */
    private static class PathEntry {
        /** 路由模式 */
        String routePattern;
        /** HTTP方法 */
        String httpMethod;
        /** 请求处理器 */
        HttpHandler requestHandler;

        /**
         * 构造函数
         *
         * @param routePattern   路由模式
         * @param requestHandler 请求处理器
         */
        public PathEntry(String routePattern, HttpHandler requestHandler) {
            this.routePattern = routePattern;
            this.requestHandler = requestHandler;
        }

        /**
         * 构造函数
         *
         * @param routePattern   路由模式
         * @param httpMethod     HTTP方法
         * @param requestHandler 请求处理器
         */
        public PathEntry(String routePattern, String httpMethod, HttpHandler requestHandler) {
            this.routePattern = routePattern;
            this.httpMethod = httpMethod;
            this.requestHandler = requestHandler;
        }

        public String getHttpMethod() {
            return httpMethod;
        }

        public void setHttpMethod(String httpMethod) {
            this.httpMethod = httpMethod;
        }

        public String getRoutePattern() {
            return routePattern;
        }

        public void setRoutePattern(String routePattern) {
            this.routePattern = routePattern;
        }

        public HttpHandler getRequestHandler() {
            return requestHandler;
        }

        public void setRequestHandler(HttpHandler requestHandler) {
            this.requestHandler = requestHandler;
        }
    }

}
