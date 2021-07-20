package com.dzy.gulimall.cart;

import com.baomidou.mybatisplus.autoconfigure.IdentifierGeneratorAutoConfiguration;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class, MybatisPlusAutoConfiguration.class,
        IdentifierGeneratorAutoConfiguration.class})
public class GulimallCartMain {
    public static void main(String[] args) {
        SpringApplication.run(GulimallCartMain.class, args);
    }
}
