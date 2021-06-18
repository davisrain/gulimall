package com.dzy.gulimall.product.dao;

import com.dzy.gulimall.product.entity.AttrAttrgroupRelationEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dzy.gulimall.product.vo.AttrAttrgroupRelationVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 属性&属性分组关联
 * 
 * @author zhengyu_dai
 * @email zhengyu_dai@foxmail.com
 * @date 2021-05-25 11:24:02
 */
@Mapper
public interface AttrAttrgroupRelationDao extends BaseMapper<AttrAttrgroupRelationEntity> {

    void deleteBatchByAttrIds(@Param("attrIds") List<Long> attrIds);

    void deleteBatchByRelations(@Param("relationVos") List<AttrAttrgroupRelationVo> relationVos);
}
