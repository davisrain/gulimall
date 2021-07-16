package com.dzy.gulimall.product.service.impl;

import com.dzy.gulimall.product.entity.SkuImagesEntity;
import com.dzy.gulimall.product.entity.SpuInfoDescEntity;
import com.dzy.gulimall.product.service.AttrGroupService;
import com.dzy.gulimall.product.service.SkuImagesService;
import com.dzy.gulimall.product.service.SkuSaleAttrValueService;
import com.dzy.gulimall.product.service.SpuInfoDescService;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dzy.common.utils.PageUtils;
import com.dzy.common.utils.Query;

import com.dzy.gulimall.product.dao.SkuInfoDao;
import com.dzy.gulimall.product.entity.SkuInfoEntity;
import com.dzy.gulimall.product.service.SkuInfoService;
import org.springframework.util.NumberUtils;


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
                //当min大于等于0的时候，才在where中拼接该条件
                if(minDecimal.compareTo(BigDecimal.ZERO) >= 0) {
                    wrapper.ge("price", min);
                }
            } catch (Exception e) {
                //ignore
                //判断min是不是数字类型，如果不是，忽略，不拼接条件
            }
        }
        if(StringUtils.isNotBlank(max)) {
            try {
                BigDecimal maxDecimal = new BigDecimal(max);
                //当max大于0的时候，才在where中拼接该条件
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
     *  根据skuId封装skuItemVo对象用于详情页展示
     */
    @Override
    public SkuItemVo item(Long skuId) {
        SkuItemVo skuItem = new SkuItemVo();
        //1.查询sku的基本信息 `pms_sku_info`
        CompletableFuture<SkuInfoEntity> skuInfoFuture = CompletableFuture.supplyAsync(() -> {
            SkuInfoEntity skuInfo = this.getById(skuId);
            skuItem.setSkuInfo(skuInfo);
            return skuInfo;
        }, executor);
        //2.查询sku的图片信息 `pms_sku_images`
        CompletableFuture<Void> skuImagesFuture = CompletableFuture.runAsync(() -> {
            List<SkuImagesEntity> skuImages = skuImagesService.getSkuImagesBySkuId(skuId);
            skuItem.setSkuImages(skuImages);
        }, executor);
        //3.查询spu下各种sku的销售属性
        CompletableFuture<Void> skuSaleAttrFuture = skuInfoFuture.thenAcceptAsync((res) -> {
            List<SkuSaleAttrVo> skuSaleAttrs = skuSaleAttrValueService.getSkuSaleAttrsBySpuId(res.getSpuId());
            skuItem.setSkuSaleAttrs(skuSaleAttrs);
        }, executor);
        //4.查询spu的描述信息 `pms_spu_info_desc`
        CompletableFuture<Void> spuDescFuture = skuInfoFuture.thenAcceptAsync((res) -> {
            SpuInfoDescEntity spuInfoDesc = spuInfoDescService.getById(res.getSpuId());
            skuItem.setSpuInfoDesc(spuInfoDesc);
        }, executor);
        //5.查询spu的规则参数
        CompletableFuture<Void> spuBaseAttrFuture = skuInfoFuture.thenAcceptAsync((res) -> {
            List<SpuBaseAttrGroupVo> spuBaseAttrGroups = attrGroupService.getAttrGroupsWithAttrsBySpuId(res.getSpuId(), res.getCatalogId());
            skuItem.setSpuBaseAttrGroupVos(spuBaseAttrGroups);
        }, executor);
        try {
            CompletableFuture.allOf(skuImagesFuture, skuSaleAttrFuture, spuDescFuture, spuBaseAttrFuture).get();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return skuItem;
    }

}