package com.dzy.gulimall.order.rabbit;

import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;


@Component
public class RabbitAcknowledge {

    @RabbitListener(queues = "hello-java-queue")
    public void receiveMessageByManualAck(Message message, Channel channel) throws IOException {
        channel.basicQos(1);
        byte[] body = message.getBody();
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        System.out.println("获取到消息：" + new String(body));
//        try {
//            TimeUnit.SECONDS.sleep(30);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        try {
            if(deliveryTag % 2 == 0) {
                channel.basicAck(deliveryTag, false);
                System.out.println("签收了消息" + deliveryTag);
            } else {
                channel.basicNack(deliveryTag, false, true);
//                channel.basicReject(deliveryTag, true);
                System.out.println("拒签了消息" + deliveryTag);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
