package com.zhangyichuang.medicine.common.rabbitmq.constants;

/**
 * Agent 提示词同步任务相关的队列/交换机常量。
 */
public final class AgentPromptSyncQueueConstants {

    /**
     * Agent 提示词同步交换机。
     */
    public static final String EXCHANGE = "agent.prompt.sync";

    /**
     * Agent 提示词同步队列。
     */
    public static final String QUEUE = "agent.prompt.sync.q";

    /**
     * Agent 提示词同步路由键。
     */
    public static final String ROUTING = "agent.prompt.sync";

    /**
     * 同步消息队列存活时间，单位毫秒。
     */
    public static final int MESSAGE_TTL_MILLIS = 5 * 60 * 1000;

    /**
     * 工具类禁止实例化。
     */
    private AgentPromptSyncQueueConstants() {
    }
}
