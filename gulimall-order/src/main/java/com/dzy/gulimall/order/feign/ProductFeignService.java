package com.dzy.gulimall.order.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;

@FeignClient("gulimall-product")
public interface ProductFeignService {
    @GetMapping("/product/skuinfo/{skuId}/price")
    BigDecimal getNewlyPriceBySkuId(@PathVariable("skuId") Long skuId);
}
