package com.dzy.gulimall.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients(basePackages = "com.dzy.gulimall.product.feign")
@EnableDiscoveryClient
@SpringBootApplication
public class GulimallProductMain {
    public static void main(String[] args) {
        SpringApplication.run(GulimallProductMain.class, args);
    }
}
