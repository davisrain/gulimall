package com.dzy.gulimall.product.vo;

import lombok.Data;

import java.util.List;

@Data
public class SpuBaseAttrGroupVo {
    private String attrGroupName;
    private List<Attr> spuBaseAttrs;
}
