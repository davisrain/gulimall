package com.dzy.gulimall.order.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableConfigurationProperties(MyThreadPoolProperties.class)
public class MyThreadPoolConfiguration {

    @Bean
    public ThreadPoolExecutor myThreadPoolExecutor(MyThreadPoolProperties properties) {
        return new ThreadPoolExecutor(properties.getCorePoolSize(), properties.getMaxPoolSize(), properties.getKeepAliveTime(), TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(properties.getQueueCapacity()), Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy());
    }
}
