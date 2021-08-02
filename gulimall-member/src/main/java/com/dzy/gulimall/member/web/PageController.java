package com.dzy.gulimall.member.web;

import com.dzy.common.utils.R;
import com.dzy.common.vo.UserRespVo;
import com.dzy.gulimall.member.feign.OrderFeignService;
import com.dzy.gulimall.member.interceptor.LoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.Map;


@Controller
public class PageController {

    @Autowired
    OrderFeignService orderFeignService;

    @GetMapping("/orderList.html")
    public String orderList(@RequestParam(value = "page", defaultValue = "1") String page, Model model) {
        //展示用户对应的订单信息
        Map<String, Object> params = new HashMap<>();
        params.put("page", page);
        R r = orderFeignService.pageOrderWithItemsByMemberId(params);
        model.addAttribute("response", r);
        return "orderList";
    }
}
