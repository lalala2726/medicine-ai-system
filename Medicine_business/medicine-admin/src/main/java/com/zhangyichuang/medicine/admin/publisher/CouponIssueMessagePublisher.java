package com.zhangyichuang.medicine.admin.publisher;

import com.zhangyichuang.medicine.common.core.enums.ResponseCode;
import com.zhangyichuang.medicine.common.core.exception.ServiceException;
import com.zhangyichuang.medicine.common.rabbitmq.constants.CouponIssueQueueConstants;
import com.zhangyichuang.medicine.model.mq.CouponIssueMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 优惠券发券消息发布器。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponIssueMessagePublisher {

    /**
     * RabbitMQ 消息发送模板。
     */
    private final RabbitTemplate rabbitTemplate;

    /**
     * 发布发券消息。
     *
     * @param message 发券消息
     */
    public void publish(CouponIssueMessage message) {
        if (message == null || message.getTemplateId() == null || message.getUserId() == null) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "发券消息不能为空");
        }
        try {
            rabbitTemplate.convertAndSend(
                    CouponIssueQueueConstants.EXCHANGE,
                    CouponIssueQueueConstants.ROUTING,
                    message
            );
            log.info("优惠券发券消息已发布，templateId={}, userId={}", message.getTemplateId(), message.getUserId());
        } catch (Exception ex) {
            log.error("发布优惠券发券消息失败，templateId={}, userId={}", message.getTemplateId(), message.getUserId(), ex);
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "发送发券消息失败: " + ex.getMessage());
        }
    }
}
