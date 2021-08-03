package com.dzy.gulimall.seckill.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.dzy.common.utils.R;
import com.dzy.gulimall.seckill.feign.CouponFeignService;
import com.dzy.gulimall.seckill.feign.ProductFeignService;
import com.dzy.gulimall.seckill.service.SeckillService;
import com.dzy.gulimall.seckill.vo.SeckillSessionWithSkus;
import com.dzy.gulimall.seckill.vo.SkuInfoVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SeckillServiceImpl implements SeckillService {

    @Autowired
    CouponFeignService couponFeignService;

    @Autowired
    ProductFeignService productFeignService;

    @Override
    public void uploadSeckillSkuLatest3Days() {
        //1.远程查询最近三天的秒杀活动场次
        R r = couponFeignService.latest3DaysSession();
        if(r.getCode() == 0) {
            //开始上架
            List<SeckillSessionWithSkus> sessions = r.getData(new TypeReference<List<SeckillSessionWithSkus>>(){});
            //1.缓存活动场次信息
            //2.缓存秒杀商品信息
            //3.

        }
    }
}
