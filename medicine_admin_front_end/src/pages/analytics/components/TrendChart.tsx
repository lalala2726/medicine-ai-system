import React, { useMemo, useState } from 'react';
import { Space, Typography, Segmented, Tag, Empty } from 'antd';
import { ProCard } from '@ant-design/pro-components';
import ReactECharts from 'echarts-for-react';
import { LineChartOutlined } from '@ant-design/icons';
import { AnalyticsTypes } from '@/api/mall/analytics';
import { useThemeContext } from '@/contexts/ThemeContext';
import { formatCurrency, formatNumber, formatPercent } from '../utils';
import styles from '../index.module.less';

const { Text } = Typography;

type TrendMode = 'sales' | 'afterSale';

interface TrendChartProps {
  loading: boolean;
  trendData?: AnalyticsTypes.AnalyticsTrendVo;
  rangeSummary?: AnalyticsTypes.AnalyticsRangeSummaryVo;
}

const granularityMap: Record<string, string> = {
  DAY: '日',
  WEEK: '周',
  MONTH: '月',
};

const TrendChart: React.FC<TrendChartProps> = ({ loading, trendData, rangeSummary }) => {
  const { isDark } = useThemeContext();
  const [trendMode, setTrendMode] = useState<TrendMode>('sales');

  const summaryMetrics = useMemo(
    () => [
      { key: 'paidAmount', title: '周期成交金额', value: formatCurrency(rangeSummary?.paidAmount) },
      {
        key: 'netPaidAmount',
        title: '周期净成交额',
        value: formatCurrency(rangeSummary?.netPaidAmount),
      },
      {
        key: 'paidOrderCount',
        title: '周期支付订单',
        value: formatNumber(rangeSummary?.paidOrderCount),
      },
      {
        key: 'averageOrderAmount',
        title: '客单价',
        value: formatCurrency(rangeSummary?.averageOrderAmount),
      },
      {
        key: 'refundAmount',
        title: '周期退款金额',
        value: formatCurrency(rangeSummary?.refundAmount),
      },
      { key: 'refundRate', title: '退款率', value: formatPercent(rangeSummary?.refundRate) },
      {
        key: 'afterSaleApplyCount',
        title: '售后申请数',
        value: formatNumber(rangeSummary?.afterSaleApplyCount),
      },
      {
        key: 'returnRefundQuantity',
        title: '退货退款件数',
        value: formatNumber(rangeSummary?.returnRefundQuantity),
      },
    ],
    [rangeSummary],
  );

  const trendBaseData = useMemo(
    () =>
      (trendData?.points || []).map((item) => ({
        label: item.label || '',
        paidOrderCount: Number(item.paidOrderCount) || 0,
        paidAmount: Number(item.paidAmount) || 0,
        refundAmount: Number(item.refundAmount) || 0,
        afterSaleApplyCount: Number(item.afterSaleApplyCount) || 0,
      })),
    [trendData?.points],
  );

  const echartsOption = useMemo(() => {
    if (!trendBaseData.length) return null;

    const isSales = trendMode === 'sales';
    const amountField = isSales ? 'paidAmount' : 'refundAmount';
    const countField = isSales ? 'paidOrderCount' : 'afterSaleApplyCount';
    const amountTitle = isSales ? '成交金额' : '退款金额';
    const countTitle = isSales ? '支付订单' : '售后申请';
    const amountColor = isSales ? '#1677ff' : '#f5222d';
    const countColor = isSales ? '#fa8c16' : '#722ed1';

    const xData = trendBaseData.map((d) => d.label);
    const amountData = trendBaseData.map((d) => d[amountField as keyof typeof d] as number);
    const countData = trendBaseData.map((d) => d[countField as keyof typeof d] as number);

    const textColor = isDark ? 'rgba(255,255,255,0.65)' : '#8c8c8c';
    const axisLineColor = isDark ? '#303030' : '#e8e8e8';
    const splitLineColor = isDark ? 'rgba(255,255,255,0.06)' : 'rgba(0,0,0,0.06)';

    return {
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'cross' },
        backgroundColor: isDark ? '#1f1f1f' : '#fff',
        borderColor: isDark ? '#303030' : '#e8e8e8',
        textStyle: { color: isDark ? 'rgba(255,255,255,0.85)' : '#1f1f1f' },
      },
      legend: {
        data: [amountTitle, countTitle],
        top: 0,
        textStyle: { color: textColor },
      },
      grid: {
        left: 16,
        right: 16,
        bottom: 8,
        top: 48,
        containLabel: true,
      },
      xAxis: {
        type: 'category',
        data: xData,
        axisLabel: {
          color: textColor,
          fontSize: 11,
          rotate: xData.length > 15 ? 45 : 0,
        },
        axisLine: { lineStyle: { color: axisLineColor } },
        axisTick: { show: false },
      },
      yAxis: [
        {
          type: 'value',
          name: amountTitle,
          nameTextStyle: { color: textColor, fontSize: 11 },
          axisLabel: { color: textColor, fontSize: 11 },
          splitLine: { lineStyle: { color: splitLineColor, type: 'dashed' } },
          axisLine: { show: false },
        },
        {
          type: 'value',
          name: countTitle,
          nameTextStyle: { color: textColor, fontSize: 11 },
          axisLabel: { color: textColor, fontSize: 11 },
          splitLine: { show: false },
          axisLine: { show: false },
        },
      ],
      series: [
        {
          name: amountTitle,
          type: 'bar',
          yAxisIndex: 0,
          data: amountData,
          itemStyle: {
            color: amountColor,
            borderRadius: [4, 4, 0, 0],
          },
          barMaxWidth: 32,
          label: {
            show: true,
            position: 'top',
            fontSize: 10,
            color: textColor,
            formatter: (params: any) => (params.value > 0 ? params.value : ''),
          },
        },
        {
          name: countTitle,
          type: 'line',
          yAxisIndex: 1,
          data: countData,
          lineStyle: {
            width: 2,
            type: 'dashed',
            color: countColor,
          },
          itemStyle: { color: countColor },
          symbol: 'circle',
          symbolSize: 6,
          smooth: false,
        },
      ],
    };
  }, [trendBaseData, trendMode, isDark]);

  return (
    <ProCard
      className={styles.mainCard}
      style={{ marginBottom: 16 }}
      title={
        <Space>
          <LineChartOutlined style={{ color: '#1677ff' }} />
          <Text strong style={{ fontSize: 16 }}>
            核心趋势追踪
          </Text>
        </Space>
      }
      extra={
        <Segmented
          options={[
            { label: '成交趋势', value: 'sales' },
            { label: '售后趋势', value: 'afterSale' },
          ]}
          value={trendMode}
          onChange={(value) => setTrendMode(value as TrendMode)}
        />
      }
      headerBordered
      loading={loading}
    >
      <div className={styles.summaryStrip}>
        {summaryMetrics.map((item) => (
          <div key={item.key} className={styles.summaryMetric}>
            <Text type="secondary">{item.title}</Text>
            <div className={styles.metricValue}>{item.value}</div>
          </div>
        ))}
      </div>

      <div className={styles.chartArea}>
        {trendBaseData.length ? (
          <div className={styles.trendPanelWrap}>
            <div className={styles.trendPanelHeader}>
              <Space>
                <Tag color="#1677ff">
                  {granularityMap[trendData?.granularity || 'DAY'] || trendData?.granularity}
                </Tag>
                <Text type="secondary">最近{trendData?.days || 30}天，完整时间轴自动补零</Text>
              </Space>
            </div>
            <ReactECharts
              option={echartsOption || {}}
              style={{ height: 400 }}
              theme={isDark ? 'dark' : undefined}
              notMerge
            />
          </div>
        ) : (
          <Empty description="当前范围暂无趋势数据" />
        )}
      </div>
    </ProCard>
  );
};

export default TrendChart;
