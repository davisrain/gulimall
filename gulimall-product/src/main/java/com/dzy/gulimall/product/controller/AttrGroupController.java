package com.dzy.gulimall.product.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.dzy.gulimall.product.entity.AttrEntity;
import com.dzy.gulimall.product.service.AttrService;
import com.dzy.gulimall.product.service.CategoryService;
import com.dzy.gulimall.product.vo.AttrAttrgroupRelationVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dzy.gulimall.product.entity.AttrGroupEntity;
import com.dzy.gulimall.product.service.AttrGroupService;
import com.dzy.common.utils.PageUtils;
import com.dzy.common.utils.R;



/**
 * 属性分组
 *
 * @author zhengyu_dai
 * @email zhengyu_dai@foxmail.com
 * @date 2021-05-25 11:24:02
 */
@RestController
@RequestMapping("product/attrgroup")
public class AttrGroupController {
    @Autowired
    private AttrGroupService attrGroupService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private AttrService attrService;
    /**
     * 列表
     */
    @RequestMapping("/list/{categoryId}")
    public R list(@RequestParam Map<String, Object> params, @PathVariable Long categoryId){
//        PageUtils page = attrGroupService.queryPage(params);
        PageUtils page = attrGroupService.queryPage(params, categoryId);
        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{attrGroupId}")
    //@RequiresPermissions("product:attrgroup:info")
    public R info(@PathVariable("attrGroupId") Long attrGroupId){
		AttrGroupEntity attrGroup = attrGroupService.getById(attrGroupId);
        Long[] catalogPath = categoryService.getCatelogPath(attrGroup.getCatelogId());
        attrGroup.setCatelogPath(catalogPath);
        return R.ok().put("attrGroup", attrGroup);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("product:attrgroup:save")
    public R save(@RequestBody AttrGroupEntity attrGroup){
		attrGroupService.save(attrGroup);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    //@RequiresPermissions("product:attrgroup:update")
    public R update(@RequestBody AttrGroupEntity attrGroup){
		attrGroupService.updateById(attrGroup);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("product:attrgroup:delete")
    public R delete(@RequestBody Long[] attrGroupIds){
		attrGroupService.removeByIds(Arrays.asList(attrGroupIds));

        return R.ok();
    }

    /**
     *  获取属性分组的关联的所有属性
     */
    @GetMapping("/{attrGroupId}/attr/relation")
    public R getAttrRelation(@PathVariable("attrGroupId") Long attrGroupId) {
        List<AttrEntity> attrs = attrService.getAttrsByAttrGroupId(attrGroupId);
        return R.ok().put("data", attrs);
    }

    /**
     *  添加属性与分组关联关系
     */
    @PostMapping("/attr/relation")
    public R addAttrRelation() {
        return R.ok();
    }

    /**
     *  删除属性与分组的关联关系
     */
    @PostMapping("/attr/relation/delete")
    public R deleteAttrRelations(@RequestBody List<AttrAttrgroupRelationVo> relationVos) {
        attrGroupService.deleteAttrRelations(relationVos);
        return R.ok();
    }

}
