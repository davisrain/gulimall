package com.dzy.gulimall.ware.listener;

import com.dzy.common.to.mq.StockLockTo;
import com.dzy.gulimall.ware.service.WareSkuService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;


@Service
@RabbitListener(queues = "stock.release.stock.queue")
public class StockReleaseListener {

    @Autowired
    WareSkuService wareSkuService;

    /**
     *  1、订单下单成功，但是由于超时没有支付或者用户自动取消的原因，导致订单状态不成功，需要解锁库存
     *  2、远程调用库存锁定方法成功，但是下单业务在后面的逻辑出现异常，导致订单数据回滚，此时没有订单数据，需要解锁库存
     *  3、库存锁定业务出现异常，导致订单业务也回滚，此时库存和订单、以及库存锁定任务单都没有数据，但是rabbitmq里面可能会有
     *      部分成功锁定库存的消息，此时接收到消息不需要解锁库存。
     */
    @RabbitHandler
    public void releaseStock(StockLockTo stockLockTo, Message message, Channel channel) throws IOException {
        try {
            wareSkuService.releaseStock(stockLockTo, message, channel);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), true);
        }
    }

}
