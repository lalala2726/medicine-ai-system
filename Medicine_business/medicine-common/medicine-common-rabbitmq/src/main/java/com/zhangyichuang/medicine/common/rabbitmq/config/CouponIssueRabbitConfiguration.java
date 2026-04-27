package com.zhangyichuang.medicine.common.rabbitmq.config;

import com.zhangyichuang.medicine.common.rabbitmq.constants.CouponIssueQueueConstants;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 优惠券发券 MQ 配置。
 */
@Configuration
public class CouponIssueRabbitConfiguration {

    /**
     * 创建发券交换机。
     *
     * @return 发券交换机
     */
    @Bean
    public DirectExchange couponIssueExchange() {
        return ExchangeBuilder.directExchange(CouponIssueQueueConstants.EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * 创建发券队列。
     *
     * @return 发券队列
     */
    @Bean
    public Queue couponIssueQueue() {
        return QueueBuilder.durable(CouponIssueQueueConstants.QUEUE).build();
    }

    /**
     * 创建发券绑定关系。
     *
     * @param couponIssueQueue    发券队列
     * @param couponIssueExchange 发券交换机
     * @return 发券绑定关系
     */
    @Bean
    public Binding couponIssueBinding(Queue couponIssueQueue, DirectExchange couponIssueExchange) {
        return BindingBuilder.bind(couponIssueQueue)
                .to(couponIssueExchange)
                .with(CouponIssueQueueConstants.ROUTING);
    }
}
