package com.dzy.gulimall.member.interceptor;

import com.dzy.common.constant.AuthServerConstant;
import com.dzy.common.vo.UserRespVo;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LoginInterceptor implements HandlerInterceptor {

    public static ThreadLocal<UserRespVo> userThreadLocal = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String requestURI = request.getRequestURI();
        boolean match = new AntPathMatcher().match("/member/**", requestURI);
        if(match) {
            return true;
        }
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
