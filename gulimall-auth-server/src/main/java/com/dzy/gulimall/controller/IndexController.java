package com.dzy.gulimall.controller;

import com.dzy.gulimall.config.MyWebConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class IndexController {

    /**
     *  页面跳转可以使用SpringMVC提供的viewController进行配置，不用写跳转空方法了
     * @see MyWebConfiguration
     */
//    @GetMapping({"login","/login.html"})
//    public String loginPage() {
//        return "login";
//    }
//
//    @GetMapping({"register", "register.html"})
//    public String registerPage(){
//        return "register";
//    }
}
