import dayjs from 'dayjs';
import { buildAgentMonitorModelDetailPath } from '@/router/paths';
import {
  DEFAULT_AGENT_MONITOR_RANGE_MINUTES,
  buildFilterStateFromSearch,
  buildMonitorRequest,
  buildRangeFilterState,
  createDefaultAgentMonitorFilterState,
  formatDuration,
  formatNumber,
  formatRate,
  resolveBucketMinutes,
} from './shared';

/** 固定开始时间，用于验证请求时间格式。 */
const FIXED_START_TIME = dayjs('2026-04-30 08:00:00');

/** 固定结束时间，用于验证请求时间格式。 */
const FIXED_END_TIME = dayjs('2026-04-30 09:00:00');

describe('agent monitor shared helpers', () => {
  it('creates default filter state for the latest 24 hours', () => {
    const defaultState = createDefaultAgentMonitorFilterState();

    expect(defaultState.rangeMinutes).toBe(DEFAULT_AGENT_MONITOR_RANGE_MINUTES);
    expect(defaultState.bucketMinutes).toBe(60);
    expect(defaultState.timeRange[1].diff(defaultState.timeRange[0], 'minute')).toBe(
      DEFAULT_AGENT_MONITOR_RANGE_MINUTES,
    );
  });

  it('resolves bucket minutes from quick range minutes', () => {
    expect(resolveBucketMinutes(5)).toBe(5);
    expect(resolveBucketMinutes(180)).toBe(10);
    expect(resolveBucketMinutes(360)).toBe(30);
    expect(resolveBucketMinutes(1440)).toBe(60);
    expect(resolveBucketMinutes(4320)).toBe(180);
    expect(resolveBucketMinutes(10080)).toBe(360);
  });

  it('builds request params with trimmed model and slot values', () => {
    const request = buildMonitorRequest({
      timeRange: [FIXED_START_TIME, FIXED_END_TIME],
      rangeMinutes: 60,
      conversationType: 'client',
      provider: ' aliyun ',
      modelName: ' qwen3.5-plus ',
      slot: ' clientAssistant.serviceNodeModel ',
      status: 'success',
      bucketMinutes: 10,
    });

    expect(request).toEqual({
      startTime: '2026-04-30 08:00:00',
      endTime: '2026-04-30 09:00:00',
      conversationType: 'client',
      provider: 'aliyun',
      modelName: 'qwen3.5-plus',
      slot: 'clientAssistant.serviceNodeModel',
      status: 'success',
      bucketMinutes: 10,
    });
  });

  it('parses monitor filter state from search params', () => {
    const filterState = buildFilterStateFromSearch(
      '?startTime=2026-04-30%2008%3A00%3A00&endTime=2026-04-30%2009%3A00%3A00&rangeMinutes=60&bucketMinutes=10&conversationType=client&provider=aliyun&modelName=qwen3.5-plus&slot=clientSlot&status=success',
    );

    expect(filterState.rangeMinutes).toBe(60);
    expect(filterState.bucketMinutes).toBe(10);
    expect(filterState.conversationType).toBe('client');
    expect(filterState.provider).toBe('aliyun');
    expect(filterState.modelName).toBe('qwen3.5-plus');
    expect(filterState.slot).toBe('clientSlot');
    expect(filterState.status).toBe('success');
    expect(filterState.timeRange[0].format('YYYY-MM-DD HH:mm:ss')).toBe('2026-04-30 08:00:00');
    expect(filterState.timeRange[1].format('YYYY-MM-DD HH:mm:ss')).toBe('2026-04-30 09:00:00');
  });

  it('builds range filter state and keeps non-time filters', () => {
    const previousState = {
      timeRange: [FIXED_START_TIME, FIXED_END_TIME] as [dayjs.Dayjs, dayjs.Dayjs],
      rangeMinutes: 60,
      conversationType: 'admin',
      provider: 'aliyun',
      modelName: 'qwen3.5-plus',
      slot: 'adminSlot',
      status: 'success',
      bucketMinutes: 10,
    };

    const nextState = buildRangeFilterState(previousState, 5);

    expect(nextState.rangeMinutes).toBe(5);
    expect(nextState.bucketMinutes).toBe(5);
    expect(nextState.conversationType).toBe(previousState.conversationType);
    expect(nextState.provider).toBe(previousState.provider);
    expect(nextState.modelName).toBe(previousState.modelName);
    expect(nextState.slot).toBe(previousState.slot);
    expect(nextState.status).toBe(previousState.status);
    expect(nextState.timeRange[1].diff(nextState.timeRange[0], 'minute')).toBe(5);
  });

  it('formats monitor numbers, rates and durations', () => {
    expect(formatNumber(153319)).toBe('153,319');
    expect(formatRate(99.456)).toBe('99.46%');
    expect(formatDuration(850)).toBe('850 ms');
    expect(formatDuration(2293)).toBe('2.29 s');
  });

  it('builds model detail route with range params', () => {
    const path = buildAgentMonitorModelDetailPath({
      provider: 'aliyun',
      modelName: 'qwen3.5-plus',
      startTime: '2026-04-30 08:00:00',
      endTime: '2026-04-30 09:00:00',
      rangeMinutes: 60,
      bucketMinutes: 10,
    });
    const url = new URL(path, 'http://localhost:8000');

    expect(url.pathname).toBe('/llm-manage/agent-observability/monitor/model-detail');
    expect(url.searchParams.get('provider')).toBe('aliyun');
    expect(url.searchParams.get('modelName')).toBe('qwen3.5-plus');
    expect(url.searchParams.get('rangeMinutes')).toBe('60');
    expect(url.searchParams.get('bucketMinutes')).toBe('10');
  });
});
