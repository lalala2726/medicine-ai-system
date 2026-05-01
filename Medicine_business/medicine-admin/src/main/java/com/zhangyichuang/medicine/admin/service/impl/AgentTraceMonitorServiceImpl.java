package com.zhangyichuang.medicine.admin.service.impl;

import com.zhangyichuang.medicine.admin.model.request.AgentTraceMonitorRequest;
import com.zhangyichuang.medicine.admin.model.vo.*;
import com.zhangyichuang.medicine.admin.repository.AgentTraceMongoRepository;
import com.zhangyichuang.medicine.admin.service.AgentTraceMonitorService;
import com.zhangyichuang.medicine.common.core.enums.ResponseCode;
import com.zhangyichuang.medicine.common.core.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Agent Trace 监控服务实现。
 */
@Service
@RequiredArgsConstructor
public class AgentTraceMonitorServiceImpl implements AgentTraceMonitorService {

    /**
     * 成功状态值。
     */
    private static final String STATUS_SUCCESS = "success";

    /**
     * 默认监控查询时间范围小时数。
     */
    private static final int DEFAULT_MONITOR_RANGE_HOURS = 24;

    /**
     * 默认监控趋势时间桶分钟数。
     */
    private static final int DEFAULT_MONITOR_BUCKET_MINUTES = 60;

    /**
     * 每分钟毫秒数。
     */
    private static final long MILLIS_PER_MINUTE = 60_000L;

    /**
     * 百分比小数位数。
     */
    private static final int RATE_SCALE = 2;

    /**
     * 模型排行分组键分隔符。
     */
    private static final String MODEL_RANKING_KEY_SEPARATOR = "\u0000";

    /**
     * Agent Trace Mongo 仓储。
     */
    private final AgentTraceMongoRepository agentTraceMongoRepository;

    /**
     * 查询 Agent Trace 监控概览。
     *
     * @param request 监控查询参数。
     * @return 监控概览指标。
     */
    @Override
    public AgentTraceMonitorSummaryVo getMonitorSummary(AgentTraceMonitorRequest request) {
        AgentTraceMonitorRequest normalizedRequest = normalizeMonitorRequest(request);
        List<Document> usageDocuments = agentTraceMongoRepository.listModelTokenUsage(normalizedRequest);
        return buildMonitorAccumulator(usageDocuments).toSummaryVo();
    }

    /**
     * 查询 Agent Trace 监控趋势。
     *
     * @param request 监控查询参数。
     * @return 趋势时间点列表。
     */
    @Override
    public List<AgentTraceMonitorTimelinePointVo> getMonitorTimeline(AgentTraceMonitorRequest request) {
        AgentTraceMonitorRequest normalizedRequest = normalizeMonitorRequest(request);
        List<Document> usageDocuments = agentTraceMongoRepository.listModelTokenUsage(normalizedRequest);
        return buildMonitorTimeline(usageDocuments, normalizedRequest.getBucketMinutes());
    }

    /**
     * 查询 Agent Trace 监控图表数据。
     *
     * @param request 监控查询参数。
     * @return 监控图表数据。
     */
    @Override
    public AgentTraceMonitorChartsVo getMonitorCharts(AgentTraceMonitorRequest request) {
        AgentTraceMonitorRequest normalizedRequest = normalizeMonitorRequest(request);
        List<Document> usageDocuments = agentTraceMongoRepository.listModelTokenUsage(normalizedRequest);
        List<AgentTraceMonitorTimelinePointVo> timelinePoints = buildMonitorTimeline(
                usageDocuments,
                normalizedRequest.getBucketMinutes()
        );
        return buildMonitorCharts(timelinePoints);
    }

    /**
     * 查询 Agent Trace 模型排行。
     *
     * @param request 监控查询参数。
     * @return 模型排行列表。
     */
    @Override
    public List<AgentTraceMonitorModelRankingVo> getMonitorModelRanking(AgentTraceMonitorRequest request) {
        AgentTraceMonitorRequest normalizedRequest = normalizeMonitorRequest(request);
        List<Document> usageDocuments = agentTraceMongoRepository.listModelTokenUsage(normalizedRequest);
        Map<String, MonitorAccumulator> accumulatorByModel = new LinkedHashMap<>();
        for (Document usageDocument : usageDocuments) {
            String provider = readDocumentString(usageDocument, "provider");
            String modelName = readDocumentString(usageDocument, "model_name");
            String key = (provider == null ? "" : provider) + MODEL_RANKING_KEY_SEPARATOR
                    + (modelName == null ? "" : modelName);
            MonitorAccumulator accumulator = accumulatorByModel.computeIfAbsent(
                    key,
                    ignored -> new MonitorAccumulator(provider, modelName)
            );
            accumulator.add(usageDocument);
        }
        return accumulatorByModel.values()
                .stream()
                .map(MonitorAccumulator::toModelRankingVo)
                .sorted(this::compareModelRanking)
                .toList();
    }

    /**
     * 查询 Agent Trace 单模型详情。
     *
     * @param request 监控查询参数。
     * @return 单模型监控详情。
     */
    @Override
    public AgentTraceMonitorModelDetailVo getMonitorModelDetail(AgentTraceMonitorRequest request) {
        AgentTraceMonitorRequest normalizedRequest = normalizeMonitorRequest(request);
        if (!StringUtils.hasText(normalizedRequest.getModelName())) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "模型名称不能为空");
        }
        List<Document> usageDocuments = agentTraceMongoRepository.listModelTokenUsage(normalizedRequest);
        AgentTraceMonitorModelDetailVo detailVo = new AgentTraceMonitorModelDetailVo();
        detailVo.setProvider(normalizedRequest.getProvider());
        detailVo.setModelName(normalizedRequest.getModelName());
        detailVo.setSummary(buildMonitorAccumulator(usageDocuments).toSummaryVo());
        detailVo.setTimeline(buildMonitorTimeline(usageDocuments, normalizedRequest.getBucketMinutes()));
        return detailVo;
    }

    /**
     * 标准化监控查询参数。
     *
     * @param request 原始监控查询参数。
     * @return 标准化后的监控查询参数。
     */
    private AgentTraceMonitorRequest normalizeMonitorRequest(AgentTraceMonitorRequest request) {
        AgentTraceMonitorRequest sourceRequest = request == null ? new AgentTraceMonitorRequest() : request;
        Date endTime = sourceRequest.getEndTime() == null ? new Date() : sourceRequest.getEndTime();
        Date startTime = sourceRequest.getStartTime() == null
                ? new Date(endTime.getTime() - DEFAULT_MONITOR_RANGE_HOURS * 60L * MILLIS_PER_MINUTE)
                : sourceRequest.getStartTime();
        if (startTime.after(endTime)) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "开始时间不能晚于结束时间");
        }
        AgentTraceMonitorRequest normalizedRequest = new AgentTraceMonitorRequest();
        normalizedRequest.setStartTime(startTime);
        normalizedRequest.setEndTime(endTime);
        normalizedRequest.setConversationType(normalizeText(sourceRequest.getConversationType()));
        normalizedRequest.setProvider(normalizeText(sourceRequest.getProvider()));
        normalizedRequest.setModelName(normalizeText(sourceRequest.getModelName()));
        normalizedRequest.setSlot(normalizeText(sourceRequest.getSlot()));
        normalizedRequest.setStatus(normalizeText(sourceRequest.getStatus()));
        normalizedRequest.setBucketMinutes(resolveBucketMinutes(sourceRequest.getBucketMinutes()));
        return normalizedRequest;
    }

    /**
     * 标准化文本查询值。
     *
     * @param value 原始文本值。
     * @return 去除首尾空白后的文本；空文本返回 null。
     */
    private String normalizeText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    /**
     * 解析时间桶分钟数。
     *
     * @param bucketMinutes 原始时间桶分钟数。
     * @return 有效时间桶分钟数。
     */
    private Integer resolveBucketMinutes(Integer bucketMinutes) {
        if (bucketMinutes == null || bucketMinutes <= 0) {
            return DEFAULT_MONITOR_BUCKET_MINUTES;
        }
        return bucketMinutes;
    }

    /**
     * 构建监控聚合器。
     *
     * @param usageDocuments Token 用量明细文档列表。
     * @return 监控聚合器。
     */
    private MonitorAccumulator buildMonitorAccumulator(List<Document> usageDocuments) {
        MonitorAccumulator accumulator = new MonitorAccumulator(null, null);
        if (usageDocuments == null || usageDocuments.isEmpty()) {
            return accumulator;
        }
        for (Document usageDocument : usageDocuments) {
            accumulator.add(usageDocument);
        }
        return accumulator;
    }

    /**
     * 构建监控趋势数据。
     *
     * @param usageDocuments Token 用量明细文档列表。
     * @param bucketMinutes  时间桶分钟数。
     * @return 趋势时间点列表。
     */
    private List<AgentTraceMonitorTimelinePointVo> buildMonitorTimeline(
            List<Document> usageDocuments,
            Integer bucketMinutes
    ) {
        if (usageDocuments == null || usageDocuments.isEmpty()) {
            return List.of();
        }
        long bucketMillis = resolveBucketMinutes(bucketMinutes) * MILLIS_PER_MINUTE;
        Map<Long, MonitorAccumulator> accumulatorByBucket = new TreeMap<>();
        for (Document usageDocument : usageDocuments) {
            Date startedAt = usageDocument.getDate("started_at");
            if (startedAt == null) {
                continue;
            }
            long bucketStartMillis = Math.floorDiv(startedAt.getTime(), bucketMillis) * bucketMillis;
            MonitorAccumulator accumulator = accumulatorByBucket.computeIfAbsent(
                    bucketStartMillis,
                    ignored -> new MonitorAccumulator(null, null)
            );
            accumulator.add(usageDocument);
        }
        List<AgentTraceMonitorTimelinePointVo> timelinePoints = new ArrayList<>();
        for (Map.Entry<Long, MonitorAccumulator> entry : accumulatorByBucket.entrySet()) {
            timelinePoints.add(entry.getValue().toTimelinePointVo(entry.getKey(), bucketMillis));
        }
        return timelinePoints;
    }

    /**
     * 构建监控图表数据。
     *
     * @param timelinePoints 通用趋势时间点列表。
     * @return 监控图表视图对象。
     */
    private AgentTraceMonitorChartsVo buildMonitorCharts(List<AgentTraceMonitorTimelinePointVo> timelinePoints) {
        AgentTraceMonitorChartsVo chartsVo = new AgentTraceMonitorChartsVo();
        if (timelinePoints == null || timelinePoints.isEmpty()) {
            chartsVo.setCallTrend(List.of());
            chartsVo.setDurationTrend(List.of());
            chartsVo.setTokenCacheTrend(List.of());
            return chartsVo;
        }
        chartsVo.setCallTrend(timelinePoints.stream().map(this::toCallTrendPointVo).toList());
        chartsVo.setDurationTrend(timelinePoints.stream().map(this::toDurationTrendPointVo).toList());
        chartsVo.setTokenCacheTrend(timelinePoints.stream().map(this::toTokenCachePointVo).toList());
        return chartsVo;
    }

    /**
     * 转换调用次数趋势时间点。
     *
     * @param source 通用趋势时间点。
     * @return 调用次数趋势时间点。
     */
    private AgentTraceMonitorCallTrendPointVo toCallTrendPointVo(AgentTraceMonitorTimelinePointVo source) {
        AgentTraceMonitorCallTrendPointVo vo = new AgentTraceMonitorCallTrendPointVo();
        vo.setBucketStart(source.getBucketStart());
        vo.setBucketEnd(source.getBucketEnd());
        vo.setCallCount(source.getCallCount());
        vo.setSuccessCount(source.getSuccessCount());
        vo.setErrorCount(source.getErrorCount());
        return vo;
    }

    /**
     * 转换耗时趋势时间点。
     *
     * @param source 通用趋势时间点。
     * @return 耗时趋势时间点。
     */
    private AgentTraceMonitorDurationTrendPointVo toDurationTrendPointVo(AgentTraceMonitorTimelinePointVo source) {
        AgentTraceMonitorDurationTrendPointVo vo = new AgentTraceMonitorDurationTrendPointVo();
        vo.setBucketStart(source.getBucketStart());
        vo.setBucketEnd(source.getBucketEnd());
        vo.setAvgDurationMs(source.getAvgDurationMs());
        vo.setMaxDurationMs(source.getMaxDurationMs());
        return vo;
    }

    /**
     * 转换 Token 与缓存图表时间点。
     *
     * @param source 通用趋势时间点。
     * @return Token 与缓存图表时间点。
     */
    private AgentTraceMonitorTokenCachePointVo toTokenCachePointVo(AgentTraceMonitorTimelinePointVo source) {
        AgentTraceMonitorTokenCachePointVo vo = new AgentTraceMonitorTokenCachePointVo();
        vo.setBucketStart(source.getBucketStart());
        vo.setBucketEnd(source.getBucketEnd());
        vo.setInputTokens(source.getInputTokens());
        vo.setOutputTokens(source.getOutputTokens());
        vo.setTotalTokens(source.getTotalTokens());
        vo.setCacheReadTokens(source.getCacheReadTokens());
        vo.setCacheWriteTokens(source.getCacheWriteTokens());
        vo.setCacheTotalTokens(source.getCacheTotalTokens());
        return vo;
    }

    /**
     * 比较模型排行顺序。
     *
     * @param left  左侧模型排行。
     * @param right 右侧模型排行。
     * @return 排序比较结果。
     */
    private int compareModelRanking(AgentTraceMonitorModelRankingVo left, AgentTraceMonitorModelRankingVo right) {
        int totalTokenCompare = Long.compare(
                nullToZero(right.getTotalTokens()),
                nullToZero(left.getTotalTokens())
        );
        if (totalTokenCompare != 0) {
            return totalTokenCompare;
        }
        int callCountCompare = Long.compare(
                nullToZero(right.getCallCount()),
                nullToZero(left.getCallCount())
        );
        if (callCountCompare != 0) {
            return callCountCompare;
        }
        String leftModelName = left.getModelName() == null ? "" : left.getModelName();
        String rightModelName = right.getModelName() == null ? "" : right.getModelName();
        return leftModelName.compareTo(rightModelName);
    }

    /**
     * 将空 Long 转为 0。
     *
     * @param value 原始 Long 值。
     * @return 非空 Long 值。
     */
    private long nullToZero(Long value) {
        return value == null ? 0L : value;
    }

    /**
     * 计算百分比。
     *
     * @param numerator   分子。
     * @param denominator 分母。
     * @return 百分比；分母为 0 时返回 0。
     */
    private BigDecimal calculateRate(long numerator, long denominator) {
        if (denominator <= 0) {
            return BigDecimal.ZERO.setScale(RATE_SCALE, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), RATE_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 从 Document 中读取字符串字段。
     *
     * @param document Mongo 文档。
     * @param key      字段名。
     * @return 字符串字段；不存在时返回 null。
     */
    private String readDocumentString(Document document, String key) {
        if (document == null || !StringUtils.hasText(key)) {
            return null;
        }
        Object value = document.get(key);
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    /**
     * 将 Mongo 数值字段转换成 Long。
     *
     * @param value 原始字段值。
     * @return Long 值；字段为空时返回 null。
     */
    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }

    /**
     * Agent 模型 Token 用量监控聚合器。
     */
    private class MonitorAccumulator {

        /**
         * 模型供应商。
         */
        private final String provider;

        /**
         * 真实模型名称。
         */
        private final String modelName;

        /**
         * 调用次数。
         */
        private long callCount;

        /**
         * 成功次数。
         */
        private long successCount;

        /**
         * 失败次数。
         */
        private long errorCount;

        /**
         * 输入 Token 数。
         */
        private long inputTokens;

        /**
         * 输出 Token 数。
         */
        private long outputTokens;

        /**
         * 总 Token 数。
         */
        private long totalTokens;

        /**
         * 缓存命中 Token 数。
         */
        private long cacheReadTokens;

        /**
         * 缓存创建 Token 数。
         */
        private long cacheWriteTokens;

        /**
         * 缓存总 Token 数。
         */
        private long cacheTotalTokens;

        /**
         * 总耗时毫秒。
         */
        private long durationTotalMs;

        /**
         * 最大耗时毫秒。
         */
        private long maxDurationMs;

        /**
         * 构造监控聚合器。
         *
         * @param provider  模型供应商。
         * @param modelName 真实模型名称。
         */
        private MonitorAccumulator(String provider, String modelName) {
            this.provider = provider;
            this.modelName = modelName;
        }

        /**
         * 添加一条 Token 用量明细。
         *
         * @param usageDocument Token 用量明细 Mongo 文档。
         * @return 无返回值。
         */
        private void add(Document usageDocument) {
            if (usageDocument == null) {
                return;
            }
            callCount++;
            if (STATUS_SUCCESS.equals(usageDocument.getString("status"))) {
                successCount++;
            } else {
                errorCount++;
            }
            inputTokens += nullToZero(toLong(usageDocument.get("input_tokens")));
            outputTokens += nullToZero(toLong(usageDocument.get("output_tokens")));
            totalTokens += nullToZero(toLong(usageDocument.get("total_tokens")));
            cacheReadTokens += nullToZero(toLong(usageDocument.get("cache_read_tokens")));
            cacheWriteTokens += nullToZero(toLong(usageDocument.get("cache_write_tokens")));
            cacheTotalTokens += nullToZero(toLong(usageDocument.get("cache_total_tokens")));
            long durationMs = nullToZero(toLong(usageDocument.get("duration_ms")));
            durationTotalMs += durationMs;
            maxDurationMs = Math.max(maxDurationMs, durationMs);
        }

        /**
         * 转换为监控概览视图对象。
         *
         * @return 监控概览视图对象。
         */
        private AgentTraceMonitorSummaryVo toSummaryVo() {
            AgentTraceMonitorSummaryVo vo = new AgentTraceMonitorSummaryVo();
            fillSummaryFields(vo);
            return vo;
        }

        /**
         * 转换为趋势时间点视图对象。
         *
         * @param bucketStartMillis 时间桶开始时间毫秒。
         * @param bucketMillis      时间桶毫秒数。
         * @return 趋势时间点视图对象。
         */
        private AgentTraceMonitorTimelinePointVo toTimelinePointVo(long bucketStartMillis, long bucketMillis) {
            AgentTraceMonitorTimelinePointVo vo = new AgentTraceMonitorTimelinePointVo();
            vo.setBucketStart(new Date(bucketStartMillis));
            vo.setBucketEnd(new Date(bucketStartMillis + bucketMillis));
            vo.setCallCount(callCount);
            vo.setSuccessCount(successCount);
            vo.setErrorCount(errorCount);
            vo.setInputTokens(inputTokens);
            vo.setOutputTokens(outputTokens);
            vo.setTotalTokens(totalTokens);
            vo.setCacheReadTokens(cacheReadTokens);
            vo.setCacheWriteTokens(cacheWriteTokens);
            vo.setCacheTotalTokens(cacheTotalTokens);
            vo.setAvgDurationMs(callCount <= 0 ? 0L : durationTotalMs / callCount);
            vo.setMaxDurationMs(maxDurationMs);
            return vo;
        }

        /**
         * 转换为模型排行视图对象。
         *
         * @return 模型排行视图对象。
         */
        private AgentTraceMonitorModelRankingVo toModelRankingVo() {
            AgentTraceMonitorModelRankingVo vo = new AgentTraceMonitorModelRankingVo();
            vo.setProvider(provider);
            vo.setModelName(modelName);
            vo.setCallCount(callCount);
            vo.setSuccessCount(successCount);
            vo.setErrorCount(errorCount);
            vo.setSuccessRate(calculateRate(successCount, callCount));
            vo.setErrorRate(calculateRate(errorCount, callCount));
            vo.setInputTokens(inputTokens);
            vo.setOutputTokens(outputTokens);
            vo.setTotalTokens(totalTokens);
            vo.setCacheTotalTokens(cacheTotalTokens);
            vo.setAvgDurationMs(callCount <= 0 ? 0L : durationTotalMs / callCount);
            vo.setMaxDurationMs(maxDurationMs);
            return vo;
        }

        /**
         * 填充概览视图对象公共字段。
         *
         * @param vo 监控概览视图对象。
         * @return 无返回值。
         */
        private void fillSummaryFields(AgentTraceMonitorSummaryVo vo) {
            vo.setCallCount(callCount);
            vo.setSuccessCount(successCount);
            vo.setErrorCount(errorCount);
            vo.setSuccessRate(calculateRate(successCount, callCount));
            vo.setErrorRate(calculateRate(errorCount, callCount));
            vo.setInputTokens(inputTokens);
            vo.setOutputTokens(outputTokens);
            vo.setTotalTokens(totalTokens);
            vo.setCacheReadTokens(cacheReadTokens);
            vo.setCacheWriteTokens(cacheWriteTokens);
            vo.setCacheTotalTokens(cacheTotalTokens);
            vo.setAvgDurationMs(callCount <= 0 ? 0L : durationTotalMs / callCount);
            vo.setMaxDurationMs(maxDurationMs);
        }
    }
}
