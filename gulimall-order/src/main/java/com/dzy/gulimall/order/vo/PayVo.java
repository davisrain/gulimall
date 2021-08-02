package com.dzy.gulimall.order.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PayVo {
    private String out_trade_no;        //订单号
    private String subject;             //订单名称
    private String totalAmount;         //订单金额
    private String body;                //订单描述
}
