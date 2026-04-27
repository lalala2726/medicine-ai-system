package com.zhangyichuang.medicine.common.rabbitmq.config;

import com.zhangyichuang.medicine.common.rabbitmq.constants.LoginLogQueueConstants;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 登录日志 MQ 配置。
 */
@Configuration
public class LoginLogRabbitConfiguration {

    @Bean
    public DirectExchange loginLogExchange() {
        return ExchangeBuilder
                .directExchange(LoginLogQueueConstants.EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public Queue loginLogQueue() {
        return QueueBuilder.durable(LoginLogQueueConstants.QUEUE).build();
    }

    @Bean
    public Binding loginLogBinding(Queue loginLogQueue, DirectExchange loginLogExchange) {
        return BindingBuilder.bind(loginLogQueue)
                .to(loginLogExchange)
                .with(LoginLogQueueConstants.ROUTING);
    }
}
