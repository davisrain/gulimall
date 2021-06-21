package com.dzy.gulimall.ware.service.impl;

import com.dzy.common.constant.WareConstant;
import com.dzy.gulimall.ware.service.WareSkuService;
import com.dzy.gulimall.ware.vo.PurchaseDoneVo;
import com.dzy.gulimall.ware.vo.PurchaseItemVo;
import com.dzy.gulimall.ware.vo.PurchaseMergeVo;
import com.dzy.gulimall.ware.entity.PurchaseDetailEntity;
import com.dzy.gulimall.ware.service.PurchaseDetailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dzy.common.utils.PageUtils;
import com.dzy.common.utils.Query;

import com.dzy.gulimall.ware.dao.PurchaseDao;
import com.dzy.gulimall.ware.entity.PurchaseEntity;
import com.dzy.gulimall.ware.service.PurchaseService;
import org.springframework.transaction.annotation.Transactional;



@Service("purchaseService")
public class PurchaseServiceImpl extends ServiceImpl<PurchaseDao, PurchaseEntity> implements PurchaseService {

    @Autowired
    PurchaseDetailService purchaseDetailService;

    @Autowired
    WareSkuService wareSkuService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<PurchaseEntity> page = this.page(
                new Query<PurchaseEntity>().getPage(params),
                new QueryWrapper<PurchaseEntity>()
        );

        return new PageUtils(page);
    }

    /**
     *  查询未领取的采购单
     * @param params
     */
    @Override
    public PageUtils getUnreceiveList(Map<String, Object> params) {
        QueryWrapper<PurchaseEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", WareConstant.PurchaseStatusEnum.CREATED.getCode()).or()
                .eq("status", WareConstant.PurchaseStatusEnum.ASSIGNED.getCode());
        IPage<PurchaseEntity> page = this.page(new Query<PurchaseEntity>().getPage(params), queryWrapper);
        return new PageUtils(page);
    }

    /**
     *  合并采购需求
     */
    @Transactional
    @Override
    public void mergePurchaseDetail(PurchaseMergeVo mergeVo) {
        Long purchaseId = mergeVo.getPurchaseId();
        //如果没有传purchaseId，需要自己新建一个
        if(purchaseId == null) {
            PurchaseEntity purchaseEntity = new PurchaseEntity();
            purchaseEntity.setStatus(WareConstant.PurchaseStatusEnum.CREATED.getCode());
            purchaseEntity.setCreateTime(new Date());
            purchaseEntity.setUpdateTime(new Date());
            this.save(purchaseEntity);
            purchaseId = purchaseEntity.getId();
        }
        //验证采购单状态只能是新建或分配的
        PurchaseEntity purchase = this.getById(purchaseId);
        if(purchase.getStatus() > WareConstant.PurchaseStatusEnum.ASSIGNED.getCode())
            return;
        //验证采购单明细状态只能是新建或分配的
        List<Long> items = mergeVo.getItems();
        if(items.isEmpty())
            return;
        List<PurchaseDetailEntity> purchaseDetailEntities = purchaseDetailService.listByIds(items);
        Long finalPurchaseId = purchaseId;
        List<PurchaseDetailEntity> purchaseDetails = purchaseDetailEntities.stream().filter(purchaseDetailEntity ->
                purchaseDetailEntity.getStatus() <= WareConstant.PurchaseDetailStatusEnum.ASSIGNED.getCode())
                .map(purchaseDetailEntity -> {
            PurchaseDetailEntity purchaseDetail = new PurchaseDetailEntity();
            purchaseDetail.setId(purchaseDetailEntity.getId());
            purchaseDetail.setPurchaseId(finalPurchaseId);
            purchaseDetail.setStatus(WareConstant.PurchaseDetailStatusEnum.ASSIGNED.getCode());
            return purchaseDetail;
        }).collect(Collectors.toList());
        purchaseDetailService.updateBatchById(purchaseDetails);
        //当我们是更新purchase的时候，需要修改对应purchase的更新时间
        purchase.setUpdateTime(new Date());
        this.updateById(purchase);
    }

    /**
     *  领取采购单
     */
    @Transactional
    @Override
    public void receivedPurchase(List<Long> purchaseIds) {
        if(purchaseIds.isEmpty())
            return;
        //1、根据采购单id拿到采购单对象
        QueryWrapper<PurchaseEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("id", purchaseIds).and(w -> {
            w.eq("status", WareConstant.PurchaseStatusEnum.CREATED.getCode())
                    .or().eq("status", WareConstant.PurchaseStatusEnum.ASSIGNED.getCode());
        });
        List<PurchaseEntity> purchases = list(queryWrapper);
        if(purchases.isEmpty())
            return;
        //2、修改采购单的状态和更新时间
        List<PurchaseEntity> updatePurchases = purchases.stream().map(purchase -> {
            PurchaseEntity updatePurchase = new PurchaseEntity();
            updatePurchase.setId(purchase.getId());
            updatePurchase.setStatus(WareConstant.PurchaseStatusEnum.RECEIVED.getCode());
            updatePurchase.setUpdateTime(new Date());
            return updatePurchase;
        }).collect(Collectors.toList());
        updateBatchById(updatePurchases);
        //3、根据采购单id拿到采购单明细对象
        List<Long> allowPurchaseIds = purchases.stream().map(PurchaseEntity::getId).collect(Collectors.toList());
        List<PurchaseDetailEntity> purchaseDetails = purchaseDetailService.list(
                new QueryWrapper<PurchaseDetailEntity>().in("purchase_id", allowPurchaseIds));
        //4、修改采购明细的状态
        List<PurchaseDetailEntity> updatePurchaseDetails = purchaseDetails.stream().map(purchaseDetail -> {
            PurchaseDetailEntity updatePurchaseDetail = new PurchaseDetailEntity();
            updatePurchaseDetail.setId(purchaseDetail.getId());
            updatePurchaseDetail.setStatus(WareConstant.PurchaseDetailStatusEnum.BUYING.getCode());
            return updatePurchaseDetail;
        }).collect(Collectors.toList());
        purchaseDetailService.updateBatchById(updatePurchaseDetails);
    }


    /**
     *  完成采购
     */
    @Transactional
    @Override
    public void donePurchase(PurchaseDoneVo doneVo) {
        PurchaseEntity purchase = getById(doneVo.getId());
        if(purchase == null)
            return;
        //1、更新采购明细的状态
        boolean purchaseSuccess = true;     //用于判定采购单是否成功
        List<PurchaseItemVo> items = doneVo.getItems();
        List<PurchaseDetailEntity> purchaseDetails = new ArrayList<>();
        List<Long> successPurchaseDetailIds = new ArrayList<>();
        for (PurchaseItemVo item : items) {
            //首先剔除状态不正确的采购单明细
            if(item.getStatus() != WareConstant.PurchaseDetailStatusEnum.FAILED.getCode() &&
            item.getStatus() != WareConstant.PurchaseDetailStatusEnum.FINISH.getCode())
                continue;
            if(item.getStatus() == WareConstant.PurchaseDetailStatusEnum.FAILED.getCode()) {
                purchaseSuccess = false;
            } else
                successPurchaseDetailIds.add(item.getItemId());
            PurchaseDetailEntity purchaseDetail = new PurchaseDetailEntity();
            purchaseDetail.setId(item.getItemId());
            purchaseDetail.setStatus(item.getStatus());
            //TODO 可以在purchaseDetail表中添加失败原因字段，以及部分失败时，部分采购成功了多少数量的字段
            purchaseDetails.add(purchaseDetail);
        }
        purchaseDetailService.updateBatchById(purchaseDetails);
        //2、更新采购单的状态
        PurchaseEntity updatePurchase = new PurchaseEntity();
        updatePurchase.setId(doneVo.getId());
        updatePurchase.setStatus(purchaseSuccess? WareConstant.PurchaseStatusEnum.FINISH.getCode() :
                WareConstant.PurchaseStatusEnum.HAS_ERROR.getCode());
        updateById(updatePurchase);
        //3、商品入库
        wareSkuService.addStockByPurchaseDetailIds(successPurchaseDetailIds);
    }

}