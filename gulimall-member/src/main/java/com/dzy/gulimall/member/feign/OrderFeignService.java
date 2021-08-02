package com.dzy.gulimall.member.feign;

import com.dzy.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient("gulimall-order")
public interface OrderFeignService {

    @PostMapping("/order/order/getOrderWithItems")
    R pageOrderWithItemsByMemberId(@RequestBody Map<String, Object> params);
}
