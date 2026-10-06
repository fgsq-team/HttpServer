package com.fgsqw.httpserver.utils;

import com.fgsqw.httpserver.HttpConstant;
import com.fgsqw.httpserver.Request;
import com.fgsqw.httpserver.Response;
import com.fgsqw.httpserver.Url;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * HTTP工具类
 * 提供HTTP请求发送、流转发、请求解析等功能
 *
 * @author fgsq
 */
public class HttpUtil {

    /**
     * 日志
     */
    private static final Logger logger = LoggerFactory.getLogger(HttpUtil.class);

    /** 默认超时时间（毫秒） */
    private static final int TIMEOUT = 10000;

    /**
     * 发送GET请求
     *
     * @param requestUrl 请求URL
     * @return 响应结果
     */
    public static String sendGetRequest(String requestUrl) throws Exception {
        return sendHttpRequest(requestUrl, HttpConstant.METHOD_GET, null, null);
    }

    /**
     * 发送GET请求
     *
     * @param requestUrl 请求URL
     * @return 响应结果
     */
    public static String sendGetRequest(String requestUrl, Map<String, String> headers) throws Exception {
        return sendHttpRequest(requestUrl, HttpConstant.METHOD_GET, headers, null);
    }

    /**
     * 发送POST请求
     *
     * @param requestUrl 请求URL
     * @param requestBody  请求体
     * @return 响应结果
     */
    public static String sendPostRequest(String requestUrl, String requestBody) throws Exception {
        return sendHttpRequest(requestUrl, HttpConstant.METHOD_POST, null, requestBody);
    }

    /**
     * 发送POST请求
     *
     * @param requestUrl 请求URL
     * @param requestBody  请求体
     * @return 响应结果
     */
    public static String sendPostRequest(String requestUrl, Map<String, String> headers, String requestBody) throws Exception {
        return sendHttpRequest(requestUrl, HttpConstant.METHOD_POST, headers, requestBody);
    }

    /**
     * 发送HTTP请求（GET/POST）
     *
     * @param requestUrl 请求URL
     * @param method     请求方法（GET/POST）
     * @param requestBody 请求体（仅POST需要）
     * @return 响应结果
     */
    private static String sendHttpRequest(String requestUrl, String method, Map<String, String> headers, String requestBody) throws Exception {
        URL url = new URL(requestUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        try {
            // 设置通用的请求属性
            connection.setRequestMethod(method);
            connection.setConnectTimeout(TIMEOUT);
            connection.setReadTimeout(TIMEOUT);
            connection.setDoInput(true);

            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    connection.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }

            if (HttpConstant.METHOD_POST.equalsIgnoreCase(method)) {
                connection.setDoOutput(true); // 允许输出
                try (OutputStream os = connection.getOutputStream()) {
                    os.write(requestBody.getBytes());
                    os.flush();
                }
            }

            // 获取响应
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line).append("\n");
                }

                reader.close();
                for (Map.Entry<String, List<String>> stringListEntry : connection.getHeaderFields().entrySet()) {
                    if (stringListEntry.getKey() != null && headers != null) {
                        headers.put(stringListEntry.getKey(), stringListEntry.getValue().get(0));
                    }
                }
                return response.toString();
            } else {
                throw new RuntimeException("请求失败，错误码: " + responseCode);
            }
        } finally {
            connection.disconnect();
        }
    }

    /**
     * 使用流式方式转发原始请求
     */
    public static void forwardStreamRequest(
            Request request, Response response, String targetUrl, OutputStream proxyOutputStream
    ) throws Exception {
        URL url = new URL(targetUrl + request.genFullRequestPath());
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            // 设置请求方法
            connection.setRequestMethod(request.getRequestMethod());
            connection.setDoInput(true);
            // 如果是 GET 请求，不需要输出流
            connection.setDoOutput(!HttpConstant.METHOD_GET.equals(request.getRequestMethod()));
            connection.addRequestProperty(HttpConstant.CONNECTION, HttpConstant.CLOSE);
            // 转发所有请求头
            for (Map.Entry<String, String> header : request.getHeaders().entrySet()) {
                if (header.getKey().equalsIgnoreCase(HttpConstant.CONNECTION)) {
                    continue;
                }
                if (header.getKey().equalsIgnoreCase(HttpConstant.ACCEPT_ENCODING)) {
                    continue;
                }
                if (header.getKey().equalsIgnoreCase(HttpConstant.TRANSFER_ENCODING)) {
                    continue;
                }
                connection.setRequestProperty(header.getKey(), header.getValue());
            }
            String contentLength = request.getHeaderValue(HttpConstant.CONTENT_LENGTH);
            // 如果不是 GET 请求，才处理请求体
            if (!HttpConstant.METHOD_GET.equals(request.getRequestMethod())
                    && (StringUtils.isEmpty(contentLength) || Integer.parseInt(contentLength) > 0)) {
                OutputStream out = connection.getOutputStream();
                InputStream in = request.getInputStream();
                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalBytes = 0;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                    totalBytes += bytesRead;
                    if (totalBytes >= Integer.parseInt(contentLength)) {
                        break;
                    }
                }
                out.flush();
            }
            // 检查响应码
            int responseCode = connection.getResponseCode();
            Map<String, List<String>> headerFields = connection.getHeaderFields();
            for (Map.Entry<String, List<String>> entry : headerFields.entrySet()) {
                if (entry.getKey() == null) {
                    continue;
                }
                if (entry.getKey().equalsIgnoreCase(HttpConstant.CONNECTION)) {
                    continue;
                }
                if (entry.getKey().equalsIgnoreCase((HttpConstant.ACCEPT_ENCODING))) {
                    continue;
                }
                if (entry.getKey().equalsIgnoreCase((HttpConstant.TRANSFER_ENCODING))) {
                    continue;
                }
                for (String value : entry.getValue()) {
                    response.addHeader(entry.getKey(), value);
                }
            }
            response.setStatus(responseCode, connection.getResponseMessage());
            InputStream inputStream = connection.getInputStream();
            if (proxyOutputStream != null) {
                ProxyInputStream proxyInputStream = new ProxyInputStream(proxyOutputStream);
                proxyInputStream.setInputStream(inputStream);
                response.writeDirectStream(proxyInputStream);
            } else {
                response.writeDirectStream(inputStream);
            }
        } finally {
            connection.disconnect();
        }
    }

    /**
     * 代理输入流内部类
     * 用于流式转发时同时写入输出流
     */
    public static class ProxyInputStream extends InputStream {

        /** 输入流 */
        private InputStream inputStream;
        /** 输出流 */
        private final OutputStream outputStream;

        /**
         * 构造函数
         *
         * @param outputStream 输出流
         */
        public ProxyInputStream(OutputStream outputStream) {
            this.outputStream = outputStream;
        }

        /**
         * 设置输入流
         *
         * @param inputStream 输入流
         */
        public void setInputStream(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public int read() throws IOException {
            int read = inputStream.read();
            if (read != -1) {
                outputStream.write(read);
            }
            return read;
        }

        @Override
        public int read(byte[] b) throws IOException {
            int read = inputStream.read(b);
            if (read != -1) {
                outputStream.write(b, 0, read);
            }
            return read;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int read = inputStream.read(b, off, len);
            if (read != -1) {
                outputStream.write(b, off, read);
            }
            return read;
        }

        @Override
        public long skip(long n) throws IOException {
            return inputStream.skip(n);

        }

        @Override
        public int available() throws IOException {
            return inputStream.available();
        }

        @Override
        public void close() throws IOException {
            inputStream.close();
        }
    }

    /**
     * 从输入流解析请求
     *
     * @param clientInputStream 客户端输入流
     * @param clientSocket      客户端Socket
     * @param initialDataPrefix 初始数据前缀
     * @return 解析后的请求对象
     */
    public static Request parseRequestFromStream(InputStream clientInputStream, Socket clientSocket, String initialDataPrefix) throws IOException {
        Request request = new Request(clientSocket);
        String line;
        int lineNum = 0;
        try {
            while ((line = readHttpLine(clientInputStream)) != null) {
                if (StringUtils.isEmpty(line)) {
                    break;
                }
                // 解析请求行
                if (!parseRequestLine(lineNum, line, request, initialDataPrefix)) {
                    return null;
                }
                lineNum++;
            }
            return request;
        } catch (Exception e) {
            logger.error("从输入流解析请求失败", e);
            return null;
        }
    }

    /**
     * 解析请求行
     *
     * @param lineNum           行号
     * @param line              请求行内容
     * @param request           请求对象
     * @param initialDataPrefix 初始数据前缀
     * @return 是否成功解析
     */
    public static boolean parseRequestLine(int lineNum, String line, Request request, String initialDataPrefix) {
        try {
            // 处理初始数据前缀
            if (lineNum == 0 && initialDataPrefix != null) {
                line = initialDataPrefix + line;
            }
            int index = line.indexOf(" ");
            if (index == -1) {
                return false;
            }
            // 请求头数据
            String key = line.substring(0, index);
            String value = line.substring(index + 1);

            // 首行HTTP版本
            if (lineNum == 0) {
                String[] s = value.split(" ");
                String url = s[0];
                request.setRequestFullPath(url);
                int i = url.indexOf("?");
                // 如果有路径请求参数
                if (i > 0) {
                    String decodedRequestUrl = url.substring(0, i);
                    // 请求路径
                    request.setRequestPath(URLDecoder.decode(decodedRequestUrl, "UTF-8"));
                    // 路径请求参数截取
                    request.setRequestQueryParams(url.substring(i + 1));
                    // 路径请求参数解析成Map
                    if (!StringUtils.isEmpty(request.getRequestQueryParams())) {
                        String[] split = request.getRequestQueryParams().split("&");
                        for (String p : split) {
                            if (p.contains("=")) {
                                String paramsKey = p.substring(0, p.indexOf("="));
                                String paramsValue = p.substring(p.indexOf("=") + 1);
                                request.addPathParams(
                                        URLDecoder.decode(paramsKey, "UTF-8"),
                                        URLDecoder.decode(paramsValue, "UTF-8")
                                );
                            } else {
                                request.addPathParams(
                                        URLDecoder.decode(p, "UTF-8"), null
                                );
                            }
                        }
                    }
                } else {
                    // 没有请求参数
                    // 请求路径
                    request.setRequestPath(URLDecoder.decode(url, "UTF-8"));
                    request.setRequestFullPath(request.getRequestPath());
                }
                // 请求协议 POST/GET
                request.setRequestMethod(key);
            } else {
                int i = key.indexOf(":");
                if (i > 0) {
                    key = key.substring(0, i);
                }
            }
            request.addHeader(key, value);
            return true;
        } catch (Exception e) {
            logger.error("解析请求行失败", e);
            return false;
        }
    }

    /**
     * 读取HTTP请求行
     *
     * @param is 输入流
     * @return HTTP请求行
     * @throws IOException 输入流异常
     */
    public static String readHttpLine(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        while (true) {
            int read = is.read();
            if (read == -1) {
                return null;
            }
            char c = (char) read;
            if (c == '\r') {
                c = (char) is.read();
                if (c == '\n') {
                    break;
                }
            }
            sb.append((char) read);
        }
        return sb.toString();
    }

    /**
     * 读取HTTP请求行（字节数组）
     *
     * @param is 输入流
     * @return HTTP请求行字节数组
     * @throws IOException 输入流异常
     */
    public static byte[] readHttpLineByte(InputStream is) throws IOException {
        List<Byte> byteList = new ArrayList<>();
        while (true) {
            int read = is.read();
            if (read == -1) {
                return null;
            }
            char c = (char) read;
            byteList.add((byte) c);
            if ((c == '\r')) {
                c = (char) is.read();
                byteList.add((byte) c);
                if (c == '\n') {
                    break;
                }
            }
        }
        byte[] bytes = new byte[byteList.size()];
        for (int i = 0; i < byteList.size(); i++) {
            bytes[i] = byteList.get(i);
        }
        return bytes;
    }

    /**
     * 解析请求URL（兼容IPv6）
     *
     * @param url URL字符串
     * @return 解析后的Url对象
     */
    public static Url parseHttpUrl(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        if (!url.contains(HttpConstant.PROTOCOL_SPILT)) {
            return null;
        }
        Url urlObj = new Url();
        // 提取协议
        String protocol = url.substring(0, url.indexOf(HttpConstant.PROTOCOL_SPILT));
        String host;
        String path;
        int port;
        // 去除协议部分
        String remaining = url.substring(protocol.length() + 3); // Skip "://"
        // 提取主机和端口（兼容IPv6）
        int slashIndex = remaining.indexOf('/');
        if (slashIndex == -1) {
            slashIndex = remaining.length(); // 整个字符串都是主机和端口
        }
        String hostPort = remaining.substring(0, slashIndex);

        // 处理IPv6地址（检查是否存在方括号）
        int colonIndex = hostPort.lastIndexOf(':'); // 使用lastIndexOf处理IPv6中的冒号
        int bracketEndIndex = hostPort.lastIndexOf(']'); // 查找右方括号

        // 如果存在右方括号，说明是IPv6地址
        if (bracketEndIndex != -1) {
            // IPv6格式: [address]:port
            host = hostPort.substring(0, bracketEndIndex + 1); // 包含方括号
            if (colonIndex > bracketEndIndex) {
                // 存在端口号
                port = Integer.parseInt(hostPort.substring(colonIndex + 1));
            } else {
                // 使用默认端口
                port = (protocol.equals("ws") || protocol.equals("http")) ? 80 : 443; // 默认端口
            }
        } else {
            // IPv4或域名格式
            int colonIdx = hostPort.indexOf(':');
            if (colonIdx != -1) {
                host = hostPort.substring(0, colonIdx);
                port = Integer.parseInt(hostPort.substring(colonIdx + 1));
            } else {
                host = hostPort;
                port = (protocol.equals("ws") || protocol.equals("http")) ? 80 : 443; // 默认端口
            }
        }
        // 提取路径
        if (slashIndex < remaining.length()) {
            path = remaining.substring(slashIndex);
        } else {
            path = "/"; // 默认路径
        }
        urlObj.setProtocol(protocol);
        urlObj.setHost(host);
        urlObj.setPort(port);
        urlObj.setPath(path);
        urlObj.setQuery(url.contains("?") ? url.substring(url.indexOf("?")) : null);
        return urlObj;
    }



}
