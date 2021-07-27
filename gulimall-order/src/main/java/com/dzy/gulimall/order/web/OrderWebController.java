package com.dzy.gulimall.order.web;

import com.dzy.gulimall.order.service.OrderService;
import com.dzy.gulimall.order.vo.OrderConfirmVo;
import com.dzy.gulimall.order.vo.OrderSubmitResponseVo;
import com.dzy.gulimall.order.vo.OrderSubmitVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

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

    @PostMapping("/submitOrder")
    public String submitOrder(OrderSubmitVo orderSubmitVo) {
        //下单：验令牌，创建订单，验价格，锁库存...
        OrderSubmitResponseVo response = orderService.submitOrder(orderSubmitVo);
        if(response.getCode() == 0) {
            //下单成功返回支付页
            return "pay";
        }
        //下单失败返回确认页重新确认订单信息
        return "redirect:http://order.gulimall.com/toTrade";
    }
}
