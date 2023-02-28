package com.fgsqw.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadUtils {


    private static final ExecutorService executorService = Executors.newFixedThreadPool(20);

    /**
     * 创建并直接启动线程
     *
     * @param task
     */
    public static void runThread(Runnable task) {
        executorService.submit(task);
    }

}
