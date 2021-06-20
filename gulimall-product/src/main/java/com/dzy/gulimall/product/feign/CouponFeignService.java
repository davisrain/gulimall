package com.dzy.gulimall.product.feign;

import com.dzy.common.to.SkuReductionTo;
import com.dzy.common.to.SpuBoundsTo;
import com.dzy.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@FeignClient("gulimall-coupon")
public interface CouponFeignService {

    @RequestMapping("/coupon/spubounds/save")
    R saveSpuBounds(@RequestBody SpuBoundsTo spuBounds);

    @PostMapping("/coupon/skufullreduction/saveInfo")
    R saveSkuReductions(@RequestBody SkuReductionTo skuReductionTo);
}
