package com.dzy.common.to.mq;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SeckillOrderTo {
    private String orderSn;
    private Long promotionSessionId;
    private Long skuId;
    private Long memberId;
    private BigDecimal seckillPrice;
    private Integer num;
}
