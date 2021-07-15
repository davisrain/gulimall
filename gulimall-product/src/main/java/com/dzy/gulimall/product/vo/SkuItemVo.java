package com.dzy.gulimall.product.vo;

import com.dzy.gulimall.product.entity.SkuImagesEntity;
import com.dzy.gulimall.product.entity.SkuInfoEntity;
import com.dzy.gulimall.product.entity.SpuInfoDescEntity;
import lombok.Data;

import java.util.List;

@Data
public class SkuItemVo {

    //1.查询sku的基本信息 `pms_sku_info`
    private SkuInfoEntity skuInfo;
    //2.查询sku的图片信息 `pms_sku_images`
    private List<SkuImagesEntity> skuImages;
    //3.查询spu下各种sku的销售属性
    private List<SkuSaleAttrVo> skuSaleAttrs;
    //4.查询spu的描述信息 `pms_spu_info_desc`
    private SpuInfoDescEntity spuInfoDesc;
    //5.查询spu的规则参数
    private List<SpuBaseAttrGroupVo> spuBaseAttrGroupVos;
    //6.是否有货
    private Boolean hasStock = true;


}
