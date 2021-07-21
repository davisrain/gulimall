package com.dzy.gulimall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dzy.common.utils.PageUtils;
import com.dzy.gulimall.product.entity.SkuSaleAttrValueEntity;
import com.dzy.gulimall.product.vo.SkuSaleAttrVo;

import java.util.List;
import java.util.Map;

/**
 * sku销售属性&值
 *
 * @author zhengyu_dai
 * @email zhengyu_dai@foxmail.com
 * @date 2021-05-25 11:24:02
 */
public interface SkuSaleAttrValueService extends IService<SkuSaleAttrValueEntity> {

    PageUtils queryPage(Map<String, Object> params);

    List<SkuSaleAttrVo> getSkuSaleAttrsBySpuId(Long spuId);

    List<String> getSkuSaleAttrValueAsStringList(Long skuId);
}

