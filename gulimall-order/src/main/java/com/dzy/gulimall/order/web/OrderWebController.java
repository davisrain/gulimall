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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
    public String submitOrder(OrderSubmitVo orderSubmitVo, Model model, RedirectAttributes redirectAttributes) {
        //下单：验令牌，创建订单，验价格，锁库存...
        OrderSubmitResponseVo response = orderService.submitOrder(orderSubmitVo);
        Integer code = response.getCode();
        if(code == 0) {
            //下单成功返回支付页
            model.addAttribute("order", response.getOrderEntity());
            return "pay";
        }
        //下单失败返回确认页重新确认订单信息
        String msg = "下单失败；";
        switch (code) {
            case 1: msg += "订单信息过期，请刷新后重试"; break;
            case 2: msg += "价格发生变化，请确认后重试"; break;
            case 3: msg += "商品库存不足";break;
        }
        redirectAttributes.addFlashAttribute("msg", msg);
        return "redirect:http://order.gulimall.com/toTrade";
    }
}
