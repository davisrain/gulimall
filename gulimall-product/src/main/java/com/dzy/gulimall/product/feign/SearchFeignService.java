package com.dzy.gulimall.product.feign;


import com.dzy.common.to.es.SkuEsModel;
import com.dzy.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient("gulimall-search")
public interface SearchFeignService {

    @PostMapping("/search/save/product")
    R upProduct(@RequestBody List<SkuEsModel> skuEsModels);
}
