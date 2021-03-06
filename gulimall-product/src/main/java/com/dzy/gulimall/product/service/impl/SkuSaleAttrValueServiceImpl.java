package com.dzy.gulimall.product.service.impl;

import com.dzy.gulimall.product.vo.SkuSaleAttrVo;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dzy.common.utils.PageUtils;
import com.dzy.common.utils.Query;

import com.dzy.gulimall.product.dao.SkuSaleAttrValueDao;
import com.dzy.gulimall.product.entity.SkuSaleAttrValueEntity;
import com.dzy.gulimall.product.service.SkuSaleAttrValueService;


@Service("skuSaleAttrValueService")
public class SkuSaleAttrValueServiceImpl extends ServiceImpl<SkuSaleAttrValueDao, SkuSaleAttrValueEntity> implements SkuSaleAttrValueService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SkuSaleAttrValueEntity> page = this.page(
                new Query<SkuSaleAttrValueEntity>().getPage(params),
                new QueryWrapper<SkuSaleAttrValueEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<SkuSaleAttrVo> getSkuSaleAttrsBySpuId(Long spuId) {

        return baseMapper.getSkuSaleAttrsBySpuId(spuId);
    }

    @Override
    public List<String> getSkuSaleAttrValueAsStringList(Long skuId) {
        return baseMapper.getSkuSaleAttrValueAsStringList(skuId);
    }

}