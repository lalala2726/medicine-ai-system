package com.zhangyichuang.medicine.common.rabbitmq.config;

import com.zhangyichuang.medicine.common.rabbitmq.constants.OrderTimeoutQueueConstants;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 订单支付超时消息的 RabbitMQ 基础设施配置。
 */
@Configuration
public class OrderTimeoutRabbitConfiguration {

    /**
     * 声明订单超时延迟交换机。
     *
     * @return 订单超时延迟交换机
     */
    @Bean
    public DirectExchange orderTimeoutDelayExchange() {
        return ExchangeBuilder
                .directExchange(OrderTimeoutQueueConstants.DELAY_EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * 声明订单超时延迟队列，并将过期消息投递到处理交换机。
     *
     * @return 订单超时延迟队列
     */
    @Bean
    public Queue orderTimeoutDelayQueue() {
        return QueueBuilder
                .durable(OrderTimeoutQueueConstants.DELAY_QUEUE)
                .deadLetterExchange(OrderTimeoutQueueConstants.PROCESS_EXCHANGE)
                .deadLetterRoutingKey(OrderTimeoutQueueConstants.PROCESS_ROUTING_KEY)
                .build();
    }

    /**
     * 绑定订单超时延迟队列与延迟交换机。
     *
     * @param orderTimeoutDelayQueue    订单超时延迟队列
     * @param orderTimeoutDelayExchange 订单超时延迟交换机
     * @return 队列绑定关系
     */
    @Bean
    public Binding orderTimeoutDelayBinding(Queue orderTimeoutDelayQueue,
                                            DirectExchange orderTimeoutDelayExchange) {
        return BindingBuilder.bind(orderTimeoutDelayQueue)
                .to(orderTimeoutDelayExchange)
                .with(OrderTimeoutQueueConstants.DELAY_ROUTING_KEY);
    }

    /**
     * 声明订单超时处理交换机。
     *
     * @return 订单超时处理交换机
     */
    @Bean
    public DirectExchange orderTimeoutProcessExchange() {
        return ExchangeBuilder
                .directExchange(OrderTimeoutQueueConstants.PROCESS_EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * 声明订单超时处理队列。
     *
     * @return 订单超时处理队列
     */
    @Bean
    public Queue orderTimeoutProcessQueue() {
        return QueueBuilder
                .durable(OrderTimeoutQueueConstants.PROCESS_QUEUE)
                .build();
    }

    /**
     * 绑定订单超时处理队列与处理交换机。
     *
     * @param orderTimeoutProcessQueue    订单超时处理队列
     * @param orderTimeoutProcessExchange 订单超时处理交换机
     * @return 队列绑定关系
     */
    @Bean
    public Binding orderTimeoutProcessBinding(Queue orderTimeoutProcessQueue,
                                              DirectExchange orderTimeoutProcessExchange) {
        return BindingBuilder.bind(orderTimeoutProcessQueue)
                .to(orderTimeoutProcessExchange)
                .with(OrderTimeoutQueueConstants.PROCESS_ROUTING_KEY);
    }
}
