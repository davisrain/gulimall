package com.dzy.gulimall.order.rabbit;


import com.dzy.gulimall.order.entity.OrderReturnReasonEntity;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
//@Component
public class RabbitListenerAnnotation {

    /**
     * @RabbitListener 注解可以标注在方法上，表示这个方法是一个消息消费者，
     * 并且需要使用queues属性来监听消费的队列。
     * queues属性可以指定多个队列，即一个消费者可以监听多个队列。
     *
     * 接收方法的参数：
     *  1、Message类型，是amqp.code包下的类，里面包含了消息的消息头和消息体
     *      message.getMessageProperties()方法可以获得消息头属性。
     *      message.getBody()方法可以获得消息体的内容的字节数组。
     *  2、T 可以使用传入消息的本身类型
     *  3、可以使用Channel类型，表示接收消息的通道
     */
    @RabbitListener(queues = {"hello-java-queue"})
    public void receiveMessage(Message message,
                               OrderReturnReasonEntity reason,
                               Channel channel) {
        log.info("接收到的消息为：{}", message);
        MessageProperties messageProperties = message.getMessageProperties();
        byte[] body = message.getBody();
        log.info("消息内容为：{}", reason);
        log.info("接收消息的通道为：{}\r\n", channel);
    }

    /**
     * 如果有多个消费者监听同一个队列，每条消息只会有一个消费者收到（前提是交换机类型是direct）；
     */
    @RabbitListener(queues = {"hello-java-queue"})
    public void consumer1(Message message) {
        byte[] body = message.getBody();
        log.info("消费者1接收到的消息内容为：{}", new String(body));
    }

    @RabbitListener(queues = {"hello-java-queue"})
    public void consumer2(Message message) {
        byte[] body = message.getBody();
        log.info("消费者2接收到的消息内容为：{}", new String(body));
    }

    /**
     * 如果消费者处理消息的方法耗时较长，下一个消息会在上一个消息处理完成之后才会消费者接收到。
     */
    @RabbitListener(queues = {"hello-java-queue"})
    public void handleMessageForLongTime(Message message) throws InterruptedException {
        byte[] body = message.getBody();
        log.info("接收到消息：{}", new String(body));
        TimeUnit.SECONDS.sleep(3);
        log.info("消息处理完成");
    }
}
