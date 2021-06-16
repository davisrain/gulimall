package com.dzy.gulimall.product.service.impl;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
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
        if(categoryId == 0) {
            if(StringUtils.isNotEmpty(key)) {
                page = this.page(new Query<AttrGroupEntity>().getPage(params),
                        new QueryWrapper<AttrGroupEntity>().and(obj -> {
                                    obj.eq("attr_group_id", key).or().like("attr_group_name", key);
                                }));
            } else {
                page = this.page(new Query<AttrGroupEntity>().getPage(params),
                        new QueryWrapper<AttrGroupEntity>());
            }
        } else {
            if(StringUtils.isNotEmpty(key)) {
                page = this.page(new Query<AttrGroupEntity>().getPage(params),
                        new QueryWrapper<AttrGroupEntity>().eq("catelog_id", categoryId)
                .and(obj -> {
                    obj.eq("attr_group_id", key).or().like("attr_group_name", key);
                }));
            } else {
                page = this.page(new Query<AttrGroupEntity>().getPage(params),
                        new QueryWrapper<AttrGroupEntity>().eq("catelog_id", categoryId));
            }
        }
        return new PageUtils(page);
    }

}