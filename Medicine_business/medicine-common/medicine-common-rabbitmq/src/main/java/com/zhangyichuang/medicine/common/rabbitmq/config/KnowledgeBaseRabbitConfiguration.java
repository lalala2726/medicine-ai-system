package com.zhangyichuang.medicine.common.rabbitmq.config;

import com.zhangyichuang.medicine.common.rabbitmq.constants.KnowledgeBaseQueueConstants;
import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 知识库导入的基础交换机/队列配置。
 */
@Configuration
public class KnowledgeBaseRabbitConfiguration {

    @Bean
    public DirectExchange knowledgeBaseImportExchange() {
        return ExchangeBuilder
                .directExchange(KnowledgeBaseQueueConstants.EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public Queue knowledgeBaseImportDocumentQueue() {
        return QueueBuilder.durable(KnowledgeBaseQueueConstants.COMMAND_QUEUE).build();
    }

    @Bean
    public Queue knowledgeBaseImportResultQueue() {
        return QueueBuilder.durable(KnowledgeBaseQueueConstants.RESULT_QUEUE).build();
    }

    @Bean
    public Binding knowledgeBaseImportDocumentBinding(Queue knowledgeBaseImportDocumentQueue,
                                                      @Qualifier("knowledgeBaseImportExchange")
                                                      DirectExchange knowledgeBaseImportExchange) {
        return BindingBuilder.bind(knowledgeBaseImportDocumentQueue)
                .to(knowledgeBaseImportExchange)
                .with(KnowledgeBaseQueueConstants.ROUTING_COMMAND);
    }

    @Bean
    public Binding knowledgeBaseImportResultBinding(Queue knowledgeBaseImportResultQueue,
                                                    @Qualifier("knowledgeBaseImportExchange")
                                                    DirectExchange knowledgeBaseImportExchange) {
        return BindingBuilder.bind(knowledgeBaseImportResultQueue)
                .to(knowledgeBaseImportExchange)
                .with(KnowledgeBaseQueueConstants.ROUTING_RESULT);
    }

    @Bean
    public Queue knowledgeBaseVectorDeleteQueue() {
        return QueueBuilder.durable(KnowledgeBaseQueueConstants.VECTOR_DELETE_QUEUE).build();
    }

    @Bean
    public Binding knowledgeBaseVectorDeleteBinding(Queue knowledgeBaseVectorDeleteQueue,
                                                    @Qualifier("knowledgeBaseImportExchange")
                                                    DirectExchange knowledgeBaseImportExchange) {
        return BindingBuilder.bind(knowledgeBaseVectorDeleteQueue)
                .to(knowledgeBaseImportExchange)
                .with(KnowledgeBaseQueueConstants.ROUTING_VECTOR_DELETE);
    }

    @Bean
    public Queue knowledgeBaseDeleteQueue() {
        return QueueBuilder.durable(KnowledgeBaseQueueConstants.KB_DELETE_QUEUE).build();
    }

    @Bean
    public Binding knowledgeBaseDeleteBinding(Queue knowledgeBaseDeleteQueue,
                                              @Qualifier("knowledgeBaseImportExchange")
                                              DirectExchange knowledgeBaseImportExchange) {
        return BindingBuilder.bind(knowledgeBaseDeleteQueue)
                .to(knowledgeBaseImportExchange)
                .with(KnowledgeBaseQueueConstants.ROUTING_KB_DELETE);
    }

    @Bean
    public Queue knowledgeBaseChunkUpdateQueue() {
        return QueueBuilder.durable(KnowledgeBaseQueueConstants.CHUNK_UPDATE_QUEUE).build();
    }

    @Bean
    public Binding knowledgeBaseChunkUpdateBinding(Queue knowledgeBaseChunkUpdateQueue,
                                                   @Qualifier("knowledgeBaseImportExchange")
                                                   DirectExchange knowledgeBaseImportExchange) {
        return BindingBuilder.bind(knowledgeBaseChunkUpdateQueue)
                .to(knowledgeBaseImportExchange)
                .with(KnowledgeBaseQueueConstants.ROUTING_CHUNK_UPDATE);
    }

    @Bean
    public DirectExchange knowledgeChunkRebuildExchange() {
        return ExchangeBuilder
                .directExchange(KnowledgeBaseQueueConstants.CHUNK_REBUILD_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public Queue knowledgeChunkRebuildCommandQueue() {
        return QueueBuilder.durable(KnowledgeBaseQueueConstants.CHUNK_REBUILD_COMMAND_QUEUE).build();
    }

    @Bean
    public Queue knowledgeChunkRebuildResultQueue() {
        return QueueBuilder.durable(KnowledgeBaseQueueConstants.CHUNK_REBUILD_RESULT_QUEUE).build();
    }

    @Bean
    public Binding knowledgeChunkRebuildCommandBinding(Queue knowledgeChunkRebuildCommandQueue,
                                                       @Qualifier("knowledgeChunkRebuildExchange")
                                                       DirectExchange knowledgeChunkRebuildExchange) {
        return BindingBuilder.bind(knowledgeChunkRebuildCommandQueue)
                .to(knowledgeChunkRebuildExchange)
                .with(KnowledgeBaseQueueConstants.ROUTING_CHUNK_REBUILD_COMMAND);
    }

    @Bean
    public Binding knowledgeChunkRebuildResultBinding(Queue knowledgeChunkRebuildResultQueue,
                                                      @Qualifier("knowledgeChunkRebuildExchange")
                                                      DirectExchange knowledgeChunkRebuildExchange) {
        return BindingBuilder.bind(knowledgeChunkRebuildResultQueue)
                .to(knowledgeChunkRebuildExchange)
                .with(KnowledgeBaseQueueConstants.ROUTING_CHUNK_REBUILD_RESULT);
    }

    @Bean
    public DirectExchange knowledgeChunkAddExchange() {
        return ExchangeBuilder
                .directExchange(KnowledgeBaseQueueConstants.CHUNK_ADD_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public Queue knowledgeChunkAddCommandQueue() {
        return QueueBuilder.durable(KnowledgeBaseQueueConstants.CHUNK_ADD_COMMAND_QUEUE).build();
    }

    @Bean
    public Queue knowledgeChunkAddResultQueue() {
        return QueueBuilder.durable(KnowledgeBaseQueueConstants.CHUNK_ADD_RESULT_QUEUE).build();
    }

    @Bean
    public Binding knowledgeChunkAddCommandBinding(Queue knowledgeChunkAddCommandQueue,
                                                   @Qualifier("knowledgeChunkAddExchange")
                                                   DirectExchange knowledgeChunkAddExchange) {
        return BindingBuilder.bind(knowledgeChunkAddCommandQueue)
                .to(knowledgeChunkAddExchange)
                .with(KnowledgeBaseQueueConstants.ROUTING_CHUNK_ADD_COMMAND);
    }

    @Bean
    public Binding knowledgeChunkAddResultBinding(Queue knowledgeChunkAddResultQueue,
                                                  @Qualifier("knowledgeChunkAddExchange")
                                                  DirectExchange knowledgeChunkAddExchange) {
        return BindingBuilder.bind(knowledgeChunkAddResultQueue)
                .to(knowledgeChunkAddExchange)
                .with(KnowledgeBaseQueueConstants.ROUTING_CHUNK_ADD_RESULT);
    }
}
