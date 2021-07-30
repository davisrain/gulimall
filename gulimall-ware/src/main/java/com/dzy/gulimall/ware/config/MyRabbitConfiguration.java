package com.dzy.gulimall.ware.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class MyRabbitConfiguration {


    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     *  Exchange、Queue、Binding这些组件在第一次有消费监听的时候才会声明到Rabbit中
     */

    @Bean
    public Exchange stockEventExchange() {
        return new TopicExchange("stock-event-exchange", true, false);
    }

    @Bean
    public Queue stockDelayQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", "stock-event-exchange");
        args.put("x-dead-letter-routing-key", "stock.release");
        args.put("x-message-ttl", 120000);
        return new Queue("stock.delay.queue", true, false,false, args);
    }

    @Bean
    public Queue stockReleaseStockQueue() {
        return new Queue("stock.release.stock.queue", true, false, false);
    }

    @Bean
    public Binding stockLockedBinding() {
        //String destination, DestinationType destinationType, String exchange, String routingKey,
        //			@Nullable Map<String, Object> arguments
        return new Binding("stock.delay.queue",
                Binding.DestinationType.QUEUE,
                "stock-event-exchange",
                "stock.locked",
                null);
    }

    @Bean
    public Binding stockReleaseBinding() {
        return new Binding("stock.release.stock.queue",
                Binding.DestinationType.QUEUE,
                "stock-event-exchange",
                "stock.release.#",
                null);
    }
}
