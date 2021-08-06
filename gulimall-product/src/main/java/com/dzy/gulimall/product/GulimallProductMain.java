package com.dzy.gulimall.product;

import com.dzy.gulimall.product.config.MyImportBeanDefinitionRegistrar;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

import javax.naming.Context;
import java.lang.reflect.Field;

@EnableRedisHttpSession
@EnableFeignClients(basePackages = "com.dzy.gulimall.product.feign")
@EnableDiscoveryClient
@SpringBootApplication
@Import({MyImportBeanDefinitionRegistrar.class})
public class GulimallProductMain {
    public static void main(String[] args) throws IllegalAccessException {
        ConfigurableApplicationContext context = SpringApplication.run(GulimallProductMain.class, args);
        Object bean = context.getBean("org.springframework.boot.autoconfigure.AutoConfigurationPackages");
        Field[] fields = bean.getClass().getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            System.out.println(field.get(bean));
        }
        System.out.println(bean);
    }
}
