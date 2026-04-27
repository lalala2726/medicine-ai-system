package com.zhangyichuang.medicine.common.rabbitmq.config;

import com.zhangyichuang.medicine.common.rabbitmq.constants.OperationLogQueueConstants;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 操作日志 MQ 配置。
 */
@Configuration
public class OperationLogRabbitConfiguration {

    @Bean
    public DirectExchange operationLogExchange() {
        return ExchangeBuilder
                .directExchange(OperationLogQueueConstants.EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public Queue operationLogQueue() {
        return QueueBuilder.durable(OperationLogQueueConstants.QUEUE).build();
    }

    @Bean
    public Binding operationLogBinding(Queue operationLogQueue, DirectExchange operationLogExchange) {
        return BindingBuilder.bind(operationLogQueue)
                .to(operationLogExchange)
                .with(OperationLogQueueConstants.ROUTING);
    }
}
