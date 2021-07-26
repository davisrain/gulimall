package com.dzy.gulimall.order.vo;

import lombok.Data;

@Data
public class OrderItemHasStockVo {
    private Long skuId;
    private Boolean hasStock;
}
