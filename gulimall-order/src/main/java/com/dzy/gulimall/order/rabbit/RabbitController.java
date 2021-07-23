package com.dzy.gulimall.order.rabbit;

import com.dzy.gulimall.order.entity.OrderEntity;
import com.dzy.gulimall.order.entity.OrderReturnReasonEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.UUID;

@Slf4j
@RestController
public class RabbitController {

    @Autowired
    RabbitTemplate rabbitTemplate;

    @GetMapping("/sendMessage")
    public String sendMessage(@RequestParam(value = "num", defaultValue = "10") Integer num) {
        for(int i = 0; i < num; i++) {
            OrderReturnReasonEntity reason = new OrderReturnReasonEntity();
            reason.setId(1L);
            reason.setCreateTime(new Date());
            reason.setName("哈哈" + i);
//            rabbitTemplate.convertAndSend("hello-java-exchange", "hello.java", reason,
//                    new CorrelationData(UUID.randomUUID().toString()));
            rabbitTemplate.convertAndSend("hello-java-exchange", "hello.java", reason);
            log.info("消息发送完成，内容是：{}", reason);
        }
        return "ok";
    }

    @GetMapping("/sendMultiTypeMessage")
    public String sendMultiTypeMessage(@RequestParam(value = "num", defaultValue = "10") Integer num) {
        for(int i = 0; i < num; i++) {
            if(i % 2 == 0) {
                OrderReturnReasonEntity reason = new OrderReturnReasonEntity();
                reason.setId(1L);
                reason.setCreateTime(new Date());
                reason.setName("哈哈" + i);
                rabbitTemplate.convertAndSend("hello-java-exchange", "hello.java", reason);
            } else {
                OrderEntity order = new OrderEntity();
                order.setId(1L);
                order.setCreateTime(new Date());
                order.setNote("订单" + i);
                rabbitTemplate.convertAndSend("hello-java-exchange", "hello.java11", order);
            }
        }
        return "ok";
    }
}
