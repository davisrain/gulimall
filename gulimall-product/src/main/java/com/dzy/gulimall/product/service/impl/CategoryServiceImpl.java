package com.dzy.gulimall.product.service.impl;

import com.dzy.gulimall.product.service.CategoryBrandRelationService;
import com.dzy.gulimall.product.vo.Catalog2Vo;
import org.apache.commons.lang.StringUtils;
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

import com.dzy.gulimall.product.dao.CategoryDao;
import com.dzy.gulimall.product.entity.CategoryEntity;
import com.dzy.gulimall.product.service.CategoryService;
import org.springframework.transaction.annotation.Transactional;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    @Autowired
    CategoryBrandRelationService categoryBrandRelationService;

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

    @Override
    public Long[] getCatelogPath(Long catelogId) {
        List<Long> list = new ArrayList<>();
        findCatelogPath(catelogId, list);
        Collections.reverse(list);
        return list.toArray(new Long[0]);
    }

    public void findCatelogPath(Long catelogId, List<Long> list) {
        list.add(catelogId);
        CategoryEntity category = this.getById(catelogId);
        if(category != null && category.getParentCid() != 0) {
            findCatelogPath(category.getParentCid(), list);
        }
    }

    @Override
    @Transactional
    public void updateDetail(CategoryEntity category) {
        updateById(category);
        if(StringUtils.isNotEmpty(category.getName())) {
            categoryBrandRelationService.updateCategory(category.getCatId(), category.getName());
            //TODO 更新其他关联
        }
    }

    @Override
    public List<CategoryEntity> getLevel1Categories() {

       return baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", 0));
    }

    @Override
    public Map<String, List<Catalog2Vo>> getCatalogJson() {
        List<CategoryEntity> level1Categories = getLevel1Categories();
        Map<String, List<Catalog2Vo>> result = level1Categories.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            List<CategoryEntity> level2Categories = baseMapper.selectList(new QueryWrapper<CategoryEntity>()
                    .eq("parent_cid", v.getCatId()));
            return level2Categories.stream().map(level2Category -> {
                List<CategoryEntity> level3Categories = baseMapper.selectList(new QueryWrapper<CategoryEntity>()
                        .eq("parent_cid", level2Category.getCatId()));
                List<Catalog2Vo.Catalog3Vo> catalog3Vos = level3Categories.stream().map(level3Category ->
                        new Catalog2Vo.Catalog3Vo(level2Category.getCatId().toString(), level3Category.getCatId().toString(), level3Category.getName())
                ).collect(Collectors.toList());
                return new Catalog2Vo(v.getCatId().toString(), level2Category.getCatId().toString(), level2Category.getName(), catalog3Vos);
            }).collect(Collectors.toList());
        }));
        return result;
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