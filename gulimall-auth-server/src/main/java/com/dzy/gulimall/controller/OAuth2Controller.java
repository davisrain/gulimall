package com.dzy.gulimall.controller;

import com.alibaba.fastjson.TypeReference;
import com.dzy.common.utils.R;
import com.dzy.gulimall.Feign.MemberFeignService;
import com.dzy.gulimall.to.WeiboAccessTokenTo;
import com.dzy.common.vo.UserRespVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@Controller
public class OAuth2Controller {
    @Autowired
    MemberFeignService memberFeignService;

    @Autowired
    RestTemplate restTemplate;

    @GetMapping("/oauth2/weibo/success")
    public String weiboLogin(@RequestParam("code") String code, RedirectAttributes redirectAttributes,
                             HttpSession session, HttpServletResponse servletResponse) {
        //1.根据code去获取accessToken
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", "YOUR_CLIENT_ID");
        params.add("client_secret", "YOUR_CLIENT_SECRET");
        params.add("grand_type", "authorization_code");
        params.add("redirect_uri", "http://auth.gulimall.com/oauth2/weibo/success");
        params.add("code", code);
        WeiboAccessTokenTo weiboAccessTokenTo = restTemplate.postForObject("https://api.weibo.com/oauth2/access_tocken", params, WeiboAccessTokenTo.class);
        //2.拿到accessToken去调用member远程服务的微博登录功能
        R r = memberFeignService.login(weiboAccessTokenTo);
        //3.根据返回的用户信息决定跳转页面
        if(r.getCode() == 0) {
            UserRespVo user = r.getData("data", new TypeReference<UserRespVo>() {});
            /**
             *  服务器向浏览器发送cookie让其保存的逻辑如下，如果我们可以修改cookie的domain作用域为父域名，
             *  那么就能在访问所有子域的时候带上该cookie
             */
//            Cookie cookie = new Cookie("JSESSIONID", "");
//            cookie.setDomain("");
//            servletResponse.addCookie(cookie);
            session.setAttribute("loginUser", user);
            return "redirect:http://gulimall.com";
        } else {
            Map<String, String> errors = new HashMap<>();
            errors.put("msg", r.getMsg());
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:http://auth.gulimall.com/login.html";
        }
    }
}
