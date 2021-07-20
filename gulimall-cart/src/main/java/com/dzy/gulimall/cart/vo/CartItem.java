package com.dzy.gulimall.cart.vo;

import java.math.BigDecimal;
import java.util.List;

public class CartItem {
    private Integer skuId;
    private Boolean check = true;
    private String title;
    private String image;
    private List<String> saleAttrs;
    private BigDecimal price;
    private Integer count;
    private BigDecimal totalPrice;

    public Integer getSkuId() {
        return skuId;
    }

    public void setSkuId(Integer skuId) {
        this.skuId = skuId;
    }

    public Boolean getCheck() {
        return check;
    }

    public void setCheck(Boolean check) {
        this.check = check;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public List<String> getSaleAttrs() {
        return saleAttrs;
    }

    public void setSaleAttrs(List<String> saleAttrs) {
        this.saleAttrs = saleAttrs;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public BigDecimal getTotalPrice() {
        this.totalPrice = this.price.multiply(new BigDecimal(String.valueOf(this.count)));
        return this.totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }
}
