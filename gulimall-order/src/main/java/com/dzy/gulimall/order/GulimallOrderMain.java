package com.dzy.gulimall.order;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableRabbit
@SpringBootApplication
public class GulimallOrderMain {
    public static void main(String[] args) {
        SpringApplication.run(GulimallOrderMain.class, args);
    }
}
