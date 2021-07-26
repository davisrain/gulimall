package com.dzy.gulimall.order.interceptor;

import com.dzy.common.constant.AuthServerConstant;
import com.dzy.common.vo.UserRespVo;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LoginUserInterceptor implements HandlerInterceptor {

    public static final ThreadLocal<UserRespVo> userThreadLocal = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        UserRespVo user = (UserRespVo) request.getSession().getAttribute(AuthServerConstant.LOGIN_USER);
        if(user != null) {
            userThreadLocal.set(user);
            return true;
        } else {
            response.sendRedirect("http://auth.gulimall.com/login.html");
            return false;
        }
    }
}
