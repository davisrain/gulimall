package com.dzy.gulimall.order.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

@Configuration
public class MyFeignConfiguration {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                //RequestContextHolder拿到刚进来的这个请求,RequestContextHolder里面有一个ThreadLocal，用来保存当前线程的请求数据。
                ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if(requestAttributes != null) {
                    HttpServletRequest request = requestAttributes.getRequest();
                    //同步请求头数据，Cookie
                    String cookie = request.getHeader("Cookie");
                    template.header("Cookie", cookie);
                }
            }
        };
    }
}
