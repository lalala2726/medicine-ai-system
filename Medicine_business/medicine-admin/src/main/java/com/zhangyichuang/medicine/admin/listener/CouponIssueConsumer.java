package com.zhangyichuang.medicine.admin.listener;

import com.zhangyichuang.medicine.admin.service.CouponAdminService;
import com.zhangyichuang.medicine.common.rabbitmq.constants.CouponIssueQueueConstants;
import com.zhangyichuang.medicine.model.mq.CouponIssueMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 优惠券发券 MQ 消费者。
 */
@Component
@RequiredArgsConstructor
public class CouponIssueConsumer {

    /**
     * 管理端优惠券服务。
     */
    private final CouponAdminService couponAdminService;

    /**
     * 消费发券消息并执行落库。
     *
     * @param message 发券消息
     */
    @RabbitListener(queues = CouponIssueQueueConstants.QUEUE)
    public void handle(CouponIssueMessage message) {
        couponAdminService.consumeIssueCoupon(message);
    }
}
