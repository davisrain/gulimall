
import com.dzy.gulimall.order.entity.OrderReturnReasonEntity;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Date;

@Slf4j
@SpringBootTest(classes = {com.dzy.gulimall.order.GulimallOrderMain.class})
public class MyTest {

    @Autowired
    AmqpAdmin amqpAdmin;

    /**
     *  1、如何创建exchange、queue、binding
     *      1.1、使用AmqpAdmin进行创建
     */
    @Test
    public void createExchange() {
        //String name, boolean durable, boolean autoDelete, Map<String, Object> arguments
        DirectExchange directExchange = new DirectExchange("hello-java-exchange", true, false);
        amqpAdmin.declareExchange(directExchange);
        log.info("Exchange【{}】创建成功", "hello-java-exchange");
    }

    @Test
    public void createQueue() {
        //String name, boolean durable, boolean exclusive, boolean autoDelete,
        //			@Nullable Map<String, Object> arguments
        Queue queue = new Queue("hello-java-queue", true, false, false);
        amqpAdmin.declareQueue(queue);
        log.info("Queue【{}】创建成功", "hello-java-queue");
    }

    @Test
    public void createBinding() {
        /**
         * String destination, 【目的地】
         * DestinationType destinationType, 【目的地的类型，因为交换机可以绑定交换机，也可以绑定队列】
         * String exchange, 【需要绑定的交换机】
         * String routingKey,【路由键】
         * @Nullable Map<String, Object> arguments
         */
        Binding binding = new Binding("hello-java-queue",
                Binding.DestinationType.QUEUE,
                "hello-java-exchange",
                "hello.java", null);
        amqpAdmin.declareBinding(binding);
        log.info("Binding【{}】创建成功", "hello-java-binding");
    }

    @Autowired
    RabbitTemplate rabbitTemplate;

    /**
     *  2、如何收发信息
     *      2.1、使用RabbitTemplate发消息
     */
    @Test
    public void sendMessageTest() {
//        String msg = "Hello World!";
//        rabbitTemplate.convertAndSend("hello-java-exchange", "hello.java", msg);
//        log.info("消息发送成功{}", msg);
        OrderReturnReasonEntity reason = new OrderReturnReasonEntity();
        reason.setId(1L);
        reason.setCreateTime(new Date());
        reason.setName("哈哈！");
        rabbitTemplate.convertAndSend("hello-java-exchange", "hello.java", reason);
    }
}
