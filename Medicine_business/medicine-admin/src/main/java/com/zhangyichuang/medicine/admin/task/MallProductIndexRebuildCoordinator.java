package com.zhangyichuang.medicine.admin.task;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.admin.config.ProductIndexRebuildProperties;
import com.zhangyichuang.medicine.admin.mapper.MallProductMapper;
import com.zhangyichuang.medicine.admin.model.vo.EsIndexConfigVo;
import com.zhangyichuang.medicine.common.redis.core.DistributedLockExecutor;
import com.zhangyichuang.medicine.model.dto.MallProductDetailDto;
import com.zhangyichuang.medicine.model.elasticsearch.document.MallProductDocument;
import com.zhangyichuang.medicine.model.request.MallProductListQueryRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 商品索引全量重建协调器。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MallProductIndexRebuildCoordinator {

    /**
     * 启动自动重建触发策略说明。
     */
    private static final String STARTUP_TRIGGER_POLICY = "索引缺失或为空时启动后台重建";

    /**
     * 启动触发来源标识。
     */
    private static final String TRIGGER_SOURCE_STARTUP = "startup";

    /**
     * 手动触发来源标识。
     */
    private static final String TRIGGER_SOURCE_MANUAL = "manual";

    /**
     * 商品索引重建分布式锁名称。
     */
    private static final String REBUILD_LOCK_NAME = "medicine:mall_product:index:rebuild";

    /**
     * 商品索引重建配置。
     */
    private final ProductIndexRebuildProperties productIndexRebuildProperties;

    /**
     * 商品 Mapper。
     */
    private final MallProductMapper mallProductMapper;

    /**
     * 商品索引任务。
     */
    private final MallProductSearchIndexer mallProductSearchIndexer;

    /**
     * 分布式锁执行器。
     */
    private final DistributedLockExecutor distributedLockExecutor;

    /**
     * Elasticsearch 操作对象。
     */
    private final ElasticsearchOperations elasticsearchOperations;

    /**
     * 当前 Bean 的代理提供器，用于触发异步方法。
     */
    private final ObjectProvider<MallProductIndexRebuildCoordinator> rebuildCoordinatorProvider;

    /**
     * 当前实例内是否正在执行商品索引重建。
     */
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * 当前已处理的商品数量。
     */
    private final AtomicLong processedCount = new AtomicLong();

    /**
     * 当前预计处理的商品总数量。
     */
    private final AtomicLong totalCount = new AtomicLong();

    /**
     * 当前已完成的批次数量。
     */
    private final AtomicLong batchCount = new AtomicLong();

    /**
     * 最近一次触发来源。
     */
    private volatile String triggerSource;

    /**
     * 最近一次开始时间。
     */
    private volatile LocalDateTime startedTime;

    /**
     * 最近一次完成时间。
     */
    private volatile LocalDateTime finishedTime;

    /**
     * 最近一次错误信息。
     */
    private volatile String lastError;

    /**
     * 读取 Elasticsearch 与商品索引概览。
     *
     * @return Elasticsearch 与商品索引概览
     */
    public EsIndexConfigVo getEsIndexConfig() {
        EsIndexSnapshot snapshot = loadEsIndexSnapshot();
        EsIndexConfigVo vo = new EsIndexConfigVo();
        vo.setEsAvailable(snapshot.esAvailable());
        vo.setIndexName(MallProductDocument.INDEX_NAME);
        vo.setIndexExists(snapshot.indexExists());
        vo.setDocumentCount(snapshot.documentCount());
        vo.setStartupAutoRebuildEnabled(productIndexRebuildProperties.isStartupEnabled());
        vo.setStartupTriggerPolicy(STARTUP_TRIGGER_POLICY);
        vo.setRebuildStatus(buildRebuildStatusVo());
        return vo;
    }

    /**
     * 手动触发商品索引全量重建。
     *
     * @return true 表示已成功提交异步任务
     */
    public boolean triggerManualRebuild() {
        return triggerRebuild(TRIGGER_SOURCE_MANUAL);
    }

    /**
     * 启动完成后检查是否需要自动触发商品索引重建。
     */
    public void triggerStartupRebuildIfNeeded() {
        if (!productIndexRebuildProperties.isStartupEnabled()) {
            return;
        }
        EsIndexSnapshot snapshot = loadEsIndexSnapshot();
        if (!snapshot.esAvailable()) {
            log.warn("跳过启动商品索引重建，Elasticsearch 当前不可用");
            return;
        }
        if (snapshot.indexExists() && snapshot.documentCount() > 0L) {
            log.info("跳过启动商品索引重建，商品索引已存在且文档数为 {}", snapshot.documentCount());
            return;
        }
        boolean started = triggerRebuild(TRIGGER_SOURCE_STARTUP);
        if (!started) {
            log.info("启动商品索引重建任务已在当前实例执行中，忽略重复触发");
        }
    }

    /**
     * 异步执行商品索引全量重建。
     *
     * @param currentTriggerSource 当前触发来源
     */
    @Async
    public void executeRebuildAsync(String currentTriggerSource) {
        try {
            boolean executed = distributedLockExecutor.tryExecuteOrElse(
                    REBUILD_LOCK_NAME,
                    productIndexRebuildProperties.getLockWaitMillis(),
                    productIndexRebuildProperties.getLockLeaseMillis(),
                    () -> {
                        doRebuild(currentTriggerSource);
                        return true;
                    },
                    () -> false
            );
            if (!executed) {
                updateLastError("商品索引重建任务已在其他实例执行中");
                log.info("跳过商品索引重建，其他实例正在执行任务");
            }
        } catch (Exception ex) {
            String errorMessage = resolveErrorMessage(ex);
            updateLastError(errorMessage);
            log.error("商品索引重建失败，triggerSource={}", currentTriggerSource, ex);
        } finally {
            finishedTime = LocalDateTime.now();
            running.set(false);
        }
    }

    /**
     * 统一触发商品索引全量重建。
     *
     * @param currentTriggerSource 当前触发来源
     * @return true 表示已成功提交异步任务
     */
    private boolean triggerRebuild(String currentTriggerSource) {
        if (!running.compareAndSet(false, true)) {
            return false;
        }
        resetRuntimeStatus(currentTriggerSource);
        rebuildCoordinatorProvider.getObject().executeRebuildAsync(currentTriggerSource);
        return true;
    }

    /**
     * 执行商品索引全量重建。
     *
     * @param currentTriggerSource 当前触发来源
     */
    private void doRebuild(String currentTriggerSource) {
        ensureIndexExists();
        int batchSize = resolveBatchSize();
        long currentPageNum = 1L;
        long totalPages = 0L;

        do {
            Page<MallProductDetailDto> page = listProductPage(currentPageNum, batchSize);
            if (currentPageNum == 1L) {
                totalCount.set(page.getTotal());
                totalPages = page.getPages();
                log.info("开始商品索引全量重建，triggerSource={}，totalCount={}，batchSize={}",
                        currentTriggerSource,
                        page.getTotal(),
                        batchSize);
            }
            if (CollectionUtils.isEmpty(page.getRecords())) {
                break;
            }
            mallProductSearchIndexer.reindexBatch(page.getRecords());
            processedCount.addAndGet(page.getRecords().size());
            batchCount.incrementAndGet();
            currentPageNum++;
        } while (currentPageNum <= totalPages);

        log.info("商品索引全量重建完成，triggerSource={}，processedCount={}，batchCount={}",
                currentTriggerSource,
                processedCount.get(),
                batchCount.get());
    }

    /**
     * 分页查询待重建的商品列表。
     *
     * @param pageNum   当前页码
     * @param batchSize 批次大小
     * @return 当前页商品列表
     */
    private Page<MallProductDetailDto> listProductPage(long pageNum, int batchSize) {
        MallProductListQueryRequest request = new MallProductListQueryRequest();
        request.setPageNum((int) pageNum);
        request.setPageSize(batchSize);
        return mallProductMapper.listMallProductWithCategory(request.toPage(), request);
    }

    /**
     * 构建商品索引重建状态视图对象。
     *
     * @return 商品索引重建状态视图对象
     */
    private EsIndexConfigVo.ProductIndexRebuildStatusVo buildRebuildStatusVo() {
        EsIndexConfigVo.ProductIndexRebuildStatusVo vo = new EsIndexConfigVo.ProductIndexRebuildStatusVo();
        vo.setRunning(running.get());
        vo.setTriggerSource(triggerSource);
        vo.setProcessedCount(processedCount.get());
        vo.setTotalCount(totalCount.get());
        vo.setBatchCount(batchCount.get());
        vo.setStartedTime(startedTime);
        vo.setFinishedTime(finishedTime);
        vo.setLastError(lastError);
        return vo;
    }

    /**
     * 读取 Elasticsearch 商品索引快照。
     *
     * @return Elasticsearch 商品索引快照
     */
    private EsIndexSnapshot loadEsIndexSnapshot() {
        try {
            boolean indexExists = elasticsearchOperations.indexOps(MallProductDocument.class).exists();
            if (!indexExists) {
                return new EsIndexSnapshot(true, false, 0L);
            }
            Query matchAllQuery = Query.of(queryBuilder -> queryBuilder.matchAll(matchAllQueryBuilder -> matchAllQueryBuilder));
            NativeQuery countQuery = NativeQuery.builder()
                    .withQuery(matchAllQuery)
                    .build();
            long documentCount = elasticsearchOperations.count(countQuery, MallProductDocument.class);
            return new EsIndexSnapshot(true, true, documentCount);
        } catch (Exception ex) {
            log.warn("读取商品索引快照失败: {}", ex.getMessage());
            return new EsIndexSnapshot(false, false, 0L);
        }
    }

    /**
     * 确保商品索引存在。
     */
    private void ensureIndexExists() {
        var indexOps = elasticsearchOperations.indexOps(MallProductDocument.class);
        if (indexOps.exists()) {
            return;
        }
        indexOps.create();
        indexOps.putMapping(indexOps.createMapping());
    }

    /**
     * 解析全量重建批次大小。
     *
     * @return 归一化后的批次大小
     */
    private int resolveBatchSize() {
        int configuredBatchSize = productIndexRebuildProperties.getBatchSize();
        return configuredBatchSize > 0 ? configuredBatchSize : 100;
    }

    /**
     * 重置本次运行时状态。
     *
     * @param currentTriggerSource 当前触发来源
     */
    private void resetRuntimeStatus(String currentTriggerSource) {
        triggerSource = currentTriggerSource;
        processedCount.set(0L);
        totalCount.set(0L);
        batchCount.set(0L);
        startedTime = LocalDateTime.now();
        finishedTime = null;
        lastError = null;
    }

    /**
     * 更新最近一次错误信息。
     *
     * @param errorMessage 错误信息
     */
    private void updateLastError(String errorMessage) {
        lastError = errorMessage;
    }

    /**
     * 解析异常信息。
     *
     * @param ex 异常对象
     * @return 归一化后的异常信息
     */
    private String resolveErrorMessage(Exception ex) {
        if (ex == null) {
            return "商品索引重建失败";
        }
        if (StringUtils.hasText(ex.getMessage())) {
            return ex.getMessage();
        }
        return ex.getClass().getSimpleName();
    }

    /**
     * Elasticsearch 商品索引快照。
     *
     * @param esAvailable   Elasticsearch 是否可用
     * @param indexExists   商品索引是否存在
     * @param documentCount 商品索引文档数量
     */
    private record EsIndexSnapshot(boolean esAvailable, boolean indexExists, long documentCount) {
    }
}
