package com.zhangyichuang.medicine.admin.publisher;

import com.zhangyichuang.medicine.common.rabbitmq.constants.OperationLogQueueConstants;
import com.zhangyichuang.medicine.model.mq.OperationLogMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 操作日志 MQ 生产者。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OperationLogPublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发布操作日志消息。
     */
    public void publish(OperationLogMessage message) {
        if (message == null) {
            return;
        }
        try {
            rabbitTemplate.convertAndSend(
                    OperationLogQueueConstants.EXCHANGE,
                    OperationLogQueueConstants.ROUTING,
                    message
            );
        } catch (Exception ex) {
            log.warn("Failed to publish operation log message", ex);
        }
    }
}
