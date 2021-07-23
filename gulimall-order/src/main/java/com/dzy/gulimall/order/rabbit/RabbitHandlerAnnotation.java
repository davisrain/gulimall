package com.dzy.gulimall.order.rabbit;


import com.dzy.gulimall.order.entity.OrderEntity;
import com.dzy.gulimall.order.entity.OrderReturnReasonEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
//@Component
@RabbitListener(queues = "hello-java-queue")
public class RabbitHandlerAnnotation {

    @RabbitHandler
    public void receiveOrderReturnReason(OrderReturnReasonEntity reason) {
        log.info("接收到OrderReturnReason类型的消息：{}", reason);
    }

    @RabbitHandler
    public void receiveOrder(OrderEntity order) {
        log.info("接收到Order类型的消息：{}", order);
    }
}
