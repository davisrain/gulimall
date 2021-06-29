package com.dzy.gulimall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dzy.common.utils.PageUtils;
import com.dzy.gulimall.product.entity.AttrEntity;
import com.dzy.gulimall.product.vo.AttrRespVo;
import com.dzy.gulimall.product.vo.AttrVo;

import java.util.List;
import java.util.Map;

/**
 * 商品属性
 *
 * @author zhengyu_dai
 * @email zhengyu_dai@foxmail.com
 * @date 2021-05-25 11:24:03
 */
public interface AttrService extends IService<AttrEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void saveAttr(AttrVo attr);

    PageUtils queryAttrPage(Map<String, Object> params, Long catelogId, String attrType);

    AttrRespVo getAttrInfo(Long attrId);

    void updateAttr(AttrVo attr);

    void removeAttrs(List<Long> attrIds);

    List<AttrEntity> getRelationAttrsByAttrGroupId(Long attrGroupId);

    PageUtils getNoRelationAttrsByAttrGroupId(Long attrGroupId, Map<String, Object> params);

    List<Long> getSearchAttrIdsByIds(List<Long> attrIds);
}

