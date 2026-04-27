package com.zhangyichuang.medicine.common.rabbitmq.config;

import com.zhangyichuang.medicine.common.rabbitmq.constants.AgentConfigQueueConstants;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentConfigRabbitConfigurationTests {

    private final AgentConfigRabbitConfiguration configuration = new AgentConfigRabbitConfiguration();

    @Test
    void agentConfigRefreshQueue_ShouldUseFiveMinuteTtl() {
        Queue queue = configuration.agentConfigRefreshQueue();

        assertEquals(AgentConfigQueueConstants.QUEUE, queue.getName());
        assertTrue(queue.isDurable());
        assertEquals(AgentConfigQueueConstants.MESSAGE_TTL_MILLIS, queue.getArguments().get("x-message-ttl"));
    }

    @Test
    void agentConfigRefreshBinding_ShouldUseExpectedRoutingKey() {
        Queue queue = configuration.agentConfigRefreshQueue();
        DirectExchange exchange = configuration.agentConfigRefreshExchange();

        Binding binding = configuration.agentConfigRefreshBinding(queue, exchange);

        assertEquals(AgentConfigQueueConstants.QUEUE, binding.getDestination());
        assertEquals(AgentConfigQueueConstants.EXCHANGE, binding.getExchange());
        assertEquals(AgentConfigQueueConstants.ROUTING, binding.getRoutingKey());
    }
}
