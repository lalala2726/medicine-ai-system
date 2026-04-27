package com.zhangyichuang.medicine.client.publisher;

import com.zhangyichuang.medicine.common.rabbitmq.constants.LoginLogQueueConstants;
import com.zhangyichuang.medicine.model.mq.LoginLogMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 客户端登录日志 MQ 生产者。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginLogPublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发布登录日志消息。
     */
    public void publish(LoginLogMessage message) {
        if (message == null) {
            return;
        }
        try {
            rabbitTemplate.convertAndSend(
                    LoginLogQueueConstants.EXCHANGE,
                    LoginLogQueueConstants.ROUTING,
                    message
            );
        } catch (Exception ex) {
            log.warn("Failed to publish login log message", ex);
        }
    }
}
