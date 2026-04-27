package com.zhangyichuang.medicine.common.rabbitmq.constants;

/**
 * Agent 提示词刷新相关的队列/交换机常量。
 */
public final class AgentPromptQueueConstants {

    /**
     * Agent 提示词刷新交换机。
     */
    public static final String EXCHANGE = "agent.prompt.refresh";

    /**
     * Agent 提示词刷新队列。
     */
    public static final String QUEUE = "agent.prompt.refresh.q";

    /**
     * Agent 提示词刷新路由键。
     */
    public static final String ROUTING = "agent.prompt.refresh";

    /**
     * 刷新消息队列存活时间，单位毫秒。
     */
    public static final int MESSAGE_TTL_MILLIS = 5 * 60 * 1000;

    private AgentPromptQueueConstants() {
    }
}
