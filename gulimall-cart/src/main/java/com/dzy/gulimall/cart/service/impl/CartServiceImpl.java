package com.dzy.gulimall.cart.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.dzy.common.constant.CartConstant;
import com.dzy.common.utils.R;
import com.dzy.gulimall.cart.Interceptor.CartInterceptor;
import com.dzy.gulimall.cart.feign.ProductFeignService;
import com.dzy.gulimall.cart.service.CartService;
import com.dzy.gulimall.cart.to.SkuInfoTo;
import com.dzy.gulimall.cart.to.UserInfoTo;
import com.dzy.gulimall.cart.vo.CartItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

@Service("cartService")
public class CartServiceImpl implements CartService {

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    ProductFeignService productFeignService;

    @Autowired
    ThreadPoolExecutor executor;


    @Override
    public CartItem addToCart(Long skuId, Integer num) {
        CartItem cartItem = new CartItem();
        //因为两个两个远程调用可以异步进行，所以使用CompletableFuture来进行异步编排
        //1.根据skuId调用远程服务查询到sku的信息
        CompletableFuture<Void> getSkuInfoFuture = CompletableFuture.runAsync(() -> {
            R r = productFeignService.getSkuInfo(skuId);
            if (r.getCode() == 0) {
                SkuInfoTo skuInfo = r.getData("skuInfo", new TypeReference<SkuInfoTo>() {
                });
                cartItem.setSkuId(skuId);
                cartItem.setTitle(skuInfo.getSkuTitle());
                cartItem.setImage(skuInfo.getSkuDefaultImg());
                cartItem.setPrice(skuInfo.getPrice());
                cartItem.setCount(num);
            }
        }, executor);

        //2.根据skuId调用远程服务查询到skuSaleAttrValue的信息
        CompletableFuture<Void> getSkuSaleAttrValueFuture = CompletableFuture.runAsync(() -> {
            List<String> skuSaleAttrValues = productFeignService.getSkuSaleAttrValues(skuId);
            cartItem.setSaleAttrs(skuSaleAttrValues);
        }, executor);
        try {
            CompletableFuture.allOf(getSkuInfoFuture, getSkuSaleAttrValueFuture).get();
        } catch (Exception e) {
            e.printStackTrace();
        }
        //3.将cartItem的值保存到redis中
        BoundHashOperations<String, Object, Object> redisOperations = getRedisOperations();
        redisOperations.put(String.valueOf(skuId), JSON.toJSONString(cartItem));
        return cartItem;
    }

    private BoundHashOperations<String, Object, Object> getRedisOperations() {
        UserInfoTo userInfo = CartInterceptor.threadLocal.get();
        String key;
        if(userInfo.getUserId() != null) {
            //登录之后的情况
            key = CartConstant.REDIS_CART_PREFIX + userInfo.getUserId();
        } else {
            //没有登录的情况，使用user-key
            key = CartConstant.REDIS_CART_PREFIX + userInfo.getUserKey();
        }
        BoundHashOperations<String, Object, Object> operations = redisTemplate.boundHashOps(key);
        return operations;
    }
}
