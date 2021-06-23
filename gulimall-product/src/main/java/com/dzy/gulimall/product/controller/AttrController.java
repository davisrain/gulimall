package com.dzy.gulimall.product.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.dzy.gulimall.product.entity.ProductAttrValueEntity;
import com.dzy.gulimall.product.service.ProductAttrValueService;
import com.dzy.gulimall.product.vo.AttrRespVo;
import com.dzy.gulimall.product.vo.AttrVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dzy.gulimall.product.entity.AttrEntity;
import com.dzy.gulimall.product.service.AttrService;
import com.dzy.common.utils.PageUtils;
import com.dzy.common.utils.R;



/**
 * 商品属性
 *
 * @author zhengyu_dai
 * @email zhengyu_dai@foxmail.com
 * @date 2021-05-25 11:24:03
 */
@RestController
@RequestMapping("product/attr")
public class AttrController {
    @Autowired
    private AttrService attrService;

    @Autowired
    ProductAttrValueService productAttrValueService;

    @GetMapping("/{attrType}/list/{catelogId}")
    public R attrBaseList(@RequestParam Map<String, Object> params,
                          @PathVariable("catelogId") Long catelogId,
                          @PathVariable("attrType") String attrType) {
        PageUtils page = attrService.queryAttrPage(params, catelogId, attrType);
        return R.ok().put("page", page);
    }

    /**
     * 列表
     */
    @RequestMapping("/list")
    //@RequiresPermissions("product:attr:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = attrService.queryPage(params);

        return R.ok().put("page", page);
    }

    /**
     *  获取spu规格
     */
    @GetMapping("/base/listforspu/{spuId}")
    public R listForSpu(@PathVariable("spuId") Long spuId) {
        List<ProductAttrValueEntity> attrValues =  productAttrValueService.listForSpu(spuId);
        return R.ok().put("data", attrValues);
    }
    /**
     * 信息
     */
    @RequestMapping("/info/{attrId}")
    public R info(@PathVariable("attrId") Long attrId){
		AttrRespVo attr = attrService.getAttrInfo(attrId);

        return R.ok().put("attr", attr);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@RequestBody AttrVo attr){
		attrService.saveAttr(attr);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody AttrVo attr){
		attrService.updateAttr(attr);
        return R.ok();
    }

    /**
     *  修改商品规格
     */
    @PostMapping("/update/{spuId}")
    public R updateAttrValueBySpuId(@PathVariable("spuId") Long spuId,
                                    @RequestBody List<ProductAttrValueEntity> attrValues) {
        productAttrValueService.updateAttrValueBySpuId(spuId, attrValues);
        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("product:attr:delete")
    public R delete(@RequestBody Long[] attrIds){
		attrService.removeAttrs(Arrays.asList(attrIds));
        return R.ok();
    }

}
