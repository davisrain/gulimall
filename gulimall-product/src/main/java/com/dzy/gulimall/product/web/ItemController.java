package com.dzy.gulimall.product.web;

import com.dzy.gulimall.product.service.SkuInfoService;
import com.dzy.gulimall.product.vo.SkuItemVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class ItemController {

    @Autowired
    SkuInfoService skuInfoService;

    @GetMapping("/{skuId}.html")
    public String itemPage(@PathVariable("skuId") Long skuId, Model model) {
        //查询sku对应的详情信息
        SkuItemVo skuItemVo = skuInfoService.item(skuId);
        model.addAttribute("skuItem", skuItemVo);
        return "item";
    }
}
