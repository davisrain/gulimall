package com.dzy.gulimall.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MyWebConfiguration implements WebMvcConfigurer {
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("login.html").setViewName("login");
        registry.addViewController("login").setViewName("login");
        registry.addViewController("register.html").setViewName("register");
        registry.addViewController("register").setViewName("register");
    }
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
