package com.zhangyichuang.medicine.common.rabbitmq.constants;

/**
 * 操作日志相关 MQ 常量。
 */
public final class OperationLogQueueConstants {

    /**
     * 操作日志交换机。
     */
    public static final String EXCHANGE = "medicine.operation.log.exchange";

    /**
     * 操作日志队列。
     */
    public static final String QUEUE = "medicine.operation.log.queue";

    /**
     * 操作日志路由键。
     */
    public static final String ROUTING = "operation.log.record";

    private OperationLogQueueConstants() {
    }
}
