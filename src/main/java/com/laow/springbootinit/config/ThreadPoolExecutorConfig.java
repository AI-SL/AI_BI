package com.laow.springbootinit.config;

import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class ThreadPoolExecutorConfig {

    ThreadFactory threadFactory = new ThreadFactory() {
        private int count = 1;
        @Override
        public Thread newThread(@NotNull Runnable r) {
            Thread thread = new Thread(r);
            thread.setName("线程" + count);
            count++;
            return thread;
        }
    };

    /**
     * 创建线程池，这里设置任务队列为4是作为测试用的，正常应该设置10000
     * @return
     */
    @Bean
    public ThreadPoolExecutor threadPoolExecutor() {
        return new ThreadPoolExecutor(
                10,
                20,
                100,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(4), threadFactory);
    }

}
