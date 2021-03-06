package com.dzy.gulimall.seckill.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.dzy.common.to.mq.SeckillOrderTo;
import com.dzy.common.utils.R;
import com.dzy.common.vo.UserRespVo;
import com.dzy.gulimall.seckill.feign.CouponFeignService;
import com.dzy.gulimall.seckill.feign.ProductFeignService;
import com.dzy.gulimall.seckill.interceptor.LoginInterceptor;
import com.dzy.gulimall.seckill.service.SeckillService;
import com.dzy.gulimall.seckill.to.SeckillSkuRedisTo;
import com.dzy.gulimall.seckill.vo.SeckillSessionWithSkus;
import com.dzy.gulimall.seckill.vo.SeckillSkuVo;
import com.dzy.gulimall.seckill.vo.SkuInfoVo;
import org.redisson.api.RLock;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.sql.Time;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;
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
    RabbitTemplate rabbitTemplate;

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

    /**
     * 查询当前时间可以秒杀的商品
     */
    @Override
    public List<SeckillSkuRedisTo> getCurrentSeckillSkus() {
        //1.查询当前时间的秒杀场次
        long currentTime = new Date().getTime();
        Set<String> keys = redisTemplate.keys(SESSIONS_CACHE_PREFIX + "*");
        for (String key : keys) {
            String[] timeRange = key.replace(SESSIONS_CACHE_PREFIX, "").split("_");
            if(currentTime >= Long.parseLong(timeRange[0]) && currentTime <= Long.parseLong(timeRange[1])) {
                //2.查询该场次所包含的所有sku
                List<String> skuKeys = redisTemplate.opsForList().range(key, 0, -1);
                BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SKUS_CACHE_PREFIX);
                List<String> skuValues = hashOps.multiGet(skuKeys);
                List<SeckillSkuRedisTo> redisTos = skuValues.stream().map(skuValue ->
                    JSON.parseObject(skuValue, SeckillSkuRedisTo.class)
                ).collect(Collectors.toList());
                return redisTos;
            }
        }
        return null;
    }

    /**
     *  根据skuId查询参与秒杀的sku信息
     */
    @Override
    public SeckillSkuRedisTo getSeckillSkuInfo(Long skuId) {
        List<SeckillSkuRedisTo> skuRedisTos = new ArrayList<>();
        //1.查询到所有带skuId的sku信息
        BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SKUS_CACHE_PREFIX);
        Set<String> keys = hashOps.keys();
        if(keys != null && keys.size() > 0) {
            String regExp = "\\d+_" + skuId;
            for (String key : keys) {
                if(key.matches(regExp)) {
                    String value = hashOps.get(key);
                    SeckillSkuRedisTo skuRedisTo = JSON.parseObject(value, SeckillSkuRedisTo.class);
                    skuRedisTos.add(skuRedisTo);
                }
            }
        }
        //2.遍历sku集合，找到结束时间大于当前时间，且开始时间最早的sku信息，进行返回
        long currentTime = new Date().getTime();
        List<SeckillSkuRedisTo> sortedSkus = skuRedisTos.stream().filter(skuRedisTo -> skuRedisTo.getEndTime() > currentTime).
                sorted(Comparator.comparingLong(SeckillSkuRedisTo::getStartTime)).collect(Collectors.toList());
        if(sortedSkus.size() > 0) {
            SeckillSkuRedisTo seckillSku = sortedSkus.get(0);
            //3.判断当前是否处于秒杀时间中，如果不是，隐藏随机码
            if(currentTime >= seckillSku.getStartTime()){
                //ignore
            } else {
                seckillSku.setRandomCode(null);
            }
            return seckillSku;
        }
        return null;
    }

    /**
     *  秒杀方法
     *  TODO 1.秒杀上架的时候，每个商品设置一个过期信息，并且需要锁定库存
     *  TODO 2.秒杀后续的流程，简化了收货地址等信息，以及库存解锁和关闭订单等操作
     */
    @Override
    public String seckill(String seckillId, Integer num, String code) {
        //去缓存中查询对应的sku秒杀信息
        BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SKUS_CACHE_PREFIX);
        String seckillSkuJson = hashOps.get(seckillId);
        //1.验证sku秒杀信息是否存在
        if(StringUtils.isEmpty(seckillSkuJson))
            return null;
        //2.验证秒杀时间
        SeckillSkuRedisTo seckillSku = JSON.parseObject(seckillSkuJson, SeckillSkuRedisTo.class);
        Long startTime = seckillSku.getStartTime();
        Long endTime = seckillSku.getEndTime();
        long currentTime = new Date().getTime();
        if(currentTime < startTime || currentTime > endTime)
            return null;
        //3.验证随机码
        if(!code.equals(seckillSku.getRandomCode()))
            return null;
        //4.验证购买件数
        if(num != seckillSku.getSeckillLimit().intValue())
            return null;
        //5.幂等性保证，去redis中占位，一旦占位之后，下次请求就不能通过 key 使用 userId_sessionId_skuId
        UserRespVo user = LoginInterceptor.userThreadLocal.get();
        Long userId = user.getId();
        String key = userId + "_" + seckillId;
        //给占位锁设置过期时间，时间就为秒杀活动结束的时间
        Boolean newComing = redisTemplate.opsForValue().setIfAbsent(key, num.toString(), endTime - currentTime, TimeUnit.MILLISECONDS);
        if(!newComing)
            return null;
        //6.获取信号量
        RSemaphore semaphore = redissonClient.getSemaphore(STOCK_CACHE_PREFIX + code);
        //不用timeout等待时间，如果获取失败，立即返回false
        boolean permit = semaphore.tryAcquire(num);
        if(!permit)
            return null;
        //7.创建订单号，给MQ发送消息
        String orderSn = IdWorker.getTimeId();
        //TODO 给MQ发送消息
        SeckillOrderTo seckillOrder = new SeckillOrderTo();
        seckillOrder.setOrderSn(orderSn);
        seckillOrder.setPromotionSessionId(seckillSku.getPromotionSessionId());
        seckillOrder.setSkuId(seckillSku.getSkuId());
        seckillOrder.setSeckillPrice(seckillSku.getSeckillPrice());
        seckillOrder.setNum(num);
        seckillOrder.setMemberId(user.getId());
        rabbitTemplate.convertAndSend("order-event-exchange", "order.seckill.order", seckillOrder);
        return orderSn;
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
