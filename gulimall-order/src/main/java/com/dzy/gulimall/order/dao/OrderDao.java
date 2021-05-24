package com.dzy.gulimall.order.dao;

import com.dzy.gulimall.order.entity.OrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单
 * 
 * @author zhengyu_dai
 * @email zhengyu_dai@foxmail.com
 * @date 2021-05-24 23:21:32
 */
@Mapper
public interface OrderDao extends BaseMapper<OrderEntity> {
	
}
