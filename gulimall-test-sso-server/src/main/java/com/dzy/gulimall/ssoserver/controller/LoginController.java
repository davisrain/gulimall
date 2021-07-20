package com.dzy.gulimall.ssoserver.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;

@Controller
public class LoginController {

    @Autowired
    StringRedisTemplate redisTemplate;

    @GetMapping("/login.html")
    public String loginPage(@RequestParam("redirect_url") String redirectUrl, Model model,
                            @CookieValue(value = "sso_token", required = false) String token) {
        //跳转登录页的时候，先判断一下cookie中有没有sso_token的值，如果有的话，说明用户之前在其他系统登录过，
        //直接重定向到回调的地址并且拼上token，让请求登录的系统拿到token值来获取用户信息。
        if(StringUtils.hasText(token)) {
            return "redirect:" + redirectUrl + "?token=" +token;
        }
        //如果没有token的值，就去到登录页面进行登录
        model.addAttribute("redirectUrl", redirectUrl);
        return "login";
    }

    @PostMapping("/login")
    public String login(String username, String password, String redirectUrl, HttpServletResponse response) {
        if(StringUtils.hasText(username) && StringUtils.hasText(password)) {
            //登录成功，将登录的数据存到redis，并且在cookie中留下痕迹，方便下次用户访问一个未登录的系统，
            //跳转到认证服务器时能够带上这个cookie。
            String token = UUID.randomUUID().toString().replace("-", "");
            redisTemplate.opsForValue().set(token, username);
            Cookie cookie = new Cookie("sso_token", token);
            response.addCookie(cookie);
            //重定向到指定路径,将token拼接到地址上返回
            return "redirect:" + redirectUrl + "?token=" + token;
        } else {
            return "login";
        }
    }

    @ResponseBody
    @GetMapping("/userinfo")
    public String userInfo(@RequestParam("token") String token) {
        String userInfo = redisTemplate.opsForValue().get(token);
        return userInfo;
    }
}
