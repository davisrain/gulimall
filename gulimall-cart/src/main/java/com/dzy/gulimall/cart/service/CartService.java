package com.dzy.gulimall.cart.service;

import com.dzy.gulimall.cart.vo.CartItem;

public interface CartService {
    CartItem addToCart(Long skuId, Integer num);
}
