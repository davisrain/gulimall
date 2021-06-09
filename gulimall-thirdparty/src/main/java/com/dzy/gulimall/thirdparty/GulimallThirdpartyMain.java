package com.dzy.gulimall.thirdparty;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class GulimallThirdpartyMain {
    public static void main(String[] args) {
        SpringApplication.run(GulimallThirdpartyMain.class, args);
    }
}
