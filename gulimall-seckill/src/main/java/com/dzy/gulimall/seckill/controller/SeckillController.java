package com.dzy.gulimall.seckill.controller;

import com.dzy.common.utils.R;
import com.dzy.gulimall.seckill.service.SeckillService;
import com.dzy.gulimall.seckill.to.SeckillSkuRedisTo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SeckillController {

    @Autowired
    SeckillService seckillService;

    @GetMapping("/currentSeckillSkus")
    public R getCurrentSeckillSkus() {
        List<SeckillSkuRedisTo> skus = seckillService.getCurrentSeckillSkus();
        return R.ok().setData(skus);
    }

    @GetMapping("/sku/seckill/{skuId}")
    public R getSeckillSkuInfo(@PathVariable("skuId") Long skuId) {
        SeckillSkuRedisTo skuRedisTo = seckillService.getSeckillSkuInfo(skuId);
        return R.ok().setData(skuRedisTo);
    }

    @GetMapping("/seckill")
    public R seckill(@RequestParam("seckillId") String seckillId,
                     @RequestParam("num") Integer num,
                     @RequestParam("code") String code) {
        return null;
    }
}
