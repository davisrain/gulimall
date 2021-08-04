package com.dzy.gulimall.seckill.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.dzy.common.utils.R;
import com.dzy.gulimall.seckill.feign.CouponFeignService;
import com.dzy.gulimall.seckill.feign.ProductFeignService;
import com.dzy.gulimall.seckill.service.SeckillService;
import com.dzy.gulimall.seckill.to.SeckillSkuRedisTo;
import com.dzy.gulimall.seckill.vo.SeckillSessionWithSkus;
import com.dzy.gulimall.seckill.vo.SeckillSkuVo;
import com.dzy.gulimall.seckill.vo.SkuInfoVo;
import org.redisson.api.RLock;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class SeckillServiceImpl implements SeckillService {

    @Autowired
    CouponFeignService couponFeignService;

    @Autowired
    ProductFeignService productFeignService;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    RedissonClient redissonClient;

    private final String SESSIONS_CACHE_PREFIX = "seckill:sessions:";
    private final String SKUS_CACHE_PREFIX = "seckill:skus";
    private final String STOCK_CACHE_PREFIX ="seckill:stock:";
    private final String SECKILL_UPLOAD_LOCK = "seckill:upload:lock";

    @Override
    public void uploadSeckillSkuLatest3Days() {
        //添加分布式锁，防止分布式的情况下，多个定时任务同时执行，导致秒杀商品上架多次
        RLock lock = redissonClient.getLock(SECKILL_UPLOAD_LOCK);
        lock.lock(10, TimeUnit.SECONDS);
        try {
            //1.远程查询最近三天的秒杀活动场次
            R r = couponFeignService.latest3DaysSession();
            if (r.getCode() == 0) {
                //开始上架
                List<SeckillSessionWithSkus> sessions = r.getData(new TypeReference<List<SeckillSessionWithSkus>>() {
                });
                //2.缓存活动场次信息
                saveSeckillSessions(sessions);
                //3.缓存秒杀商品信息
                saveSeckillSkus(sessions);
            }
        } finally {
            lock.unlock();
        }
    }


    private void saveSeckillSessions(List<SeckillSessionWithSkus> sessions) {
        sessions.forEach(session -> {
            long startTime = session.getStartTime().getTime();
            long endTime = session.getEndTime().getTime();
            //session的key使用 前缀 + 起始时间的毫秒数 _ 结束时间的毫秒数
            String key = SESSIONS_CACHE_PREFIX + startTime + "_" + endTime;
            //保证幂等性
            if(!redisTemplate.hasKey(key)) {
                //session的value使用sessionId_skuId的集合，拼上sessionId是为了区分每个场次下的不同的sku
                List<String> skuIds = session.getRelationSkus().stream()
                        .map(sku -> sku.getPromotionSessionId().toString() + "_" + sku.getSkuId().toString())
                        .collect(Collectors.toList());
                redisTemplate.opsForList().leftPushAll(key, skuIds);
            }
        });
    }

    private void saveSeckillSkus(List<SeckillSessionWithSkus> sessions) {
        BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(SKUS_CACHE_PREFIX);
        sessions.forEach(session -> {
            session.getRelationSkus().forEach(relationSku -> {
                //使用sessionId_skuId作为hash的key，为了区分不同场次下的sku
                String key = relationSku.getPromotionSessionId() + "_" + relationSku.getSkuId();
                //保证幂等性
                if(!hashOps.hasKey(key)) {
                    SeckillSkuRedisTo redisTo = new SeckillSkuRedisTo();
                    //1.保存sku的基本信息
                    //远程调用商品服务查询sku的基本信息
                    R r = productFeignService.getSkuInfo(relationSku.getSkuId());
                    if (r.getCode() == 0) {
                        SkuInfoVo skuInfo = r.getData("skuInfo", new TypeReference<SkuInfoVo>() {
                        });
                        redisTo.setSkuInfoVo(skuInfo);
                    }
                    //2.保存sku的秒杀信息
                    BeanUtils.copyProperties(relationSku, redisTo);
                    //3.设置当前商品的秒杀时间信息
                    redisTo.setStartTime(session.getStartTime().getTime());
                    redisTo.setEndTime(session.getEndTime().getTime());
                    //4.设置商品的随机码
                    String randomCode = UUID.randomUUID().toString().replace("-", "");
                    redisTo.setRandomCode(randomCode);
                    //5.用商品的秒杀库存作为分布式的信号量，限流
                    RSemaphore semaphore = redissonClient.getSemaphore(STOCK_CACHE_PREFIX + randomCode);
                    //商品可以秒杀的数量作为信号量
                    semaphore.trySetPermits(redisTo.getSeckillCount().intValue());
                    String redisJson = JSON.toJSONString(redisTo);
                    hashOps.put(key, redisJson);
                }
            });
        });
    }
}
