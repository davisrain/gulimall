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

    @GetMapping("/boss")
    public String employees(Model model, HttpSession session, @RequestParam(value = "token", required = false) String token) {
        if(StringUtils.hasText(token)) {
            ResponseEntity<String> entity = restTemplate.getForEntity("http://ssoserver.com:8080/userinfo?token=" + token, String.class);
            String userInfo = entity.getBody();
            session.setAttribute("loginUser", userInfo);
        }
        if(session.getAttribute("loginUser") == null) {
            return "redirect:http://ssoserver.com:8080/login.html?redirect_url=http://client2.com:8082/boss";
        }
        List<String> employees = new ArrayList<>();
        employees.add("zhangsan");
        employees.add("lisi");
        model.addAttribute("employees", employees);
        return "boss";
    }
}
