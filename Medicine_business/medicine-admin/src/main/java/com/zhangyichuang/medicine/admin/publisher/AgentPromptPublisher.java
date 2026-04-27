package com.zhangyichuang.medicine.admin.publisher;

import com.zhangyichuang.medicine.common.core.enums.ResponseCode;
import com.zhangyichuang.medicine.common.core.exception.ServiceException;
import com.zhangyichuang.medicine.common.rabbitmq.constants.AgentPromptQueueConstants;
import com.zhangyichuang.medicine.model.mq.AgentPromptRefreshMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Agent 提示词刷新 MQ 生产者。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentPromptPublisher {

    /**
     * RabbitMQ 消息模板。
     */
    private final RabbitTemplate rabbitTemplate;

    /**
     * 发布 Agent 提示词刷新消息。
     *
     * @param message 刷新消息
     */
    public void publishRefresh(AgentPromptRefreshMessage message) {
        if (message == null) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "Agent提示词刷新消息不能为空");
        }
        try {
            rabbitTemplate.convertAndSend(
                    AgentPromptQueueConstants.EXCHANGE,
                    AgentPromptQueueConstants.ROUTING,
                    message
            );
        } catch (Exception ex) {
            log.error("发布Agent提示词刷新消息失败, prompt_key={}", message.getPrompt_key(), ex);
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "发送Agent提示词刷新消息失败: " + ex.getMessage());
        }
    }
}
