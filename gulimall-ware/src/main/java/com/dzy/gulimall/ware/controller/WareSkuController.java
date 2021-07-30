package com.dzy.gulimall.ware.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.dzy.common.exception.BizCodeEnum;
import com.dzy.gulimall.ware.exception.NoStockException;
import com.dzy.gulimall.ware.to.WareLockTo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dzy.gulimall.ware.entity.WareSkuEntity;
import com.dzy.gulimall.ware.service.WareSkuService;
import com.dzy.common.utils.PageUtils;
import com.dzy.common.utils.R;



/**
 * 商品库存
 *
 * @author zhengyu_dai
 * @email zhengyu_dai@foxmail.com
 * @date 2021-05-24 23:29:19
 */
@RestController
@RequestMapping("ware/waresku")
public class WareSkuController {
    @Autowired
    private WareSkuService wareSkuService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = wareSkuService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    //@RequiresPermissions("ware:waresku:info")
    public R info(@PathVariable("id") Long id){
		WareSkuEntity wareSku = wareSkuService.getById(id);

        return R.ok().put("wareSku", wareSku);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("ware:waresku:save")
    public R save(@RequestBody WareSkuEntity wareSku){
		wareSkuService.save(wareSku);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    //@RequiresPermissions("ware:waresku:update")
    public R update(@RequestBody WareSkuEntity wareSku){
		wareSkuService.updateById(wareSku);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("ware:waresku:delete")
    public R delete(@RequestBody Long[] ids){
		wareSkuService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

    /**
     * 根据skuIds查询该sku是否有库存
     */
    @PostMapping("/hasStock")
    public R getHasStockBySkuIds(@RequestBody List<Long> skuIds) {
        return R.ok().setData(wareSkuService.getHasStockBySkuIds(skuIds));
    }

    /**
     *  锁定库存
     */
    @PostMapping("/lock")
    public R lockStock(@RequestBody WareLockTo wareLockTo) {
        try {
            wareSkuService.lockStock(wareLockTo);
            return R.ok();
        } catch (NoStockException e) {
            return R.error(BizCodeEnum.NO_STOCK_EXCEPTION);
        } catch (Exception e) {
            return R.error(BizCodeEnum.WARE_UNKNOWN_EXCEPTION);
        }
    }

}
