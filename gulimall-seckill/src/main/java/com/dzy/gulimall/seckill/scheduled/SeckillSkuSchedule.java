package com.dzy.gulimall.seckill.scheduled;

import com.dzy.gulimall.seckill.service.SeckillService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 *  秒杀商品的定时上架
 *      1、每天晚上3点，上架最近三天需要秒杀的商品
 */
@Slf4j
@Service
public class SeckillSkuSchedule {

    @Autowired
    SeckillService seckillService;

    @Scheduled(cron = "0 0 3 * * ?")
    public void uploadSeckillSkuLatest3Days() {
        //1.重复上架无须处理
        seckillService.uploadSeckillSkuLatest3Days();
    }
}