package com.zhangyichuang.medicine.admin.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhangyichuang.medicine.admin.integration.MedicineAgentClient;
import com.zhangyichuang.medicine.admin.mapper.KbDocumentMapper;
import com.zhangyichuang.medicine.admin.model.dto.KnowledgeBaseDocumentDto;
import com.zhangyichuang.medicine.admin.model.request.DocumentDeleteRequest;
import com.zhangyichuang.medicine.admin.model.request.DocumentListRequest;
import com.zhangyichuang.medicine.admin.model.request.DocumentUpdateFileNameRequest;
import com.zhangyichuang.medicine.admin.model.request.KnowledgeBaseImportRequest;
import com.zhangyichuang.medicine.admin.publisher.KnowledgePublisher;
import com.zhangyichuang.medicine.admin.service.KbBaseService;
import com.zhangyichuang.medicine.admin.service.KbDocumentChunkService;
import com.zhangyichuang.medicine.admin.service.KbDocumentService;
import com.zhangyichuang.medicine.common.core.enums.ResponseCode;
import com.zhangyichuang.medicine.common.core.exception.ServiceException;
import com.zhangyichuang.medicine.common.core.utils.Assert;
import com.zhangyichuang.medicine.common.redis.core.RedisCache;
import com.zhangyichuang.medicine.common.security.base.BaseService;
import com.zhangyichuang.medicine.model.entity.KbBase;
import com.zhangyichuang.medicine.model.entity.KbDocument;
import com.zhangyichuang.medicine.model.entity.KbDocumentChunk;
import com.zhangyichuang.medicine.model.enums.KbDocumentChunkStageEnum;
import com.zhangyichuang.medicine.model.enums.KbDocumentStageEnum;
import com.zhangyichuang.medicine.model.enums.KnowledgeChunkModeEnum;
import com.zhangyichuang.medicine.model.mq.KnowledgeImportDocumentMessage;
import com.zhangyichuang.medicine.model.mq.KnowledgeImportResultMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author Chuang
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KbDocumentServiceImpl extends ServiceImpl<KbDocumentMapper, KbDocument>
        implements KbDocumentService, BaseService {

    /**
     * 切片启用状态。
     */
    private static final int CHUNK_STATUS_ENABLED = 0;

    /**
     * 切片同步最大重试次数。
     */
    private static final int CHUNK_SYNC_MAX_RETRY = 3;

    /**
     * 切片同步失败后的重试间隔，单位毫秒。
     */
    private static final long CHUNK_SYNC_RETRY_INTERVAL_MS = 200L;

    /**
     * Redis 中记录文档最新版本号的 Key 前缀。
     */
    private static final String REDIS_LATEST_VERSION_KEY_PREFIX = "kb:latest:";

    /**
     * Redis 最新版本号缓存保留天数。
     */
    private static final long REDIS_LATEST_VERSION_TTL_DAYS = 7L;

    /**
     * 知识库导入文档消息类型，协议值沿用 knowledge_import_command。
     */
    private static final String IMPORT_DOCUMENT_MESSAGE_TYPE = "knowledge_import_command";

    /**
     * 系统回写文档阶段时使用的更新人标识。
     */
    private static final String SYSTEM_UPDATER = "system";

    private final KbBaseService kbBaseService;
    private final RedisCache redisCache;
    private final KnowledgePublisher knowledgePublisher;
    private final MedicineAgentClient medicineAgentClient;
    private final KbDocumentChunkService kbDocumentChunkService;

    @Override
    public Page<KnowledgeBaseDocumentDto> listDocument(Long knowledgeBaseId, DocumentListRequest request) {
        Assert.notNull(request, "查询参数不能为空");
        Assert.isPositive(knowledgeBaseId, "知识库ID必须大于0");
        KbBase kbBase = kbBaseService.getKnowledgeBaseById(knowledgeBaseId);
        Assert.isTrue(kbBase != null, "知识库不存在");
        Page<KnowledgeBaseDocumentDto> page = request.toPage();
        return baseMapper.listDocument(page, knowledgeBaseId, request);
    }

    @Override
    public KnowledgeBaseDocumentDto getDocumentDetailById(Long id) {
        Assert.isPositive(id, "文档ID必须大于0");
        KnowledgeBaseDocumentDto document = baseMapper.getDocumentDetailById(id);
        Assert.isTrue(document != null, "文档不存在");
        return document;
    }

    @Override
    public KbDocument getDocumentById(Long id) {
        Assert.isPositive(id, "文档ID必须大于0");
        KbDocument document = getById(id);
        Assert.isTrue(document != null, "文档不存在");
        return document;
    }

    @Override
    public boolean updateDocumentFileName(DocumentUpdateFileNameRequest request) {
        Assert.notNull(request, "文档文件名更新请求不能为空");
        Assert.isPositive(request.getId(), "文档ID必须大于0");

        String fileName = strip(request.getFileName());
        Assert.notEmpty(fileName, "文件名不能为空");

        KbDocument document = getDocumentById(request.getId());
        if (Objects.equals(fileName, document.getFileName())) {
            return true;
        }

        document.setFileName(fileName);
        document.setUpdateBy(getUsername());
        document.setUpdatedAt(new Date());
        return updateById(document);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDocument(Long id) {
        KbDocument document = getDocumentById(id);
        KbBase kbBase = kbBaseService.getKnowledgeBaseById(document.getKnowledgeBaseId());
        return deleteDocumentsInternal(kbBase, List.of(document));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDocuments(@Validated DocumentDeleteRequest request) {
        Assert.notNull(request, "删除文档请求不能为空");
        List<Long> documentIds = normalizeDocumentIds(request.getDocumentIds());

        List<KbDocument> documents = listByIds(documentIds);
        Assert.isTrue(documents.size() == documentIds.size(), "文档不存在");
        Set<Long> knowledgeBaseIds = new LinkedHashSet<>();
        for (KbDocument document : documents) {
            Assert.notNull(document.getKnowledgeBaseId(), "文档知识库ID不能为空");
            knowledgeBaseIds.add(document.getKnowledgeBaseId());
        }
        Assert.isTrue(knowledgeBaseIds.size() == 1, "仅支持删除同一知识库下的文档");
        KbBase kbBase = kbBaseService.getKnowledgeBaseById(knowledgeBaseIds.iterator().next());
        return deleteDocumentsInternal(kbBase, documents);
    }

    /**
     * 发起文档导入。
     * <p>
     * 流程：按知识库ID读取配置 -> 为每个文件新建文档记录 ->
     * 生成/写入最新版本号 -> 发送 MQ 导入文档消息。
     * </p>
     *
     * @param request 导入请求参数（知识库ID、文件详情集合、切片参数）
     */
    @Override
    public void importDocument(@Validated KnowledgeBaseImportRequest request) {
        Assert.isPositive(request.getKnowledgeBaseId(), "知识库ID必须大于0");
        KbBase kbBase = kbBaseService.getKnowledgeBaseById(request.getKnowledgeBaseId());
        Assert.isTrue(kbBase != null, "知识库不存在");
        Assert.notEmpty(kbBase.getEmbeddingModel(), "知识库向量模型未配置");
        NormalizedChunkSettings chunkSettings = normalizeChunkSettings(request);

        String username = getUsername();
        for (KnowledgeBaseImportRequest.FileDetail rawFileDetail : request.getFileDetails()) {
            Assert.notNull(rawFileDetail, "文件详情不能为空");
            String fileUrl = strip(rawFileDetail.getFileUrl());
            String fileName = strip(rawFileDetail.getFileName());
            String fileType = resolveFileType(rawFileDetail);
            Assert.notEmpty(fileUrl, "文件地址不能为空");
            Assert.notEmpty(fileName, "文件名不能为空");

            KbDocument document = buildPendingDocument(kbBase.getId(), fileUrl, fileName, fileType, chunkSettings, username);
            boolean saved = save(document);
            Assert.isTrue(saved && document.getId() != null, "创建导入文档失败");

            String bizKey = buildBizKey(kbBase.getKnowledgeName(), document.getId());
            Long version = nextVersionAndSetLatest(bizKey);
            KnowledgeImportDocumentMessage importDocumentMessage =
                    buildImportDocumentMessage(kbBase, document, bizKey, version);
            try {
                knowledgePublisher.publishImportDocument(importDocumentMessage);
            } catch (Exception ex) {
                markDocumentFailed(document.getId(), ex.getMessage(), username);
                throw ex;
            }
        }
    }

    /**
     * 处理知识库导入结果消息。
     * <p>
     * 仅处理最新版本事件；若收到旧版本消息则直接丢弃。
     * 收到 COMPLETED 时先将文档置为 INSERTING，并投递切片同步消息。
     * </p>
     *
     * @param message AI 回传的导入结果消息
     */
    @Override
    public void handleImportResult(KnowledgeImportResultMessage message) {
        if (message == null) {
            return;
        }
        if (isStaleVersionMessage(message)) {
            return;
        }

        log.info("接收知识库导入结果: task_uuid={}, biz_key={}, version={}, stage={}, message={}",
                message.getTask_uuid(), message.getBiz_key(), message.getVersion(), message.getStage(), message.getMessage());

        Long documentId = message.getDocument_id();
        if (documentId == null || documentId <= 0) {
            log.warn("知识库导入结果缺少有效 document_id: task_uuid={}, biz_key={}", message.getTask_uuid(), message.getBiz_key());
            return;
        }

        KbDocument document = getById(documentId);
        if (document == null) {
            log.warn("知识库导入结果对应文档不存在: document_id={}, task_uuid={}", documentId, message.getTask_uuid());
            return;
        }

        KbDocumentStageEnum incomingStage = KbDocumentStageEnum.fromCode(message.getStage());
        if (incomingStage == null || !incomingStage.isAiCallbackStage()) {
            log.warn("忽略未知或不支持的知识库导入阶段: task_uuid={}, document_id={}, stage={}",
                    message.getTask_uuid(), documentId, message.getStage());
            return;
        }
        boolean completedStage = KbDocumentStageEnum.COMPLETED == incomingStage;
        document.setStage(completedStage
                ? KbDocumentStageEnum.INSERTING.getCode()
                : incomingStage.getCode());
        String callbackFileType = normalizeFileType(strip(message.getFile_type()));
        if (StringUtils.hasText(callbackFileType)) {
            document.setFileType(callbackFileType);
        }
        Long callbackFileSize = message.getFile_size();
        if (callbackFileSize != null && callbackFileSize >= 0L) {
            document.setFileSize(callbackFileSize);
        }
        if (KbDocumentStageEnum.FAILED == incomingStage) {
            document.setLastError(message.getMessage());
        } else {
            document.setLastError(null);
        }
        document.setUpdateBy(SYSTEM_UPDATER);
        document.setUpdatedAt(new Date());
        boolean updated = updateById(document);
        if (!updated) {
            log.warn("更新知识库导入状态失败: document_id={}, task_uuid={}", documentId, message.getTask_uuid());
            return;
        }

        if (!completedStage) {
            return;
        }

        try {
            knowledgePublisher.publishImportChunkUpdate(message);
        } catch (Exception ex) {
            log.error("投递切片同步消息失败: task_uuid={}, document_id={}", message.getTask_uuid(), documentId, ex);
            markDocumentFailed(documentId, ex.getMessage(), SYSTEM_UPDATER);
        }
    }

    /**
     * 处理切片同步消息。
     * <p>
     * 拉取切片并写库成功后再将文档阶段改为 COMPLETED。
     * </p>
     *
     * @param message 切片同步消息
     */
    @Override
    public void handleChunkUpdateResult(KnowledgeImportResultMessage message) {
        if (message == null) {
            return;
        }
        if (isStaleVersionMessage(message)) {
            return;
        }
        if (!KbDocumentStageEnum.COMPLETED.matches(message.getStage())) {
            log.info("跳过非 COMPLETED 切片同步消息: task_uuid={}, stage={}", message.getTask_uuid(), message.getStage());
            return;
        }

        Long documentId = message.getDocument_id();
        if (documentId == null || documentId <= 0) {
            log.warn("切片同步消息缺少有效 document_id: task_uuid={}, biz_key={}", message.getTask_uuid(), message.getBiz_key());
            return;
        }
        KbDocument document = getById(documentId);
        if (document == null) {
            log.warn("切片同步消息对应文档不存在: document_id={}, task_uuid={}", documentId, message.getTask_uuid());
            return;
        }

        String knowledgeName = resolveKnowledgeName(message);
        if (!StringUtils.hasText(knowledgeName)) {
            markDocumentFailed(documentId, "知识库名称不能为空", SYSTEM_UPDATER);
            return;
        }

        for (int attempt = 1; attempt <= CHUNK_SYNC_MAX_RETRY; attempt++) {
            try {
                List<MedicineAgentClient.DocumentChunkRow> rows =
                        medicineAgentClient.listDocumentChunks(knowledgeName, documentId);
                List<KbDocumentChunk> chunks = mapDocumentChunks(rows, documentId, document.getKnowledgeBaseId());
                kbDocumentChunkService.replaceByDocumentId(documentId, chunks);
                markDocumentCompleted(documentId);
                log.info("知识库切片同步成功: task_uuid={}, document_id={}, chunk_count={}",
                        message.getTask_uuid(), documentId, chunks.size());
                return;
            } catch (Exception ex) {
                log.warn("知识库切片同步失败: task_uuid={}, document_id={}, attempt={}/{}, error={}",
                        message.getTask_uuid(), documentId, attempt, CHUNK_SYNC_MAX_RETRY, extractErrorMessage(ex));
                if (attempt < CHUNK_SYNC_MAX_RETRY) {
                    sleepBeforeRetry(attempt);
                } else {
                    markDocumentFailed(documentId, extractErrorMessage(ex), SYSTEM_UPDATER);
                }
            }
        }
    }

    /**
     * 构建待导入文档的初始记录（PENDING）。
     *
     * @param knowledgeBaseId 知识库主键
     * @param fileUrl         文件访问地址
     * @param fileName        文件名
     * @param fileType        文件类型
     * @param chunkSettings   归一化后的切片设置
     * @param username        操作人账号
     * @return 待持久化的文档实体
     */
    private KbDocument buildPendingDocument(Long knowledgeBaseId, String fileUrl, String fileName, String fileType,
                                            NormalizedChunkSettings chunkSettings, String username) {
        Date now = new Date();
        return KbDocument.builder()
                .knowledgeBaseId(knowledgeBaseId)
                .fileUrl(fileUrl)
                .fileName(fileName)
                .fileType(fileType)
                .chunkMode(chunkSettings.chunkMode())
                .chunkSize(chunkSettings.chunkSize())
                .chunkOverlap(chunkSettings.chunkOverlap())
                .stage(KbDocumentStageEnum.PENDING.getCode())
                .createBy(username)
                .updateBy(username)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    /**
     * 组装知识库导入文档消息体。
     *
     * @param kbBase   知识库配置实体
     * @param document 文档实体（含 documentId）
     * @param bizKey   业务键
     * @param version  当前版本号
     * @return 可投递到 MQ 的导入文档消息
     */
    private KnowledgeImportDocumentMessage buildImportDocumentMessage(KbBase kbBase, KbDocument document,
                                                                      String bizKey, Long version) {
        return KnowledgeImportDocumentMessage.builder()
                .message_type(IMPORT_DOCUMENT_MESSAGE_TYPE)
                .task_uuid(UUID.randomUUID().toString())
                .biz_key(bizKey)
                .version(version)
                .knowledge_name(kbBase.getKnowledgeName())
                .document_id(document.getId())
                .file_url(document.getFileUrl())
                .embedding_model(kbBase.getEmbeddingModel())
                .chunk_size(document.getChunkSize())
                .chunk_overlap(document.getChunkOverlap())
                .created_at(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(OffsetDateTime.now(ZoneOffset.UTC)))
                .build();
    }

    /**
     * 归一化导入请求中的切片设置。
     *
     * @param request 导入请求
     * @return 归一化后的切片设置
     */
    private NormalizedChunkSettings normalizeChunkSettings(KnowledgeBaseImportRequest request) {
        KnowledgeChunkModeEnum chunkMode = KnowledgeChunkModeEnum.fromCode(strip(request.getChunkMode()));
        Assert.isParamTrue(chunkMode != null, "切片模式不支持");

        if (!chunkMode.isCustom()) {
            return new NormalizedChunkSettings(chunkMode.getCode(), chunkMode.getChunkSize(), chunkMode.getChunkOverlap());
        }

        KnowledgeBaseImportRequest.CustomChunkMode customChunkMode = request.getCustomChunkMode();
        Assert.notNull(customChunkMode, "自定义模式参数不能为空");
        Integer chunkSize = customChunkMode.getChunkSize();
        Integer chunkOverlap = customChunkMode.getChunkOverlap();
        Assert.notNull(chunkSize, "自定义模式切片大小不能为空");
        Assert.notNull(chunkOverlap, "自定义模式切片重叠大小不能为空");
        Assert.isParamTrue(chunkSize >= KnowledgeChunkModeEnum.CUSTOM_CHUNK_SIZE_MIN
                        && chunkSize <= KnowledgeChunkModeEnum.CUSTOM_CHUNK_SIZE_MAX,
                "自定义模式切片大小必须在100到6000之间");
        Assert.isParamTrue(chunkOverlap >= KnowledgeChunkModeEnum.CUSTOM_CHUNK_OVERLAP_MIN
                        && chunkOverlap <= KnowledgeChunkModeEnum.CUSTOM_CHUNK_OVERLAP_MAX,
                "自定义模式切片重叠大小必须在0到1000之间");
        return new NormalizedChunkSettings(chunkMode.getCode(), chunkSize, chunkOverlap);
    }

    private String strip(String value) {
        return value == null ? null : value.strip();
    }

    private String resolveFileType(KnowledgeBaseImportRequest.FileDetail fileDetail) {
        String fileName = strip(fileDetail.getFileName());
        String extension = extractFileExtension(fileName);
        if (StringUtils.hasText(extension)) {
            return extension;
        }
        return normalizeFileType(strip(fileDetail.getFileType()));
    }

    private String extractFileExtension(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return null;
        }
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex < 0 || lastDotIndex == fileName.length() - 1) {
            return null;
        }
        return normalizeFileType(fileName.substring(lastDotIndex + 1));
    }

    private String normalizeFileType(String fileType) {
        if (!StringUtils.hasText(fileType)) {
            return null;
        }
        String normalized = fileType.strip().toLowerCase(Locale.ROOT);
        return normalized.startsWith(".") ? normalized.substring(1) : normalized;
    }

    /**
     * 生成导入判旧业务键。
     *
     * @param knowledgeName 知识库名称
     * @param documentId    文档ID
     * @return 业务键（knowledgeName:documentId）
     */
    private String buildBizKey(String knowledgeName, Long documentId) {
        return knowledgeName + ":" + documentId;
    }

    /**
     * 生成并写入 bizKey 的最新版本号。
     * <p>
     * 使用 Redis 自增保证版本单调递增，并刷新 latest key 的 TTL。
     * </p>
     *
     * @param bizKey 业务键
     * @return 新版本号（>=1）
     */
    private Long nextVersionAndSetLatest(String bizKey) {
        String latestKey = latestVersionKey(bizKey);
        Long version = redisCache.increment(latestKey);
        if (version == null || version <= 0) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "生成导入版本号失败");
        }
        redisCache.expire(latestKey, REDIS_LATEST_VERSION_TTL_DAYS, TimeUnit.DAYS);
        return version;
    }

    /**
     * 读取业务键对应的最新版本号。
     *
     * @param bizKey 业务键
     * @return 最新版本号；无值或无法解析时返回 null
     */
    private Long getLatestVersion(String bizKey) {
        if (!StringUtils.hasText(bizKey)) {
            return null;
        }
        Object value = redisCache.getCacheObject(latestVersionKey(bizKey));
        if (value == null) {
            return null;
        }
        try {
            if (value instanceof Number number) {
                return number.longValue();
            }
            return Long.parseLong(String.valueOf(value));
        } catch (Exception ex) {
            log.error("解析知识库最新版本失败: biz_key={}, value={}", bizKey, value, ex);
            return null;
        }
    }

    /**
     * 构建最新版本 Redis Key。
     *
     * @param bizKey 业务键
     * @return latest 版本 key
     */
    private String latestVersionKey(String bizKey) {
        return REDIS_LATEST_VERSION_KEY_PREFIX + bizKey;
    }

    /**
     * 判断当前消息是否为旧版本。
     *
     * @param message 当前收到的导入结果消息
     * @return true 表示消息版本落后于 Redis 中记录的最新版本；false 表示可继续处理
     */
    private boolean isStaleVersionMessage(KnowledgeImportResultMessage message) {
        String bizKey = message.getBiz_key();
        Long messageVersion = message.getVersion();
        Long latestVersion = getLatestVersion(bizKey);
        if (latestVersion != null && messageVersion != null && messageVersion < latestVersion) {
            log.info("丢弃知识库导入旧结果: task_uuid={}, biz_key={}, version={}, latest_version={}, stage={}",
                    message.getTask_uuid(), bizKey, messageVersion, latestVersion, message.getStage());
            return true;
        }
        return false;
    }

    /**
     * 将 AI 端返回的切片列表映射为本地切片实体。
     *
     * @param rows                    AI 端返回的切片行列表
     * @param fallbackDocumentId      当单条切片缺少文档ID时使用的兜底文档ID
     * @param fallbackKnowledgeBaseId 当单条切片缺少知识库ID时使用的兜底知识库ID
     * @return 可直接写入本地数据库的切片实体列表
     */
    private List<KbDocumentChunk> mapDocumentChunks(List<MedicineAgentClient.DocumentChunkRow> rows,
                                                    Long fallbackDocumentId,
                                                    Long fallbackKnowledgeBaseId) {
        List<KbDocumentChunk> chunks = new ArrayList<>();
        if (rows == null || rows.isEmpty()) {
            return chunks;
        }
        Date now = new Date();
        for (MedicineAgentClient.DocumentChunkRow row : rows) {
            if (row == null) {
                continue;
            }
            Long rowDocumentId = row.getDocument_id() == null ? fallbackDocumentId : row.getDocument_id();
            KbDocumentChunk chunk = KbDocumentChunk.builder()
                    .documentId(rowDocumentId)
                    .knowledgeBaseId(fallbackKnowledgeBaseId)
                    .chunkIndex(row.getChunk_index())
                    .content(row.getContent())
                    .vectorId(row.getId() == null ? null : String.valueOf(row.getId()))
                    .charCount(row.getChar_count())
                    .status(resolveChunkStatus(row.getStatus()))
                    .stage(KbDocumentChunkStageEnum.COMPLETED.getCode())
                    .createdAt(toDate(row.getCreated_at_ts(), now))
                    .updatedAt(now)
                    .build();
            chunks.add(chunk);
        }
        return chunks;
    }

    /**
     * 将毫秒时间戳转换为 {@link Date} 对象。
     *
     * @param milliseconds 毫秒时间戳
     * @param fallback     当时间戳为空或非法时返回的兜底时间
     * @return 转换后的时间对象；无效时返回 fallback
     */
    private Date toDate(Long milliseconds, Date fallback) {
        if (milliseconds == null || milliseconds <= 0) {
            return fallback;
        }
        return new Date(milliseconds);
    }

    /**
     * 从消息中解析知识库业务名称。
     * <p>
     * 优先读取 message 中的 knowledge_name，缺失时再从 biz_key 的前缀解析。
     * </p>
     *
     * @param message 导入结果或切片同步消息
     * @return 解析出的知识库名称；无法解析时返回 null
     */
    private String resolveKnowledgeName(KnowledgeImportResultMessage message) {
        if (StringUtils.hasText(message.getKnowledge_name())) {
            return message.getKnowledge_name().trim();
        }
        String bizKey = message.getBiz_key();
        if (!StringUtils.hasText(bizKey)) {
            return null;
        }
        int separatorIndex = bizKey.indexOf(':');
        if (separatorIndex <= 0) {
            return null;
        }
        return bizKey.substring(0, separatorIndex);
    }

    /**
     * 在切片同步失败后按次数进行短暂等待，避免立即重试。
     *
     * @param attempt 当前重试次数，从 1 开始
     */
    private void sleepBeforeRetry(int attempt) {
        try {
            Thread.sleep(CHUNK_SYNC_RETRY_INTERVAL_MS * attempt);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 删除文档的通用实现。
     * <p>
     * 先调用 AI 端删除向量数据，再删除本地切片和文档记录。
     * </p>
     *
     * @param kbBase    文档所属知识库
     * @param documents 待删除文档列表
     * @return true 表示删除成功
     */
    private boolean deleteDocumentsInternal(KbBase kbBase, List<KbDocument> documents) {
        Assert.notNull(kbBase, "知识库不存在");
        Assert.notEmpty(kbBase.getKnowledgeName(), "知识库名称不能为空");
        Assert.notEmpty(documents, "文档不存在");

        List<Long> documentIds = documents.stream()
                .map(KbDocument::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Assert.notEmpty(documentIds, "文档不存在");

        medicineAgentClient.deleteDocuments(kbBase.getKnowledgeName(), documentIds);
        kbDocumentChunkService.removeByDocumentIds(documentIds);

        boolean removed = removeByIds(documentIds);
        Assert.isTrue(removed, "删除文档失败");
        return true;
    }

    /**
     * 对外部传入的文档ID列表进行去重和正整数校验。
     *
     * @param documentIds 原始文档ID列表
     * @return 去重后的文档ID列表，保持原有顺序
     */
    private List<Long> normalizeDocumentIds(List<Long> documentIds) {
        Assert.notEmpty(documentIds, "文档ID不能为空");
        LinkedHashSet<Long> distinctIds = new LinkedHashSet<>();
        for (Long documentId : documentIds) {
            Assert.isPositive(documentId, "文档ID必须大于0");
            distinctIds.add(documentId);
        }
        return new ArrayList<>(distinctIds);
    }

    /**
     * 标准化切片状态。
     *
     * @param status AI 端返回的切片状态
     * @return 当 status 为空时返回启用状态，否则返回原始状态值
     */
    private Integer resolveChunkStatus(Integer status) {
        if (status == null) {
            return CHUNK_STATUS_ENABLED;
        }
        return status;
    }

    /**
     * 将指定文档标记为导入完成阶段。
     *
     * @param documentId 文档ID
     */
    private void markDocumentCompleted(Long documentId) {
        KbDocument completedDocument = getById(documentId);
        if (completedDocument == null) {
            return;
        }
        completedDocument.setStage(KbDocumentStageEnum.COMPLETED.getCode());
        completedDocument.setLastError(null);
        completedDocument.setUpdateBy(SYSTEM_UPDATER);
        completedDocument.setUpdatedAt(new Date());
        boolean updated = updateById(completedDocument);
        if (!updated) {
            log.warn("更新知识库文档完成状态失败: document_id={}", documentId);
        }
    }

    /**
     * 将指定文档标记为失败阶段。
     *
     * @param documentId   文档ID
     * @param errorMessage 错误信息
     * @param username     操作人账号
     */
    private void markDocumentFailed(Long documentId, String errorMessage, String username) {
        KbDocument failedDocument = getById(documentId);
        if (failedDocument == null) {
            return;
        }
        failedDocument.setStage(KbDocumentStageEnum.FAILED.getCode());
        failedDocument.setLastError(StringUtils.hasText(errorMessage) ? errorMessage : "unknown error");
        failedDocument.setUpdateBy(username);
        failedDocument.setUpdatedAt(new Date());
        boolean updated = updateById(failedDocument);
        if (!updated) {
            log.warn("更新知识库文档失败状态失败: document_id={}", documentId);
        }
    }

    /**
     * 提取异常中的可读错误信息。
     *
     * @param ex 异常对象
     * @return 异常消息；当异常或消息为空时返回 unknown error
     */
    private String extractErrorMessage(Exception ex) {
        if (ex == null || !StringUtils.hasText(ex.getMessage())) {
            return "unknown error";
        }
        return ex.getMessage();
    }

    /**
     * 归一化后的切片设置。
     *
     * @param chunkMode    切片模式
     * @param chunkSize    切片长度
     * @param chunkOverlap 切片重叠长度
     */
    private record NormalizedChunkSettings(String chunkMode, Integer chunkSize, Integer chunkOverlap) {
    }

}
