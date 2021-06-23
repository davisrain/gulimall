package com.dzy.gulimall.product.service.impl;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dzy.common.utils.PageUtils;
import com.dzy.common.utils.Query;

import com.dzy.gulimall.product.dao.ProductAttrValueDao;
import com.dzy.gulimall.product.entity.ProductAttrValueEntity;
import com.dzy.gulimall.product.service.ProductAttrValueService;
import org.springframework.transaction.annotation.Transactional;


@Service("productAttrValueService")
public class ProductAttrValueServiceImpl extends ServiceImpl<ProductAttrValueDao, ProductAttrValueEntity> implements ProductAttrValueService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<ProductAttrValueEntity> page = this.page(
                new Query<ProductAttrValueEntity>().getPage(params),
                new QueryWrapper<ProductAttrValueEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<ProductAttrValueEntity> listForSpu(Long spuId) {
        List<ProductAttrValueEntity> attrValues = baseMapper.selectList(new QueryWrapper<ProductAttrValueEntity>().eq("spu_id", spuId));
        return attrValues;
    }

    /**
     *  修改商品规格
     */
    @Override
    @Transactional
    public void updateAttrValueBySpuId(Long spuId, List<ProductAttrValueEntity> attrValues) {
        //1、先将spu对应的属性删除
        baseMapper.delete(new QueryWrapper<ProductAttrValueEntity>().eq("spu_id", spuId));
        //2、再将属性进行插入
        attrValues.forEach(attrValue -> {
            attrValue.setSpuId(spuId);
        });
        this.saveBatch(attrValues);
    }

}