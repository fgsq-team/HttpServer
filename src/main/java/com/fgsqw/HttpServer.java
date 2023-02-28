package com.fgsqw;


import com.fgsqw.utils.IOUtil;
import com.fgsqw.utils.StringUtils;
import com.fgsqw.utils.ThreadUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


/**
 * 简易HttpServer服务
 * @Author: fgsqme
 */
public class HttpServer {

    private int port = 8080;
    private boolean running = false;
    private ServerSocket serverSocket;
    private final Map<String, HttpHandler> handlerMap = new HashMap<>();

    public HttpServer(int port) {
        this.port = port;
    }

    public void addPath(String path, HttpHandler handler) {
        handlerMap.put(path, handler);
    }

    /**
     * 启动监听
     */
    public void start() throws IOException {
        running = true;
        serverSocket = new ServerSocket(port);
        ThreadUtils.runThread(() -> {
            try {
                while (running) {
                    Socket socket = serverSocket.accept();
                    ThreadUtils.runThread(() -> {
                        OutputStream outputStream = null;
                        InputStream inputStream = null;
                        try {
                            inputStream = socket.getInputStream();
                            outputStream = socket.getOutputStream();
                            Request request = new Request(inputStream);
                            String line = null;
                            int lineNum = 0;
                            while ((line = IOUtil.readHttpLine(inputStream)) != null) {
                                if (StringUtils.isEmpty(line)) {
                                    break;
                                }
                                int index = line.indexOf(" ");
                                // 请求头key value解析
                                String key = line.substring(0, index);
                                String value = line.substring(index + 1);
                                // 首行HTTP版本以及请求协议
                                if (lineNum == 0) {
                                    String[] s = value.split(" ");
                                    String url = s[0];
                                    url = URLDecoder.decode(url, "UTF-8");
                                    int i = url.indexOf("?");
                                    if (i > 0) {
                                        // 请求路径截取
                                        request.setRequestURL(url.substring(0, i));
                                        // 路径请求参数截取
                                        request.setRequestURLParams(url.substring(i + 1));
                                        // 路几个请求参数解析
                                        if (!StringUtils.isEmpty(request.getRequestURLParams())) {
                                            if (!StringUtils.isEmpty(request.getRequestURLParams())) {
                                                String[] split = request.getRequestURLParams().split("&");
                                                for (String p : split) {
                                                    if (p.contains("=")) {
                                                        String[] split1 = p.split("=");
                                                        request.addPathParams(split1[0], split1[1]);
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        request.setRequestURL(url);
                                    }
                                    // 请求协议
                                    request.setRequestMethod(key);
                                    request.addHeader(key, value);
                                } else {
                                    int i = key.indexOf(":");
                                    if (i > 0) {
                                        key = key.substring(0, i);
                                    }
                                    request.addHeader(key, value);
                                }
                                lineNum++;
                            }
                            request.setHeaderReady(true);
                            Set<String> strings = handlerMap.keySet();
                            boolean isMatch = false;
                            // 路径匹配调用
                            for (String path : strings) {
                                isMatch = request.getRequestURL().startsWith(path);
                                if (isMatch) {
                                    HttpHandler httpHandler = handlerMap.get(path);
                                    if (httpHandler != null) {
                                        httpHandler.handle(request, new Response(outputStream));
                                    }
                                    break;
                                }
                            }
                            // 找不到路径返回404
                            if (!isMatch) {
                                Response response = new Response(outputStream);
                                response.write404();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            try {
                                socket.shutdownInput();
                                socket.shutdownOutput();
                                socket.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void stop() {
        if (running) {
            running = false;
        }
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
