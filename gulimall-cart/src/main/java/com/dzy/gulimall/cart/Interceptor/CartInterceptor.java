package com.dzy.gulimall.cart.Interceptor;

import com.dzy.common.constant.AuthServerConstant;
import com.dzy.common.constant.CartConstant;
import com.dzy.common.vo.UserRespVo;
import com.dzy.gulimall.cart.to.UserInfoTo;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;

public class CartInterceptor implements HandlerInterceptor {

    public static final ThreadLocal<UserInfoTo> threadLocal = new ThreadLocal<>();
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        UserInfoTo userInfo = new UserInfoTo();
        //如果session里面有用户对象，将userId放入userInfo中
        UserRespVo user = (UserRespVo) request.getSession().getAttribute(AuthServerConstant.LOGIN_USER);
        if(user != null) {
            userInfo.setUserId(user.getId());
        }
        //如果cookie里面有临时用户，放入userInfo中
        Cookie[] cookies = request.getCookies();
        if(cookies != null) {
            for (Cookie cookie : cookies) {
                if(cookie.getName().equals(CartConstant.TEMP_USER_COOKIE_NAME)) {
                    userInfo.setUserKey(cookie.getValue());
                    //如果cookie里面有user-key，设置是否已经有临时用户，
                    //如果没有的话，需要在请求返回的时候向浏览器添加临时用户的cookie
                    userInfo.setHasTempUser(true);
                }
            }
        }

        //将userInfo放入ThreadLocal中，方便同一线程的后续操作使用
        threadLocal.set(userInfo);
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        UserInfoTo userInfoTo = threadLocal.get();
        if(!userInfoTo.getHasTempUser()) {
            String uuid = UUID.randomUUID().toString();
            Cookie cookie = new Cookie(CartConstant.TEMP_USER_COOKIE_NAME, uuid);
            cookie.setDomain("gulimall.com");
            cookie.setMaxAge(CartConstant.TEMP_USER_COOKIE_EXPIRES);
            response.addCookie(cookie);
        }
        //请求结束的时候将threadLocal进行remove，因为线程池里面的线程不会回收，
        //会导致当前线程中的ThreadLocalMap里面的value一直得不到回收而导致内存泄露。
        //ThreadLocalMap里面的key（也就是ThreadLocal对象）因为在这个map中被声明为WeakReference，
        //在下一次GC的时候就会回收。因此造成内存泄露的是value值。
        threadLocal.remove();
    }
}
