package com.dzy.gulimall.ware.Vo;

import lombok.Data;

import java.util.List;

@Data
public class PurchaseMergeVo {
   private Long purchaseId; //整单id
   private List<Long> items;    //合并项集合
}
