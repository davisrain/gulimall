package com.dzy.gulimall.ware.vo;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class PurchaseItemVo {
    @NotNull(message = "采购单明细Id不能为空")
    private Long itemId;
    @NotNull(message = "采购单明细状态不能为空")
    private Integer status;
    private String reason;
}
