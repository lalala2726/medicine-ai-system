import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { Button, Empty, Select, Space, Typography, message } from 'antd';
import { PageContainer } from '@ant-design/pro-components';
import ReactECharts from 'echarts-for-react';
import dayjs from 'dayjs';
import { useLocation, useNavigate } from 'react-router-dom';
import {
  getAgentTraceMonitorModelDetail,
  type AgentTraceMonitorTypes,
} from '@/api/agent/traceMonitor';
import { useThemeContext } from '@/contexts/ThemeContext';
import { useAgentObservabilitySecondaryMenu } from '@/pages/llm-manage/agent-observability/shared';
import {
  AGENT_MONITOR_RANGE_OPTIONS,
  type AgentMonitorFilterState,
  buildFilterStateFromSearch,
  buildMonitorRequest,
  buildMonitorRequestFromSearch,
  buildRangeFilterState,
  formatDuration,
  formatNumber,
  formatRate,
} from '@/pages/llm-manage/agent-monitor/shared';
import { routePaths } from '@/router/paths';
import styles from './index.module.less';

const { Text } = Typography;

/**
 * 构建图表公共配置。
 * @param isDark 是否深色主题。
 * @param xData 横轴时间文本。
 * @param yAxisName 纵轴名称。
 * @returns ECharts 公共配置。
 */
function buildChartBaseOption(isDark: boolean, xData: string[], yAxisName?: string) {
  const textColor = isDark ? 'rgba(255,255,255,0.65)' : '#6b7280';
  const axisLineColor = isDark ? '#303030' : '#e5e7eb';
  const splitLineColor = isDark ? 'rgba(255,255,255,0.06)' : 'rgba(0,0,0,0.06)';
  return {
    tooltip: {
      trigger: 'axis',
      backgroundColor: isDark ? '#1f1f1f' : '#fff',
      borderColor: isDark ? '#303030' : '#e5e7eb',
      textStyle: { color: isDark ? 'rgba(255,255,255,0.85)' : '#111827' },
    },
    legend: {
      top: 0,
      textStyle: { color: textColor },
    },
    grid: {
      left: 16,
      right: 16,
      bottom: 12,
      top: 48,
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: xData,
      axisLabel: {
        color: textColor,
        rotate: xData.length > 12 ? 40 : 0,
      },
      axisLine: { lineStyle: { color: axisLineColor } },
      axisTick: { show: false },
    },
    yAxis: {
      type: 'value',
      name: yAxisName,
      axisLabel: { color: textColor },
      splitLine: { lineStyle: { color: splitLineColor, type: 'dashed' } },
    },
  };
}

/**
 * Agent 单模型监控详情页。
 * @returns 单模型详情页面节点。
 */
const AgentMonitorModelDetailPage: React.FC = () => {
  useAgentObservabilitySecondaryMenu();
  const { isDark } = useThemeContext();
  const navigate = useNavigate();
  const location = useLocation();
  const [messageApi, contextHolder] = message.useMessage();
  const [loading, setLoading] = useState(false);
  const [detail, setDetail] = useState<AgentTraceMonitorTypes.ModelDetailVo>();
  const initialFilterState = useMemo(
    () => buildFilterStateFromSearch(location.search),
    [location.search],
  );
  const [filterState, setFilterState] = useState<AgentMonitorFilterState>(initialFilterState);

  /** 当前查询参数。 */
  const requestParams = useMemo(() => {
    const queryParams = buildMonitorRequestFromSearch(location.search);
    return {
      ...queryParams,
      ...buildMonitorRequest(filterState),
      provider: queryParams.provider,
      modelName: queryParams.modelName,
    };
  }, [filterState, location.search]);

  /**
   * 加载单模型详情。
   * @returns 无返回值。
   */
  const loadDetail = useCallback(async () => {
    if (!requestParams.modelName) {
      setDetail(undefined);
      return;
    }
    setLoading(true);
    try {
      const nextDetail = await getAgentTraceMonitorModelDetail(requestParams);
      setDetail(nextDetail);
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '加载模型详情失败';
      messageApi.error(errorMessage);
    } finally {
      setLoading(false);
    }
  }, [messageApi, requestParams]);

  useEffect(() => {
    loadDetail();
  }, [loadDetail]);

  /** 模型趋势数据。 */
  const timelinePoints = useMemo(() => detail?.timeline || [], [detail?.timeline]);

  /** 横轴时间文本。 */
  const xData = useMemo(
    () => timelinePoints.map((item) => dayjs(item.bucketStart).format('MM-DD HH:mm')),
    [timelinePoints],
  );

  /** 调用次数图表配置。 */
  const callChartOption = useMemo(
    () => ({
      ...buildChartBaseOption(isDark, xData, '调用次数'),
      series: [
        {
          name: '总调用',
          type: 'line',
          data: timelinePoints.map((item) => Number(item.callCount || 0)),
          itemStyle: { color: '#1677ff' },
          lineStyle: { color: '#1677ff', width: 2 },
          smooth: true,
        },
        {
          name: '成功',
          type: 'line',
          data: timelinePoints.map((item) => Number(item.successCount || 0)),
          itemStyle: { color: '#52c41a' },
          smooth: true,
        },
        {
          name: '失败',
          type: 'line',
          data: timelinePoints.map((item) => Number(item.errorCount || 0)),
          itemStyle: { color: '#ff4d4f' },
          smooth: true,
        },
      ],
    }),
    [isDark, timelinePoints, xData],
  );

  /** 耗时图表配置。 */
  const durationChartOption = useMemo(
    () => ({
      ...buildChartBaseOption(isDark, xData, '耗时 ms'),
      series: [
        {
          name: '平均耗时',
          type: 'line',
          data: timelinePoints.map((item) => Number(item.avgDurationMs || 0)),
          itemStyle: { color: '#fa8c16' },
          lineStyle: { color: '#fa8c16', width: 2 },
          smooth: true,
        },
        {
          name: '最大耗时',
          type: 'line',
          data: timelinePoints.map((item) => Number(item.maxDurationMs || 0)),
          itemStyle: { color: '#722ed1' },
          lineStyle: { color: '#722ed1', width: 2 },
          smooth: true,
        },
      ],
    }),
    [isDark, timelinePoints, xData],
  );

  /** Token 与缓存图表配置。 */
  const tokenCacheChartOption = useMemo(
    () => ({
      ...buildChartBaseOption(isDark, xData, 'Token'),
      series: [
        {
          name: '总 Token',
          type: 'line',
          data: timelinePoints.map((item) => Number(item.totalTokens || 0)),
          itemStyle: { color: '#1677ff' },
          lineStyle: { color: '#1677ff', width: 2 },
          smooth: true,
        },
        {
          name: '输入 Token',
          type: 'line',
          data: timelinePoints.map((item) => Number(item.inputTokens || 0)),
          itemStyle: { color: '#52c41a' },
          smooth: true,
        },
        {
          name: '输出 Token',
          type: 'line',
          data: timelinePoints.map((item) => Number(item.outputTokens || 0)),
          itemStyle: { color: '#faad14' },
          smooth: true,
        },
        {
          name: '缓存总 Token',
          type: 'line',
          data: timelinePoints.map((item) => Number(item.cacheTotalTokens || 0)),
          itemStyle: { color: '#13c2c2' },
          lineStyle: { color: '#13c2c2', width: 2, type: 'dashed' },
          smooth: true,
        },
        {
          name: '缓存命中 Token',
          type: 'line',
          data: timelinePoints.map((item) => Number(item.cacheReadTokens || 0)),
          itemStyle: { color: '#722ed1' },
          lineStyle: { color: '#722ed1', type: 'dashed' },
          smooth: true,
        },
        {
          name: '缓存创建 Token',
          type: 'line',
          data: timelinePoints.map((item) => Number(item.cacheWriteTokens || 0)),
          itemStyle: { color: '#eb2f96' },
          lineStyle: { color: '#eb2f96', type: 'dashed' },
          smooth: true,
        },
      ],
    }),
    [isDark, timelinePoints, xData],
  );

  /**
   * 返回监控面板。
   * @returns 无返回值。
   */
  const goBackToDashboard = useCallback(() => {
    const searchParams = new URLSearchParams(location.search);
    const backProvider = searchParams.get('backProvider');
    const backModelName = searchParams.get('backModelName');
    const backRequest = buildMonitorRequest(filterState);
    searchParams.delete('provider');
    searchParams.delete('modelName');
    searchParams.delete('backProvider');
    searchParams.delete('backModelName');
    searchParams.set('startTime', backRequest.startTime || '');
    searchParams.set('endTime', backRequest.endTime || '');
    searchParams.set('rangeMinutes', String(filterState.rangeMinutes));
    searchParams.set(
      'bucketMinutes',
      String(backRequest.bucketMinutes || filterState.bucketMinutes),
    );
    if (backProvider) {
      searchParams.set('provider', backProvider);
    }
    if (backModelName) {
      searchParams.set('modelName', backModelName);
    }
    const searchText = searchParams.toString();
    navigate(
      searchText ? `${routePaths.llmAgentMonitor}?${searchText}` : routePaths.llmAgentMonitor,
    );
  }, [filterState, location.search, navigate]);

  /**
   * 处理详情页时间范围切换。
   * @param minutes 快捷时间范围分钟数。
   * @returns 无返回值。
   */
  const handleRangeChange = useCallback((minutes: number) => {
    setFilterState((prev) => buildRangeFilterState(prev, minutes));
  }, []);

  return (
    <PageContainer title="模型监控详情" className={styles.page}>
      {contextHolder}
      <div className={styles.detailHeader}>
        <div>
          <div className={styles.detailTitle}>
            {detail?.modelName || requestParams.modelName || '-'}
          </div>
          <Space size={12} wrap>
            <Text type="secondary">
              供应商：{detail?.provider || requestParams.provider || '-'}
            </Text>
            <Text type="secondary">桶：{requestParams.bucketMinutes || 60} 分钟</Text>
          </Space>
        </div>
        <Space>
          <Select
            className={styles.rangeSelect}
            value={filterState.rangeMinutes}
            options={AGENT_MONITOR_RANGE_OPTIONS}
            onChange={(value) => handleRangeChange(Number(value))}
          />
          <Button onClick={goBackToDashboard}>返回监控面板</Button>
        </Space>
      </div>

      <div className={styles.metricsGrid}>
        <div className={styles.metricCard}>
          <div className={styles.metricLabel}>调用次数</div>
          <div className={styles.metricValue}>{formatNumber(detail?.summary?.callCount)}</div>
          <div className={styles.metricSubText}>
            成功 {formatNumber(detail?.summary?.successCount)} / 失败{' '}
            {formatNumber(detail?.summary?.errorCount)}
          </div>
        </div>
        <div className={styles.metricCard}>
          <div className={styles.metricLabel}>成功率</div>
          <div className={styles.metricValue}>{formatRate(detail?.summary?.successRate)}</div>
          <div className={styles.metricSubText}>
            失败率 {formatRate(detail?.summary?.errorRate)}
          </div>
        </div>
        <div className={styles.metricCard}>
          <div className={styles.metricLabel}>总 Token</div>
          <div className={styles.metricValue}>{formatNumber(detail?.summary?.totalTokens)}</div>
          <div className={styles.metricSubText}>
            输入 {formatNumber(detail?.summary?.inputTokens)} / 输出{' '}
            {formatNumber(detail?.summary?.outputTokens)}
          </div>
        </div>
        <div className={styles.metricCard}>
          <div className={styles.metricLabel}>缓存 Token</div>
          <div className={styles.metricValue}>
            {formatNumber(detail?.summary?.cacheTotalTokens)}
          </div>
          <div className={styles.metricSubText}>
            命中 {formatNumber(detail?.summary?.cacheReadTokens)} / 创建{' '}
            {formatNumber(detail?.summary?.cacheWriteTokens)}
          </div>
        </div>
        <div className={styles.metricCard}>
          <div className={styles.metricLabel}>平均耗时</div>
          <div className={styles.metricValue}>{formatDuration(detail?.summary?.avgDurationMs)}</div>
          <div className={styles.metricSubText}>
            最大 {formatDuration(detail?.summary?.maxDurationMs)}
          </div>
        </div>
      </div>

      {timelinePoints.length ? (
        <div className={styles.detailChartGrid}>
          <div className={styles.panel}>
            <div className={styles.panelHeader}>
              <Text strong>调用次数趋势</Text>
            </div>
            <div className={styles.panelBody}>
              <ReactECharts
                option={callChartOption}
                style={{ height: 320 }}
                theme={isDark ? 'dark' : undefined}
                notMerge
                showLoading={loading}
              />
            </div>
          </div>
          <div className={styles.panel}>
            <div className={styles.panelHeader}>
              <Text strong>耗时趋势</Text>
            </div>
            <div className={styles.panelBody}>
              <ReactECharts
                option={durationChartOption}
                style={{ height: 320 }}
                theme={isDark ? 'dark' : undefined}
                notMerge
                showLoading={loading}
              />
            </div>
          </div>
          <div className={styles.panel}>
            <div className={styles.panelHeader}>
              <Text strong>Token 与缓存趋势</Text>
            </div>
            <div className={styles.panelBody}>
              <ReactECharts
                option={tokenCacheChartOption}
                style={{ height: 320 }}
                theme={isDark ? 'dark' : undefined}
                notMerge
                showLoading={loading}
              />
            </div>
          </div>
        </div>
      ) : (
        <div className={styles.panel}>
          <div className={styles.panelBody}>
            <Empty description={loading ? '正在加载模型详情' : '当前模型暂无趋势数据'} />
          </div>
        </div>
      )}
    </PageContainer>
  );
};

export default AgentMonitorModelDetailPage;
