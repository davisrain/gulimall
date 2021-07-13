package com.dzy.gulimall.search;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients(basePackages = {"com.dzy.gulimall.search.feign"})
@SpringBootApplication
@EnableDiscoveryClient
public class GulimallSearchMain {
    public static void main(String[] args) {
        SpringApplication.run(GulimallSearchMain.class, args);
    }
}
