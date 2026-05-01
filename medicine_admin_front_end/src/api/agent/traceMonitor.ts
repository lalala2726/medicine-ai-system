import { requestClient } from '@/utils/request';

export namespace AgentTraceMonitorTypes {
  /** Agent Trace 监控查询参数。 */
  export interface MonitorRequest {
    /** 统计开始时间。 */
    startTime?: string;
    /** 统计结束时间。 */
    endTime?: string;
    /** 会话类型：admin/client。 */
    conversationType?: string;
    /** 模型供应商。 */
    provider?: string;
    /** 真实模型名称。 */
    modelName?: string;
    /** 模型槽位。 */
    slot?: string;
    /** 调用状态。 */
    status?: string;
    /** 时间桶分钟数。 */
    bucketMinutes?: number;
  }

  /** Agent Trace 监控概览。 */
  export interface SummaryVo {
    /** 模型调用总次数。 */
    callCount?: number;
    /** 成功次数。 */
    successCount?: number;
    /** 失败次数。 */
    errorCount?: number;
    /** 成功率百分比。 */
    successRate?: number;
    /** 失败率百分比。 */
    errorRate?: number;
    /** 输入 Token 数。 */
    inputTokens?: number;
    /** 输出 Token 数。 */
    outputTokens?: number;
    /** 总 Token 数。 */
    totalTokens?: number;
    /** 缓存命中 Token 数。 */
    cacheReadTokens?: number;
    /** 缓存创建 Token 数。 */
    cacheWriteTokens?: number;
    /** 缓存总 Token 数。 */
    cacheTotalTokens?: number;
    /** 平均耗时毫秒。 */
    avgDurationMs?: number;
    /** 最大耗时毫秒。 */
    maxDurationMs?: number;
  }

  /** Agent Trace 监控趋势时间点。 */
  export interface TimelinePointVo {
    /** 时间桶开始时间。 */
    bucketStart?: string;
    /** 时间桶结束时间。 */
    bucketEnd?: string;
    /** 模型调用总次数。 */
    callCount?: number;
    /** 成功次数。 */
    successCount?: number;
    /** 失败次数。 */
    errorCount?: number;
    /** 输入 Token 数。 */
    inputTokens?: number;
    /** 输出 Token 数。 */
    outputTokens?: number;
    /** 总 Token 数。 */
    totalTokens?: number;
    /** 缓存命中 Token 数。 */
    cacheReadTokens?: number;
    /** 缓存创建 Token 数。 */
    cacheWriteTokens?: number;
    /** 缓存总 Token 数。 */
    cacheTotalTokens?: number;
    /** 平均耗时毫秒。 */
    avgDurationMs?: number;
    /** 最大耗时毫秒。 */
    maxDurationMs?: number;
  }

  /** Agent Trace 调用次数趋势时间点。 */
  export interface CallTrendPointVo {
    /** 时间桶开始时间。 */
    bucketStart?: string;
    /** 时间桶结束时间。 */
    bucketEnd?: string;
    /** 模型调用总次数。 */
    callCount?: number;
    /** 成功次数。 */
    successCount?: number;
    /** 失败次数。 */
    errorCount?: number;
  }

  /** Agent Trace 耗时趋势时间点。 */
  export interface DurationTrendPointVo {
    /** 时间桶开始时间。 */
    bucketStart?: string;
    /** 时间桶结束时间。 */
    bucketEnd?: string;
    /** 平均耗时毫秒。 */
    avgDurationMs?: number;
    /** 最大耗时毫秒。 */
    maxDurationMs?: number;
  }

  /** Agent Trace Token 与缓存图表时间点。 */
  export interface TokenCachePointVo {
    /** 时间桶开始时间。 */
    bucketStart?: string;
    /** 时间桶结束时间。 */
    bucketEnd?: string;
    /** 输入 Token 数。 */
    inputTokens?: number;
    /** 输出 Token 数。 */
    outputTokens?: number;
    /** 总 Token 数。 */
    totalTokens?: number;
    /** 缓存命中 Token 数。 */
    cacheReadTokens?: number;
    /** 缓存创建 Token 数。 */
    cacheWriteTokens?: number;
    /** 缓存总 Token 数。 */
    cacheTotalTokens?: number;
  }

  /** Agent Trace 监控图表数据。 */
  export interface ChartsVo {
    /** 调用次数趋势图表数据。 */
    callTrend?: CallTrendPointVo[];
    /** 耗时趋势图表数据。 */
    durationTrend?: DurationTrendPointVo[];
    /** Token 与缓存图表数据。 */
    tokenCacheTrend?: TokenCachePointVo[];
  }

  /** Agent Trace 模型排行。 */
  export interface ModelRankingVo extends SummaryVo {
    /** 模型供应商。 */
    provider?: string;
    /** 真实模型名称。 */
    modelName?: string;
  }

  /** Agent Trace 单模型详情。 */
  export interface ModelDetailVo {
    /** 模型供应商。 */
    provider?: string;
    /** 真实模型名称。 */
    modelName?: string;
    /** 模型汇总指标。 */
    summary?: SummaryVo;
    /** 模型趋势数据。 */
    timeline?: TimelinePointVo[];
  }
}

/**
 * 查询 Agent Trace 监控概览。
 * @param params 监控查询参数。
 * @returns 监控概览指标。
 */
export async function getAgentTraceMonitorSummary(params?: AgentTraceMonitorTypes.MonitorRequest) {
  return requestClient.get<AgentTraceMonitorTypes.SummaryVo>('/agent/trace/monitor/summary', {
    params,
  });
}

/**
 * 查询 Agent Trace 监控趋势。
 * @param params 监控查询参数。
 * @returns 监控趋势时间点列表。
 */
export async function getAgentTraceMonitorTimeline(params?: AgentTraceMonitorTypes.MonitorRequest) {
  return requestClient.get<AgentTraceMonitorTypes.TimelinePointVo[]>(
    '/agent/trace/monitor/timeline',
    { params },
  );
}

/**
 * 查询 Agent Trace 监控图表数据。
 * @param params 监控查询参数。
 * @returns 监控图表数据。
 */
export async function getAgentTraceMonitorCharts(params?: AgentTraceMonitorTypes.MonitorRequest) {
  return requestClient.get<AgentTraceMonitorTypes.ChartsVo>('/agent/trace/monitor/charts', {
    params,
  });
}

/**
 * 查询 Agent Trace 模型排行。
 * @param params 监控查询参数。
 * @returns 模型排行列表。
 */
export async function getAgentTraceMonitorModelRanking(
  params?: AgentTraceMonitorTypes.MonitorRequest,
) {
  return requestClient.get<AgentTraceMonitorTypes.ModelRankingVo[]>(
    '/agent/trace/monitor/model-ranking',
    { params },
  );
}

/**
 * 查询 Agent Trace 单模型监控详情。
 * @param params 监控查询参数。
 * @returns 单模型监控详情。
 */
export async function getAgentTraceMonitorModelDetail(
  params?: AgentTraceMonitorTypes.MonitorRequest,
) {
  return requestClient.get<AgentTraceMonitorTypes.ModelDetailVo>(
    '/agent/trace/monitor/model-detail',
    { params },
  );
}
