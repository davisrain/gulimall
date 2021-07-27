package com.dzy.gulimall.order.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderSubmitVo {
    private Long addressId; //收货地址id
    //商品信息不用传，再次去购物车里面取最新的数据
    private Integer payType;    //支付方式
    private BigDecimal payPrice;    //应付价格，同购物车查询到的最近价格进行比价
    //用户信息也不用传，可以在session中取到
    //优惠、发票信息，暂时不涉及
    private String orderToken;      //防重令牌

}
