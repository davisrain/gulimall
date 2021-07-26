package com.dzy.gulimall.order.config;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@EnableRabbit
@Configuration
public class MyRabbitConfiguration {

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @PostConstruct      //表示在MyRabbitConfiguration对象构造完后执行这个方法
    public void customRabbitTemplate() {
        rabbitTemplate.setConfirmCallback(new RabbitTemplate.ConfirmCallback() {
            /**
             *
             * @param correlationData   消息的唯一表示，里面有消息的id
             * @param ack                broker是否成功收到消息
             * @param cause              失败的原因
             */
            @Override
            public void confirm(CorrelationData correlationData, boolean ack, String cause) {
                System.out.println("CorrelationData : " + correlationData);
                System.out.println("ack : " + ack);
                System.out.println("cause : " + cause);
            }
        });

        rabbitTemplate.setReturnCallback(new RabbitTemplate.ReturnCallback() {
            /**
             *  当消息从交换机路由到队列失败时才会调用这个方法
             * @param message       投递到queue失败的消息
             * @param replyCode     失败的状态码
             * @param replyText     失败的信息
             * @param exchange      投递消息的交换机
             * @param routingKey    消息的路由键
             */
            @Override
            public void returnedMessage(Message message, int replyCode, String replyText, String exchange, String routingKey) {
                System.out.println("message : " + message);
                System.out.println("replyCode : " + replyCode);
                System.out.println("replyText : " + replyText);
                System.out.println("exchange : " + exchange);
                System.out.println("routingKey : " + routingKey);
            }
        });
    }
}
