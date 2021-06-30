package com.dzy.gulimall.search.controller;

import com.dzy.common.exception.BizCodeEnum;
import com.dzy.common.to.es.SkuEsModel;
import com.dzy.common.utils.R;
import com.dzy.gulimall.search.service.ProductSaveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequestMapping("/search/save")
@RestController
public class ElasticSaveController {

    @Autowired
    ProductSaveService productSaveService;

    @PostMapping("/product")
    public R upProduct(@RequestBody List<SkuEsModel> skuEsModels) {
        boolean success = productSaveService.upProduct(skuEsModels);
        if(success)
            return R.ok();
        return R.error(BizCodeEnum.PRODUCT_UP_EXCEPTION.getCode(), BizCodeEnum.PRODUCT_UP_EXCEPTION.getMsg());
    }
}
