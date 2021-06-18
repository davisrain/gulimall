package com.dzy.gulimall.product.service.impl;

import com.dzy.gulimall.product.dao.AttrAttrgroupRelationDao;
import com.dzy.gulimall.product.vo.AttrAttrgroupRelationVo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dzy.common.utils.PageUtils;
import com.dzy.common.utils.Query;

import com.dzy.gulimall.product.dao.AttrGroupDao;
import com.dzy.gulimall.product.entity.AttrGroupEntity;
import com.dzy.gulimall.product.service.AttrGroupService;


@Service("attrGroupService")
public class AttrGroupServiceImpl extends ServiceImpl<AttrGroupDao, AttrGroupEntity> implements AttrGroupService {

    @Autowired
    AttrAttrgroupRelationDao attrAttrgroupRelationDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrGroupEntity> page = this.page(
                new Query<AttrGroupEntity>().getPage(params),
                new QueryWrapper<AttrGroupEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params, Long categoryId) {
        IPage<AttrGroupEntity> page;
        String key = (String) params.get("key");
        QueryWrapper<AttrGroupEntity> wrapper = new QueryWrapper<AttrGroupEntity>();
        if(StringUtils.isNotEmpty(key)) {
            wrapper.and(obj -> {
                obj.eq("attr_group_id", key).or().like("attr_group_name", key);
            });
        }
        if(categoryId == 0) {
            page = this.page(new Query<AttrGroupEntity>().getPage(params),
                        wrapper);
        } else {
            page = this.page(new Query<AttrGroupEntity>().getPage(params),
                        wrapper.eq("catelog_id", categoryId));
        }
        return new PageUtils(page);
    }

    @Override
    public void deleteAttrRelations(List<AttrAttrgroupRelationVo> relationVos) {
        attrAttrgroupRelationDao.deleteBatchByRelations(relationVos);
    }

}