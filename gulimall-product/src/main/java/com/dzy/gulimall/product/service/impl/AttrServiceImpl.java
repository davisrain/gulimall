package com.dzy.gulimall.product.service.impl;

import com.dzy.common.constant.ProductConstant;
import com.dzy.gulimall.product.dao.AttrAttrgroupRelationDao;
import com.dzy.gulimall.product.dao.AttrGroupDao;
import com.dzy.gulimall.product.dao.CategoryDao;
import com.dzy.gulimall.product.entity.AttrAttrgroupRelationEntity;
import com.dzy.gulimall.product.entity.AttrGroupEntity;
import com.dzy.gulimall.product.entity.CategoryEntity;
import com.dzy.gulimall.product.vo.AttrRespVo;
import com.dzy.gulimall.product.vo.AttrVo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dzy.common.utils.PageUtils;
import com.dzy.common.utils.Query;

import com.dzy.gulimall.product.dao.AttrDao;
import com.dzy.gulimall.product.entity.AttrEntity;
import com.dzy.gulimall.product.service.AttrService;
import org.springframework.transaction.annotation.Transactional;


@Service("attrService")
public class AttrServiceImpl extends ServiceImpl<AttrDao, AttrEntity> implements AttrService {

    @Autowired
    AttrAttrgroupRelationDao attrAttrgroupRelationDao;

    @Autowired
    AttrGroupDao attrGroupDao;

    @Autowired
    CategoryDao categoryDao;

    @Autowired
    CategoryServiceImpl categoryService;


    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrEntity> page = this.page(
                new Query<AttrEntity>().getPage(params),
                new QueryWrapper<AttrEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    @Transactional
    public void saveAttr(AttrVo attr) {
        AttrEntity attrEntity = new AttrEntity();
        BeanUtils.copyProperties(attr, attrEntity);
        save(attrEntity);
        Long attrGroupId = attr.getAttrGroupId();
        if(ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode() == attr.getAttrType() && attrGroupId != null) {
            AttrAttrgroupRelationEntity attrAttrgroupRelation = new AttrAttrgroupRelationEntity();
            attrAttrgroupRelation.setAttrGroupId(attrGroupId);
            attrAttrgroupRelation.setAttrId(attrEntity.getAttrId());
            attrAttrgroupRelationDao.insert(attrAttrgroupRelation);
        }
    }

    @Override
    public PageUtils queryAttrPage(Map<String, Object> params, Long catelogId, String attrType) {
        QueryWrapper<AttrEntity> wrapper = new QueryWrapper<AttrEntity>()
                .eq("attr_type",
                        attrType.equalsIgnoreCase(ProductConstant.AttrEnum.ATTR_TYPE_BASE.getMsg())?
                                ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode()
                                :ProductConstant.AttrEnum.ATTR_TYPE_SALE.getCode());
        if(catelogId != 0) {
            wrapper.eq("catelog_id", catelogId);
        }
        String key = (String) params.get("key");
        if(StringUtils.isNotBlank(key)) {
            wrapper.and(obj -> {
                obj.eq("attr_id", key).or().like("attr_name", key);
            });
        }
        IPage<AttrEntity> page = this.page(new Query<AttrEntity>().getPage(params), wrapper);
        List<AttrEntity> records = page.getRecords();
        List<AttrRespVo> attrRespVos = records.stream().map(attrEntity -> {
            AttrRespVo attrRespVo = new AttrRespVo();
            BeanUtils.copyProperties(attrEntity, attrRespVo);
            if(attrType.equalsIgnoreCase(ProductConstant.AttrEnum.ATTR_TYPE_BASE.getMsg())) {
                AttrAttrgroupRelationEntity attrAttrgroupRelation = attrAttrgroupRelationDao.selectOne(new QueryWrapper<AttrAttrgroupRelationEntity>()
                        .eq("attr_id", attrEntity.getAttrId()));
                if (attrAttrgroupRelation != null) {
                    AttrGroupEntity attrGroup = attrGroupDao.selectById(attrAttrgroupRelation.getAttrGroupId());
                    attrRespVo.setGroupName(attrGroup.getAttrGroupName());
                }
            }
            CategoryEntity category = categoryDao.selectById(attrEntity.getCatelogId());
            if (category != null) {
                attrRespVo.setCatelogName(category.getName());
            }
            return attrRespVo;
        }).collect(Collectors.toList());
        PageUtils pageUtils = new PageUtils(page);
        pageUtils.setList(attrRespVos);
        return pageUtils;
    }

    @Override
    public AttrRespVo getAttrInfo(Long attrId) {
        AttrEntity attrEntity = baseMapper.selectById(attrId);
        AttrRespVo attrRespVo = new AttrRespVo();
        BeanUtils.copyProperties(attrEntity, attrRespVo);
        //获取catelogPath
        CategoryEntity category = categoryDao.selectById(attrEntity.getCatelogId());
        List<Long> list = new ArrayList<Long>();
        if(category != null)
            categoryService.findCatelogPath(category.getCatId(), list);
        Collections.reverse(list);
        attrRespVo.setCatelogPath(list.toArray(new Long[0]));
        if(ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode() == attrEntity.getAttrType()) {
            //获取attrGroupId
            AttrAttrgroupRelationEntity attrAttrgroupRelation = attrAttrgroupRelationDao.selectOne(
                    new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attrId));
            attrRespVo.setAttrGroupId(attrAttrgroupRelation != null ? attrAttrgroupRelation.getAttrGroupId() : null);
        }
        return attrRespVo;
    }

    @Transactional
    @Override
    public void updateAttr(AttrVo attr) {
        AttrEntity attrEntity = new AttrEntity();
        BeanUtils.copyProperties(attr, attrEntity);
        updateById(attrEntity);
        Long attrGroupId = attr.getAttrGroupId();
        Long attrId = attr.getAttrId();
        if(ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode() == attr.getAttrType() && attrGroupId != null) {
            AttrAttrgroupRelationEntity oriAttrAttrgroupRel = attrAttrgroupRelationDao.selectOne(
                    new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attrId));
            if (oriAttrAttrgroupRel == null) {
                AttrAttrgroupRelationEntity attrAttrgroupRel = new AttrAttrgroupRelationEntity();
                attrAttrgroupRel.setAttrId(attrId);
                attrAttrgroupRel.setAttrGroupId(attrGroupId);
                attrAttrgroupRelationDao.insert(attrAttrgroupRel);
            } else {
                if (!oriAttrAttrgroupRel.getAttrGroupId().equals(attrGroupId)) {
                    oriAttrAttrgroupRel.setAttrGroupId(attrGroupId);
                    attrAttrgroupRelationDao.updateById(oriAttrAttrgroupRel);
                }
            }
        }
    }

    @Override
    @Transactional
    public void removeAttrs(List<Long> attrIds) {
        removeByIds(attrIds);
        attrAttrgroupRelationDao.deleteBatchByAttrIds(attrIds);
    }

    @Override
    public List<AttrEntity> getAttrsByAttrGroupId(Long attrGroupId) {
        List<AttrAttrgroupRelationEntity> relations = attrAttrgroupRelationDao.selectList(
                new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_group_id", attrGroupId));
        List<Long> attrIds = relations.stream().map(AttrAttrgroupRelationEntity::getAttrId).collect(Collectors.toList());
        return baseMapper.selectBatchIds(attrIds);
    }

}