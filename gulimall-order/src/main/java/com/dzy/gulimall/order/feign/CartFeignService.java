package com.dzy.gulimall.order.feign;

import com.dzy.gulimall.order.vo.OrderItemVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient("gulimall-cart")
public interface CartFeignService {

    @GetMapping("/currentCartItems")
    List<OrderItemVo> getCurrentCartItems();
}
