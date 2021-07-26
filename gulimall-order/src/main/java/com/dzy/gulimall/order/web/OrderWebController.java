package com.dzy.gulimall.order.web;

import com.dzy.gulimall.order.service.OrderService;
import com.dzy.gulimall.order.vo.OrderConfirmVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class OrderWebController {

    @Autowired
    OrderService orderService;

    @GetMapping("/toTrade")
    public String toTrade(Model model) {

        OrderConfirmVo orderConfirm = orderService.confirmOrder();
        model.addAttribute("orderConfirm", orderConfirm);
        return "confirm";
    }
}
