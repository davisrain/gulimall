package com.dzy.gulimall.cart.vo;

import java.math.BigDecimal;
import java.util.List;

/**
 * 整个购物车
 *  需要计算的属性，必须重写它的get方法，保证每次获取都重新计算。
 */
public class Cart {
    private List<CartItem> items;
    private Integer countNum;   //商品数量
    private Integer countType;  //商品类型数量
    private BigDecimal totalAmount; //购物车商品总价
    private BigDecimal reduce = new BigDecimal("0.00");  //商品减免价格

    public List<CartItem> getItems() {
        return items;
    }

    public void setItems(List<CartItem> items) {
        this.items = items;
    }

    public Integer getCountNum() {
        int count = 0;
        if(items != null) {
            for (CartItem item : items) {
                count += item.getCount();
            }
        }
        this.countNum = count;
        return this.countNum;
    }

    public Integer getCountType() {
        int count = 0;
        if(items != null) {
            count = items.size();
        }
        this.countType = count;
        return this.countType;
    }

    public BigDecimal getTotalAmount() {
        BigDecimal totalAmount = new BigDecimal("0.00");
        if(items != null) {
            for (CartItem item : items) {
                if(item.getCheck())
                    totalAmount = totalAmount.add(item.getTotalPrice());
            }
        }
        this.totalAmount = totalAmount.subtract(getReduce());
        return this.totalAmount;
    }

    public BigDecimal getReduce() {
        return reduce;
    }

    public void setReduce(BigDecimal reduce) {
        this.reduce = reduce;
    }
}
