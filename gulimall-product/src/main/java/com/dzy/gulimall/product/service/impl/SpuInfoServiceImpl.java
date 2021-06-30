package com.dzy.gulimall.product.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.dzy.common.constant.ProductConstant;
import com.dzy.common.to.SkuHasStockTo;
import com.dzy.common.to.SkuReductionTo;
import com.dzy.common.to.SpuBoundsTo;
import com.dzy.common.to.es.SkuEsModel;
import com.dzy.common.utils.R;
import com.dzy.gulimall.product.entity.AttrEntity;
import com.dzy.gulimall.product.entity.BrandEntity;
import com.dzy.gulimall.product.entity.CategoryEntity;
import com.dzy.gulimall.product.entity.ProductAttrValueEntity;
import com.dzy.gulimall.product.entity.SkuImagesEntity;
import com.dzy.gulimall.product.entity.SkuInfoEntity;
import com.dzy.gulimall.product.entity.SkuSaleAttrValueEntity;
import com.dzy.gulimall.product.entity.SpuImagesEntity;
import com.dzy.gulimall.product.entity.SpuInfoDescEntity;
import com.dzy.gulimall.product.feign.CouponFeignService;
import com.dzy.gulimall.product.feign.SearchFeignService;
import com.dzy.gulimall.product.feign.WareFeignService;
import com.dzy.gulimall.product.service.AttrService;
import com.dzy.gulimall.product.service.BrandService;
import com.dzy.gulimall.product.service.CategoryService;
import com.dzy.gulimall.product.service.ProductAttrValueService;
import com.dzy.gulimall.product.service.SkuImagesService;
import com.dzy.gulimall.product.service.SkuInfoService;
import com.dzy.gulimall.product.service.SkuSaleAttrValueService;
import com.dzy.gulimall.product.service.SpuImagesService;
import com.dzy.gulimall.product.service.SpuInfoDescService;
import com.dzy.gulimall.product.vo.Attr;
import com.dzy.gulimall.product.vo.BaseAttrs;
import com.dzy.gulimall.product.vo.Bounds;
import com.dzy.gulimall.product.vo.Images;
import com.dzy.gulimall.product.vo.Skus;
import com.dzy.gulimall.product.vo.SpuSaveVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dzy.common.utils.PageUtils;
import com.dzy.common.utils.Query;

import com.dzy.gulimall.product.dao.SpuInfoDao;
import com.dzy.gulimall.product.entity.SpuInfoEntity;
import com.dzy.gulimall.product.service.SpuInfoService;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service("spuInfoService")
public class SpuInfoServiceImpl extends ServiceImpl<SpuInfoDao, SpuInfoEntity> implements SpuInfoService {

    @Autowired
    SpuInfoDescService  spuInfoDescService;

    @Autowired
    SpuImagesService spuImagesService;

    @Autowired
    AttrService attrService;

    @Autowired
    ProductAttrValueService productAttrValueService;

    @Autowired
    SkuInfoService skuInfoService;

    @Autowired
    SkuImagesService skuImagesService;

    @Autowired
    SkuSaleAttrValueService skuSaleAttrValueService;

    @Autowired
    CouponFeignService couponFeignService;

    @Autowired
    BrandService brandService;

    @Autowired
    CategoryService categoryService;

    @Autowired
    WareFeignService wareFeignService;

    @Autowired
    SearchFeignService searchFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                new QueryWrapper<SpuInfoEntity>()
        );

        return new PageUtils(page);
    }

    //TODO 高级部分完善
    @Transactional
    @Override
    public void saveSpuInfo(SpuSaveVo spuSaveVo) {
        //1、保存spu的基本信息  pms_spu_info
        SpuInfoEntity spuInfo = new SpuInfoEntity();
        BeanUtils.copyProperties(spuSaveVo, spuInfo);
        spuInfo.setCreateTime(new Date());
        spuInfo.setUpdateTime(new Date());
        this.save(spuInfo);
        //2、保存spu的描述图片  pms_spu_info_desc
        SpuInfoDescEntity spuInfoDesc = new SpuInfoDescEntity();
        spuInfoDesc.setSpuId(spuInfo.getId());
        spuInfoDesc.setDecript(String.join(",", spuSaveVo.getDecript()));
        spuInfoDescService.save(spuInfoDesc);
        //3、保存spu的图片集    pms_spu_images
        List<String> images = spuSaveVo.getImages();
        spuImagesService.saveImages(spuInfo.getId(), images);
        //4、保存spu的规格参数  pms_product_attr_value
        List<BaseAttrs> baseAttrs = spuSaveVo.getBaseAttrs();
        List<ProductAttrValueEntity> productAttrValues = baseAttrs.stream().map(baseAttr -> {
            ProductAttrValueEntity productAttrValue = new ProductAttrValueEntity();
            productAttrValue.setSpuId(spuInfo.getId());
            productAttrValue.setAttrId(baseAttr.getAttrId());
            AttrEntity attr = attrService.getById(baseAttr.getAttrId());
            productAttrValue.setAttrName(attr.getAttrName());
            productAttrValue.setAttrValue(baseAttr.getAttrValues());
            productAttrValue.setQuickShow(baseAttr.getShowDesc());
            return productAttrValue;
        }).collect(Collectors.toList());
        productAttrValueService.saveBatch(productAttrValues);
        //5、保存spu的积分信息  gulimall_sms.sms_spu_bounds
        Bounds bounds = spuSaveVo.getBounds();
        SpuBoundsTo spuBoundsTo = new SpuBoundsTo();
        BeanUtils.copyProperties(bounds, spuBoundsTo);
        spuBoundsTo.setSpuId(spuInfo.getId());
        R r = couponFeignService.saveSpuBounds(spuBoundsTo);
        if(r.getCode() != 0) {
            log.error("远程保存spu积分信息失败");
        }
        //6、保存当前spu对应的所有sku的信息
        List<Skus> skus = spuSaveVo.getSkus();
        skus.forEach(sku -> {
            //6.1)、sku的基本信息     pms_sku_info
            List<Images> skuImages = sku.getImages();
            String defaultImg = null;
            for (Images skuImage : skuImages) {
                if (skuImage.getDefaultImg() == 1)
                    defaultImg = skuImage.getImgUrl();
            }
            SkuInfoEntity skuInfo = new SkuInfoEntity();
            BeanUtils.copyProperties(sku, skuInfo);
            skuInfo.setSpuId(spuInfo.getId());
            skuInfo.setCatalogId(spuInfo.getCatalogId());
            skuInfo.setBrandId(spuInfo.getBrandId());
            skuInfo.setSaleCount(0L);
            skuInfo.setSkuDefaultImg(defaultImg);
            skuInfoService.save(skuInfo);

            Long skuId = skuInfo.getSkuId();
            //6.2)、sku的图片信息     pms_sku_images
            List<SkuImagesEntity> skuImagesEntities = skuImages.stream().map(skuImage -> {
                SkuImagesEntity skuImagesEntity = new SkuImagesEntity();
                skuImagesEntity.setSkuId(skuId);
                skuImagesEntity.setImgUrl(skuImage.getImgUrl());
                skuImagesEntity.setDefaultImg(skuImage.getDefaultImg());
                return skuImagesEntity;
            }).filter(skuImagesEntity -> StringUtils.isNotBlank(skuImagesEntity.getImgUrl())) //没有图片路径的不用保存
                    .collect(Collectors.toList());
            skuImagesService.saveBatch(skuImagesEntities);
            //6.3)、sku的销售属性信息   pms_sku_sale_attr_value
            List<Attr> attrs = sku.getAttr();
            List<SkuSaleAttrValueEntity> skuSaleAttrValues = attrs.stream().map(attr -> {
                SkuSaleAttrValueEntity skuSaleAttrValue = new SkuSaleAttrValueEntity();
                skuSaleAttrValue.setSkuId(skuId);
                skuSaleAttrValue.setAttrId(attr.getAttrId());
                skuSaleAttrValue.setAttrName(attr.getAttrName());
                skuSaleAttrValue.setAttrValue(attr.getAttrValue());
                return skuSaleAttrValue;
            }).collect(Collectors.toList());
            skuSaleAttrValueService.saveBatch(skuSaleAttrValues);
            //6.4)、sku的优惠信息     gulimall_sms.sms_sku_full_reduction/sms_sku_ladder/sms_member_price
            SkuReductionTo skuReductionTo = new SkuReductionTo();
            BeanUtils.copyProperties(sku, skuReductionTo);
            skuReductionTo.setSkuId(skuId);
            R r1 = couponFeignService.saveSkuReductions(skuReductionTo);
            if (r1.getCode() != 0) {
                log.error("远程保存sku优惠信息失败");
            }
        });


    }

    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {
        QueryWrapper<SpuInfoEntity> wrapper = new QueryWrapper<>();
        String key = (String) params.get("key");
        String catelogId = (String) params.get("catelogId");
        String brandId = (String) params.get("brandId");
        String status = (String) params.get("status");
        if(StringUtils.isNotBlank(key))
            wrapper.and(w -> {
                w.eq("spu_id", key).or().like("spu_name", key);
            });
        if(StringUtils.isNotEmpty(catelogId) && !"0".equals(catelogId))
            wrapper.eq("catalog_id", catelogId);
        if(StringUtils.isNotEmpty(brandId) && !"0".equals(catelogId))
            wrapper.eq("brand_id", brandId);
        if(StringUtils.isNotEmpty(status))
            wrapper.eq("publish_status", status);
        IPage<SpuInfoEntity> page = this.page(new Query<SpuInfoEntity>().getPage(params), wrapper);
        return new PageUtils(page);
    }

    /**
     *  商品上架
     */
    @Override
    public void up(Long spuId) {
        //1.查出spuId对应的sku
        List<SkuInfoEntity> skus = skuInfoService.getSkusBySpuId(spuId);
        //2.查询spu对应的规格属性值
        List<ProductAttrValueEntity> attrValues = productAttrValueService.listForSpu(spuId);
        List<Long> attrIds = attrValues.stream().map(ProductAttrValueEntity::getAttrId).collect(Collectors.toList());
        List<Long> searchAttrIds = attrService.getSearchAttrIdsByIds(attrIds);
        List<SkuEsModel.Attr> attrs = attrValues.stream().filter(attrValue ->
                searchAttrIds.contains(attrValue.getAttrId())).map(attrValue -> {
            SkuEsModel.Attr attr = new SkuEsModel.Attr();
            BeanUtils.copyProperties(attrValue, attr);
            return attr;
        }).collect(Collectors.toList());
        //3.远程调用库存微服务查询skus是否有库存
        Map<Long, Boolean> stockMap = null;
        try {
            List<Long> skuIds = skus.stream().map(SkuInfoEntity::getSkuId).collect(Collectors.toList());
            R r = wareFeignService.getHasStockBySkuIds(skuIds);
            if(r.getCode() == 0) {
                List<SkuHasStockTo> skuHasStockTos = r.getData(new TypeReference<List<SkuHasStockTo>>() {
                });
                stockMap = skuHasStockTos.stream()
                        .collect(Collectors.toMap(SkuHasStockTo::getSkuId, SkuHasStockTo::getHasStock));
            } else {
                throw new Exception((String)r.get("msg"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //4.将sku转换为SkuEsModel
        Map<Long, Boolean> finalStockMap = stockMap;
        List<SkuEsModel> skuEsModels = skus.stream().map(sku -> {
            SkuEsModel skuEsModel = new SkuEsModel();
            BeanUtils.copyProperties(sku, skuEsModel);
            skuEsModel.setSkuPrice(sku.getPrice());
            skuEsModel.setSkuImg(sku.getSkuDefaultImg());
            //调用categoryService和brandService查询名字
            CategoryEntity category = categoryService.getById(sku.getCatalogId());
            skuEsModel.setCatalogName(category.getName());
            BrandEntity brand = brandService.getById(sku.getBrandId());
            skuEsModel.setBrandName(brand.getName());
            skuEsModel.setBrandImg(brand.getLogo());
            //设置attrs
            skuEsModel.setAttrs(attrs);
            //热度评分
            skuEsModel.setHotScore(0L);
            //完成调用库存微服务查询
            skuEsModel.setHasStock(finalStockMap == null? true : finalStockMap.get(sku.getSkuId()));
            return skuEsModel;
        }).collect(Collectors.toList());
        //远程调用search微服务的接口上传到es
        R r = searchFeignService.upProduct(skuEsModels);
        if(r.getCode() == 0) {
            //远程调用成功，修改商品上架状态
            baseMapper.updatePublishStatusBySpuId(spuId, ProductConstant.StatusEnum.SPU_UP.getCode());
        } else {
            //远程调用失败
            //TODO 重复调用？接口幂等性：重试机制？
        }
    }


}