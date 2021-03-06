package com.dzy.gulimall.product.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.dzy.common.utils.R;
import com.dzy.gulimall.product.entity.SkuImagesEntity;
import com.dzy.gulimall.product.entity.SpuInfoDescEntity;
import com.dzy.gulimall.product.entity.SpuInfoEntity;
import com.dzy.gulimall.product.feign.SeckillFeignService;
import com.dzy.gulimall.product.service.AttrGroupService;
import com.dzy.gulimall.product.service.SkuImagesService;
import com.dzy.gulimall.product.service.SkuSaleAttrValueService;
import com.dzy.gulimall.product.service.SpuInfoDescService;
import com.dzy.gulimall.product.service.SpuInfoService;
import com.dzy.gulimall.product.vo.SeckillSkuInfoVo;
import com.dzy.gulimall.product.vo.SkuItemVo;
import com.dzy.gulimall.product.vo.SkuSaleAttrVo;
import com.dzy.gulimall.product.vo.SpuBaseAttrGroupVo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dzy.common.utils.PageUtils;
import com.dzy.common.utils.Query;

import com.dzy.gulimall.product.dao.SkuInfoDao;
import com.dzy.gulimall.product.entity.SkuInfoEntity;
import com.dzy.gulimall.product.service.SkuInfoService;

import javax.annotation.Resource;


@Service("skuInfoService")
public class SkuInfoServiceImpl extends ServiceImpl<SkuInfoDao, SkuInfoEntity> implements SkuInfoService {

    @Autowired
    SkuImagesService skuImagesService;

    @Autowired
    SpuInfoDescService spuInfoDescService;

    @Autowired
    AttrGroupService attrGroupService;

    @Autowired
    SkuSaleAttrValueService skuSaleAttrValueService;

    @Resource
    SeckillFeignService seckillFeignService;

    @Autowired
    SpuInfoService spuInfoService;

    @Autowired
    ThreadPoolExecutor executor;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SkuInfoEntity> page = this.page(
                new Query<SkuInfoEntity>().getPage(params),
                new QueryWrapper<SkuInfoEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {
        QueryWrapper<SkuInfoEntity> wrapper = new QueryWrapper<>();
        String key = (String) params.get("key");
        String catelogId = (String) params.get("catelogId");
        String brandId = (String) params.get("brandId");
        String min = (String) params.get("min");
        String max = (String) params.get("max");
        if(StringUtils.isNotBlank(key))
            wrapper.and(w -> {
                w.eq("sku_id", key).or().like("sku_name", key);
            });
        if(StringUtils.isNotEmpty(catelogId) && !"0".equalsIgnoreCase(catelogId))
            wrapper.eq("catalog_id", catelogId);
        if(StringUtils.isNotEmpty(brandId) && !"0".equalsIgnoreCase(brandId))
            wrapper.eq("brand_id", brandId);
        if(StringUtils.isNotBlank(min)) {
            try {
                BigDecimal minDecimal = new BigDecimal(min);
                //???min????????????0??????????????????where??????????????????
                if(minDecimal.compareTo(BigDecimal.ZERO) >= 0) {
                    wrapper.ge("price", min);
                }
            } catch (Exception e) {
                //ignore
                //??????min???????????????????????????????????????????????????????????????
            }
        }
        if(StringUtils.isNotBlank(max)) {
            try {
                BigDecimal maxDecimal = new BigDecimal(max);
                //???max??????0??????????????????where??????????????????
                if(maxDecimal.compareTo(BigDecimal.ZERO) > 0) {
                    wrapper.le("price", max);
                }
            } catch (Exception e) {
                //ignore
            }
        }
        IPage<SkuInfoEntity> page = this.page(new Query<SkuInfoEntity>().getPage(params), wrapper);
        return new PageUtils(page);
    }

    @Override
    public List<SkuInfoEntity> getSkusBySpuId(Long spuId) {
        return this.list(new QueryWrapper<SkuInfoEntity>().eq("spu_id", spuId));
    }

    /**
     *  ??????skuId??????skuItemVo???????????????????????????
     */
    @Override
    public SkuItemVo item(Long skuId) {
        SkuItemVo skuItem = new SkuItemVo();
        //1.??????sku??????????????? `pms_sku_info`
        CompletableFuture<SkuInfoEntity> skuInfoFuture = CompletableFuture.supplyAsync(() -> {
            SkuInfoEntity skuInfo = this.getById(skuId);
            skuItem.setSkuInfo(skuInfo);
            return skuInfo;
        }, executor);
        //2.??????sku??????????????? `pms_sku_images`
        CompletableFuture<Void> skuImagesFuture = CompletableFuture.runAsync(() -> {
            List<SkuImagesEntity> skuImages = skuImagesService.getSkuImagesBySkuId(skuId);
            skuItem.setSkuImages(skuImages);
        }, executor);
        //3.??????spu?????????sku???????????????
        CompletableFuture<Void> skuSaleAttrFuture = skuInfoFuture.thenAcceptAsync((res) -> {
            List<SkuSaleAttrVo> skuSaleAttrs = skuSaleAttrValueService.getSkuSaleAttrsBySpuId(res.getSpuId());
            skuItem.setSkuSaleAttrs(skuSaleAttrs);
        }, executor);
        //4.??????spu??????????????? `pms_spu_info_desc`
        CompletableFuture<Void> spuDescFuture = skuInfoFuture.thenAcceptAsync((res) -> {
            SpuInfoDescEntity spuInfoDesc = spuInfoDescService.getById(res.getSpuId());
            skuItem.setSpuInfoDesc(spuInfoDesc);
        }, executor);
        //5.??????spu???????????????
        CompletableFuture<Void> spuBaseAttrFuture = skuInfoFuture.thenAcceptAsync((res) -> {
            List<SpuBaseAttrGroupVo> spuBaseAttrGroups = attrGroupService.getAttrGroupsWithAttrsBySpuId(res.getSpuId(), res.getCatalogId());
            skuItem.setSpuBaseAttrGroupVos(spuBaseAttrGroups);
        }, executor);
        //6.????????????sku?????????????????????
        CompletableFuture<Void> seckillFuture = CompletableFuture.runAsync(() -> {
            R r = seckillFeignService.getSeckillSkuInfo(skuId);
            if (r.getCode() == 0) {
                SeckillSkuInfoVo seckillSkuInfo = r.getData(new TypeReference<SeckillSkuInfoVo>() {
                });
                skuItem.setSeckillSkuInfo(seckillSkuInfo);
            }
        }, executor);
        try {
            CompletableFuture.allOf(skuImagesFuture, skuSaleAttrFuture, spuDescFuture, spuBaseAttrFuture, seckillFuture).get();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return skuItem;
    }

    @Override
    public BigDecimal getNewlyPriceBySkuId(Long skuId) {
        SkuInfoEntity skuInfo = getById(skuId);
        return skuInfo.getPrice();
    }

    @Override
    public SkuInfoEntity getSkuInfoWithSpuInfo(Long skuId) {
        SkuInfoEntity skuInfo = getById(skuId);
        SpuInfoEntity spuInfo = spuInfoService.getById(skuInfo.getSpuId());
        skuInfo.setSpuInfo(spuInfo);
        return skuInfo;
    }

}