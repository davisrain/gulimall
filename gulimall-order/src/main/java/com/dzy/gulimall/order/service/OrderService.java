package com.dzy.gulimall.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dzy.common.utils.PageUtils;
import com.dzy.gulimall.order.entity.OrderEntity;
import com.dzy.gulimall.order.vo.OrderConfirmVo;
import com.dzy.gulimall.order.vo.OrderSubmitResponseVo;
import com.dzy.gulimall.order.vo.OrderSubmitVo;

import java.util.Map;

/**
 * 订单
 *
 * @author zhengyu_dai
 * @email zhengyu_dai@foxmail.com
 * @date 2021-05-24 23:21:32
 */
public interface OrderService extends IService<OrderEntity> {

    PageUtils queryPage(Map<String, Object> params);

    OrderConfirmVo confirmOrder();

    OrderSubmitResponseVo submitOrder(OrderSubmitVo orderSubmitVo);

    OrderEntity getOrderByOrderSn(String orderSn);
}

