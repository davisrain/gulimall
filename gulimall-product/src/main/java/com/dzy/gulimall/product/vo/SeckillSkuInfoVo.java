package com.dzy.gulimall.product.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SeckillSkuInfoVo {
    private Long promotionId;
    private Long promotionSessionId;
    private Long skuId;
    /**
     *  商品秒杀随机码
     */
    private String randomCode;
    private BigDecimal seckillPrice;
    private BigDecimal seckillCount;
    private BigDecimal seckillLimit;
    private Integer seckillSort;
    private Long startTime;
    private Long endTime;
}
