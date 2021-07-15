package com.dzy.gulimall.product.service.impl;

import com.dzy.gulimall.product.dao.AttrAttrgroupRelationDao;
import com.dzy.gulimall.product.entity.AttrEntity;
import com.dzy.gulimall.product.service.AttrService;
import com.dzy.gulimall.product.vo.AttrAttrgroupRelationVo;
import com.dzy.gulimall.product.vo.AttrGroupWithAttrsVo;
import com.dzy.gulimall.product.vo.SpuBaseAttrGroupVo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    @Autowired
    AttrService attrService;

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

    /**
     *  获取分类下所有分组&关联属性
     * @param catelogId Long
     * @return List<AttrGroupWithAttrsVo>
     */
    @Override
    public List<AttrGroupWithAttrsVo> getAttrGroupsWithAttrsByCatId(Long catelogId) {
        List<AttrGroupEntity> attrGroups = list(new QueryWrapper<AttrGroupEntity>().eq("catelog_id", catelogId));
        List<AttrGroupWithAttrsVo> attrGroupsWithAttrs = attrGroups.stream().map(attrGroup -> {
            AttrGroupWithAttrsVo attrGroupWithAttrs = new AttrGroupWithAttrsVo();
            BeanUtils.copyProperties(attrGroup, attrGroupWithAttrs);
            List<AttrEntity> attrs = attrService.getRelationAttrsByAttrGroupId(attrGroup.getAttrGroupId());
            attrGroupWithAttrs.setAttrs(attrs);
            return attrGroupWithAttrs;
        }).collect(Collectors.toList());
        return attrGroupsWithAttrs;
    }

    @Override
    public List<SpuBaseAttrGroupVo> getAttrGroupsWithAttrsBySpuId(Long spuId, Long catalogId) {

        return baseMapper.getAttrGroupsWithAttrsBySpuId(spuId, catalogId);
    }

}