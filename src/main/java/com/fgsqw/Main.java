package com.fgsqw;

import com.fgsqw.utils.IOUtil;

import java.io.IOException;
import java.io.InputStream;

/**
 * demo
 * @Author: fgsqme
 */
public class Main {

    public static void main(String[] args) throws IOException {
        HttpServer httpServer = new HttpServer(80);
        httpServer.addPath("/", (request, response) -> {
            InputStream is = ClassLoader.getSystemResourceAsStream("index.html");
            if (is != null) {
                byte[] bytes = IOUtil.readBytes(is);
                response.writeBytes(bytes, Response.TEXT_CONTEXT_TYPE);
            } else {
                response.write404();
            }
        });

        httpServer.start();
    }
}