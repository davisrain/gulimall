package com.dzy.gulimall.order.listener;

import com.dzy.gulimall.order.entity.OrderEntity;
import com.dzy.gulimall.order.entity.OrderItemEntity;
import com.dzy.gulimall.order.service.OrderService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RabbitListener(queues = "order.release.order.queue")
public class OrderCloseListener {

    @Autowired
    OrderService orderService;

    @RabbitHandler
    public void closeOrder(OrderEntity order, Message message, Channel channel) throws IOException {
        try {
            orderService.closeOrder(order);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), true);
        }
    }
}
