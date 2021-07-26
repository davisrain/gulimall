package com.dzy.gulimall.order.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderItemVo {
    private Long skuId;
    private Boolean check;
    private String title;
    private String image;
    private List<String> saleAttrs;
    private BigDecimal price;
    private Integer count;
    private BigDecimal totalPrice;
}
