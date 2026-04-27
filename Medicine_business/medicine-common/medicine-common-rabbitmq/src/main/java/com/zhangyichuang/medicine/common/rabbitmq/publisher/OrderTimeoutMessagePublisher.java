package com.zhangyichuang.medicine.common.rabbitmq.publisher;

import com.zhangyichuang.medicine.common.rabbitmq.constants.OrderTimeoutQueueConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * 订单支付超时消息发布器。
 */
@Component
public class OrderTimeoutMessagePublisher {

    /**
     * 日志记录器。
     */
    private static final Logger log = LoggerFactory.getLogger(OrderTimeoutMessagePublisher.class);

    /**
     * RabbitMQ 消息发送模板。
     */
    private final RabbitTemplate rabbitTemplate;

    /**
     * 构造订单支付超时消息发布器。
     *
     * @param rabbitTemplate RabbitMQ 消息发送模板
     */
    public OrderTimeoutMessagePublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * 发布订单支付超时延迟消息。
     *
     * @param orderNo 订单编号
     * @param delay   延迟时长
     * @param unit    延迟时间单位
     */
    public void publishOrderTimeout(String orderNo, long delay, TimeUnit unit) {
        Assert.hasText(orderNo, "订单号不能为空");
        Assert.notNull(unit, "延迟时间单位不能为空");
        long delayMillis = unit.toMillis(delay);
        Assert.isTrue(delayMillis > 0, "延迟毫秒数必须大于0");
        Message message = MessageBuilder
                .withBody(orderNo.getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_TEXT_PLAIN)
                .setContentEncoding(StandardCharsets.UTF_8.name())
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .setExpiration(Long.toString(delayMillis))
                .build();
        rabbitTemplate.send(
                OrderTimeoutQueueConstants.DELAY_EXCHANGE,
                OrderTimeoutQueueConstants.DELAY_ROUTING_KEY,
                message
        );
        log.info("订单 {} 已发布 RabbitMQ 延迟过期消息，延迟 {} ms", orderNo, delayMillis);
    }
}
