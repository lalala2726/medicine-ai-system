package com.zhangyichuang.medicine.common.rabbitmq.config;

import com.zhangyichuang.medicine.common.rabbitmq.constants.AgentPromptQueueConstants;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Agent 提示词刷新消息的基础交换机/队列配置。
 */
@Configuration
public class AgentPromptRabbitConfiguration {

    /**
     * 声明 Agent 提示词刷新交换机。
     *
     * @return 直连交换机
     */
    @Bean
    public DirectExchange agentPromptRefreshExchange() {
        return ExchangeBuilder
                .directExchange(AgentPromptQueueConstants.EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * 声明 Agent 提示词刷新队列，并设置 5 分钟消息过期。
     *
     * @return 刷新队列
     */
    @Bean
    public Queue agentPromptRefreshQueue() {
        return QueueBuilder
                .durable(AgentPromptQueueConstants.QUEUE)
                .ttl(AgentPromptQueueConstants.MESSAGE_TTL_MILLIS)
                .build();
    }

    /**
     * 绑定 Agent 提示词刷新队列与交换机。
     *
     * @param agentPromptRefreshQueue    刷新队列
     * @param agentPromptRefreshExchange 刷新交换机
     * @return 队列绑定关系
     */
    @Bean
    public Binding agentPromptRefreshBinding(Queue agentPromptRefreshQueue,
                                             DirectExchange agentPromptRefreshExchange) {
        return BindingBuilder.bind(agentPromptRefreshQueue)
                .to(agentPromptRefreshExchange)
                .with(AgentPromptQueueConstants.ROUTING);
    }
}
