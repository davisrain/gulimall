package com.dzy.gulimall.order.listener;

import com.dzy.gulimall.order.service.OrderService;
import com.dzy.gulimall.order.vo.PayAsyncVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class OrderPayedListener {

    @Autowired
    OrderService orderService;

    @PostMapping("/payed/notify")
    public String OrderPayed(PayAsyncVo payAsyncVo) {
        System.out.println("收到异步通知...");
        return orderService.orderPayed(payAsyncVo);
    }
}
