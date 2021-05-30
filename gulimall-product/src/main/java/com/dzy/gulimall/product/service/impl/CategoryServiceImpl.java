package com.dzy.gulimall.product.service.impl;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dzy.common.utils.PageUtils;
import com.dzy.common.utils.Query;

import com.dzy.gulimall.product.dao.CategoryDao;
import com.dzy.gulimall.product.entity.CategoryEntity;
import com.dzy.gulimall.product.service.CategoryService;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<CategoryEntity> listByTree() {
        //1、查出所有分类
        List<CategoryEntity> categoryEntities = baseMapper.selectList(null);
        //2、组装树形结构
        List<CategoryEntity> categories = categoryEntities.stream().filter(category -> category.getParentCid() == 0)
                .map(category -> {
                    category.setSubCategories(getSubCategories(category, categoryEntities));
                    return category;
                }).sorted((category1, category2) -> (category1.getSort() == null? 0 : category1.getSort()) - (category2.getSort() == null? 0: category2.getSort()))
                .collect(Collectors.toList());
        return categories;
    }

    @Override
    public int removeCategoriesByIds(List<Long> catIds) {
        //TODO 检查当前删除的分类，是否被别的地方引用
        return baseMapper.deleteBatchIds(catIds);
    }

    private List<CategoryEntity> getSubCategories(CategoryEntity root, List<CategoryEntity> all) {
        return all.stream().filter(category -> category.getParentCid().equals(root.getCatId()))
                .map(category -> {
                    category.setSubCategories(getSubCategories(category, all));
                    return category;
                }).sorted((category1, category2) -> (category1.getSort() == null? 0 : category1.getSort()) - (category2.getSort() == null? 0: category2.getSort()))
                .collect(Collectors.toList());
    }

}