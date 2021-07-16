package com.dzy.gulimall.product.vo;

import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;

@Data
public class AttrValuesWithSkuIdsVo {
    private String attrValue;
    private String skuIds;
}
