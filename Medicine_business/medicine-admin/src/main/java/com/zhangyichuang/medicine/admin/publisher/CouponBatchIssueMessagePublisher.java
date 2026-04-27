package com.zhangyichuang.medicine.admin.publisher;

import com.zhangyichuang.medicine.common.core.enums.ResponseCode;
import com.zhangyichuang.medicine.common.core.exception.ServiceException;
import com.zhangyichuang.medicine.common.rabbitmq.constants.CouponBatchIssueQueueConstants;
import com.zhangyichuang.medicine.model.mq.CouponBatchIssueMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 优惠券批量发券消息发布器。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponBatchIssueMessagePublisher {

    /**
     * RabbitMQ 消息发送模板。
     */
    private final RabbitTemplate rabbitTemplate;

    /**
     * 发布批量发券消息。
     *
     * @param message 批量发券消息
     */
    public void publish(CouponBatchIssueMessage message) {
        if (message == null || message.getTemplateId() == null || !StringUtils.hasText(message.getIssueTargetType())) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "批量发券消息不能为空");
        }
        try {
            rabbitTemplate.convertAndSend(
                    CouponBatchIssueQueueConstants.EXCHANGE,
                    CouponBatchIssueQueueConstants.ROUTING,
                    message
            );
            log.info("优惠券批量发券消息已发布，templateId={}, issueTargetType={}",
                    message.getTemplateId(), message.getIssueTargetType());
        } catch (Exception ex) {
            log.error("发布优惠券批量发券消息失败，templateId={}, issueTargetType={}",
                    message.getTemplateId(), message.getIssueTargetType(), ex);
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "发送批量发券消息失败: " + ex.getMessage());
        }
    }
}
