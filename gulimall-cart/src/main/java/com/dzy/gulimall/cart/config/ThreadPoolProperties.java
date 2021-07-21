package com.dzy.gulimall.cart.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "my-thread-pool.config")
@Data
public class ThreadPoolProperties {
    private Integer corePoolSize;
    private Integer maxPoolSize;
    private Long keepAliveTime;
    private Integer queueCapacity;
}
