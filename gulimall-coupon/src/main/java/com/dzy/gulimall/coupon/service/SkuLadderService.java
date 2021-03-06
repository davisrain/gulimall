package com.dzy.gulimall.coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dzy.common.utils.PageUtils;
import com.dzy.gulimall.coupon.entity.SkuLadderEntity;

import java.util.Map;

/**
 * 商品阶梯价格
 *
 * @author zhengyu_dai
 * @email zhengyu_dai@foxmail.com
 * @date 2021-05-25 11:18:16
 */
public interface SkuLadderService extends IService<SkuLadderEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

