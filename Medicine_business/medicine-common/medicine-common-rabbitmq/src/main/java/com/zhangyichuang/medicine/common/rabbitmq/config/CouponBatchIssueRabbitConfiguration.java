package com.zhangyichuang.medicine.common.rabbitmq.config;

import com.zhangyichuang.medicine.common.rabbitmq.constants.CouponBatchIssueQueueConstants;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 优惠券批量发券 MQ 配置。
 */
@Configuration
public class CouponBatchIssueRabbitConfiguration {

    /**
     * 创建批量发券交换机。
     *
     * @return 批量发券交换机
     */
    @Bean
    public DirectExchange couponBatchIssueExchange() {
        return ExchangeBuilder.directExchange(CouponBatchIssueQueueConstants.EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * 创建批量发券队列。
     *
     * @return 批量发券队列
     */
    @Bean
    public Queue couponBatchIssueQueue() {
        return QueueBuilder.durable(CouponBatchIssueQueueConstants.QUEUE).build();
    }

    /**
     * 创建批量发券绑定关系。
     *
     * @param couponBatchIssueQueue    批量发券队列
     * @param couponBatchIssueExchange 批量发券交换机
     * @return 批量发券绑定关系
     */
    @Bean
    public Binding couponBatchIssueBinding(Queue couponBatchIssueQueue, DirectExchange couponBatchIssueExchange) {
        return BindingBuilder.bind(couponBatchIssueQueue)
                .to(couponBatchIssueExchange)
                .with(CouponBatchIssueQueueConstants.ROUTING);
    }
}
