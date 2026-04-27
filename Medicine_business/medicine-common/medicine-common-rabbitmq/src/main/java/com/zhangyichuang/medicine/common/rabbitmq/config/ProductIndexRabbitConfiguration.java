package com.zhangyichuang.medicine.common.rabbitmq.config;

import com.zhangyichuang.medicine.common.rabbitmq.constants.ProductIndexQueueConstants;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 商品索引事件的基础交换机/队列配置。
 */
@Configuration
@EnableRabbit
public class ProductIndexRabbitConfiguration {

    @Bean
    public MessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public DirectExchange productIndexExchange() {
        return ExchangeBuilder
                .directExchange(ProductIndexQueueConstants.EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public Queue productIndexQueue() {
        return QueueBuilder.durable(ProductIndexQueueConstants.QUEUE).build();
    }

    @Bean
    public Binding productIndexUpsertBinding(Queue productIndexQueue, DirectExchange productIndexExchange) {
        return BindingBuilder.bind(productIndexQueue)
                .to(productIndexExchange)
                .with(ProductIndexQueueConstants.ROUTING_UPSERT);
    }

    @Bean
    public Binding productIndexDeleteBinding(Queue productIndexQueue, DirectExchange productIndexExchange) {
        return BindingBuilder.bind(productIndexQueue)
                .to(productIndexExchange)
                .with(ProductIndexQueueConstants.ROUTING_DELETE);
    }
}
