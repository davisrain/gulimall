package com.dzy.gulimall.coupon.service.impl;

import com.dzy.common.to.MemberPrice;
import com.dzy.common.to.SkuReductionTo;
import com.dzy.gulimall.coupon.entity.MemberPriceEntity;
import com.dzy.gulimall.coupon.entity.SkuLadderEntity;
import com.dzy.gulimall.coupon.service.MemberPriceService;
import com.dzy.gulimall.coupon.service.SkuLadderService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dzy.common.utils.PageUtils;
import com.dzy.common.utils.Query;

import com.dzy.gulimall.coupon.dao.SkuFullReductionDao;
import com.dzy.gulimall.coupon.entity.SkuFullReductionEntity;
import com.dzy.gulimall.coupon.service.SkuFullReductionService;
import org.springframework.transaction.annotation.Transactional;


@Service("skuFullReductionService")
public class SkuFullReductionServiceImpl extends ServiceImpl<SkuFullReductionDao, SkuFullReductionEntity> implements SkuFullReductionService {

    @Autowired
    SkuLadderService skuLadderService;

    @Autowired
    MemberPriceService memberPriceService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SkuFullReductionEntity> page = this.page(
                new Query<SkuFullReductionEntity>().getPage(params),
                new QueryWrapper<SkuFullReductionEntity>()
        );

        return new PageUtils(page);
    }

    @Transactional
    @Override
    public void saveInfo(SkuReductionTo skuReductionTo) {
        //1、sms_sku_full_reduction
            //当满减价格大于0时，才插入满减信息
        if(skuReductionTo.getFullPrice().compareTo(BigDecimal.ZERO) > 0) {
            SkuFullReductionEntity skuFullReduction = new SkuFullReductionEntity();
            BeanUtils.copyProperties(skuReductionTo, skuFullReduction);
            skuFullReduction.setAddOther(skuReductionTo.getPriceStatus());
            save(skuFullReduction);
        }
        //2、sms_sku_ladder
            //当满几件打折的件数大于0时，才插入打折信息
        if(skuReductionTo.getFullCount() > 0) {
            SkuLadderEntity skuLadder = new SkuLadderEntity();
            BeanUtils.copyProperties(skuReductionTo, skuLadder);
            skuLadder.setAddOther(skuReductionTo.getCountStatus());
            skuLadderService.save(skuLadder);
        }
        //3、sms_member_price
        List<MemberPrice> memberPrices = skuReductionTo.getMemberPrice();
        List<MemberPriceEntity> memberPriceEntities = memberPrices.stream().map(memberPrice -> {
            MemberPriceEntity memberPriceEntity = new MemberPriceEntity();
            memberPriceEntity.setSkuId(skuReductionTo.getSkuId());
            memberPriceEntity.setMemberLevelId(memberPrice.getId());
            memberPriceEntity.setMemberLevelName(memberPrice.getName());
            memberPriceEntity.setMemberPrice(memberPrice.getPrice());
            memberPriceEntity.setAddOther(1);
            return memberPriceEntity;
        }).filter(memberPriceEntity -> memberPriceEntity.getMemberPrice().compareTo(BigDecimal.ZERO) > 0) //只有会员价大于0才满足条件
                .collect(Collectors.toList());
        memberPriceService.saveBatch(memberPriceEntities);
    }

}