package com.dzy.gulimall.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dzy.gulimall.order.config.AliPayTemplate;
import com.dzy.gulimall.order.entity.OrderEntity;
import com.dzy.gulimall.order.entity.OrderItemEntity;
import com.dzy.gulimall.order.service.OrderItemService;
import com.dzy.gulimall.order.service.OrderService;
import com.dzy.gulimall.order.service.PayService;
import com.dzy.gulimall.order.vo.PayVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.RoundingMode;
import java.util.List;

@Service("payService")
public class PayServiceImpl implements PayService {

    @Autowired
    OrderService orderService;

    @Autowired
    OrderItemService orderItemService;

    @Autowired
    AliPayTemplate aliPayTemplate;
    @Override
    public String payOrder(String orderSn) {
        //1.根据订单号查询到订单的信息封装成PayVo
        OrderEntity order = orderService.getOne(new QueryWrapper<OrderEntity>().eq("order_sn", orderSn));
        List<OrderItemEntity> orderItems = orderItemService.list(new QueryWrapper<OrderItemEntity>().eq("order_sn", orderSn));
        PayVo payVo = new PayVo();
        payVo.setOut_trade_no(orderSn);
        payVo.setSubject(orderItems.get(0).getSkuName());
        payVo.setBody(orderItems.get(0).getSkuAttrsVals());
        payVo.setTotalAmount(order.getPayAmount().setScale(2, RoundingMode.HALF_UP).toString());
        String response = null;
        try {
            response = aliPayTemplate.payOrder(payVo);
        } catch (Exception e){
            e.printStackTrace();
        }
        return response;
    }
}
