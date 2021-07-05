package com.dzy.gulimall.product.config;


import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class MyRedissonConfiguration {

    @Bean(destroyMethod="shutdown")
    RedissonClient redisson() throws IOException {
        Config config = new Config();
        //地址需要使用redis://协议格式，如果是https，需要使用rediss://
        config.useSingleServer().setAddress("redis://47.104.204.228:6379");
        return Redisson.create(config);
    }
}
