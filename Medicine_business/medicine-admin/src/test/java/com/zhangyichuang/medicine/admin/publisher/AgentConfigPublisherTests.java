package com.zhangyichuang.medicine.admin.publisher;

import com.zhangyichuang.medicine.common.core.exception.ServiceException;
import com.zhangyichuang.medicine.common.rabbitmq.constants.AgentConfigQueueConstants;
import com.zhangyichuang.medicine.model.mq.AgentConfigRefreshMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AgentConfigPublisherTests {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private AgentConfigPublisher agentConfigPublisher;

    @Test
    void publishRefresh_ShouldSendMessageToRefreshExchange() {
        AgentConfigRefreshMessage message = AgentConfigRefreshMessage.builder()
                .message_type("agent_config_refresh")
                .redis_key("agent:config:all")
                .build();

        agentConfigPublisher.publishRefresh(message);

        verify(rabbitTemplate).convertAndSend(
                AgentConfigQueueConstants.EXCHANGE,
                AgentConfigQueueConstants.ROUTING,
                message
        );
    }

    @Test
    void publishRefresh_ShouldThrowServiceException_WhenRabbitTemplateFails() {
        AgentConfigRefreshMessage message = AgentConfigRefreshMessage.builder()
                .message_type("agent_config_refresh")
                .redis_key("agent:config:all")
                .build();
        doThrow(new RuntimeException("mq down"))
                .when(rabbitTemplate)
                .convertAndSend(
                        AgentConfigQueueConstants.EXCHANGE,
                        AgentConfigQueueConstants.ROUTING,
                        message
                );

        ServiceException exception = assertThrows(ServiceException.class,
                () -> agentConfigPublisher.publishRefresh(message));

        assertEquals("发送Agent配置刷新消息失败: mq down", exception.getMessage());
    }
}
