package com.dzy.gulimall.product.feign;

import com.dzy.common.to.SkuHasStockTo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient("gulimall-ware")
public interface WareFeignService {

    @PostMapping("/ware/waresku/hasStock")
    List<SkuHasStockTo> getHasStockBySkuIds(@RequestBody List<Long> skuIds);
}
