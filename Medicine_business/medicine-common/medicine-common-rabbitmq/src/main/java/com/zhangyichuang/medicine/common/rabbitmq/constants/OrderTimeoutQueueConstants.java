package com.zhangyichuang.medicine.common.rabbitmq.constants;

/**
 * 订单支付超时相关的交换机、队列和路由键常量。
 */
public final class OrderTimeoutQueueConstants {

    /**
     * 订单超时延迟交换机。
     */
    public static final String DELAY_EXCHANGE = "order.timeout.delay.exchange";

    /**
     * 订单超时延迟队列。
     */
    public static final String DELAY_QUEUE = "order.timeout.delay.queue";

    /**
     * 订单超时延迟路由键。
     */
    public static final String DELAY_ROUTING_KEY = "order.timeout.delay";

    /**
     * 订单超时处理交换机。
     */
    public static final String PROCESS_EXCHANGE = "order.timeout.process.exchange";

    /**
     * 订单超时处理队列。
     */
    public static final String PROCESS_QUEUE = "order.timeout.process.queue";

    /**
     * 订单超时处理路由键。
     */
    public static final String PROCESS_ROUTING_KEY = "order.timeout.process";

    private OrderTimeoutQueueConstants() {
    }
}
