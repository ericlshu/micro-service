package cn.itcast.mq.spring;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class SpringAmqpTest
{
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Test
    public void testSendMessage2SimpleQueue() throws InterruptedException
    {
        String message = "Hello, Spring AMQP!";
        CorrelationData correlationData = new CorrelationData(UUID.randomUUID().toString());
        correlationData.getFuture().addCallback(
                result ->
                {
                    assert result != null;
                    if (result.isAck())
                        // 消息成功发送到exchange，返回ack
                        log.info("消息发送成功, ID:[{}]", correlationData.getId());
                    else
                        // 消息发送失败，没有到达交换机，返回nack
                        log.warn("消息发送失败, ID:[{}], 原因[{}]", correlationData.getId(), result.getReason());
                },
                // 消息发送过程中出现异常，没有收到回执
                ex -> log.error("消息发送异常, ID:[{}], 原因[{}]", correlationData.getId(), ex.getMessage())
        );
        rabbitTemplate.convertAndSend("amq.topic", "simple.test", message, correlationData);
        Thread.sleep(1000);
    }

    @Test
    public void testDurableMessage()
    {
        Message message = MessageBuilder
                .withBody("RabbitMQ高级特性之消息持久化".getBytes(StandardCharsets.UTF_8))
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .build();
        rabbitTemplate.convertAndSend("simple.queue", message);
    }

    /**
     * 消息超时时间与队列超时时间均有配置时，以较短的时间配置为准
     */
    @Test
    public void testTTLMessage()
    {
        Message message = MessageBuilder
                .withBody("RabbitMQ高级特性之TTL消息".getBytes(StandardCharsets.UTF_8))
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .setExpiration("5000")
                .build();
        rabbitTemplate.convertAndSend("ttl.direct", "ttl", message);
        log.info("TTL消息[{}]发送成功!", message);
    }

    @Test
    public void testSendDelayMessage()
    {
        String msg = "SpringAMQP延迟队列消息！";
        Message message = MessageBuilder
                .withBody(msg.getBytes(StandardCharsets.UTF_8))
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .setHeader("x-delay", 5000)
                .build();
        log.info("延迟消息[{}]发送成功!", msg);
        CorrelationData correlationData = new CorrelationData(UUID.randomUUID().toString());
        rabbitTemplate.convertAndSend("delay.direct", "delay", message, correlationData);
    }

    @Test
    public void testLazyQueue()
    {
        String msg = "RabbitMQ惰性队列消息";
        for (int i = 0; i < 10_000; i++)
        {
            Message message = MessageBuilder
                    .withBody((msg + "[" + i + "]").getBytes(StandardCharsets.UTF_8))
                    .setDeliveryMode(MessageDeliveryMode.NON_PERSISTENT)
                    .build();
            rabbitTemplate.convertAndSend("lazy.queue", message);
        }
    }

    @Test
    public void testNormalQueue()
    {
        String msg = "RabbitMQ普通队列消息";
        for (int i = 0; i < 10_000; i++)
        {
            Message message = MessageBuilder
                    .withBody((msg + "[" + i + "]").getBytes(StandardCharsets.UTF_8))
                    .setDeliveryMode(MessageDeliveryMode.NON_PERSISTENT)
                    .build();
            rabbitTemplate.convertAndSend("normal.queue", message);
        }
    }
}
