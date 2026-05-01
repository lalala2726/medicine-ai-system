package com.zhangyichuang.medicine.admin.service.impl;

import com.zhangyichuang.medicine.admin.model.request.AgentTraceMonitorRequest;
import com.zhangyichuang.medicine.admin.model.vo.*;
import com.zhangyichuang.medicine.admin.repository.AgentTraceMongoRepository;
import com.zhangyichuang.medicine.common.core.exception.ServiceException;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Agent Trace 监控服务单元测试。
 */
@ExtendWith(MockitoExtension.class)
class AgentTraceMonitorServiceImplTests {

    /**
     * 测试用第一时间桶起点。
     */
    private static final Date BUCKET_ONE_START = Date.from(Instant.parse("2026-04-30T00:00:00Z"));

    /**
     * 测试用第一时间桶内第二条明细时间。
     */
    private static final Date BUCKET_ONE_SECOND_POINT = Date.from(Instant.parse("2026-04-30T00:20:00Z"));

    /**
     * 测试用第二时间桶起点。
     */
    private static final Date BUCKET_TWO_START = Date.from(Instant.parse("2026-04-30T01:00:00Z"));

    /**
     * 测试用查询结束时间。
     */
    private static final Date QUERY_END = Date.from(Instant.parse("2026-04-30T02:00:00Z"));

    /**
     * Agent Trace Mongo 仓储 mock。
     */
    @Mock
    private AgentTraceMongoRepository agentTraceMongoRepository;

    /**
     * 被测 Agent Trace 监控服务。
     */
    private AgentTraceMonitorServiceImpl agentTraceMonitorService;

    /**
     * 初始化被测服务。
     */
    @BeforeEach
    void setUp() {
        agentTraceMonitorService = new AgentTraceMonitorServiceImpl(agentTraceMongoRepository);
    }

    /**
     * 验证概览统计会汇总调用量、成功失败率、Token、缓存和耗时。
     */
    @Test
    void getMonitorSummary_ShouldAggregateCallTokenCacheAndDurationMetrics() {
        when(agentTraceMongoRepository.listModelTokenUsage(any())).thenReturn(sampleUsageDocuments());

        AgentTraceMonitorSummaryVo summaryVo = agentTraceMonitorService.getMonitorSummary(baseRequest());

        assertEquals(3L, summaryVo.getCallCount());
        assertEquals(2L, summaryVo.getSuccessCount());
        assertEquals(1L, summaryVo.getErrorCount());
        assertEquals(new BigDecimal("66.67"), summaryVo.getSuccessRate());
        assertEquals(new BigDecimal("33.33"), summaryVo.getErrorRate());
        assertEquals(600L, summaryVo.getInputTokens());
        assertEquals(60L, summaryVo.getOutputTokens());
        assertEquals(660L, summaryVo.getTotalTokens());
        assertEquals(80L, summaryVo.getCacheReadTokens());
        assertEquals(30L, summaryVo.getCacheWriteTokens());
        assertEquals(110L, summaryVo.getCacheTotalTokens());
        assertEquals(2000L, summaryVo.getAvgDurationMs());
        assertEquals(3000L, summaryVo.getMaxDurationMs());

        AgentTraceMonitorRequest normalizedRequest = captureRepositoryRequest();
        assertEquals(BUCKET_ONE_START, normalizedRequest.getStartTime());
        assertEquals(QUERY_END, normalizedRequest.getEndTime());
        assertEquals("client", normalizedRequest.getConversationType());
        assertEquals("aliyun", normalizedRequest.getProvider());
        assertEquals(60, normalizedRequest.getBucketMinutes());
    }

    /**
     * 验证图表接口会拆分调用次数、耗时、Token 与缓存三类趋势。
     */
    @Test
    void getMonitorCharts_ShouldSplitTimelineIntoCallDurationAndTokenCacheCharts() {
        when(agentTraceMongoRepository.listModelTokenUsage(any())).thenReturn(sampleUsageDocuments());

        AgentTraceMonitorChartsVo chartsVo = agentTraceMonitorService.getMonitorCharts(baseRequest());

        assertEquals(2, chartsVo.getCallTrend().size());
        assertEquals(2L, chartsVo.getCallTrend().get(0).getCallCount());
        assertEquals(1L, chartsVo.getCallTrend().get(0).getSuccessCount());
        assertEquals(1L, chartsVo.getCallTrend().get(0).getErrorCount());
        assertEquals(1L, chartsVo.getCallTrend().get(1).getCallCount());
        assertEquals(1500L, chartsVo.getDurationTrend().get(0).getAvgDurationMs());
        assertEquals(2000L, chartsVo.getDurationTrend().get(0).getMaxDurationMs());
        assertEquals(3000L, chartsVo.getDurationTrend().get(1).getAvgDurationMs());
        assertEquals(300L, chartsVo.getTokenCacheTrend().get(0).getInputTokens());
        assertEquals(30L, chartsVo.getTokenCacheTrend().get(0).getOutputTokens());
        assertEquals(550L, chartsVo.getTokenCacheTrend().get(0).getTotalTokens());
        assertEquals(80L, chartsVo.getTokenCacheTrend().get(0).getCacheReadTokens());
        assertEquals(10L, chartsVo.getTokenCacheTrend().get(0).getCacheWriteTokens());
        assertEquals(90L, chartsVo.getTokenCacheTrend().get(0).getCacheTotalTokens());
        assertEquals(20L, chartsVo.getTokenCacheTrend().get(1).getCacheTotalTokens());
    }

    /**
     * 验证通用趋势接口会按时间桶聚合并跳过无 started_at 明细。
     */
    @Test
    void getMonitorTimeline_ShouldBucketDocumentsByStartTime() {
        when(agentTraceMongoRepository.listModelTokenUsage(any())).thenReturn(sampleUsageDocumentsWithMissingStartedAt());

        List<AgentTraceMonitorTimelinePointVo> timeline = agentTraceMonitorService.getMonitorTimeline(baseRequest());

        assertEquals(2, timeline.size());
        assertEquals(BUCKET_ONE_START, timeline.get(0).getBucketStart());
        assertEquals(Date.from(Instant.parse("2026-04-30T01:00:00Z")), timeline.get(0).getBucketEnd());
        assertEquals(2L, timeline.get(0).getCallCount());
        assertEquals(1L, timeline.get(0).getSuccessCount());
        assertEquals(1L, timeline.get(0).getErrorCount());
        assertEquals(BUCKET_TWO_START, timeline.get(1).getBucketStart());
        assertEquals(1L, timeline.get(1).getCallCount());
    }

    /**
     * 验证模型排行按总 Token、调用量和模型名排序，并按真实模型名分组。
     */
    @Test
    void getMonitorModelRanking_ShouldGroupByProviderAndModelThenSortByTotalTokens() {
        when(agentTraceMongoRepository.listModelTokenUsage(any())).thenReturn(sampleUsageDocuments());

        List<AgentTraceMonitorModelRankingVo> rankingVos = agentTraceMonitorService.getMonitorModelRanking(baseRequest());

        assertEquals(2, rankingVos.size());
        assertEquals("qwen3.5-plus", rankingVos.get(0).getModelName());
        assertEquals(2L, rankingVos.get(0).getCallCount());
        assertEquals(1L, rankingVos.get(0).getSuccessCount());
        assertEquals(1L, rankingVos.get(0).getErrorCount());
        assertEquals(550L, rankingVos.get(0).getTotalTokens());
        assertEquals(90L, rankingVos.get(0).getCacheTotalTokens());
        assertEquals(1500L, rankingVos.get(0).getAvgDurationMs());
        assertEquals("qwen3.5-flash", rankingVos.get(1).getModelName());
        assertEquals(110L, rankingVos.get(1).getTotalTokens());
    }

    /**
     * 验证模型详情会要求模型名，并返回当前模型的概览和趋势。
     */
    @Test
    void getMonitorModelDetail_ShouldReturnSummaryAndTimelineForSpecifiedModel() {
        AgentTraceMonitorRequest request = baseRequest();
        request.setModelName(" qwen3.5-plus ");
        when(agentTraceMongoRepository.listModelTokenUsage(any())).thenReturn(sampleQwenPlusUsageDocuments());

        AgentTraceMonitorModelDetailVo detailVo = agentTraceMonitorService.getMonitorModelDetail(request);

        assertEquals("aliyun", detailVo.getProvider());
        assertEquals("qwen3.5-plus", detailVo.getModelName());
        assertNotNull(detailVo.getSummary());
        assertEquals(2L, detailVo.getSummary().getCallCount());
        assertEquals(550L, detailVo.getSummary().getTotalTokens());
        assertEquals(1, detailVo.getTimeline().size());
        assertEquals(2L, detailVo.getTimeline().get(0).getCallCount());

        AgentTraceMonitorRequest normalizedRequest = captureRepositoryRequest();
        assertEquals("qwen3.5-plus", normalizedRequest.getModelName());
    }

    /**
     * 验证模型详情缺少模型名时抛出业务异常。
     */
    @Test
    void getMonitorModelDetail_WhenModelNameMissing_ShouldThrowServiceException() {
        AgentTraceMonitorRequest request = baseRequest();
        request.setModelName(" ");

        assertThrows(ServiceException.class, () -> agentTraceMonitorService.getMonitorModelDetail(request));
    }

    /**
     * 验证开始时间晚于结束时间时抛出业务异常，避免查询条件错误。
     */
    @Test
    void getMonitorSummary_WhenStartTimeAfterEndTime_ShouldThrowServiceException() {
        AgentTraceMonitorRequest request = new AgentTraceMonitorRequest();
        request.setStartTime(QUERY_END);
        request.setEndTime(BUCKET_ONE_START);

        assertThrows(ServiceException.class, () -> agentTraceMonitorService.getMonitorSummary(request));
    }

    /**
     * 构造基础监控查询参数。
     *
     * @return 监控查询参数。
     */
    private AgentTraceMonitorRequest baseRequest() {
        AgentTraceMonitorRequest request = new AgentTraceMonitorRequest();
        request.setStartTime(BUCKET_ONE_START);
        request.setEndTime(QUERY_END);
        request.setConversationType(" client ");
        request.setProvider(" aliyun ");
        request.setBucketMinutes(60);
        return request;
    }

    /**
     * 捕获传入 Mongo 仓储的标准化查询参数。
     *
     * @return 标准化后的查询参数。
     */
    private AgentTraceMonitorRequest captureRepositoryRequest() {
        ArgumentCaptor<AgentTraceMonitorRequest> requestCaptor =
                ArgumentCaptor.forClass(AgentTraceMonitorRequest.class);
        verify(agentTraceMongoRepository).listModelTokenUsage(requestCaptor.capture());
        return requestCaptor.getValue();
    }

    /**
     * 构造包含多个模型、成功和失败状态的 Token 明细样本。
     *
     * @return Token 明细 Mongo 文档列表。
     */
    private List<Document> sampleUsageDocuments() {
        return List.of(
                usageDocument(
                        "aliyun",
                        "qwen3.5-plus",
                        "success",
                        BUCKET_ONE_START,
                        100,
                        10,
                        110,
                        80,
                        10,
                        90,
                        1000
                ),
                usageDocument(
                        "aliyun",
                        "qwen3.5-plus",
                        "error",
                        BUCKET_ONE_SECOND_POINT,
                        200,
                        20,
                        440,
                        0,
                        0,
                        0,
                        2000
                ),
                usageDocument(
                        "aliyun",
                        "qwen3.5-flash",
                        "success",
                        BUCKET_TWO_START,
                        300,
                        30,
                        110,
                        0,
                        20,
                        20,
                        3000
                )
        );
    }

    /**
     * 构造包含缺失 started_at 明细的趋势样本。
     *
     * @return Token 明细 Mongo 文档列表。
     */
    private List<Document> sampleUsageDocumentsWithMissingStartedAt() {
        return List.of(
                usageDocument(
                        "aliyun",
                        "qwen3.5-plus",
                        "success",
                        BUCKET_ONE_START,
                        100,
                        10,
                        110,
                        80,
                        10,
                        90,
                        1000
                ),
                usageDocument(
                        "aliyun",
                        "qwen3.5-plus",
                        "error",
                        BUCKET_ONE_SECOND_POINT,
                        200,
                        20,
                        440,
                        0,
                        0,
                        0,
                        2000
                ),
                usageDocument(
                        "aliyun",
                        "qwen3.5-flash",
                        "success",
                        BUCKET_TWO_START,
                        300,
                        30,
                        110,
                        0,
                        20,
                        20,
                        3000
                ),
                usageDocument(
                        "aliyun",
                        "qwen3.5-flash",
                        "success",
                        null,
                        999,
                        999,
                        999,
                        999,
                        999,
                        999,
                        999
                )
        );
    }

    /**
     * 构造单模型详情使用的 Token 明细样本。
     *
     * @return Token 明细 Mongo 文档列表。
     */
    private List<Document> sampleQwenPlusUsageDocuments() {
        return sampleUsageDocuments()
                .stream()
                .filter(document -> "qwen3.5-plus".equals(document.getString("model_name")))
                .toList();
    }

    /**
     * 构造一条模型 Token 用量 Mongo 文档。
     *
     * @param provider         模型供应商。
     * @param modelName        真实模型名。
     * @param status           调用状态。
     * @param startedAt        调用开始时间。
     * @param inputTokens      输入 Token。
     * @param outputTokens     输出 Token。
     * @param totalTokens      总 Token。
     * @param cacheReadTokens  缓存命中 Token。
     * @param cacheWriteTokens 缓存创建 Token。
     * @param cacheTotalTokens 缓存总 Token。
     * @param durationMs       调用耗时毫秒。
     * @return Mongo 文档。
     */
    private Document usageDocument(
            String provider,
            String modelName,
            String status,
            Date startedAt,
            long inputTokens,
            long outputTokens,
            long totalTokens,
            long cacheReadTokens,
            long cacheWriteTokens,
            long cacheTotalTokens,
            long durationMs
    ) {
        return new Document()
                .append("provider", provider)
                .append("model_name", modelName)
                .append("status", status)
                .append("started_at", startedAt)
                .append("input_tokens", inputTokens)
                .append("output_tokens", outputTokens)
                .append("total_tokens", totalTokens)
                .append("cache_read_tokens", cacheReadTokens)
                .append("cache_write_tokens", cacheWriteTokens)
                .append("cache_total_tokens", cacheTotalTokens)
                .append("duration_ms", durationMs);
    }
}
