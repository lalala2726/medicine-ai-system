package com.zhangyichuang.medicine.admin.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
import com.zhangyichuang.medicine.admin.service.KbDocumentChunkService;
import com.zhangyichuang.medicine.common.core.enums.ResponseCode;
import com.zhangyichuang.medicine.common.core.exception.ServiceException;
import com.zhangyichuang.medicine.common.core.utils.Assert;
import com.zhangyichuang.medicine.common.redis.core.RedisCache;
import com.zhangyichuang.medicine.common.security.base.BaseService;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 知识库文档切片服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KbDocumentChunkServiceImpl extends ServiceImpl<KbDocumentChunkMapper, KbDocumentChunk>
        implements KbDocumentChunkService, BaseService {

    /**
     * 批量保存切片时的分批大小。
     */
    private static final int BATCH_SIZE = 500;

    /**
     * 切片启用状态。
     */
    private static final int CHUNK_STATUS_ENABLED = 0;

    /**
     * 切片禁用状态。
     */
    private static final int CHUNK_STATUS_DISABLED = 1;

    /**
     * 切片正在等待重建或处理中时的提示文案。
     */
    private static final String EDIT_IN_PROGRESS_MESSAGE = "当前切片已提交修改，必须等待完成后才能继续修改";

    /**
     * 单切片重建 command 消息类型。
     */
    private static final String CHUNK_REBUILD_COMMAND_MESSAGE_TYPE = "knowledge_chunk_rebuild_command";

    /**
     * 文档切片新增 command 消息类型。
     */
    private static final String CHUNK_ADD_COMMAND_MESSAGE_TYPE = "knowledge_chunk_add_command";

    /**
     * AI 返回“旧版本已被替代”时的关键字。
     */
    private static final String STALE_RESULT_MESSAGE = "已被更新版本替代";

    /**
     * 单切片编辑最新版本 Redis key 前缀。
     */
    private static final String CHUNK_EDIT_LATEST_VERSION_KEY_PREFIX = "kb:chunk_edit:latest_version:";

    /**
     * 单切片编辑最新版本 Redis key 保留天数。
     */
    private static final long CHUNK_EDIT_LATEST_VERSION_TTL_DAYS = 7L;

    private final KbDocumentMapper kbDocumentMapper;
    private final KbBaseService kbBaseService;
    /**
     * 已废弃的切片历史服务，后续会随 kb_document_chunk_history 表一起删除。
     */
    @Deprecated(since = "1.0-beta", forRemoval = true)
    private final KbDocumentChunkHistoryService kbDocumentChunkHistoryService;
    private final MedicineAgentClient medicineAgentClient;
    private final RedisCache redisCache;
    private final KnowledgePublisher knowledgePublisher;
    private final PlatformTransactionManager transactionManager;

    /**
     * 分页查询指定文档下的切片列表。
     *
     * @param request 查询参数
     * @return 切片分页结果
     */
    @Override
    public Page<KbDocumentChunk> listDocumentChunk(Long documentId, DocumentChunkListRequest request) {
        Assert.notNull(request, "查询参数不能为空");
        Assert.isPositive(documentId, "文档ID必须大于0");
        Assert.isTrue(kbDocumentMapper.selectById(documentId) != null, "文档不存在");
        return baseMapper.listDocumentChunk(request.toPage(), documentId, request);
    }

    /**
     * 根据切片ID查询切片详情。
     *
     * @param id 切片ID
     * @return 切片详情
     */
    @Override
    public KbDocumentChunk getDocumentChunkById(Long id) {
        Assert.isPositive(id, "切片ID必须大于0");
        KbDocumentChunk chunk = baseMapper.selectById(id);
        Assert.isTrue(chunk != null, "文档切片不存在");
        return chunk;
    }

    /**
     * 新增手工补充切片，并通过 MQ 触发 AI 侧异步向量化。
     *
     * @param request 新增请求
     * @return true 表示处理成功
     */
    @Override
    public boolean addDocumentChunk(DocumentChunkAddRequest request) {
        Assert.notNull(request, "切片新增请求不能为空");
        Assert.isPositive(request.getDocumentId(), "文档ID必须大于0");

        String content = request.getContent() == null ? null : request.getContent().trim();
        Assert.notEmpty(content, "切片内容不能为空");

        KbDocument document = getDocument(request.getDocumentId());
        Assert.isTrue(KbDocumentStageEnum.COMPLETED.matches(document.getStage()), "仅支持在已完成的文档下新增切片");
        Assert.notNull(document.getKnowledgeBaseId(), "文档知识库ID不能为空");

        KbBase kbBase = kbBaseService.getKnowledgeBaseById(document.getKnowledgeBaseId());
        Assert.notEmpty(kbBase.getKnowledgeName(), "知识库名称不能为空");
        Assert.notEmpty(kbBase.getEmbeddingModel(), "知识库向量模型未配置");

        KbDocumentChunk chunk = buildPendingChunk(document.getId(), document.getKnowledgeBaseId(), content);
        Assert.isTrue(baseMapper.insert(chunk) > 0 && chunk.getId() != null, "保存文档切片失败");

        String taskUuid = UUID.randomUUID().toString();
        try {
            knowledgePublisher.publishChunkAddCommand(buildChunkAddCommand(kbBase, chunk, taskUuid));
            return true;
        } catch (Exception ex) {
            markChunkStageFailed(chunk.getId(), "切片新增提交失败后");
            throw new ServiceException(ResponseCode.OPERATION_ERROR,
                    "内容已保存，但切片新增未成功提交: " + extractErrorMessage(ex));
        }
    }

    /**
     * 修改切片内容，并通过 Redis + MQ 触发 AI 侧单条向量重建。
     *
     * @param request 更新请求
     * @return true 表示处理成功
     */
    @Override
    public boolean updateDocumentChunkContent(DocumentChunkUpdateContentRequest request) {
        Assert.notNull(request, "切片内容更新请求不能为空");
        KbDocumentChunk chunk = getDocumentChunkById(request.getId());

        String content = request.getContent() == null ? null : request.getContent().trim();
        Assert.notEmpty(content, "切片内容不能为空");
        assertChunkEditable(chunk);
        if (Objects.equals(content, chunk.getContent())) {
            return true;
        }

        KbDocument document = getDocument(chunk.getDocumentId());
        KbBase kbBase = kbBaseService.getKnowledgeBaseById(document.getKnowledgeBaseId());
        Assert.notEmpty(kbBase.getKnowledgeName(), "知识库名称不能为空");
        Assert.notEmpty(kbBase.getEmbeddingModel(), "知识库向量模型未配置");

        long vectorId = parseStoredPositiveLong(chunk.getVectorId());
        String taskUuid = UUID.randomUUID().toString();
        persistChunkEdit(chunk, kbBase, content, vectorId, taskUuid);

        try {
            long version = nextChunkEditVersionAndSetLatest(vectorId);
            log.info("切片内容更新已落库，准备发布重建命令: chunk_id={}, document_id={}, vector_id={}, version={}, task_uuid={};",
                    chunk.getId(), chunk.getDocumentId(), vectorId, version, taskUuid);
            knowledgePublisher.publishChunkRebuildCommand(buildChunkRebuildCommand(
                    kbBase, chunk.getDocumentId(), vectorId, version, content, taskUuid));
            return true;
        } catch (Exception ex) {
            markChunkStageFailed(chunk.getId(), "切片重建提交失败后");
            throw new ServiceException(ResponseCode.OPERATION_ERROR,
                    "内容已保存，但向量重建未成功提交: " + extractErrorMessage(ex));
        }
    }

    /**
     * 修改切片状态，先调用 AI 端完成向量状态更新，再回写本地数据库。
     *
     * @param request 更新请求
     * @return true 表示处理成功
     */
    @Override
    public boolean updateDocumentChunkStatus(DocumentChunkUpdateStatusRequest request) {
        Assert.notNull(request, "切片状态更新请求不能为空");
        Assert.isPositive(request.getId(), "切片ID必须大于0");
        validateChunkStatus(request.getStatus());

        KbDocumentChunk chunk = getDocumentChunkById(request.getId());
        long vectorId = parseStoredPositiveLong(chunk.getVectorId());
        medicineAgentClient.updateDocumentChunkStatus(vectorId, request.getStatus());

        KbDocumentChunk updateEntity = KbDocumentChunk.builder()
                .id(chunk.getId())
                .status(request.getStatus())
                .updatedAt(new Date())
                .build();
        Assert.isTrue(baseMapper.updateById(updateEntity) > 0, "更新文档切片状态失败");
        return true;
    }

    /**
     * 删除单个切片。
     *
     * @param id 切片ID
     * @return 永远不会返回；当前固定抛出未开放异常
     */
    @Override
    public boolean deleteDocumentChunk(Long id) {
        Assert.isPositive(id, "切片ID必须大于0");
        throw new ServiceException(ResponseCode.OPERATION_ERROR, "切片删除暂未开放");
    }

    /**
     * 处理 AI 回传的单切片重建结果，只回写本地切片阶段。
     *
     * @param message AI 回传的结果消息
     */
    @Override
    public void handleChunkRebuildResult(KnowledgeChunkRebuildResultMessage message) {
        if (message == null) {
            return;
        }
        Long documentId = message.getDocument_id();
        Long vectorId = message.getVector_id();
        if (documentId == null || documentId <= 0 || vectorId == null || vectorId <= 0) {
            log.warn("忽略无效切片重建结果: task_uuid={}, document_id={}, vector_id={}",
                    message.getTask_uuid(), documentId, vectorId);
            return;
        }
        if (isStaleResultMessage(message)) {
            return;
        }

        KbDocumentChunkStageEnum incomingStage = resolveChunkStage(message.getStage());
        if (incomingStage == null) {
            log.warn("忽略未知切片重建阶段: task_uuid={}, stage={}", message.getTask_uuid(), message.getStage());
            return;
        }

        KbDocumentChunk chunk = baseMapper.selectOne(Wrappers.<KbDocumentChunk>lambdaQuery()
                .eq(KbDocumentChunk::getDocumentId, documentId)
                .eq(KbDocumentChunk::getVectorId, String.valueOf(vectorId))
                .last("limit 1"));
        if (chunk == null) {
            log.warn("切片重建结果未命中本地切片: task_uuid={}, document_id={}, vector_id={}",
                    message.getTask_uuid(), documentId, vectorId);
            return;
        }

        KbDocumentChunk updateEntity = new KbDocumentChunk();
        updateEntity.setId(chunk.getId());
        updateEntity.setStage(incomingStage.getCode());
        updateEntity.setUpdatedAt(new Date());
        if (baseMapper.updateById(updateEntity) <= 0) {
            log.warn("更新切片阶段失败: chunk_id={}, task_uuid={}, stage={}",
                    chunk.getId(), message.getTask_uuid(), message.getStage());
            return;
        }

        if (KbDocumentChunkStageEnum.FAILED == incomingStage) {
            if (containsStaleReplacementMessage(message.getMessage())) {
                log.info("切片重建任务被新版本替代: chunk_id={}, task_uuid={}, vector_id={}, version={}, message={}",
                        chunk.getId(), message.getTask_uuid(), vectorId, message.getVersion(), message.getMessage());
            } else {
                log.warn("切片重建失败: chunk_id={}, task_uuid={}, vector_id={}, version={}, message={}",
                        chunk.getId(), message.getTask_uuid(), vectorId, message.getVersion(), message.getMessage());
            }
            return;
        }

        log.info("切片重建结果已回写: chunk_id={}, task_uuid={}, vector_id={}, version={}, stage={}",
                chunk.getId(), message.getTask_uuid(), vectorId, message.getVersion(), incomingStage.getCode());
    }

    /**
     * 处理 AI 回传的切片新增结果，并回写本地占位切片。
     *
     * @param message AI 回传的结果消息
     */
    @Override
    public void handleChunkAddResult(KnowledgeChunkAddResultMessage message) {
        if (message == null) {
            return;
        }
        Long chunkId = message.getChunk_id();
        Long documentId = message.getDocument_id();
        if (chunkId == null || chunkId <= 0 || documentId == null || documentId <= 0) {
            log.warn("忽略无效切片新增结果: task_uuid={}, chunk_id={}, document_id={}",
                    message.getTask_uuid(), chunkId, documentId);
            return;
        }

        KbDocumentChunkStageEnum incomingStage = resolveChunkStage(message.getStage());
        if (incomingStage == null) {
            log.warn("忽略未知切片新增阶段: task_uuid={}, chunk_id={}, stage={}",
                    message.getTask_uuid(), chunkId, message.getStage());
            return;
        }

        KbDocumentChunk chunk = baseMapper.selectById(chunkId);
        if (chunk == null) {
            log.warn("切片新增结果未命中本地切片: task_uuid={}, chunk_id={}", message.getTask_uuid(), chunkId);
            return;
        }
        if (!Objects.equals(chunk.getDocumentId(), documentId)) {
            log.warn("切片新增结果文档不一致: task_uuid={}, chunk_id={}, local_document_id={}, message_document_id={}",
                    message.getTask_uuid(), chunkId, chunk.getDocumentId(), documentId);
            return;
        }
        if (shouldIgnoreChunkAddResult(chunk, message, incomingStage)) {
            return;
        }

        if (KbDocumentChunkStageEnum.COMPLETED == incomingStage
                && !hasValidChunkAddPayload(message.getVector_id(), message.getChunk_index())) {
            log.warn("切片新增完成结果缺少必要字段: task_uuid={}, chunk_id={}, vector_id={}, chunk_index={}",
                    message.getTask_uuid(), chunkId, message.getVector_id(), message.getChunk_index());
            markChunkStageFailed(chunkId, "切片新增完成结果缺少必要字段后");
            return;
        }

        KbDocumentChunk updateEntity = new KbDocumentChunk();
        updateEntity.setId(chunkId);
        updateEntity.setStage(incomingStage.getCode());
        updateEntity.setUpdatedAt(new Date());
        if (KbDocumentChunkStageEnum.COMPLETED == incomingStage) {
            updateEntity.setVectorId(String.valueOf(message.getVector_id()));
            updateEntity.setChunkIndex(message.getChunk_index());
        }
        if (baseMapper.updateById(updateEntity) <= 0) {
            log.warn("更新切片新增阶段失败: chunk_id={}, task_uuid={}, stage={}",
                    chunkId, message.getTask_uuid(), message.getStage());
            return;
        }

        if (KbDocumentChunkStageEnum.FAILED == incomingStage) {
            log.warn("切片新增失败: chunk_id={}, task_uuid={}, message={}",
                    chunkId, message.getTask_uuid(), message.getMessage());
            return;
        }

        if (KbDocumentChunkStageEnum.STARTED == incomingStage) {
            log.info("切片新增已开始处理: chunk_id={}, task_uuid={}", chunkId, message.getTask_uuid());
            return;
        }

        log.info("切片新增结果已回写: chunk_id={}, task_uuid={}, vector_id={}, chunk_index={}, stage={}",
                chunkId, message.getTask_uuid(), message.getVector_id(), message.getChunk_index(), incomingStage.getCode());
    }

    /**
     * 按文档ID替换切片数据，先删后插。
     *
     * @param documentId 文档ID
     * @param chunks     新切片列表
     */
    @Override
    public void replaceByDocumentId(Long documentId, List<KbDocumentChunk> chunks) {
        Assert.isPositive(documentId, "文档ID不能为空");

        lambdaUpdate()
                .eq(KbDocumentChunk::getDocumentId, documentId)
                .remove();

        if (CollectionUtils.isEmpty(chunks)) {
            return;
        }
        boolean saved = saveBatch(chunks, BATCH_SIZE);
        if (!saved) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "保存文档切片失败");
        }
    }

    /**
     * 按文档ID集合批量删除本地切片数据。
     *
     * @param documentIds 文档ID集合
     * @return true 表示删除成功
     */
    @Override
    public boolean removeByDocumentIds(List<Long> documentIds) {
        Assert.notEmpty(documentIds, "文档ID不能为空");
        return lambdaUpdate()
                .in(KbDocumentChunk::getDocumentId, documentIds)
                .remove();
    }

    /**
     * 在一个本地事务内完成切片内容更新和已废弃的历史记录落库。
     *
     * @param chunk    原切片实体
     * @param kbBase   所属知识库
     * @param content  新切片内容
     * @param vectorId 向量ID
     * @param taskUuid 本次编辑任务ID
     * @deprecated 历史记录写入依赖已废弃的 kb_document_chunk_history 表，后续删除表时同步移除。
     */
    @Deprecated(since = "1.0-beta", forRemoval = true)
    private void persistChunkEdit(KbDocumentChunk chunk, KbBase kbBase, String content, long vectorId, String taskUuid) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.executeWithoutResult(status -> {
            Date now = new Date();

            KbDocumentChunk updateEntity = KbDocumentChunk.builder()
                    .id(chunk.getId())
                    .content(content)
                    .charCount(content.length())
                    .stage(KbDocumentChunkStageEnum.PENDING.getCode())
                    .updatedAt(now)
                    .build();
            Assert.isTrue(baseMapper.updateById(updateEntity) > 0, "更新文档切片内容失败");

            KbDocumentChunkHistory history = KbDocumentChunkHistory.builder()
                    .documentId(chunk.getDocumentId())
                    .chunkId(chunk.getId())
                    .knowledgeName(kbBase.getKnowledgeName())
                    .vectorId(vectorId)
                    .oldContent(chunk.getContent())
                    .taskId(taskUuid)
                    .operatorId(resolveCurrentOperatorId())
                    .createdAt(now)
                    .build();
            Assert.isTrue(kbDocumentChunkHistoryService.save(history), "保存文档切片历史失败");
        });
    }

    /**
     * 构建新增切片的本地占位记录。
     *
     * @param documentId      文档ID
     * @param knowledgeBaseId 所属知识库ID
     * @param content         切片内容
     * @return 待持久化的切片实体
     */
    private KbDocumentChunk buildPendingChunk(Long documentId, Long knowledgeBaseId, String content) {
        Date now = new Date();
        return KbDocumentChunk.builder()
                .documentId(documentId)
                .knowledgeBaseId(knowledgeBaseId)
                .chunkIndex(nextChunkIndex(documentId))
                .content(content)
                .vectorId(null)
                .charCount(content.length())
                .status(CHUNK_STATUS_ENABLED)
                .stage(KbDocumentChunkStageEnum.PENDING.getCode())
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    /**
     * 查询指定文档下一条临时切片序号。
     *
     * @param documentId 文档ID
     * @return 临时切片序号
     */
    private int nextChunkIndex(Long documentId) {
        Integer maxChunkIndex = baseMapper.selectMaxChunkIndex(documentId);
        if (maxChunkIndex == null || maxChunkIndex < 0) {
            return 1;
        }
        return maxChunkIndex + 1;
    }

    /**
     * 仅允许已完成或失败态的切片再次发起修改；待处理或处理中必须串行化。
     *
     * @param chunk 当前切片
     */
    private void assertChunkEditable(KbDocumentChunk chunk) {
        if (chunk == null) {
            return;
        }
        KbDocumentChunkStageEnum currentStage = KbDocumentChunkStageEnum.fromCode(chunk.getStage());
        if (currentStage != null && currentStage.isProcessing()) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, EDIT_IN_PROGRESS_MESSAGE);
        }
    }

    /**
     * 版本号以同一个 vector_id 为粒度递增，不是全局递增。
     * 发布 MQ 前必须先写 Redis latest-version，AI 侧也不会主动删除该 key。
     *
     * @param vectorId 向量ID
     * @return 新生成的最新版本号
     */
    private long nextChunkEditVersionAndSetLatest(long vectorId) {
        String latestKey = latestChunkEditVersionKey(vectorId);
        Long version = redisCache.increment(latestKey);
        if (version == null || version <= 0) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "生成切片重建版本号失败");
        }
        redisCache.expire(latestKey, CHUNK_EDIT_LATEST_VERSION_TTL_DAYS, TimeUnit.DAYS);
        return version;
    }

    /**
     * 读取指定向量当前记录的最新切片编辑版本号。
     *
     * @param vectorId 向量ID
     * @return 最新版本号；不存在或无法解析时返回 null
     */
    private Long getLatestChunkEditVersion(long vectorId) {
        Object value = redisCache.getCacheObject(latestChunkEditVersionKey(vectorId));
        if (value == null) {
            return null;
        }
        try {
            if (value instanceof Number number) {
                return number.longValue();
            }
            return Long.parseLong(String.valueOf(value));
        } catch (Exception ex) {
            log.error("解析切片最新版本失败: vector_id={}, value={}", vectorId, value, ex);
            return null;
        }
    }

    /**
     * 生成单切片编辑 latest-version Redis key。
     *
     * @param vectorId 向量ID
     * @return Redis key
     */
    private String latestChunkEditVersionKey(long vectorId) {
        return CHUNK_EDIT_LATEST_VERSION_KEY_PREFIX + vectorId;
    }

    /**
     * 判断当前回调结果是否已落后于 Redis 中记录的最新版本。
     *
     * @param message 回调结果消息
     * @return true 表示旧版本消息，应直接丢弃
     */
    private boolean isStaleResultMessage(KnowledgeChunkRebuildResultMessage message) {
        Long vectorId = message.getVector_id();
        Long messageVersion = message.getVersion();
        if (vectorId == null || vectorId <= 0) {
            return false;
        }
        Long latestVersion = getLatestChunkEditVersion(vectorId);
        if (latestVersion != null && messageVersion != null && messageVersion < latestVersion) {
            log.info("丢弃旧版本切片重建结果: task_uuid={}, vector_id={}, message_version={}, latest_version={}, stage={}",
                    message.getTask_uuid(), vectorId, messageVersion, latestVersion, message.getStage());
            return true;
        }
        return false;
    }

    /**
     * 判断切片新增结果是否应按幂等忽略。
     *
     * @param chunk         本地切片
     * @param message       AI 回传结果
     * @param incomingStage 标准化后的切片阶段
     * @return true 表示应忽略
     */
    private boolean shouldIgnoreChunkAddResult(KbDocumentChunk chunk, KnowledgeChunkAddResultMessage message,
                                               KbDocumentChunkStageEnum incomingStage) {
        KbDocumentChunkStageEnum currentStage = KbDocumentChunkStageEnum.fromCode(chunk.getStage());
        if (currentStage == null || !currentStage.isTerminal()) {
            return false;
        }
        if (KbDocumentChunkStageEnum.COMPLETED == currentStage && KbDocumentChunkStageEnum.COMPLETED == incomingStage) {
            if (matchesCompletedChunkAddResult(chunk, message)) {
                log.info("忽略重复切片新增完成消息: chunk_id={}, task_uuid={}", chunk.getId(), message.getTask_uuid());
                return true;
            }
            log.warn("忽略不一致的重复切片新增完成消息: chunk_id={}, task_uuid={}, local_vector_id={}, message_vector_id={}, local_chunk_index={}, message_chunk_index={}",
                    chunk.getId(), message.getTask_uuid(), chunk.getVectorId(), message.getVector_id(),
                    chunk.getChunkIndex(), message.getChunk_index());
            return true;
        }
        log.info("忽略终态后的切片新增结果: chunk_id={}, task_uuid={}, current_stage={}, incoming_stage={}",
                chunk.getId(), message.getTask_uuid(), currentStage.getCode(), incomingStage.getCode());
        return true;
    }

    /**
     * 判断重复的 COMPLETED 消息是否与本地已完成数据一致。
     *
     * @param chunk   本地切片
     * @param message AI 回传结果
     * @return true 表示一致
     */
    private boolean matchesCompletedChunkAddResult(KbDocumentChunk chunk, KnowledgeChunkAddResultMessage message) {
        if (!hasValidChunkAddPayload(message.getVector_id(), message.getChunk_index())) {
            return false;
        }
        return Objects.equals(chunk.getVectorId(), String.valueOf(message.getVector_id()))
                && Objects.equals(chunk.getChunkIndex(), message.getChunk_index());
    }

    /**
     * 组装单切片重建 command 消息。
     *
     * @param kbBase     知识库实体
     * @param documentId 文档ID
     * @param vectorId   向量ID
     * @param version    当前版本号
     * @param content    新切片内容
     * @param taskUuid   任务ID
     * @return 可发送到 MQ 的 command 消息体
     */
    private KnowledgeChunkRebuildCommandMessage buildChunkRebuildCommand(KbBase kbBase, Long documentId,
                                                                         long vectorId, long version,
                                                                         String content, String taskUuid) {
        return KnowledgeChunkRebuildCommandMessage.builder()
                .message_type(CHUNK_REBUILD_COMMAND_MESSAGE_TYPE)
                .task_uuid(taskUuid)
                .knowledge_name(kbBase.getKnowledgeName())
                .document_id(documentId)
                .vector_id(vectorId)
                .version(version)
                .content(content)
                .embedding_model(kbBase.getEmbeddingModel())
                .created_at(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(OffsetDateTime.now(ZoneOffset.UTC)))
                .build();
    }

    /**
     * 组装切片新增 command 消息。
     *
     * @param kbBase   知识库实体
     * @param chunk    本地占位切片
     * @param taskUuid 任务ID
     * @return 可发送到 MQ 的 command 消息体
     */
    private KnowledgeChunkAddCommandMessage buildChunkAddCommand(KbBase kbBase, KbDocumentChunk chunk, String taskUuid) {
        return KnowledgeChunkAddCommandMessage.builder()
                .message_type(CHUNK_ADD_COMMAND_MESSAGE_TYPE)
                .task_uuid(taskUuid)
                .chunk_id(chunk.getId())
                .knowledge_name(kbBase.getKnowledgeName())
                .document_id(chunk.getDocumentId())
                .content(chunk.getContent())
                .embedding_model(kbBase.getEmbeddingModel())
                .created_at(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(OffsetDateTime.now(ZoneOffset.UTC)))
                .build();
    }

    /**
     * 当 MQ 提交失败时，将本地切片阶段回写为 FAILED。
     *
     * @param chunkId 切片ID
     */
    private void markChunkStageFailed(Long chunkId, String scene) {
        KbDocumentChunk updateEntity = new KbDocumentChunk();
        updateEntity.setId(chunkId);
        updateEntity.setStage(KbDocumentChunkStageEnum.FAILED.getCode());
        updateEntity.setUpdatedAt(new Date());
        if (baseMapper.updateById(updateEntity) <= 0) {
            log.warn("{}回写 FAILED 失败: chunk_id={}", scene, chunkId);
        }
    }

    /**
     * 校验切片状态是否在允许范围内。
     *
     * @param status 切片状态，只允许 0 或 1
     */
    private void validateChunkStatus(Integer status) {
        Assert.notNull(status, "切片状态不能为空");
        Assert.isParamTrue(status == CHUNK_STATUS_ENABLED || status == CHUNK_STATUS_DISABLED, "切片状态只允许为0或1");
    }

    /**
     * 根据文档ID查询文档实体。
     *
     * @param documentId 文档ID
     * @return 文档实体
     */
    private KbDocument getDocument(Long documentId) {
        Assert.isPositive(documentId, "文档ID必须大于0");
        KbDocument document = kbDocumentMapper.selectById(documentId);
        Assert.isTrue(document != null, "文档不存在");
        return document;
    }

    /**
     * 尝试读取当前操作人ID；未登录或无法获取时返回 null。
     *
     * @return 当前操作人ID
     */
    private Long resolveCurrentOperatorId() {
        try {
            return getUserId();
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * 将数据库中字符串形式保存的向量ID解析为正整数。
     *
     * @param value 数据库存储的向量ID
     * @return 解析后的向量ID
     */
    private long parseStoredPositiveLong(String value) {
        if (!StringUtils.hasText(value)) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "向量ID无效");
        }
        try {
            long parsed = Long.parseLong(value.trim());
            if (parsed <= 0) {
                throw new NumberFormatException("not positive");
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "向量ID无效");
        }
    }

    /**
     * 将 AI 回调阶段标准化为切片任务阶段。
     *
     * @param stage AI 回调阶段
     * @return 标准化后的切片任务阶段；无法识别时返回 null
     */
    private KnowledgeChunkTaskStageEnum resolveChunkTaskStage(String stage) {
        return KnowledgeChunkTaskStageEnum.fromCode(stage);
    }

    /**
     * 将 AI 回调阶段转换为本地切片阶段。
     *
     * @param stage AI 回调阶段
     * @return 本地切片阶段；无法识别时返回 null
     */
    private KbDocumentChunkStageEnum resolveChunkStage(String stage) {
        return KbDocumentChunkStageEnum.fromTaskStage(resolveChunkTaskStage(stage));
    }

    /**
     * 判断向量ID和切片序号是否均为正数。
     *
     * @param vectorId   向量ID
     * @param chunkIndex 切片序号
     * @return true 表示合法
     */
    private boolean hasValidChunkAddPayload(Long vectorId, Integer chunkIndex) {
        return vectorId != null && vectorId > 0 && chunkIndex != null && chunkIndex > 0;
    }

    /**
     * 判断失败信息是否表示旧任务已被新版本替代。
     *
     * @param message AI 返回的失败信息
     * @return true 表示旧任务被新版本覆盖
     */
    private boolean containsStaleReplacementMessage(String message) {
        return StringUtils.hasText(message) && message.contains(STALE_RESULT_MESSAGE);
    }

    /**
     * 提取异常消息，便于返回给上层接口。
     *
     * @param ex 异常对象
     * @return 优先返回异常 message，无值时返回异常类型名
     */
    private String extractErrorMessage(Exception ex) {
        String message = ex.getMessage();
        if (StringUtils.hasText(message)) {
            return message;
        }
        return ex.getClass().getSimpleName();
    }
}
