package com.dzy.gulimall.cart.controller;


import com.dzy.gulimall.cart.Interceptor.CartInterceptor;
import com.dzy.gulimall.cart.to.UserInfoTo;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CartController {


    /**
     *  浏览器有一个cookie：user-key，标识用户身份，一个月后过期
     *  如果第一次使用购物车功能，都会给一个临时的用户身份；
     *  浏览器以后保存，每次访问都会带上这个cookie
     *
     *  登录：session里面有用户信息
     *  没有登录：按照cookie里面的user-key来识别临时用户身份
     *  第一次进入购物车，如果没有临时用户，帮忙创建一个临时用户（即向cookie中添加一个名为user-key的值）
     */
    @GetMapping("/cartlist.html")
    public String cartListPage(Model model) {
        UserInfoTo userInfo = CartInterceptor.threadLocal.get();
        System.out.println(userInfo);
        return "cartList";
    }
}
