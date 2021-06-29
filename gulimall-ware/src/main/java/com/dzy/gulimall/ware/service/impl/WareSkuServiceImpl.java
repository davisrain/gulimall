package com.dzy.gulimall.ware.service.impl;

import com.dzy.common.to.SkuHasStockTo;
import com.dzy.common.utils.R;
import com.dzy.gulimall.ware.entity.PurchaseDetailEntity;
import com.dzy.gulimall.ware.feign.ProductFeignService;
import com.dzy.gulimall.ware.service.PurchaseDetailService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dzy.common.utils.PageUtils;
import com.dzy.common.utils.Query;

import com.dzy.gulimall.ware.dao.WareSkuDao;
import com.dzy.gulimall.ware.entity.WareSkuEntity;
import com.dzy.gulimall.ware.service.WareSkuService;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {

    @Autowired
    PurchaseDetailService purchaseDetailService;

    @Autowired
    ProductFeignService productFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<WareSkuEntity> queryWrapper = new QueryWrapper<>();
        String wareId = (String) params.get("wareId");
        String skuId = (String) params.get("skuId");
        if (StringUtils.isNotBlank(wareId))
            queryWrapper.eq("ware_id", wareId);
        if (StringUtils.isNotBlank(skuId))
            queryWrapper.eq("sku_id", skuId);
        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params),
                queryWrapper
        );

        return new PageUtils(page);
    }

    /**
     * 根据成功的采购单id进行入库
     *
     * @param successPurchaseDetailIds 成功的采购单id
     */
    @Transactional
    @Override
    @SuppressWarnings("unchecked")
    public void addStockByPurchaseDetailIds(List<Long> successPurchaseDetailIds) {
        List<PurchaseDetailEntity> purchaseDetails = purchaseDetailService.listByIds(successPurchaseDetailIds);
        purchaseDetails.forEach(purchaseDetail -> {
            //判断库存中是否有该商品，有就更新，没有就新增
            QueryWrapper<WareSkuEntity> wrapper = new QueryWrapper<>();
            wrapper.eq("sku_id", purchaseDetail.getSkuId()).eq("ware_id", purchaseDetail.getWareId());
            WareSkuEntity wareSkuEntity = baseMapper.selectOne(wrapper);
            if (wareSkuEntity == null) {
                WareSkuEntity wareSku = new WareSkuEntity();
                wareSku.setSkuId(purchaseDetail.getSkuId());
                wareSku.setWareId(purchaseDetail.getWareId());
                //使用远程调用获取sku的name,如果远程调用失败，整个事务无需回滚
                //1.使用try-catch包裹，并且忽略catch，达到效果
                //TODO 还可以用什么办法让异常出现后不回滚
                try {
                    R r = productFeignService.info(purchaseDetail.getSkuId());
                    if (r.getCode() == 0) {
                        Map<String, Object> skuInfo = (Map<String, Object>) r.get("skuInfo");
                        wareSku.setSkuName((String) skuInfo.get("skuName"));
                    }
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
                wareSku.setStock(purchaseDetail.getSkuNum());
                wareSku.setStockLocked(0);
                baseMapper.insert(wareSku);
            } else {
                baseMapper.addStock(purchaseDetail.getSkuId(), purchaseDetail.getWareId(), purchaseDetail.getSkuNum());
            }
        });
    }

    @Override
    public List<SkuHasStockTo> getHasStockBySkuIds(List<Long> skuIds) {

        return skuIds.stream().map(skuId -> {
            SkuHasStockTo skuHasStockTo = new SkuHasStockTo();
            Integer stock = baseMapper.getStock(skuId);
            skuHasStockTo.setSkuId(skuId);
            skuHasStockTo.setHasStock(stock > 0);
            return skuHasStockTo;
        }).collect(Collectors.toList());
    }


}