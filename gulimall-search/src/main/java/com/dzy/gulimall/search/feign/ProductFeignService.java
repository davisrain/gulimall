package com.dzy.gulimall.search.feign;


import com.dzy.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient("gulimall-product")
public interface ProductFeignService {

    @GetMapping("/product/attr/info/{attrId}")
    R getAttrByAttrId(@PathVariable("attrId") Long attrId);

    @PostMapping("/product/brand/list")
    R getBrandsByBrandIds(@RequestParam("brandIds") List<Long> brandIds);

    @GetMapping("/product/category/info/{catId}")
    R getCategoryByCatId(@PathVariable("catId") Long catId);
}
