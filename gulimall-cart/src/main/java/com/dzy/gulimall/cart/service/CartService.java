package com.dzy.gulimall.cart.service;

import com.dzy.gulimall.cart.vo.Cart;
import com.dzy.gulimall.cart.vo.CartItem;

import java.util.List;

public interface CartService {
    CartItem addToCart(Long skuId, Integer num);

    CartItem getCartItem(Long skuId);

    Cart getCart();

    List<CartItem> getCartItemsByCartKey(String cartKey);

    void checkCartItem(Long skuId, Integer checked);

    void changeCartItemNum(Long skuId, Integer num);

    void deleteCartItem(Long skuId);

    List<CartItem> getCurrentCartItems();

}
