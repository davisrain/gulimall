package com.dzy.gulimall.ssoclient.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;

@Controller
public class SsoController {

    @Autowired
    RestTemplate restTemplate;

    @GetMapping("/employees")
    public String employees(Model model, HttpSession session, @RequestParam(value = "token", required = false) String token) {
        //判断请求中是否带有token，如果是从认证服务器回调的，就是登录过的并且会带有token，可以通过
        //token去认证服务器拿到用户信息
        if(StringUtils.hasText(token)) {
            ResponseEntity<String> entity = restTemplate.getForEntity("http://ssoserver.com:8080/userinfo?token=" + token, String.class);
            String userInfo = entity.getBody();
            //将拿到的用户信息放入自己系统的session中
            session.setAttribute("loginUser", userInfo);
        }
        //如果自身系统的session中没有用户信息，重定向到认证服务器进行登录，并且带上登录成功之后的回调地址。
        if(session.getAttribute("loginUser") == null) {
            return "redirect:http://ssoserver.com:8080/login.html?redirect_url=http://client1.com:8081/employees";
        }
        List<String> employees = new ArrayList<>();
        employees.add("zhangsan");
        employees.add("lisi");
        model.addAttribute("employees", employees);
        return "employees";
    }
}
