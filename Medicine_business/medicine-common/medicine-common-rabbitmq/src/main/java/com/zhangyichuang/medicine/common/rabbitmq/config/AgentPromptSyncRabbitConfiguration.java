package com.zhangyichuang.medicine.common.rabbitmq.config;

import com.zhangyichuang.medicine.common.rabbitmq.constants.AgentPromptSyncQueueConstants;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Agent 提示词同步任务消息的基础交换机/队列配置。
 */
@Configuration
public class AgentPromptSyncRabbitConfiguration {

    /**
     * 声明 Agent 提示词同步交换机。
     *
     * @return 直连交换机
     */
    @Bean
    public DirectExchange agentPromptSyncExchange() {
        return ExchangeBuilder
                .directExchange(AgentPromptSyncQueueConstants.EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * 声明 Agent 提示词同步队列，并设置 5 分钟消息过期。
     *
     * @return 同步队列
     */
    @Bean
    public Queue agentPromptSyncQueue() {
        return QueueBuilder
                .durable(AgentPromptSyncQueueConstants.QUEUE)
                .ttl(AgentPromptSyncQueueConstants.MESSAGE_TTL_MILLIS)
                .build();
    }

    /**
     * 绑定 Agent 提示词同步队列与交换机。
     *
     * @param agentPromptSyncQueue    同步队列
     * @param agentPromptSyncExchange 同步交换机
     * @return 队列绑定关系
     */
    @Bean
    public Binding agentPromptSyncBinding(Queue agentPromptSyncQueue,
                                          DirectExchange agentPromptSyncExchange) {
        return BindingBuilder.bind(agentPromptSyncQueue)
                .to(agentPromptSyncExchange)
                .with(AgentPromptSyncQueueConstants.ROUTING);
    }
}
