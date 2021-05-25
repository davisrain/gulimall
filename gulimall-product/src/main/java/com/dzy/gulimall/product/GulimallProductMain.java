package com.dzy.gulimall.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class GulimallProductMain {
    public static void main(String[] args) {
        SpringApplication.run(GulimallProductMain.class, args);
    }
}
