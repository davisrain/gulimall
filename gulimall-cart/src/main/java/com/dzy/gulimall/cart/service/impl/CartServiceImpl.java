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
import com.dzy.gulimall.cart.vo.Cart;
import com.dzy.gulimall.cart.vo.CartItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

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
        BoundHashOperations<String, Object, Object> redisOperations = getRedisOperations();
        String cartItemJson = (String) redisOperations.get(skuId.toString());
        if(StringUtils.hasText(cartItemJson)) {
            //购物车里已经存在这个商品，修改数量
            CartItem cartItem = JSON.parseObject(cartItemJson, CartItem.class);
            cartItem.setCount(cartItem.getCount() + num);
            redisOperations.put(skuId.toString(), JSON.toJSONString(cartItem));
            return cartItem;
        }
        //购物车里面没有这个商品，新增
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
        redisOperations.put(String.valueOf(skuId), JSON.toJSONString(cartItem));
        return cartItem;
    }

    /**
     *  获取单个购物项
     * @param skuId
     * @return
     */
    @Override
    public CartItem getCartItem(Long skuId) {
        BoundHashOperations<String, Object, Object> redisOperations = getRedisOperations();
        String cartItemJson = (String) redisOperations.get(skuId.toString());
        return JSON.parseObject(cartItemJson, CartItem.class);
    }

    /**
     * 获取整个购物车
     * @return
     */
    @Override
    public Cart getCart() {
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        Cart cart = new Cart();
        String key = CartConstant.REDIS_CART_PREFIX + userInfoTo.getUserKey();
        List<CartItem> tempCartItems = getCartItemsByCartKey(key);
        if(userInfoTo.getUserId() != null) {
            //登录状态下
            //如果临时购物车存在数据，需要将临时购物车合并过来
            if(tempCartItems != null) {
                for (CartItem tempCartItem : tempCartItems) {
                    addToCart(tempCartItem.getSkuId(), tempCartItem.getCount());
                }
            }
            //删除临时购物车的数据
            redisTemplate.delete(key);
            //查询登录状态下购物车里面的购物项
            key = CartConstant.REDIS_CART_PREFIX + userInfoTo.getUserId();
            List<CartItem> cartItems = getCartItemsByCartKey(key);
            cart.setItems(cartItems);
        } else {
            //未登录状态下
            cart.setItems(tempCartItems);
        }
        return cart;
    }

    @Override
    public List<CartItem> getCartItemsByCartKey(String cartKey) {
        BoundHashOperations<String, Object, Object> redisOps = redisTemplate.boundHashOps(cartKey);
        List<Object> values = redisOps.values();
        if(values != null) {
            List<CartItem> cartItems = values.stream().map(value -> JSON.parseObject((String) value, CartItem.class))
                    .collect(Collectors.toList());
            return cartItems;
        }
        return null;
    }

    @Override
    public void checkCartItem(Long skuId, Integer checked) {
        BoundHashOperations<String, Object, Object> redisOps = getRedisOperations();
        CartItem cartItem = JSON.parseObject((String) redisOps.get(skuId.toString()), CartItem.class);
        cartItem.setCheck(checked == 1);
        redisOps.put(skuId.toString(), JSON.toJSONString(cartItem));
    }

    @Override
    public void changeCartItemNum(Long skuId, Integer num) {
        BoundHashOperations<String, Object, Object> redisOps = getRedisOperations();
        CartItem cartItem = JSON.parseObject((String) redisOps.get(skuId.toString()), CartItem.class);
        cartItem.setCount(num);
        redisOps.put(skuId.toString(), JSON.toJSONString(cartItem));
    }

    @Override
    public void deleteCartItem(Long skuId) {
        BoundHashOperations<String, Object, Object> redisOps = getRedisOperations();
        redisOps.delete(skuId.toString());
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
