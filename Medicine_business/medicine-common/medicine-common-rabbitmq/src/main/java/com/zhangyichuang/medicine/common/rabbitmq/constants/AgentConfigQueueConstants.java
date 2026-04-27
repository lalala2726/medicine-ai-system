package com.zhangyichuang.medicine.common.rabbitmq.constants;

/**
 * Agent 配置刷新相关的队列/交换机常量。
 */
public final class AgentConfigQueueConstants {

    /**
     * Agent 配置刷新交换机。
     */
    public static final String EXCHANGE = "agent.config.refresh";

    /**
     * Agent 配置刷新队列。
     */
    public static final String QUEUE = "agent.config.refresh.q";

    /**
     * Agent 配置刷新路由键。
     */
    public static final String ROUTING = "agent.config.refresh";

    /**
     * 刷新消息队列存活时间，单位毫秒。
     */
    public static final int MESSAGE_TTL_MILLIS = 5 * 60 * 1000;

    private AgentConfigQueueConstants() {
    }
}
