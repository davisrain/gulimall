package com.dzy.gulimall.seckill.config;

import com.alibaba.csp.sentinel.adapter.spring.webmvc.callback.BlockExceptionHandler;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.fastjson.JSON;
import com.dzy.common.exception.BizCodeEnum;
import com.dzy.common.utils.R;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Configuration
public class MySentinelConfig {
    @Bean
    public BlockExceptionHandler defaultBlockExceptionHandler() {
        return new BlockExceptionHandler() {
            @Override
            public void handle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, BlockException e) throws Exception {
                httpServletResponse.setContentType("application/json;charset=utf-8");
                httpServletResponse.getWriter().println(JSON.toJSONString(R.error(BizCodeEnum.UNKNOWN_EXCEPTION)));
            }
        };
    }

}
