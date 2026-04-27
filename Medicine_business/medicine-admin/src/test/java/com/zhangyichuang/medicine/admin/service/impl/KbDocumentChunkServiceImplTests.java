package com.zhangyichuang.medicine.admin.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.admin.integration.MedicineAgentClient;
import com.zhangyichuang.medicine.admin.mapper.KbDocumentChunkMapper;
import com.zhangyichuang.medicine.admin.mapper.KbDocumentMapper;
import com.zhangyichuang.medicine.admin.model.request.DocumentChunkAddRequest;
import com.zhangyichuang.medicine.admin.model.request.DocumentChunkListRequest;
import com.zhangyichuang.medicine.admin.model.request.DocumentChunkUpdateContentRequest;
import com.zhangyichuang.medicine.admin.model.request.DocumentChunkUpdateStatusRequest;
import com.zhangyichuang.medicine.admin.publisher.KnowledgePublisher;
import com.zhangyichuang.medicine.admin.service.KbBaseService;
import com.zhangyichuang.medicine.admin.service.KbDocumentChunkHistoryService;
import com.zhangyichuang.medicine.common.core.exception.ServiceException;
import com.zhangyichuang.medicine.common.redis.core.RedisCache;
import com.zhangyichuang.medicine.model.entity.KbBase;
import com.zhangyichuang.medicine.model.entity.KbDocument;
import com.zhangyichuang.medicine.model.entity.KbDocumentChunk;
import com.zhangyichuang.medicine.model.entity.KbDocumentChunkHistory;
import com.zhangyichuang.medicine.model.enums.KbDocumentChunkStageEnum;
import com.zhangyichuang.medicine.model.enums.KbDocumentStageEnum;
import com.zhangyichuang.medicine.model.enums.KnowledgeChunkTaskStageEnum;
import com.zhangyichuang.medicine.model.mq.KnowledgeChunkAddCommandMessage;
import com.zhangyichuang.medicine.model.mq.KnowledgeChunkAddResultMessage;
import com.zhangyichuang.medicine.model.mq.KnowledgeChunkRebuildCommandMessage;
import com.zhangyichuang.medicine.model.mq.KnowledgeChunkRebuildResultMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KbDocumentChunkServiceImplTests {

    @Mock
    private KbDocumentChunkMapper kbDocumentChunkMapper;

    @Mock
    private KbDocumentMapper kbDocumentMapper;

    @Mock
    private KbBaseService kbBaseService;

    @Mock
    private KbDocumentChunkHistoryService kbDocumentChunkHistoryService;

    @Mock
    private MedicineAgentClient medicineAgentClient;

    @Mock
    private KnowledgePublisher knowledgePublisher;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private PlatformTransactionManager transactionManager;

    private KbDocumentChunkServiceImpl kbDocumentChunkService;

    @BeforeEach
    void setUp() {
        RedisCache redisCache = new RedisCache(redisTemplate);
        kbDocumentChunkService = new KbDocumentChunkServiceImpl(
                kbDocumentMapper,
                kbBaseService,
                kbDocumentChunkHistoryService,
                medicineAgentClient,
                redisCache,
                knowledgePublisher,
                transactionManager
        );
        ReflectionTestUtils.setField(kbDocumentChunkService, "baseMapper", kbDocumentChunkMapper);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
    }

    @Test
    void listDocumentChunk_ShouldQueryByDocumentId() {
        DocumentChunkListRequest request = new DocumentChunkListRequest();
        request.setPageNum(1);
        request.setPageSize(10);

        KbDocument document = new KbDocument();
        document.setId(1001L);
        when(kbDocumentMapper.selectById(1001L)).thenReturn(document);

        Page<KbDocumentChunk> page = new Page<>(1, 10, 1);
        when(kbDocumentChunkMapper.listDocumentChunk(any(Page.class), eq(1001L), eq(request))).thenReturn(page);

        Page<KbDocumentChunk> result = kbDocumentChunkService.listDocumentChunk(1001L, request);

        assertSame(page, result);
        verify(kbDocumentMapper).selectById(1001L);
        verify(kbDocumentChunkMapper).listDocumentChunk(any(Page.class), eq(1001L), eq(request));
    }

    @Test
    void getDocumentChunkById_WhenMissing_ShouldThrow() {
        when(kbDocumentChunkMapper.selectById(2001L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> kbDocumentChunkService.getDocumentChunkById(2001L));

        assertEquals("文档切片不存在", ex.getMessage());
    }

    @Test
    void addDocumentChunk_ShouldInsertPlaceholderAndPublishCommand() {
        DocumentChunkAddRequest request = new DocumentChunkAddRequest();
        request.setDocumentId(1001L);
        request.setContent("  manual content  ");

        when(kbDocumentMapper.selectById(1001L)).thenReturn(newDocument());
        when(kbBaseService.getKnowledgeBaseById(1L)).thenReturn(newKbBase());
        when(kbDocumentChunkMapper.selectMaxChunkIndex(1001L)).thenReturn(9);
        when(kbDocumentChunkMapper.insert(any(KbDocumentChunk.class))).thenAnswer(invocation -> {
            KbDocumentChunk chunk = invocation.getArgument(0);
            chunk.setId(3001L);
            return 1;
        });

        boolean result = kbDocumentChunkService.addDocumentChunk(request);

        assertTrue(result);
        ArgumentCaptor<KbDocumentChunk> chunkCaptor = ArgumentCaptor.forClass(KbDocumentChunk.class);
        verify(kbDocumentChunkMapper).insert(chunkCaptor.capture());
        KbDocumentChunk chunk = chunkCaptor.getValue();
        assertEquals(1001L, chunk.getDocumentId());
        assertEquals(1L, chunk.getKnowledgeBaseId());
        assertEquals("manual content", chunk.getContent());
        assertEquals(14, chunk.getCharCount());
        assertEquals(10, chunk.getChunkIndex());
        assertEquals(KbDocumentChunkStageEnum.PENDING.getCode(), chunk.getStage());
        assertEquals(0, chunk.getStatus());
        assertNull(chunk.getVectorId());

        ArgumentCaptor<KnowledgeChunkAddCommandMessage> messageCaptor =
                ArgumentCaptor.forClass(KnowledgeChunkAddCommandMessage.class);
        verify(knowledgePublisher).publishChunkAddCommand(messageCaptor.capture());
        KnowledgeChunkAddCommandMessage message = messageCaptor.getValue();
        assertEquals("knowledge_chunk_add_command", message.getMessage_type());
        assertEquals(3001L, message.getChunk_id());
        assertEquals("drug_faq", message.getKnowledge_name());
        assertEquals(1001L, message.getDocument_id());
        assertEquals("manual content", message.getContent());
        assertEquals("text-embedding-v4", message.getEmbedding_model());
        assertNotNull(message.getTask_uuid());
        assertNotNull(message.getCreated_at());
    }

    @Test
    void addDocumentChunk_WhenDocumentNotCompleted_ShouldThrow() {
        DocumentChunkAddRequest request = new DocumentChunkAddRequest();
        request.setDocumentId(1001L);
        request.setContent("manual content");

        KbDocument document = newDocument();
        document.setStage(KbDocumentStageEnum.STARTED.getCode());
        when(kbDocumentMapper.selectById(1001L)).thenReturn(document);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> kbDocumentChunkService.addDocumentChunk(request));

        assertEquals("仅支持在已完成的文档下新增切片", ex.getMessage());
        verify(kbDocumentChunkMapper, never()).insert(any(KbDocumentChunk.class));
        verify(knowledgePublisher, never()).publishChunkAddCommand(any(KnowledgeChunkAddCommandMessage.class));
    }

    @Test
    void addDocumentChunk_WhenPublishFails_ShouldMarkFailed() {
        DocumentChunkAddRequest request = new DocumentChunkAddRequest();
        request.setDocumentId(1001L);
        request.setContent("manual content");

        when(kbDocumentMapper.selectById(1001L)).thenReturn(newDocument());
        when(kbBaseService.getKnowledgeBaseById(1L)).thenReturn(newKbBase());
        when(kbDocumentChunkMapper.selectMaxChunkIndex(1001L)).thenReturn(9);
        when(kbDocumentChunkMapper.insert(any(KbDocumentChunk.class))).thenAnswer(invocation -> {
            KbDocumentChunk chunk = invocation.getArgument(0);
            chunk.setId(3001L);
            return 1;
        });
        when(kbDocumentChunkMapper.updateById(any(KbDocumentChunk.class))).thenReturn(1);
        doThrow(new ServiceException("mq error"))
                .when(knowledgePublisher)
                .publishChunkAddCommand(any(KnowledgeChunkAddCommandMessage.class));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> kbDocumentChunkService.addDocumentChunk(request));

        assertEquals("内容已保存，但切片新增未成功提交: mq error", ex.getMessage());
        ArgumentCaptor<KbDocumentChunk> captor = ArgumentCaptor.forClass(KbDocumentChunk.class);
        verify(kbDocumentChunkMapper).updateById(captor.capture());
        assertEquals(3001L, captor.getValue().getId());
        assertEquals(KbDocumentChunkStageEnum.FAILED.getCode(), captor.getValue().getStage());
    }

    @Test
    void updateDocumentChunkContent_ShouldPersistHistoryAndPublishCommand() {
        DocumentChunkUpdateContentRequest request = new DocumentChunkUpdateContentRequest();
        request.setId(2001L);
        request.setContent("  新的切片内容  ");

        when(kbDocumentChunkMapper.selectById(2001L)).thenReturn(newChunk("旧的切片内容"));
        when(kbDocumentMapper.selectById(1001L)).thenReturn(newDocument());
        when(kbBaseService.getKnowledgeBaseById(1L)).thenReturn(newKbBase());
        when(kbDocumentChunkMapper.updateById(any(KbDocumentChunk.class))).thenReturn(1);
        when(kbDocumentChunkHistoryService.save(any(KbDocumentChunkHistory.class))).thenReturn(true);
        when(valueOperations.increment("kb:chunk_edit:latest_version:900001")).thenReturn(3L);
        when(redisTemplate.expire("kb:chunk_edit:latest_version:900001", 7L, TimeUnit.DAYS)).thenReturn(true);

        boolean result = kbDocumentChunkService.updateDocumentChunkContent(request);

        assertTrue(result);
        ArgumentCaptor<KbDocumentChunk> chunkCaptor = ArgumentCaptor.forClass(KbDocumentChunk.class);
        verify(kbDocumentChunkMapper).updateById(chunkCaptor.capture());
        KbDocumentChunk updated = chunkCaptor.getValue();
        assertEquals(2001L, updated.getId());
        assertEquals("新的切片内容", updated.getContent());
        assertEquals(6, updated.getCharCount());
        assertEquals(KbDocumentChunkStageEnum.PENDING.getCode(), updated.getStage());

        ArgumentCaptor<KbDocumentChunkHistory> historyCaptor = ArgumentCaptor.forClass(KbDocumentChunkHistory.class);
        verify(kbDocumentChunkHistoryService).save(historyCaptor.capture());
        KbDocumentChunkHistory history = historyCaptor.getValue();
        assertEquals(1001L, history.getDocumentId());
        assertEquals(2001L, history.getChunkId());
        assertEquals("drug_faq", history.getKnowledgeName());
        assertEquals(900001L, history.getVectorId());
        assertEquals("旧的切片内容", history.getOldContent());

        ArgumentCaptor<KnowledgeChunkRebuildCommandMessage> messageCaptor =
                ArgumentCaptor.forClass(KnowledgeChunkRebuildCommandMessage.class);
        verify(knowledgePublisher).publishChunkRebuildCommand(messageCaptor.capture());
        KnowledgeChunkRebuildCommandMessage message = messageCaptor.getValue();
        assertEquals("knowledge_chunk_rebuild_command", message.getMessage_type());
        assertEquals("drug_faq", message.getKnowledge_name());
        assertEquals(1001L, message.getDocument_id());
        assertEquals(900001L, message.getVector_id());
        assertEquals(3L, message.getVersion());
        assertEquals("新的切片内容", message.getContent());
        assertEquals("text-embedding-v4", message.getEmbedding_model());
    }

    @Test
    void updateDocumentChunkContent_WhenEditPending_ShouldThrow() {
        DocumentChunkUpdateContentRequest request = new DocumentChunkUpdateContentRequest();
        request.setId(2001L);
        request.setContent("新的切片内容");

        KbDocumentChunk chunk = newChunk("旧的切片内容");
        chunk.setStage(KbDocumentChunkStageEnum.PENDING.getCode());
        when(kbDocumentChunkMapper.selectById(2001L)).thenReturn(chunk);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> kbDocumentChunkService.updateDocumentChunkContent(request));

        assertEquals("当前切片已提交修改，必须等待完成后才能继续修改", ex.getMessage());
        verify(kbDocumentChunkMapper, never()).updateById(any(KbDocumentChunk.class));
        verify(kbDocumentChunkHistoryService, never()).save(any(KbDocumentChunkHistory.class));
        verify(knowledgePublisher, never()).publishChunkRebuildCommand(any(KnowledgeChunkRebuildCommandMessage.class));
        verify(valueOperations, never()).increment(any());
    }

    @Test
    void updateDocumentChunkContent_WhenContentUnchanged_ShouldSkipHistoryRedisAndMq() {
        DocumentChunkUpdateContentRequest request = new DocumentChunkUpdateContentRequest();
        request.setId(2001L);
        request.setContent("  相同内容 ");
        when(kbDocumentChunkMapper.selectById(2001L)).thenReturn(newChunk("相同内容"));

        boolean result = kbDocumentChunkService.updateDocumentChunkContent(request);

        assertTrue(result);
        verify(kbDocumentChunkMapper, never()).updateById(any(KbDocumentChunk.class));
        verify(kbDocumentChunkHistoryService, never()).save(any(KbDocumentChunkHistory.class));
        verify(knowledgePublisher, never()).publishChunkRebuildCommand(any(KnowledgeChunkRebuildCommandMessage.class));
        verify(valueOperations, never()).increment(any());
    }

    @Test
    void updateDocumentChunkContent_WhenVectorIdInvalid_ShouldThrow() {
        DocumentChunkUpdateContentRequest request = new DocumentChunkUpdateContentRequest();
        request.setId(2001L);
        request.setContent("新的内容");

        KbDocumentChunk chunk = newChunk("旧的切片内容");
        chunk.setVectorId("abc");
        when(kbDocumentChunkMapper.selectById(2001L)).thenReturn(chunk);
        when(kbDocumentMapper.selectById(1001L)).thenReturn(newDocument());
        when(kbBaseService.getKnowledgeBaseById(1L)).thenReturn(newKbBase());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> kbDocumentChunkService.updateDocumentChunkContent(request));

        assertEquals("向量ID无效", ex.getMessage());
        verify(kbDocumentChunkHistoryService, never()).save(any(KbDocumentChunkHistory.class));
        verify(knowledgePublisher, never()).publishChunkRebuildCommand(any(KnowledgeChunkRebuildCommandMessage.class));
    }

    @Test
    void updateDocumentChunkContent_WhenPublishFails_ShouldKeepContentAndMarkFailed() {
        DocumentChunkUpdateContentRequest request = new DocumentChunkUpdateContentRequest();
        request.setId(2001L);
        request.setContent("新的切片内容");

        when(kbDocumentChunkMapper.selectById(2001L)).thenReturn(newChunk("旧的切片内容"));
        when(kbDocumentMapper.selectById(1001L)).thenReturn(newDocument());
        when(kbBaseService.getKnowledgeBaseById(1L)).thenReturn(newKbBase());
        when(kbDocumentChunkMapper.updateById(any(KbDocumentChunk.class))).thenReturn(1);
        when(kbDocumentChunkHistoryService.save(any(KbDocumentChunkHistory.class))).thenReturn(true);
        when(valueOperations.increment("kb:chunk_edit:latest_version:900001")).thenReturn(4L);
        when(redisTemplate.expire("kb:chunk_edit:latest_version:900001", 7L, TimeUnit.DAYS)).thenReturn(true);
        doThrow(new ServiceException("mq error"))
                .when(knowledgePublisher)
                .publishChunkRebuildCommand(any(KnowledgeChunkRebuildCommandMessage.class));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> kbDocumentChunkService.updateDocumentChunkContent(request));

        assertEquals("内容已保存，但向量重建未成功提交: mq error", ex.getMessage());
        ArgumentCaptor<KbDocumentChunk> chunkCaptor = ArgumentCaptor.forClass(KbDocumentChunk.class);
        verify(kbDocumentChunkMapper, org.mockito.Mockito.times(2)).updateById(chunkCaptor.capture());
        assertEquals(KbDocumentChunkStageEnum.PENDING.getCode(), chunkCaptor.getAllValues().get(0).getStage());
        assertEquals(KbDocumentChunkStageEnum.FAILED.getCode(), chunkCaptor.getAllValues().get(1).getStage());
    }

    @Test
    void updateDocumentChunkStatus_ShouldCallAgentFirstAndThenPersistStatus() {
        DocumentChunkUpdateStatusRequest request = new DocumentChunkUpdateStatusRequest();
        request.setId(2001L);
        request.setStatus(1);

        KbDocumentChunk chunk = newChunk("旧的切片内容");
        chunk.setStatus(0);
        when(kbDocumentChunkMapper.selectById(2001L)).thenReturn(chunk);
        when(kbDocumentChunkMapper.updateById(any(KbDocumentChunk.class))).thenReturn(1);

        boolean result = kbDocumentChunkService.updateDocumentChunkStatus(request);

        assertTrue(result);
        verify(medicineAgentClient).updateDocumentChunkStatus(900001L, 1);
        ArgumentCaptor<KbDocumentChunk> captor = ArgumentCaptor.forClass(KbDocumentChunk.class);
        verify(kbDocumentChunkMapper).updateById(captor.capture());
        assertEquals(2001L, captor.getValue().getId());
        assertEquals(1, captor.getValue().getStatus());
        assertNotNull(captor.getValue().getUpdatedAt());
    }

    @Test
    void updateDocumentChunkStatus_WhenStatusUnchanged_ShouldStillCallAgentAndPersistStatus() {
        DocumentChunkUpdateStatusRequest request = new DocumentChunkUpdateStatusRequest();
        request.setId(2001L);
        request.setStatus(0);

        KbDocumentChunk chunk = newChunk("旧的切片内容");
        chunk.setStatus(0);
        when(kbDocumentChunkMapper.selectById(2001L)).thenReturn(chunk);
        when(kbDocumentChunkMapper.updateById(any(KbDocumentChunk.class))).thenReturn(1);

        boolean result = kbDocumentChunkService.updateDocumentChunkStatus(request);

        assertTrue(result);
        verify(medicineAgentClient).updateDocumentChunkStatus(900001L, 0);
        verify(kbDocumentChunkMapper).updateById(any(KbDocumentChunk.class));
    }

    @Test
    void updateDocumentChunkStatus_WhenAgentFails_ShouldNotPersistLocalStatus() {
        DocumentChunkUpdateStatusRequest request = new DocumentChunkUpdateStatusRequest();
        request.setId(2001L);
        request.setStatus(1);

        KbDocumentChunk chunk = newChunk("旧的切片内容");
        chunk.setStatus(0);
        when(kbDocumentChunkMapper.selectById(2001L)).thenReturn(chunk);
        doThrow(new ServiceException("向量记录不存在"))
                .when(medicineAgentClient)
                .updateDocumentChunkStatus(900001L, 1);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> kbDocumentChunkService.updateDocumentChunkStatus(request));

        assertEquals("向量记录不存在", ex.getMessage());
        verify(medicineAgentClient).updateDocumentChunkStatus(900001L, 1);
        verify(kbDocumentChunkMapper, never()).updateById(any(KbDocumentChunk.class));
    }

    @Test
    void handleChunkRebuildResult_WhenStarted_ShouldUpdateStage() {
        KnowledgeChunkRebuildResultMessage message = KnowledgeChunkRebuildResultMessage.builder()
                .task_uuid("task-1")
                .document_id(1001L)
                .vector_id(900001L)
                .version(3L)
                .stage(KnowledgeChunkTaskStageEnum.STARTED.getCode())
                .build();
        when(valueOperations.get("kb:chunk_edit:latest_version:900001")).thenReturn(3L);
        when(kbDocumentChunkMapper.selectOne(any())).thenReturn(newChunk("旧的切片内容"));
        when(kbDocumentChunkMapper.updateById(any(KbDocumentChunk.class))).thenReturn(1);

        kbDocumentChunkService.handleChunkRebuildResult(message);

        ArgumentCaptor<KbDocumentChunk> captor = ArgumentCaptor.forClass(KbDocumentChunk.class);
        verify(kbDocumentChunkMapper).updateById(captor.capture());
        assertEquals(KbDocumentChunkStageEnum.STARTED.getCode(), captor.getValue().getStage());
    }

    @Test
    void handleChunkRebuildResult_WhenCompleted_ShouldUpdateStage() {
        KnowledgeChunkRebuildResultMessage message = KnowledgeChunkRebuildResultMessage.builder()
                .task_uuid("task-2")
                .document_id(1001L)
                .vector_id(900001L)
                .version(3L)
                .stage(KnowledgeChunkTaskStageEnum.COMPLETED.getCode())
                .build();
        when(valueOperations.get("kb:chunk_edit:latest_version:900001")).thenReturn(3L);
        when(kbDocumentChunkMapper.selectOne(any())).thenReturn(newChunk("旧的切片内容"));
        when(kbDocumentChunkMapper.updateById(any(KbDocumentChunk.class))).thenReturn(1);

        kbDocumentChunkService.handleChunkRebuildResult(message);

        ArgumentCaptor<KbDocumentChunk> captor = ArgumentCaptor.forClass(KbDocumentChunk.class);
        verify(kbDocumentChunkMapper).updateById(captor.capture());
        assertEquals(KbDocumentChunkStageEnum.COMPLETED.getCode(), captor.getValue().getStage());
    }

    @Test
    void handleChunkRebuildResult_WhenFailed_ShouldUpdateStage() {
        KnowledgeChunkRebuildResultMessage message = KnowledgeChunkRebuildResultMessage.builder()
                .task_uuid("task-3")
                .document_id(1001L)
                .vector_id(900001L)
                .version(3L)
                .stage(KnowledgeChunkTaskStageEnum.FAILED.getCode())
                .message("已被更新版本替代")
                .build();
        when(valueOperations.get("kb:chunk_edit:latest_version:900001")).thenReturn(3L);
        when(kbDocumentChunkMapper.selectOne(any())).thenReturn(newChunk("旧的切片内容"));
        when(kbDocumentChunkMapper.updateById(any(KbDocumentChunk.class))).thenReturn(1);

        kbDocumentChunkService.handleChunkRebuildResult(message);

        ArgumentCaptor<KbDocumentChunk> captor = ArgumentCaptor.forClass(KbDocumentChunk.class);
        verify(kbDocumentChunkMapper).updateById(captor.capture());
        assertEquals(KbDocumentChunkStageEnum.FAILED.getCode(), captor.getValue().getStage());
    }

    @Test
    void handleChunkRebuildResult_WhenStaleVersion_ShouldDrop() {
        KnowledgeChunkRebuildResultMessage message = KnowledgeChunkRebuildResultMessage.builder()
                .task_uuid("task-4")
                .document_id(1001L)
                .vector_id(900001L)
                .version(2L)
                .stage(KnowledgeChunkTaskStageEnum.COMPLETED.getCode())
                .build();
        when(valueOperations.get("kb:chunk_edit:latest_version:900001")).thenReturn(3L);

        kbDocumentChunkService.handleChunkRebuildResult(message);

        verify(kbDocumentChunkMapper, never()).selectOne(any());
        verify(kbDocumentChunkMapper, never()).updateById(any(KbDocumentChunk.class));
    }

    @Test
    void handleChunkAddResult_WhenStarted_ShouldUpdateStage() {
        KnowledgeChunkAddResultMessage message = KnowledgeChunkAddResultMessage.builder()
                .task_uuid("task-add-1")
                .chunk_id(3001L)
                .document_id(1001L)
                .stage(KnowledgeChunkTaskStageEnum.STARTED.getCode())
                .build();
        KbDocumentChunk chunk = newChunk("旧的切片内容");
        chunk.setId(3001L);
        chunk.setVectorId(null);
        chunk.setStage(KbDocumentChunkStageEnum.PENDING.getCode());
        when(kbDocumentChunkMapper.selectById(3001L)).thenReturn(chunk);
        when(kbDocumentChunkMapper.updateById(any(KbDocumentChunk.class))).thenReturn(1);

        kbDocumentChunkService.handleChunkAddResult(message);

        ArgumentCaptor<KbDocumentChunk> captor = ArgumentCaptor.forClass(KbDocumentChunk.class);
        verify(kbDocumentChunkMapper).updateById(captor.capture());
        assertEquals(KbDocumentChunkStageEnum.STARTED.getCode(), captor.getValue().getStage());
    }

    @Test
    void handleChunkAddResult_WhenCompleted_ShouldUpdateChunkIndexVectorIdAndStatus() {
        KnowledgeChunkAddResultMessage message = KnowledgeChunkAddResultMessage.builder()
                .task_uuid("task-add-2")
                .chunk_id(3001L)
                .document_id(1001L)
                .stage(KnowledgeChunkTaskStageEnum.COMPLETED.getCode())
                .vector_id(900010L)
                .chunk_index(11)
                .build();
        KbDocumentChunk chunk = newChunk("旧的切片内容");
        chunk.setId(3001L);
        chunk.setVectorId(null);
        chunk.setStage(KbDocumentChunkStageEnum.PENDING.getCode());
        when(kbDocumentChunkMapper.selectById(3001L)).thenReturn(chunk);
        when(kbDocumentChunkMapper.updateById(any(KbDocumentChunk.class))).thenReturn(1);

        kbDocumentChunkService.handleChunkAddResult(message);

        ArgumentCaptor<KbDocumentChunk> captor = ArgumentCaptor.forClass(KbDocumentChunk.class);
        verify(kbDocumentChunkMapper).updateById(captor.capture());
        KbDocumentChunk updated = captor.getValue();
        assertEquals(KbDocumentChunkStageEnum.COMPLETED.getCode(), updated.getStage());
        assertEquals("900010", updated.getVectorId());
        assertEquals(11, updated.getChunkIndex());
    }

    @Test
    void handleChunkAddResult_WhenFailed_ShouldUpdateStage() {
        KnowledgeChunkAddResultMessage message = KnowledgeChunkAddResultMessage.builder()
                .task_uuid("task-add-3")
                .chunk_id(3001L)
                .document_id(1001L)
                .stage(KnowledgeChunkTaskStageEnum.FAILED.getCode())
                .message("embedding failed")
                .build();
        KbDocumentChunk chunk = newChunk("旧的切片内容");
        chunk.setId(3001L);
        chunk.setVectorId(null);
        chunk.setStage(KbDocumentChunkStageEnum.STARTED.getCode());
        when(kbDocumentChunkMapper.selectById(3001L)).thenReturn(chunk);
        when(kbDocumentChunkMapper.updateById(any(KbDocumentChunk.class))).thenReturn(1);

        kbDocumentChunkService.handleChunkAddResult(message);

        ArgumentCaptor<KbDocumentChunk> captor = ArgumentCaptor.forClass(KbDocumentChunk.class);
        verify(kbDocumentChunkMapper).updateById(captor.capture());
        assertEquals(KbDocumentChunkStageEnum.FAILED.getCode(), captor.getValue().getStage());
    }

    @Test
    void handleChunkAddResult_WhenCompletedPayloadInvalid_ShouldMarkFailed() {
        KnowledgeChunkAddResultMessage message = KnowledgeChunkAddResultMessage.builder()
                .task_uuid("task-add-4")
                .chunk_id(3001L)
                .document_id(1001L)
                .stage(KnowledgeChunkTaskStageEnum.COMPLETED.getCode())
                .chunk_index(11)
                .build();
        KbDocumentChunk chunk = newChunk("旧的切片内容");
        chunk.setId(3001L);
        chunk.setVectorId(null);
        chunk.setStage(KbDocumentChunkStageEnum.PENDING.getCode());
        when(kbDocumentChunkMapper.selectById(3001L)).thenReturn(chunk);
        when(kbDocumentChunkMapper.updateById(any(KbDocumentChunk.class))).thenReturn(1);

        kbDocumentChunkService.handleChunkAddResult(message);

        ArgumentCaptor<KbDocumentChunk> captor = ArgumentCaptor.forClass(KbDocumentChunk.class);
        verify(kbDocumentChunkMapper).updateById(captor.capture());
        assertEquals(KbDocumentChunkStageEnum.FAILED.getCode(), captor.getValue().getStage());
        assertNull(captor.getValue().getVectorId());
    }

    @Test
    void handleChunkAddResult_WhenDocumentMismatch_ShouldIgnore() {
        KnowledgeChunkAddResultMessage message = KnowledgeChunkAddResultMessage.builder()
                .task_uuid("task-add-5")
                .chunk_id(3001L)
                .document_id(1002L)
                .stage(KnowledgeChunkTaskStageEnum.STARTED.getCode())
                .build();
        KbDocumentChunk chunk = newChunk("旧的切片内容");
        chunk.setId(3001L);
        when(kbDocumentChunkMapper.selectById(3001L)).thenReturn(chunk);

        kbDocumentChunkService.handleChunkAddResult(message);

        verify(kbDocumentChunkMapper, never()).updateById(any(KbDocumentChunk.class));
    }

    @Test
    void handleChunkAddResult_WhenStageUnsupported_ShouldIgnore() {
        KnowledgeChunkAddResultMessage message = KnowledgeChunkAddResultMessage.builder()
                .task_uuid("task-add-6")
                .chunk_id(3001L)
                .document_id(1001L)
                .stage("PROCESSING")
                .build();

        kbDocumentChunkService.handleChunkAddResult(message);

        verify(kbDocumentChunkMapper, never()).selectById(any());
        verify(kbDocumentChunkMapper, never()).updateById(any(KbDocumentChunk.class));
    }

    private KbDocumentChunk newChunk(String content) {
        KbDocumentChunk chunk = new KbDocumentChunk();
        chunk.setId(2001L);
        chunk.setDocumentId(1001L);
        chunk.setVectorId("900001");
        chunk.setContent(content);
        return chunk;
    }

    private KbDocument newDocument() {
        KbDocument document = new KbDocument();
        document.setId(1001L);
        document.setKnowledgeBaseId(1L);
        document.setStage(KbDocumentStageEnum.COMPLETED.getCode());
        return document;
    }

    private KbBase newKbBase() {
        KbBase kbBase = new KbBase();
        kbBase.setId(1L);
        kbBase.setKnowledgeName("drug_faq");
        kbBase.setEmbeddingModel("text-embedding-v4");
        return kbBase;
    }
}
