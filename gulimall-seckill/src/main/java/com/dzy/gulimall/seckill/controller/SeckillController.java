package com.dzy.gulimall.seckill.controller;

import com.dzy.common.utils.R;
import com.dzy.gulimall.seckill.service.SeckillService;
import com.dzy.gulimall.seckill.to.SeckillSkuRedisTo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
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
}
