package com.dzy.gulimall.product.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttrRespVo extends AttrVo {

    private String attrGroupName;

    private String catelogName;
}
