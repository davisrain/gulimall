package com.dzy.gulimall.order.vo;

import com.dzy.gulimall.order.entity.OrderEntity;
import lombok.Data;

@Data
public class OrderSubmitResponseVo {
    private OrderEntity orderEntity;
    private Integer code;
}
