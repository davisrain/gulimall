package com.dzy.gulimall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dzy.common.utils.PageUtils;
import com.dzy.gulimall.product.entity.AttrGroupEntity;
import com.dzy.gulimall.product.vo.AttrAttrgroupRelationVo;
import com.dzy.gulimall.product.vo.AttrGroupWithAttrsVo;
import com.dzy.gulimall.product.vo.SpuBaseAttrGroupVo;

import java.util.List;
import java.util.Map;

/**
 * 属性分组
 *
 * @author zhengyu_dai
 * @email zhengyu_dai@foxmail.com
 * @date 2021-05-25 11:24:02
 */
public interface AttrGroupService extends IService<AttrGroupEntity> {

    PageUtils queryPage(Map<String, Object> params);

    PageUtils queryPage(Map<String, Object> params, Long categoryId);

    void deleteAttrRelations(List<AttrAttrgroupRelationVo> relationVos);

    List<AttrGroupWithAttrsVo> getAttrGroupsWithAttrsByCatId(Long catelogId);

    List<SpuBaseAttrGroupVo> getAttrGroupsWithAttrsBySpuId(Long spuId, Long catalogId);
}

