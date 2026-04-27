package com.zhangyichuang.medicine.admin.listener;

import com.zhangyichuang.medicine.admin.service.KbDocumentChunkService;
import com.zhangyichuang.medicine.admin.service.KbDocumentService;
import com.zhangyichuang.medicine.common.rabbitmq.constants.KnowledgeBaseQueueConstants;
import com.zhangyichuang.medicine.model.mq.KnowledgeChunkAddResultMessage;
import com.zhangyichuang.medicine.model.mq.KnowledgeChunkRebuildResultMessage;
import com.zhangyichuang.medicine.model.mq.KnowledgeImportResultMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 知识库 MQ 结果消息统一监听器。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeMqResultListener {

    private final KbDocumentService kbDocumentService;
    private final KbDocumentChunkService kbDocumentChunkService;

    /**
     * 消费 AI 回传的知识库导入结果消息，并委托服务层处理判旧与状态回写。
     *
     * @param message 导入结果消息
     */
    @RabbitListener(queues = KnowledgeBaseQueueConstants.RESULT_QUEUE)
    public void handleImportResult(KnowledgeImportResultMessage message) {
        if (shouldSkipNullMessage(message, "跳过知识库导入结果消息: message is null")) {
            return;
        }
        kbDocumentService.handleImportResult(message);
    }

    /**
     * 消费知识库切片同步消息，并委托服务层处理。
     * <p>
     * 这条链路不是单条切片回调，而是文档导入完成后主动到 AI 侧拉取分页切片；
     * 拉取成功后，本地新写入的切片会直接标记为 COMPLETED。
     * </p>
     *
     * @param message 切片同步消息
     */
    @RabbitListener(queues = KnowledgeBaseQueueConstants.CHUNK_UPDATE_QUEUE, concurrency = "1")
    public void handleChunkUpdate(KnowledgeImportResultMessage message) {
        if (shouldSkipNullMessage(message, "跳过知识库切片同步消息: message is null")) {
            return;
        }
        kbDocumentService.handleChunkUpdateResult(message);
    }

    /**
     * 消费 AI 回传的切片新增结果消息，并委托服务层处理。
     *
     * @param message 切片新增结果消息
     */
    @RabbitListener(queues = KnowledgeBaseQueueConstants.CHUNK_ADD_RESULT_QUEUE, concurrency = "1")
    public void handleChunkAddResult(KnowledgeChunkAddResultMessage message) {
        if (shouldSkipNullMessage(message, "跳过切片新增结果消息: message is null")) {
            return;
        }
        kbDocumentChunkService.handleChunkAddResult(message);
    }

    /**
     * 消费 AI 回传的单切片重建结果消息，并委托服务层处理判旧与阶段回写。
     *
     * @param message 切片重建结果消息
     */
    @RabbitListener(queues = KnowledgeBaseQueueConstants.CHUNK_REBUILD_RESULT_QUEUE, concurrency = "1")
    public void handleChunkRebuildResult(KnowledgeChunkRebuildResultMessage message) {
        if (shouldSkipNullMessage(message, "跳过切片重建结果消息: message is null")) {
            return;
        }
        kbDocumentChunkService.handleChunkRebuildResult(message);
    }

    private boolean shouldSkipNullMessage(Object message, String logMessage) {
        if (message != null) {
            return false;
        }
        log.warn(logMessage);
        return true;
    }
}
