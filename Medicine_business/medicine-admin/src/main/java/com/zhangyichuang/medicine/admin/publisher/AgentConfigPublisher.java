package com.zhangyichuang.medicine.admin.publisher;

import com.zhangyichuang.medicine.common.core.enums.ResponseCode;
import com.zhangyichuang.medicine.common.core.exception.ServiceException;
import com.zhangyichuang.medicine.common.rabbitmq.constants.AgentConfigQueueConstants;
import com.zhangyichuang.medicine.model.mq.AgentConfigRefreshMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Agent 配置刷新 MQ 生产者。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentConfigPublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发布 Agent 配置刷新消息。
     *
     * @param message 配置刷新消息
     */
    public void publishRefresh(AgentConfigRefreshMessage message) {
        if (message == null) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "Agent配置刷新消息不能为空");
        }
        try {
            rabbitTemplate.convertAndSend(
                    AgentConfigQueueConstants.EXCHANGE,
                    AgentConfigQueueConstants.ROUTING,
                    message
            );
        } catch (Exception ex) {
            log.error("发布Agent配置刷新消息失败, redis_key={}", message.getRedis_key(), ex);
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "发送Agent配置刷新消息失败: " + ex.getMessage());
        }
    }
}
