package com.zhangyichuang.medicine.admin.listener;

import com.zhangyichuang.medicine.admin.service.CouponAdminService;
import com.zhangyichuang.medicine.common.rabbitmq.constants.CouponBatchIssueQueueConstants;
import com.zhangyichuang.medicine.model.mq.CouponBatchIssueMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 优惠券批量发券 MQ 消费者。
 */
@Component
@RequiredArgsConstructor
public class CouponBatchIssueConsumer {

    /**
     * 管理端优惠券服务。
     */
    private final CouponAdminService couponAdminService;

    /**
     * 消费批量发券消息并拆分单用户发券消息。
     *
     * @param message 批量发券消息
     */
    @RabbitListener(queues = CouponBatchIssueQueueConstants.QUEUE)
    public void handle(CouponBatchIssueMessage message) {
        couponAdminService.consumeBatchIssueCoupon(message);
    }
}
