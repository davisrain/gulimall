package com.dzy.gulimall.ware.vo;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class PurchaseDoneVo {

    @NotNull(message = "采购单id不能为空")
    private Long id;
    private List<PurchaseItemVo> items;
}
