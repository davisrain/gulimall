package com.dzy.gulimall.seckill.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.dzy.common.utils.R;
import com.dzy.gulimall.seckill.feign.CouponFeignService;
import com.dzy.gulimall.seckill.feign.ProductFeignService;
import com.dzy.gulimall.seckill.service.SeckillService;
import com.dzy.gulimall.seckill.to.SeckillSkuRedisTo;
import com.dzy.gulimall.seckill.vo.SeckillSessionWithSkus;
import com.dzy.gulimall.seckill.vo.SeckillSkuVo;
import com.dzy.gulimall.seckill.vo.SkuInfoVo;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
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

    @Override
    public void uploadSeckillSkuLatest3Days() {
        //TODO 幂等性问题，防止重复上架
        //1.远程查询最近三天的秒杀活动场次
        R r = couponFeignService.latest3DaysSession();
        if(r.getCode() == 0) {
            //开始上架
            List<SeckillSessionWithSkus> sessions = r.getData(new TypeReference<List<SeckillSessionWithSkus>>(){});
            //2.缓存活动场次信息
            saveSeckillSessions(sessions);
            //3.缓存秒杀商品信息
            saveSeckillSkus(sessions);
        }
    }


    private void saveSeckillSessions(List<SeckillSessionWithSkus> sessions) {
        sessions.forEach(session -> {
            long startTime = session.getStartTime().getTime();
            long endTime = session.getEndTime().getTime();
            //session的key使用 前缀 + 起始时间的毫秒数 _ 结束时间的毫秒数
            String key = SESSIONS_CACHE_PREFIX + startTime + "_" + endTime;
            //session的value使用skuId的集合
            List<String> skuIds = session.getRelationSkus().stream().map(sku -> sku.getSkuId().toString()).collect(Collectors.toList());
            redisTemplate.opsForList().leftPushAll(key, skuIds);
        });
    }

    private void saveSeckillSkus(List<SeckillSessionWithSkus> sessions) {
        BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(SKUS_CACHE_PREFIX);
        sessions.forEach(session -> {
            session.getRelationSkus().forEach(relationSku -> {
                SeckillSkuRedisTo redisTo = new SeckillSkuRedisTo();
                //1.保存sku的基本信息
                    //远程调用商品服务查询sku的基本信息
                R r = productFeignService.getSkuInfo(relationSku.getSkuId());
                if(r.getCode() == 0) {
                    SkuInfoVo skuInfo = r.getData("skuInfo", new TypeReference<SkuInfoVo>() {});
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
                hashOps.put(redisTo.getSkuId(), redisTo);
            });
        });
    }
}
