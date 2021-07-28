package com.dzy.gulimall.ware.exception;

public class NoStockException extends RuntimeException {
    public NoStockException(Long skuId) {
        super("商品id为：" + skuId + "的商品库存不足");
    }
}
