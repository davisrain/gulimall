package com.dzy.gulimall.seckill;

import com.baomidou.mybatisplus.autoconfigure.IdentifierGeneratorAutoConfiguration;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

//@EnableRabbit     可以不加这个注解，因为秒杀服务只有发送消息，没有接收消息，@EnableRabbit是用来开启@RabbitListener注解等功能使用的
@EnableFeignClients
@EnableDiscoveryClient
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class, MybatisPlusAutoConfiguration.class, IdentifierGeneratorAutoConfiguration.class})
public class GulimallSeckillMain {
    public static void main(String[] args) {
        SpringApplication.run(GulimallSeckillMain.class, args);
    }
}
