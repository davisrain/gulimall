package com.dzy.gulimall.order.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "my-thread-pool.config")
@Data
public class MyThreadPoolProperties {
    private Integer corePoolSize;
    private Integer maxPoolSize;
    private Long keepAliveTime;
    private Integer queueCapacity;
}
