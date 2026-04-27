package com.zhangyichuang.medicine.admin.listener;

import com.zhangyichuang.medicine.admin.service.KbDocumentChunkService;
import com.zhangyichuang.medicine.admin.service.KbDocumentService;
import com.zhangyichuang.medicine.model.enums.KbDocumentStageEnum;
import com.zhangyichuang.medicine.model.enums.KnowledgeChunkTaskStageEnum;
import com.zhangyichuang.medicine.model.mq.KnowledgeChunkAddResultMessage;
import com.zhangyichuang.medicine.model.mq.KnowledgeChunkRebuildResultMessage;
import com.zhangyichuang.medicine.model.mq.KnowledgeImportResultMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class KnowledgeMqResultListenerTests {

    @Mock
    private KbDocumentService kbDocumentService;

    @Mock
    private KbDocumentChunkService kbDocumentChunkService;

    @InjectMocks
    private KnowledgeMqResultListener listener;

    @Test
    void handleImportResult_WhenMessageNull_ShouldSkip() {
        listener.handleImportResult(null);
        verify(kbDocumentService, never()).handleImportResult(null);
    }

    @Test
    void handleImportResult_ShouldDelegateToService() {
        KnowledgeImportResultMessage message = KnowledgeImportResultMessage.builder()
                .task_uuid("task-1")
                .biz_key("drug_faq:1001")
                .version(1L)
                .stage(KbDocumentStageEnum.STARTED.getCode())
                .build();

        listener.handleImportResult(message);

        verify(kbDocumentService).handleImportResult(message);
    }

    @Test
    void handleChunkUpdate_WhenMessageNull_ShouldSkip() {
        listener.handleChunkUpdate(null);
        verify(kbDocumentService, never()).handleChunkUpdateResult(null);
    }

    @Test
    void handleChunkUpdate_ShouldDelegateToService() {
        KnowledgeImportResultMessage message = KnowledgeImportResultMessage.builder()
                .task_uuid("task-2")
                .biz_key("drug_faq:1001")
                .version(1L)
                .stage(KbDocumentStageEnum.COMPLETED.getCode())
                .build();

        listener.handleChunkUpdate(message);

        verify(kbDocumentService).handleChunkUpdateResult(message);
    }

    @Test
    void handleChunkAddResult_WhenMessageNull_ShouldSkip() {
        listener.handleChunkAddResult(null);
        verify(kbDocumentChunkService, never()).handleChunkAddResult(null);
    }

    @Test
    void handleChunkAddResult_ShouldDelegateToService() {
        KnowledgeChunkAddResultMessage message = KnowledgeChunkAddResultMessage.builder()
                .task_uuid("task-3")
                .chunk_id(3001L)
                .document_id(1001L)
                .stage(KnowledgeChunkTaskStageEnum.COMPLETED.getCode())
                .vector_id(900001L)
                .chunk_index(9)
                .build();

        listener.handleChunkAddResult(message);

        verify(kbDocumentChunkService).handleChunkAddResult(message);
    }

    @Test
    void handleChunkRebuildResult_WhenMessageNull_ShouldSkip() {
        listener.handleChunkRebuildResult(null);
        verify(kbDocumentChunkService, never()).handleChunkRebuildResult(null);
    }

    @Test
    void handleChunkRebuildResult_ShouldDelegateToService() {
        KnowledgeChunkRebuildResultMessage message = KnowledgeChunkRebuildResultMessage.builder()
                .task_uuid("task-4")
                .document_id(1001L)
                .vector_id(900001L)
                .version(3L)
                .stage(KnowledgeChunkTaskStageEnum.COMPLETED.getCode())
                .build();

        listener.handleChunkRebuildResult(message);

        verify(kbDocumentChunkService).handleChunkRebuildResult(message);
    }
}
