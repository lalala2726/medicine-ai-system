package com.zhangyichuang.medicine.admin.publisher;

import com.zhangyichuang.medicine.common.core.enums.ResponseCode;
import com.zhangyichuang.medicine.common.core.exception.ServiceException;
import com.zhangyichuang.medicine.common.rabbitmq.constants.AgentPromptSyncQueueConstants;
import com.zhangyichuang.medicine.model.mq.AgentPromptSyncMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Agent 提示词同步任务 MQ 生产者。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentPromptSyncPublisher {

    /**
     * RabbitMQ 消息模板。
     */
    private final RabbitTemplate rabbitTemplate;

    /**
     * 发布 Agent 提示词同步任务消息。
     *
     * @param message 同步任务消息
     * @return 无返回值
     */
    public void publishSync(AgentPromptSyncMessage message) {
        if (message == null) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "Agent提示词同步任务消息不能为空");
        }
        try {
            rabbitTemplate.convertAndSend(
                    AgentPromptSyncQueueConstants.EXCHANGE,
                    AgentPromptSyncQueueConstants.ROUTING,
                    message
            );
        } catch (Exception ex) {
            log.error("发布Agent提示词同步任务消息失败, prompt_key={}, sync_scope={}",
                    message.getPrompt_key(), message.getSync_scope(), ex);
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "发送Agent提示词同步任务消息失败: " + ex.getMessage());
        }
    }
}
