package com.dzy.gulimall.coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dzy.common.utils.PageUtils;
import com.dzy.gulimall.coupon.entity.SmsSpuBoundsEntity;

import java.util.Map;

/**
 * 商品spu积分设置
 *
 * @author zhengyu_dai
 * @email zhengyu_dai@foxmail.com
 * @date 2021-05-24 23:00:16
 */
public interface SmsSpuBoundsService extends IService<SmsSpuBoundsEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

