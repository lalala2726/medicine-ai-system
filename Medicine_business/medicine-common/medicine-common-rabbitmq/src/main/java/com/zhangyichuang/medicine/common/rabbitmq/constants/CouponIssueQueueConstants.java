package com.zhangyichuang.medicine.common.rabbitmq.constants;

/**
 * 优惠券发券队列常量。
 */
public final class CouponIssueQueueConstants {

    /**
     * 发券交换机。
     */
    public static final String EXCHANGE = "medicine.coupon.issue.exchange";

    /**
     * 发券消费队列。
     */
    public static final String QUEUE = "medicine.coupon.issue.queue";

    /**
     * 发券路由键。
     */
    public static final String ROUTING = "coupon.issue.create";

    private CouponIssueQueueConstants() {
    }
}
