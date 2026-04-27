package com.zhangyichuang.medicine.common.rabbitmq.constants;

/**
 * 登录日志相关 MQ 常量。
 */
public final class LoginLogQueueConstants {

    /**
     * 登录日志交换机。
     */
    public static final String EXCHANGE = "medicine.login.log.exchange";

    /**
     * 登录日志队列。
     */
    public static final String QUEUE = "medicine.login.log.queue";

    /**
     * 登录日志路由键。
     */
    public static final String ROUTING = "login.log.record";

    private LoginLogQueueConstants() {
    }
}
