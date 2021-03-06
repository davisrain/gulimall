package com.dzy.gulimall.product.controller;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dzy.gulimall.product.entity.SkuInfoEntity;
import com.dzy.gulimall.product.service.SkuInfoService;
import com.dzy.common.utils.PageUtils;
import com.dzy.common.utils.R;



/**
 * sku信息
 *
 * @author zhengyu_dai
 * @email zhengyu_dai@foxmail.com
 * @date 2021-05-25 11:24:02
 */
@RestController
@RequestMapping("product/skuinfo")
public class  SkuInfoController {
    @Autowired
    private SkuInfoService skuInfoService;

    /**
     *  根据skuId获取最新的价格
     */
    @GetMapping("/{skuId}/price")
    public BigDecimal getNewlyPriceBySkuId(@PathVariable("skuId") Long skuId) {
        return skuInfoService.getNewlyPriceBySkuId(skuId);
    }

    /**
     * 列表
     */
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = skuInfoService.queryPageByCondition(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{skuId}")
    public R info(@PathVariable("skuId") Long skuId){
		SkuInfoEntity skuInfo = skuInfoService.getById(skuId);

        return R.ok().put("skuInfo", skuInfo);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("product:skuinfo:save")
    public R save(@RequestBody SkuInfoEntity skuInfo){
		skuInfoService.save(skuInfo);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    //@RequiresPermissions("product:skuinfo:update")
    public R update(@RequestBody SkuInfoEntity skuInfo){
		skuInfoService.updateById(skuInfo);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("product:skuinfo:delete")
    public R delete(@RequestBody Long[] skuIds){
		skuInfoService.removeByIds(Arrays.asList(skuIds));

        return R.ok();
    }

    @GetMapping("/skuspu/info/{skuId}")
    public R getSkuInfoWithSpuInfo(@PathVariable("skuId") Long skuId) {
        SkuInfoEntity info = skuInfoService.getSkuInfoWithSpuInfo(skuId);
        return R.ok().setData(info);
    }

}
