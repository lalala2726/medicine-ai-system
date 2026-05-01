import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { Empty, Select, Space, Table, Typography, message } from 'antd';
import { PageContainer } from '@ant-design/pro-components';
import type { ColumnsType } from 'antd/es/table';
import ReactECharts from 'echarts-for-react';
import dayjs from 'dayjs';
import { useLocation, useNavigate } from 'react-router-dom';
import {
  getAgentTraceMonitorCharts,
  getAgentTraceMonitorModelRanking,
  getAgentTraceMonitorSummary,
  type AgentTraceMonitorTypes,
} from '@/api/agent/traceMonitor';
import { useThemeContext } from '@/contexts/ThemeContext';
import { useAgentObservabilitySecondaryMenu } from '@/pages/llm-manage/agent-observability/shared';
import { buildAgentMonitorModelDetailPath } from '@/router/paths';
import styles from './index.module.less';
import {
  AGENT_MONITOR_RANGE_OPTIONS,
  type AgentMonitorFilterState,
  buildRangeFilterState,
  buildFilterStateFromSearch,
  buildMonitorRequest,
  formatDuration,
  formatNumber,
  formatRate,
} from './shared';

const { Text } = Typography;

/**
 * 构建图表公共颜色。
 * @param isDark 是否深色主题。
 * @returns 图表颜色配置。
 */
function buildChartColors(isDark: boolean) {
  return {
    textColor: isDark ? 'rgba(255,255,255,0.65)' : '#6b7280',
    axisLineColor: isDark ? '#303030' : '#e5e7eb',
    splitLineColor: isDark ? 'rgba(255,255,255,0.06)' : 'rgba(0,0,0,0.06)',
    tooltipBackground: isDark ? '#1f1f1f' : '#fff',
    tooltipBorder: isDark ? '#303030' : '#e5e7eb',
    tooltipText: isDark ? 'rgba(255,255,255,0.85)' : '#111827',
  };
}

/**
 * 构建折线图公共配置。
 * @param isDark 是否深色主题。
 * @param xData 横轴时间文本。
 * @param yAxisName 纵轴名称。
 * @returns ECharts 公共配置。
 */
function buildLineChartBaseOption(isDark: boolean, xData: string[], yAxisName?: string) {
  const colors = buildChartColors(isDark);
  return {
    tooltip: {
      trigger: 'axis',
      backgroundColor: colors.tooltipBackground,
      borderColor: colors.tooltipBorder,
      textStyle: { color: colors.tooltipText },
    },
    legend: {
      top: 0,
      textStyle: { color: colors.textColor },
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
        color: colors.textColor,
        rotate: xData.length > 12 ? 40 : 0,
      },
      axisLine: { lineStyle: { color: colors.axisLineColor } },
      axisTick: { show: false },
    },
    yAxis: {
      type: 'value',
      name: yAxisName,
      axisLabel: { color: colors.textColor },
      splitLine: { lineStyle: { color: colors.splitLineColor, type: 'dashed' } },
    },
  };
}

/**
 * 智能体监控面板页面。
 * @returns 监控面板页面节点。
 */
const AgentMonitorPage: React.FC = () => {
  useAgentObservabilitySecondaryMenu();
  const navigate = useNavigate();
  const location = useLocation();
  const { isDark } = useThemeContext();
  const initialFilterState = useMemo(
    () => buildFilterStateFromSearch(location.search),
    [location.search],
  );
  const [filterState, setFilterState] = useState<AgentMonitorFilterState>(initialFilterState);
  const [summary, setSummary] = useState<AgentTraceMonitorTypes.SummaryVo>();
  const [charts, setCharts] = useState<AgentTraceMonitorTypes.ChartsVo>();
  const [ranking, setRanking] = useState<AgentTraceMonitorTypes.ModelRankingVo[]>([]);
  const [loading, setLoading] = useState(false);
  const [messageApi, contextHolder] = message.useMessage();

  /**
   * 刷新监控面板数据。
   * @param nextQueryState 查询条件。
   * @returns 无返回值。
   */
  const loadDashboardData = useCallback(
    async (nextQueryState: AgentMonitorFilterState) => {
      setLoading(true);
      try {
        const requestParams = buildMonitorRequest(nextQueryState);
        const [nextSummary, nextCharts, nextRanking] = await Promise.all([
          getAgentTraceMonitorSummary(requestParams),
          getAgentTraceMonitorCharts(requestParams),
          getAgentTraceMonitorModelRanking(requestParams),
        ]);
        setSummary(nextSummary);
        setCharts(nextCharts || {});
        setRanking(nextRanking || []);
      } catch (error) {
        const errorMessage = error instanceof Error ? error.message : '加载监控数据失败';
        messageApi.error(errorMessage);
      } finally {
        setLoading(false);
      }
    },
    [messageApi],
  );

  useEffect(() => {
    loadDashboardData(filterState);
  }, [filterState, loadDashboardData]);

  /** 调用次数趋势数据。 */
  const callTrendPoints = useMemo(() => charts?.callTrend || [], [charts?.callTrend]);

  /** 耗时趋势数据。 */
  const durationTrendPoints = useMemo(() => charts?.durationTrend || [], [charts?.durationTrend]);

  /** Token 与缓存趋势数据。 */
  const tokenCachePoints = useMemo(() => charts?.tokenCacheTrend || [], [charts?.tokenCacheTrend]);

  /** 调用次数折线图配置。 */
  const callTrendChartOption = useMemo(() => {
    const xData = callTrendPoints.map((item) => dayjs(item.bucketStart).format('MM-DD HH:mm'));
    return {
      ...buildLineChartBaseOption(isDark, xData, '调用次数'),
      series: [
        {
          name: '总调用',
          type: 'line',
          data: callTrendPoints.map((item) => Number(item.callCount || 0)),
          itemStyle: { color: '#1677ff' },
          lineStyle: { color: '#1677ff', width: 2 },
          smooth: true,
        },
        {
          name: '成功',
          type: 'line',
          data: callTrendPoints.map((item) => Number(item.successCount || 0)),
          itemStyle: { color: '#52c41a' },
          smooth: true,
        },
        {
          name: '失败',
          type: 'line',
          data: callTrendPoints.map((item) => Number(item.errorCount || 0)),
          itemStyle: { color: '#ff4d4f' },
          smooth: true,
        },
      ],
    };
  }, [callTrendPoints, isDark]);

  /** 耗时折线图配置。 */
  const durationTrendChartOption = useMemo(() => {
    const xData = durationTrendPoints.map((item) => dayjs(item.bucketStart).format('MM-DD HH:mm'));
    return {
      ...buildLineChartBaseOption(isDark, xData, '耗时 ms'),
      series: [
        {
          name: '平均耗时',
          type: 'line',
          data: durationTrendPoints.map((item) => Number(item.avgDurationMs || 0)),
          itemStyle: { color: '#fa8c16' },
          lineStyle: { color: '#fa8c16', width: 2 },
          smooth: true,
        },
        {
          name: '最大耗时',
          type: 'line',
          data: durationTrendPoints.map((item) => Number(item.maxDurationMs || 0)),
          itemStyle: { color: '#722ed1' },
          lineStyle: { color: '#722ed1', width: 2 },
          smooth: true,
        },
      ],
    };
  }, [durationTrendPoints, isDark]);

  /** Token 与缓存折线图配置。 */
  const tokenCacheChartOption = useMemo(() => {
    const xData = tokenCachePoints.map((item) => dayjs(item.bucketStart).format('MM-DD HH:mm'));
    return {
      ...buildLineChartBaseOption(isDark, xData, 'Token'),
      series: [
        {
          name: '总 Token',
          type: 'line',
          data: tokenCachePoints.map((item) => Number(item.totalTokens || 0)),
          itemStyle: { color: '#1677ff' },
          lineStyle: { color: '#1677ff', width: 2 },
          smooth: true,
        },
        {
          name: '输入 Token',
          type: 'line',
          data: tokenCachePoints.map((item) => Number(item.inputTokens || 0)),
          itemStyle: { color: '#52c41a' },
          smooth: true,
        },
        {
          name: '输出 Token',
          type: 'line',
          data: tokenCachePoints.map((item) => Number(item.outputTokens || 0)),
          itemStyle: { color: '#faad14' },
          smooth: true,
        },
        {
          name: '缓存总 Token',
          type: 'line',
          data: tokenCachePoints.map((item) => Number(item.cacheTotalTokens || 0)),
          itemStyle: { color: '#13c2c2' },
          lineStyle: { color: '#13c2c2', width: 2, type: 'dashed' },
          smooth: true,
        },
        {
          name: '缓存命中 Token',
          type: 'line',
          data: tokenCachePoints.map((item) => Number(item.cacheReadTokens || 0)),
          itemStyle: { color: '#722ed1' },
          lineStyle: { color: '#722ed1', type: 'dashed' },
          smooth: true,
        },
        {
          name: '缓存创建 Token',
          type: 'line',
          data: tokenCachePoints.map((item) => Number(item.cacheWriteTokens || 0)),
          itemStyle: { color: '#eb2f96' },
          lineStyle: { color: '#eb2f96', type: 'dashed' },
          smooth: true,
        },
      ],
    };
  }, [isDark, tokenCachePoints]);

  /**
   * 跳转到单模型详情页。
   * @param record 模型排行记录。
   * @returns 无返回值。
   */
  const openModelDetail = useCallback(
    (record: AgentTraceMonitorTypes.ModelRankingVo) => {
      if (!record.modelName) {
        return;
      }
      navigate(
        buildAgentMonitorModelDetailPath({
          ...buildMonitorRequest(filterState),
          rangeMinutes: filterState.rangeMinutes,
          provider: record.provider,
          modelName: record.modelName,
          backProvider: filterState.provider,
          backModelName: filterState.modelName,
        }),
      );
    },
    [filterState, navigate],
  );

  /** 模型排行表格列。 */
  const rankingColumns = useMemo<ColumnsType<AgentTraceMonitorTypes.ModelRankingVo>>(
    () => [
      {
        title: '模型',
        dataIndex: 'modelName',
        render: (value: string, record) => (
          <Space direction="vertical" size={0}>
            <Text strong className={styles.modelName}>
              {value || '-'}
            </Text>
            <Text type="secondary">{record.provider || '-'}</Text>
          </Space>
        ),
      },
      {
        title: '调用',
        dataIndex: 'callCount',
        width: 84,
        align: 'right',
        render: (value: number) => formatNumber(value),
      },
      {
        title: 'Token',
        dataIndex: 'totalTokens',
        width: 96,
        align: 'right',
        render: (value: number) => formatNumber(value),
      },
      {
        title: '失败率',
        dataIndex: 'errorRate',
        width: 86,
        align: 'right',
        render: (value: number) => formatRate(value),
      },
      {
        title: '平均耗时',
        dataIndex: 'avgDurationMs',
        width: 96,
        align: 'right',
        render: (value: number) => formatDuration(value),
      },
    ],
    [],
  );

  /**
   * 处理时间范围切换。
   * @param minutes 快捷时间范围分钟数。
   * @returns 无返回值。
   */
  const handleRangeChange = useCallback((minutes: number) => {
    setFilterState((prev) => buildRangeFilterState(prev, minutes));
  }, []);

  return (
    <PageContainer title="监控面板" className={styles.page}>
      {contextHolder}

      <div className={styles.toolbar}>
        <Space direction="vertical" size={0}>
          <Text strong>智能体监控总览</Text>
          <Text type="secondary">按真实模型调用明细统计调用量、耗时、Token 与缓存表现</Text>
        </Space>
        <Select
          className={styles.rangeSelect}
          value={filterState.rangeMinutes}
          options={AGENT_MONITOR_RANGE_OPTIONS}
          onChange={(value) => handleRangeChange(Number(value))}
        />
      </div>

      <div className={styles.metricsGrid}>
        <div className={styles.metricCard}>
          <div className={styles.metricLabel}>调用次数</div>
          <div className={styles.metricValue}>{formatNumber(summary?.callCount)}</div>
          <div className={styles.metricSubText}>
            成功 {formatNumber(summary?.successCount)} / 失败 {formatNumber(summary?.errorCount)}
          </div>
        </div>
        <div className={styles.metricCard}>
          <div className={styles.metricLabel}>成功率</div>
          <div className={styles.metricValue}>{formatRate(summary?.successRate)}</div>
          <div className={styles.metricSubText}>失败率 {formatRate(summary?.errorRate)}</div>
        </div>
        <div className={styles.metricCard}>
          <div className={styles.metricLabel}>总 Token</div>
          <div className={styles.metricValue}>{formatNumber(summary?.totalTokens)}</div>
          <div className={styles.metricSubText}>
            输入 {formatNumber(summary?.inputTokens)} / 输出 {formatNumber(summary?.outputTokens)}
          </div>
        </div>
        <div className={styles.metricCard}>
          <div className={styles.metricLabel}>缓存 Token</div>
          <div className={styles.metricValue}>{formatNumber(summary?.cacheTotalTokens)}</div>
          <div className={styles.metricSubText}>
            命中 {formatNumber(summary?.cacheReadTokens)} / 创建{' '}
            {formatNumber(summary?.cacheWriteTokens)}
          </div>
        </div>
      </div>

      <div className={styles.dashboardStack}>
        <div className={styles.panel}>
          <div className={styles.panelHeader}>
            <Space direction="vertical" size={0}>
              <Text strong>模型排行</Text>
              <Text type="secondary">按当前时间范围汇总，点击模型查看详情</Text>
            </Space>
          </div>
          <div className={styles.rankingBody}>
            <Table
              rowKey={(record) => `${record.provider || ''}:${record.modelName || ''}`}
              size="small"
              loading={loading}
              columns={rankingColumns}
              dataSource={ranking}
              pagination={false}
              onRow={(record) => ({
                onClick: () => openModelDetail(record),
              })}
              rowClassName={styles.clickableRow}
            />
          </div>
        </div>

        <div className={styles.panel}>
          <div className={styles.panelHeader}>
            <Space direction="vertical" size={0}>
              <Text strong>调用次数</Text>
              <Text type="secondary">总调用、成功和失败趋势</Text>
            </Space>
          </div>
          <div className={styles.panelBody}>
            {callTrendPoints.length ? (
              <ReactECharts
                option={callTrendChartOption}
                style={{ height: 340 }}
                theme={isDark ? 'dark' : undefined}
                notMerge
                showLoading={loading}
              />
            ) : (
              <Empty description="当前范围暂无调用次数数据" />
            )}
          </div>
        </div>

        <div className={styles.panel}>
          <div className={styles.panelHeader}>
            <Space direction="vertical" size={0}>
              <Text strong>耗时</Text>
              <Text type="secondary">平均耗时和最大耗时趋势</Text>
            </Space>
          </div>
          <div className={styles.panelBody}>
            {durationTrendPoints.length ? (
              <ReactECharts
                option={durationTrendChartOption}
                style={{ height: 340 }}
                theme={isDark ? 'dark' : undefined}
                notMerge
                showLoading={loading}
              />
            ) : (
              <Empty description="当前范围暂无耗时数据" />
            )}
          </div>
        </div>

        <div className={styles.panel}>
          <div className={styles.panelHeader}>
            <Space direction="vertical" size={0}>
              <Text strong>Token 与缓存</Text>
              <Text type="secondary">Token 消耗和上下文缓存趋势</Text>
            </Space>
          </div>
          <div className={styles.panelBody}>
            {tokenCachePoints.length ? (
              <ReactECharts
                option={tokenCacheChartOption}
                style={{ height: 340 }}
                theme={isDark ? 'dark' : undefined}
                notMerge
                showLoading={loading}
              />
            ) : (
              <Empty description="当前范围暂无 Token 与缓存数据" />
            )}
          </div>
        </div>
      </div>
    </PageContainer>
  );
};

export default AgentMonitorPage;
