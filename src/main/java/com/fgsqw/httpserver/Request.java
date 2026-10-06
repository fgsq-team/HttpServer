// Request.java
package com.fgsqw.httpserver;

import com.fgsqw.httpserver.stream.BodyInputStream;
import com.fgsqw.httpserver.stream.MultUploadInputStream;
import com.fgsqw.httpserver.stream.SingleUploadInputStream;
import com.fgsqw.httpserver.utils.FileUtil;
import com.fgsqw.httpserver.utils.IOUtil;
import com.fgsqw.httpserver.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * HTTP请求类
 * 封装HTTP请求的所有信息，包括请求方法、路径、参数、头部等
 *
 * @author fgsq
 */
public class Request extends HttpBase {

    /**
     * 日志
     */
    private static final Logger logger = LoggerFactory.getLogger(Request.class);

    // 请求路径参数解析
    private final Map<String, String> queryParams = new LinkedHashMap<>();
    // 请求方式 POST/GET
    private String requestMethod;

    /**
     * 构造函数
     *
     * @param socket 客户端Socket
     * @throws IOException IO异常
     */
    public Request(Socket socket) throws IOException {
        super(socket);
    }

    /**
     * 获取路径参数
     */
    public Map<String, String> getQueryParams() {
        return queryParams;
    }

    /**
     * 获取路径参数
     *
     * @param key 参数名
     * @return 参数值
     */
    public String getQueryParam(String key) {
        return queryParams.get(key);
    }

    /**
     * @return InputStream
     * @author fgsq
     * @comments 获取原始输入流
     * @date 2024/4/27 15:05
     */
    public InputStream getInputStream() {
        return in;
    }

    /**
     * @return String
     * @author fgsq
     * @comments 获取请求体内容
     * @date 2024/4/27 15:04
     */
    public String getRequestBody() throws IOException {
        InputStream bodyInputStream = getBodyInputStream();
        if (bodyInputStream != null) {
            byte[] buffer = new byte[1024];
            StringBuilder sb = new StringBuilder();
            int ten;
            while ((ten = bodyInputStream.read(buffer)) != -1) {
                sb.append(new String(buffer, 0, ten));
            }
            return sb.toString();
        }
        return null;
    }

/*
    public String getRequestBody() throws IOException {
        if (HttpConstant.GET_METHOD.equalsIgnoreCase(requestMethod)) {
            return requestURLParams;
        } else {
            if (contentLength == 0) {
                return null;
            }
            byte[] buffer = new byte[1024];
            StringBuilder sb = new StringBuilder();
            int size = (int) contentLength;
            while (size > 0) {
                int read = is.read(buffer, 0, Math.min(size, buffer.length));
                if (read == -1) {
                    return null;
                }
                sb.append(new String(buffer, 0, read));
                size -= read;
            }
            return sb.toString();
        }
    }
*/

    /**
     * @return InputStream
     * @author fgsq
     * @comments 获取请求体输入流
     * @date 2024/4/27 15:05
     */
    public InputStream getBodyInputStream() throws IOException {
        if (HttpConstant.METHOD_POST.equalsIgnoreCase(requestMethod)
                || HttpConstant.METHOD_PUT.equalsIgnoreCase(requestMethod)) {
            int size = (int) getContentLength();
            if (size == 0) {
                return null;
            }
            return new BodyInputStream(in, size);
        }
        return null;
    }

    /**
     * @return long[]
     * @author fgsq
     * @comments 获取文件读取范围
     * @date 2024/4/27 15:06
     */
    public long[] getRangeLength() {
        String rangeHeader = getHeaderValue("Range");
        try {
            if (!StringUtils.isEmpty(rangeHeader)) {
                String[] rangeValues = rangeHeader.substring("bytes=".length()).split("-");
                long start = 0;
                long end = -1;
                if (rangeValues.length >= 1) {
                    start = Long.parseLong(rangeValues[0]);
                }
                if (rangeValues.length >= 2) {
                    end = Long.parseLong(rangeValues[1]);
                }
                return new long[]{start, end};
            }
        } catch (Exception e) {
            logger.error("异常 ", e);
        }
        return null;
    }

    /**
     * 上传结果内部类
     * 封装文件上传后的结果信息
     */
    public static class UploadResult {
        /** 文件名 */
        private String fileName;
        /** 文件路径 */
        private String filePath;
        /** 文件大小 */
        private Long fileSize;

        public String getFilePath() {
            return filePath;
        }

        public void setFilePath(String filePath) {
            this.filePath = filePath;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public Long getFileSize() {
            return fileSize;
        }

        public void setFileSize(Long fileSize) {
            this.fileSize = fileSize;
        }
    }

    /**
     * 保存上传的文件到指定路径
     */
    public UploadResult transferUploadFile(String path) throws IOException {
        SingleUploadInputStream singleUploadInputStream = new SingleUploadInputStream(this);
        File file = new File(path, singleUploadInputStream.getFileName());
        // 防止重名文件被覆盖
        file = FileUtil.avoidDuplication(file);
        OutputStream outputStream = new FileOutputStream(file);
        IOUtil.transfer(singleUploadInputStream, outputStream);
        UploadResult uploadEntity = new UploadResult();
        uploadEntity.setFileName(singleUploadInputStream.getFileName());
        uploadEntity.setFilePath(file.getPath());
        uploadEntity.setFileSize(singleUploadInputStream.getFileSize());
        return uploadEntity;
    }

    /**
     * 获取单文件上传流
     */
    public SingleUploadInputStream getSingleUploadInputStream() throws IOException {
        return new SingleUploadInputStream(this);
    }

    /**
     * 获取多文件上传流
     */
    public MultUploadInputStream getMultUploadInputStream() throws IOException {
        return new MultUploadInputStream(this);
    }

    /**
     * 设置请求方法
     *
     * @param requestMethod 请求方法
     */
    public void setRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod.toUpperCase();
    }

    /**
     * 获取请求完整路径
     *
     * @return 完整路径
     */
    public String getRequestFullPath() {
        return getUrl().getUrl();
    }

    /**
     * 设置请求完整路径
     *
     * @param requestFullPath 完整路径
     */
    public void setRequestFullPath(String requestFullPath) {
        getUrl().setUrl(requestFullPath);
    }

    /**
     * 获取请求路径
     *
     * @return 请求路径
     */
    public String getRequestPath() {
        return getUrl().getPath();
    }

    /**
     * 设置请求路径
     *
     * @param requestPath 请求路径
     */
    public void setRequestPath(String requestPath) {
        getUrl().setPath(requestPath);
    }

    /**
     * 获取请求查询参数
     *
     * @return 查询参数
     */
    public String getRequestQueryParams() {
        return getUrl().getQuery();
    }

    /**
     * 设置请求查询参数
     *
     * @param requestURLParams 查询参数
     */
    public void setRequestQueryParams(String requestURLParams) {
        getUrl().setQuery(requestURLParams);
    }

    /**
     * 获取请求方法
     *
     * @return 请求方法
     */
    public String getRequestMethod() {
        return requestMethod;
    }

    /**
     * 添加路径参数
     *
     * @param key   参数名
     * @param value 参数值
     */
    public void addPathParams(String key, String value) {
        queryParams.put(key, value);
    }

    /**
     * 生成查询参数
     *
     * @return 查询参数字符串
     */
    public String genQueryParam() {
        if (queryParams.isEmpty()) {
            return null;
        }
        StringBuilder pathParam = new StringBuilder();
        for (Map.Entry<String, String> entry : queryParams.entrySet()) {
            if (StringUtils.isEmpty(entry.getValue())) {
                pathParam.append(entry.getKey()).append("&");
            } else {
                pathParam.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
            }
        }
        return pathParam.substring(0, pathParam.length() - 1);
    }

    /**
     * 生成完整的请求路径
     *
     * @return 完整请求路径
     */
    public String genFullRequestPath() {
        String queryParam = genQueryParam();
        if (queryParam == null) {
            return getUrl().getPath();
        }
        return getUrl().getPath() + "?" + queryParam;
    }

}
