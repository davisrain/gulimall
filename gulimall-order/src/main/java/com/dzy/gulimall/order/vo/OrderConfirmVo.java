package com.dzy.gulimall.order.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class OrderConfirmVo {

    private List<MemberAddressVo> addresses;
    private List<OrderItemVo> orderItems;
    private Integer integration;
    private BigDecimal totalPrice;
    private BigDecimal payPrice;
    private Map<Long, Boolean> hasStockMap;
    private String orderToken;

    public BigDecimal getTotalPrice() {
        totalPrice = new BigDecimal("0");
        for (OrderItemVo orderItem : orderItems) {
            totalPrice = totalPrice.add(orderItem.getTotalPrice());
        }
        return totalPrice;
    }

    public BigDecimal getPayPrice() {
        payPrice = getTotalPrice();
        return payPrice;
    }

}
