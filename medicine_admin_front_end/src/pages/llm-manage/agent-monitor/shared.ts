import dayjs, { type Dayjs } from 'dayjs';
import type { AgentTraceMonitorTypes } from '@/api/agent/traceMonitor';

/** 时间格式化模板。 */
export const DATE_TIME_FORMAT = 'YYYY-MM-DD HH:mm:ss';

/** 默认监控时间范围分钟数。 */
export const DEFAULT_AGENT_MONITOR_RANGE_MINUTES = 1440;

/** 监控时间范围下拉选项。 */
export const AGENT_MONITOR_RANGE_OPTIONS = [
  { label: '最近 5 分钟', value: 5 },
  { label: '最近 10 分钟', value: 10 },
  { label: '最近 30 分钟', value: 30 },
  { label: '最近 1 小时', value: 60 },
  { label: '最近 3 小时', value: 180 },
  { label: '最近 6 小时', value: 360 },
  { label: '最近 24 小时', value: 1440 },
  { label: '最近 72 小时', value: 4320 },
  { label: '最近 7 天', value: 10080 },
];

/** 筛选表单状态。 */
export interface AgentMonitorFilterState {
  /** 时间范围。 */
  timeRange: [Dayjs, Dayjs];
  /** 快捷时间范围分钟数。 */
  rangeMinutes: number;
  /** 会话类型。 */
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
  bucketMinutes: number;
}

/**
 * 创建监控页默认筛选条件。
 * @returns 默认筛选条件。
 */
export function createDefaultAgentMonitorFilterState(): AgentMonitorFilterState {
  const endTime = dayjs();
  return {
    timeRange: [endTime.subtract(DEFAULT_AGENT_MONITOR_RANGE_MINUTES, 'minute'), endTime],
    rangeMinutes: DEFAULT_AGENT_MONITOR_RANGE_MINUTES,
    bucketMinutes: resolveBucketMinutes(DEFAULT_AGENT_MONITOR_RANGE_MINUTES),
  };
}

/**
 * 格式化数字。
 * @param value 原始数字。
 * @returns 千分位数字文本。
 */
export function formatNumber(value?: number | string): string {
  const numberValue = Number(value || 0);
  return Number.isFinite(numberValue) ? numberValue.toLocaleString() : '0';
}

/**
 * 格式化百分比。
 * @param value 原始百分比。
 * @returns 百分比文本。
 */
export function formatRate(value?: number): string {
  return `${Number(value || 0).toFixed(2)}%`;
}

/**
 * 格式化耗时。
 * @param value 耗时毫秒。
 * @returns 耗时文本。
 */
export function formatDuration(value?: number): string {
  const durationMs = Number(value || 0);
  if (durationMs >= 1000) {
    return `${(durationMs / 1000).toFixed(2)} s`;
  }
  return `${Math.round(durationMs)} ms`;
}

/**
 * 根据快捷时间范围推荐时间桶。
 * @param minutes 快捷时间范围分钟数。
 * @returns 时间桶分钟数。
 */
export function resolveBucketMinutes(minutes: number): number {
  if (minutes <= 30) return 5;
  if (minutes <= 180) return 10;
  if (minutes <= 360) return 30;
  if (minutes <= 1440) return 60;
  if (minutes <= 4320) return 180;
  return 360;
}

/**
 * 根据下拉时间范围创建下一次筛选条件。
 * @param previousState 原筛选条件。
 * @param minutes 时间范围分钟数。
 * @returns 下一次筛选条件。
 */
export function buildRangeFilterState(
  previousState: AgentMonitorFilterState,
  minutes: number,
): AgentMonitorFilterState {
  const endTime = dayjs();
  return {
    ...previousState,
    timeRange: [endTime.subtract(minutes, 'minute'), endTime],
    rangeMinutes: minutes,
    bucketMinutes: resolveBucketMinutes(minutes),
  };
}

/**
 * 从查询字符串读取文本。
 * @param searchParams 查询字符串对象。
 * @param key 查询字段名。
 * @returns 查询字段文本；空文本返回 undefined。
 */
export function readQueryText(searchParams: URLSearchParams, key: string): string | undefined {
  const value = searchParams.get(key);
  return value && value.trim() ? value.trim() : undefined;
}

/**
 * 转换筛选参数为接口请求参数。
 * @param filterState 筛选表单状态。
 * @returns 监控接口请求参数。
 */
export function buildMonitorRequest(
  filterState: AgentMonitorFilterState,
): AgentTraceMonitorTypes.MonitorRequest {
  const [startTime, endTime] = filterState.timeRange;
  return {
    startTime: startTime.format(DATE_TIME_FORMAT),
    endTime: endTime.format(DATE_TIME_FORMAT),
    conversationType: filterState.conversationType,
    provider: filterState.provider?.trim() || undefined,
    modelName: filterState.modelName?.trim() || undefined,
    slot: filterState.slot?.trim() || undefined,
    status: filterState.status,
    bucketMinutes: filterState.bucketMinutes,
  };
}

/**
 * 从页面查询字符串还原监控筛选条件。
 * @param search 页面查询字符串。
 * @returns 监控筛选条件。
 */
export function buildFilterStateFromSearch(search: string): AgentMonitorFilterState {
  const searchParams = new URLSearchParams(search);
  const startTimeText = readQueryText(searchParams, 'startTime');
  const endTimeText = readQueryText(searchParams, 'endTime');
  const startTime = startTimeText ? dayjs(startTimeText, DATE_TIME_FORMAT) : undefined;
  const endTime = endTimeText ? dayjs(endTimeText, DATE_TIME_FORMAT) : undefined;
  const bucketMinutesText = readQueryText(searchParams, 'bucketMinutes');
  const rangeMinutesText = readQueryText(searchParams, 'rangeMinutes');
  const defaultState = createDefaultAgentMonitorFilterState();
  const rangeMinutes = rangeMinutesText
    ? Number(rangeMinutesText)
    : DEFAULT_AGENT_MONITOR_RANGE_MINUTES;
  return {
    timeRange:
      startTime?.isValid() && endTime?.isValid() ? [startTime, endTime] : defaultState.timeRange,
    rangeMinutes: Number.isFinite(rangeMinutes) ? rangeMinutes : defaultState.rangeMinutes,
    conversationType: readQueryText(searchParams, 'conversationType'),
    provider: readQueryText(searchParams, 'provider'),
    modelName: readQueryText(searchParams, 'modelName'),
    slot: readQueryText(searchParams, 'slot'),
    status: readQueryText(searchParams, 'status'),
    bucketMinutes: bucketMinutesText ? Number(bucketMinutesText) : defaultState.bucketMinutes,
  };
}

/**
 * 从查询字符串构建监控请求。
 * @param search 页面查询字符串。
 * @returns 监控接口请求参数。
 */
export function buildMonitorRequestFromSearch(
  search: string,
): AgentTraceMonitorTypes.MonitorRequest {
  return buildMonitorRequest(buildFilterStateFromSearch(search));
}
