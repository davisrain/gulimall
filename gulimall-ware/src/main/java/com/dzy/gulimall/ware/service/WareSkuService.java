package com.dzy.gulimall.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dzy.common.to.SkuHasStockTo;
import com.dzy.common.to.mq.StockLockTo;
import com.dzy.common.utils.PageUtils;
import com.dzy.gulimall.ware.entity.WareSkuEntity;
import com.dzy.gulimall.ware.to.WareLockTo;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 商品库存
 *
 * @author zhengyu_dai
 * @email zhengyu_dai@foxmail.com
 * @date 2021-05-24 23:29:19
 */
public interface WareSkuService extends IService<WareSkuEntity> {

    PageUtils queryPage(Map<String, Object> params);


    void addStockByPurchaseDetailIds(List<Long> successPurchaseDetailIds);

    List<SkuHasStockTo> getHasStockBySkuIds(List<Long> skuIds);

    void lockStock(WareLockTo wareLockTo);

    void releaseStock(StockLockTo stockLockTo, Message message, Channel channel) throws Exception;
}

