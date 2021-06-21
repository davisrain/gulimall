package com.dzy.gulimall.ware.service.impl;

import com.dzy.common.constant.WareConstant;
import com.dzy.gulimall.ware.Vo.PurchaseMergeVo;
import com.dzy.gulimall.ware.entity.PurchaseDetailEntity;
import com.dzy.gulimall.ware.service.PurchaseDetailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
        List<Long> items = mergeVo.getItems();
        Long finalPurchaseId = purchaseId;
        List<PurchaseDetailEntity> purchaseDetails = items.stream().map(item -> {
            PurchaseDetailEntity purchaseDetail = new PurchaseDetailEntity();
            purchaseDetail.setId(item);
            purchaseDetail.setPurchaseId(finalPurchaseId);
            purchaseDetail.setStatus(WareConstant.PurchaseDetailStatusEnum.ASSIGNED.getCode());
            return purchaseDetail;
        }).collect(Collectors.toList());
        purchaseDetailService.updateBatchById(purchaseDetails);
        //当我们是更新purchase的时候，需要修改对应purchase的更新时间
        PurchaseEntity purchase = this.getById(purchaseId);
        purchase.setUpdateTime(new Date());
        this.updateById(purchase);
    }

}