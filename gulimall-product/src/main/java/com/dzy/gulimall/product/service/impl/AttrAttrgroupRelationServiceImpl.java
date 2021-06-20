package com.dzy.gulimall.product.service.impl;

import com.dzy.gulimall.product.vo.AttrAttrgroupRelationVo;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dzy.common.utils.PageUtils;
import com.dzy.common.utils.Query;

import com.dzy.gulimall.product.dao.AttrAttrgroupRelationDao;
import com.dzy.gulimall.product.entity.AttrAttrgroupRelationEntity;
import com.dzy.gulimall.product.service.AttrAttrgroupRelationService;


@Service("attrAttrgroupRelationService")
public class AttrAttrgroupRelationServiceImpl extends ServiceImpl<AttrAttrgroupRelationDao, AttrAttrgroupRelationEntity> implements AttrAttrgroupRelationService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrAttrgroupRelationEntity> page = this.page(
                new Query<AttrAttrgroupRelationEntity>().getPage(params),
                new QueryWrapper<AttrAttrgroupRelationEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void saveBatch(List<AttrAttrgroupRelationVo> relationVos) {
        List<AttrAttrgroupRelationEntity> relations = relationVos.stream().map(relationVo -> {
            AttrAttrgroupRelationEntity relation = new AttrAttrgroupRelationEntity();
            relation.setAttrId(relationVo.getAttrId());
            relation.setAttrGroupId(relationVo.getAttrGroupId());
            return relation;
        }).collect(Collectors.toList());
        saveBatch(relations);
    }

}