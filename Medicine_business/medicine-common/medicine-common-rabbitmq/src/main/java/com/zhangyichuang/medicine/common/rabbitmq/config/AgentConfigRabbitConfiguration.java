package com.zhangyichuang.medicine.common.rabbitmq.config;

import com.zhangyichuang.medicine.common.rabbitmq.constants.AgentConfigQueueConstants;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Agent 配置刷新消息的基础交换机/队列配置。
 */
@Configuration
public class AgentConfigRabbitConfiguration {

    /**
     * 声明 Agent 配置刷新交换机。
     *
     * @return 直连交换机
     */
    @Bean
    public DirectExchange agentConfigRefreshExchange() {
        return ExchangeBuilder
                .directExchange(AgentConfigQueueConstants.EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * 声明 Agent 配置刷新队列，并设置 5 分钟消息过期。
     *
     * @return 刷新队列
     */
    @Bean
    public Queue agentConfigRefreshQueue() {
        return QueueBuilder
                .durable(AgentConfigQueueConstants.QUEUE)
                .ttl(AgentConfigQueueConstants.MESSAGE_TTL_MILLIS)
                .build();
    }

    /**
     * 绑定 Agent 配置刷新队列与交换机。
     *
     * @param agentConfigRefreshQueue    刷新队列
     * @param agentConfigRefreshExchange 刷新交换机
     * @return 队列绑定关系
     */
    @Bean
    public Binding agentConfigRefreshBinding(Queue agentConfigRefreshQueue,
                                             DirectExchange agentConfigRefreshExchange) {
        return BindingBuilder.bind(agentConfigRefreshQueue)
                .to(agentConfigRefreshExchange)
                .with(AgentConfigQueueConstants.ROUTING);
    }
}
