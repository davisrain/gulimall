package com.dzy.gulimall.ware.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.dzy.common.utils.R;
import com.dzy.gulimall.ware.feign.MemberFeignService;
import com.dzy.gulimall.ware.vo.FareVo;
import com.dzy.gulimall.ware.vo.MemberAddressVo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dzy.common.utils.PageUtils;
import com.dzy.common.utils.Query;

import com.dzy.gulimall.ware.dao.WareInfoDao;
import com.dzy.gulimall.ware.entity.WareInfoEntity;
import com.dzy.gulimall.ware.service.WareInfoService;


@Service("wareInfoService")
public class WareInfoServiceImpl extends ServiceImpl<WareInfoDao, WareInfoEntity> implements WareInfoService {

    @Autowired
    MemberFeignService memberFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<WareInfoEntity> queryWrapper = new QueryWrapper<>();
        String key = (String) params.get("key");
        if(StringUtils.isNotBlank(key)) {
            queryWrapper.and(w -> {
                w.eq("id", key).or().like("name", key)
                        .or().like("address", key)
                        .or().like("areacode", key);
            });
        }
        IPage<WareInfoEntity> page = this.page(
                new Query<WareInfoEntity>().getPage(params),
                queryWrapper
        );
        return new PageUtils(page);
    }

    @Override
    public FareVo getFare(Long addressId) {
        R r = memberFeignService.addressInfo(addressId);
        if(r.getCode() == 0) {
            MemberAddressVo address = r.getData("memberReceiveAddress", new TypeReference<MemberAddressVo>() {});
            //模拟运费生成，直接用手机号最后一位
            String phone = address.getPhone();
            String fare = phone.substring(10);
            FareVo fareVo = new FareVo();
            fareVo.setAddress(address);
            fareVo.setFare(new BigDecimal(fare));
            return fareVo;
        }
        return null;
    }

}