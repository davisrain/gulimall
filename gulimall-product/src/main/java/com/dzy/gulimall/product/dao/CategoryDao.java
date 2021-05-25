package com.dzy.gulimall.product.dao;

import com.dzy.gulimall.product.entity.CategoryEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品三级分类
 * 
 * @author zhengyu_dai
 * @email zhengyu_dai@foxmail.com
 * @date 2021-05-25 11:24:01
 */
@Mapper
public interface CategoryDao extends BaseMapper<CategoryEntity> {
	
}
