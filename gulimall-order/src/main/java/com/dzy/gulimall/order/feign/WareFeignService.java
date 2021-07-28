package com.dzy.gulimall.order.feign;

import com.dzy.common.utils.R;
import com.dzy.gulimall.order.to.WareLockTo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient("gulimall-ware")
public interface WareFeignService {
    @PostMapping("/ware/waresku/hasStock")
    R getHasStockBySkuIds(@RequestBody List<Long> skuIds);

    @GetMapping("/ware/wareinfo/fare")
    R getFare(@RequestParam("addressId") Long addressId);

    @PostMapping("/ware/waresku//lock")
    R lockStock(@RequestBody WareLockTo wareLockTo);
}
