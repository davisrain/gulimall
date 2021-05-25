package com.dzy.gulimall.coupon.dao;

import com.dzy.gulimall.coupon.entity.CouponEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券信息
 * 
 * @author zhengyu_dai
 * @email zhengyu_dai@foxmail.com
 * @date 2021-05-25 11:18:17
 */
@Mapper
public interface CouponDao extends BaseMapper<CouponEntity> {
	
}
