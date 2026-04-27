package com.zhangyichuang.medicine.common.rabbitmq.constants;

/**
 * 优惠券批量发券队列常量。
 */
public final class CouponBatchIssueQueueConstants {

    /**
     * 批量发券交换机。
     */
    public static final String EXCHANGE = "medicine.coupon.batch.issue.exchange";

    /**
     * 批量发券消费队列。
     */
    public static final String QUEUE = "medicine.coupon.batch.issue.queue";

    /**
     * 批量发券路由键。
     */
    public static final String ROUTING = "coupon.batch.issue.create";

    private CouponBatchIssueQueueConstants() {
    }
}
