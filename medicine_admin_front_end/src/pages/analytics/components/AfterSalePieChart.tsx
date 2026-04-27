import React, { useMemo } from 'react';
import { Tabs, Empty } from 'antd';
import { ProCard } from '@ant-design/pro-components';
import ReactECharts from 'echarts-for-react';
import { AnalyticsTypes } from '@/api/mall/analytics';
import { useThemeContext } from '@/contexts/ThemeContext';
import styles from '../index.module.less';

const PIE_COLORS = [
  '#1677ff',
  '#52c41a',
  '#faad14',
  '#722ed1',
  '#f5222d',
  '#13c2c2',
  '#fa8c16',
  '#eb2f96',
  '#2f54eb',
  '#a0d911',
];

interface AfterSalePieChartProps {
  loading: boolean;
  statusDistribution?: AnalyticsTypes.AnalyticsStatusDistributionItemVo[];
  reasonDistribution?: AnalyticsTypes.AnalyticsReasonDistributionItemVo[];
}

const AfterSalePieChart: React.FC<AfterSalePieChartProps> = ({
  loading,
  statusDistribution,
  reasonDistribution,
}) => {
  const { isDark } = useThemeContext();

  const statusChartData = useMemo(
    () =>
      (statusDistribution || []).map((item) => ({
        name: item.statusName || item.status || '未知',
        value: Number(item.count) || 0,
      })),
    [statusDistribution],
  );

  const reasonChartData = useMemo(
    () =>
      (reasonDistribution || []).map((item) => ({
        name: item.reasonName || item.reason || '未知',
        value: Number(item.count) || 0,
      })),
    [reasonDistribution],
  );

  const buildPieOption = (data: { name: string; value: number }[]) => {
    const textColor = isDark ? 'rgba(255,255,255,0.85)' : '#1f1f1f';
    return {
      tooltip: {
        trigger: 'item',
        backgroundColor: isDark ? '#1f1f1f' : '#fff',
        borderColor: isDark ? '#303030' : '#e8e8e8',
        textStyle: { color: textColor },
        formatter: '{b}: {c} ({d}%)',
      },
      legend: {
        bottom: 0,
        textStyle: { color: isDark ? 'rgba(255,255,255,0.65)' : '#8c8c8c' },
      },
      color: PIE_COLORS,
      series: [
        {
          type: 'pie',
          radius: ['40%', '70%'],
          center: ['50%', '45%'],
          avoidLabelOverlap: true,
          itemStyle: {
            borderRadius: 6,
            borderColor: isDark ? '#141414' : '#fff',
            borderWidth: 2,
          },
          label: {
            show: true,
            formatter: '{b}\n{c}',
            fontWeight: 'bold',
            color: textColor,
          },
          emphasis: {
            label: {
              show: true,
              fontSize: 14,
              fontWeight: 'bold',
            },
          },
          data,
        },
      ],
    };
  };

  return (
    <ProCard ghost gutter={[16, 16]} style={{ marginTop: 16 }}>
      <ProCard
        colSpan={{ xs: 24, lg: 24 }}
        className={styles.mainCard}
        headerBordered
        title="售后分布洞察"
        loading={loading}
      >
        <Tabs
          defaultActiveKey="status"
          items={[
            {
              key: 'status',
              label: '按售后状态',
              children: (
                <div className={styles.pieArea}>
                  {statusChartData.length ? (
                    <ReactECharts
                      option={buildPieOption(statusChartData)}
                      style={{ height: 300, width: '100%' }}
                      theme={isDark ? 'dark' : undefined}
                      notMerge
                    />
                  ) : (
                    <Empty description="暂无售后状态数据" />
                  )}
                </div>
              ),
            },
            {
              key: 'reason',
              label: '按售后原因',
              children: (
                <div className={styles.pieArea}>
                  {reasonChartData.length ? (
                    <ReactECharts
                      option={buildPieOption(reasonChartData)}
                      style={{ height: 300, width: '100%' }}
                      theme={isDark ? 'dark' : undefined}
                      notMerge
                    />
                  ) : (
                    <Empty description="暂无售后原因数据" />
                  )}
                </div>
              ),
            },
          ]}
        />
      </ProCard>
    </ProCard>
  );
};

export default AfterSalePieChart;
