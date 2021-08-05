package com.dzy.gulimall.product.feign.fallback;

import com.dzy.common.exception.BizCodeEnum;
import com.dzy.common.utils.R;
import com.dzy.gulimall.product.feign.SeckillFeignService;
import org.springframework.stereotype.Component;

@Component
public class SeckillFeignServiceFallback implements SeckillFeignService {
    @Override
    public R getSeckillSkuInfo(Long skuId) {
        return R.error(BizCodeEnum.UNKNOWN_EXCEPTION);
    }
}
