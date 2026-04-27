import React, { useState } from 'react';
import { Space, Button, Typography } from 'antd';
import { PageContainer } from '@ant-design/pro-components';
import { ReloadOutlined } from '@ant-design/icons';
import { useRequest } from 'ahooks';
import dayjs, { Dayjs } from 'dayjs';
import { AnalyticsTypes, getAnalyticsDashboard, getAnalyticsTrend } from '@/api/mall/analytics';
import HeroOverview from './components/HeroOverview';
import TrendChart from './components/TrendChart';
import InsightMetrics from './components/InsightMetrics';
import SalesRankingTable from './components/SalesRankingTable';
import AfterSalePieChart from './components/AfterSalePieChart';
import styles from './index.module.less';

const { Text } = Typography;

const Analytics: React.FC = () => {
  const [days, setDays] = useState<AnalyticsTypes.DaysRange>(30);
  const [lastUpdatedAt, setLastUpdatedAt] = useState<Dayjs>(() => dayjs());

  const {
    data: dashboardData,
    loading: dashboardLoading,
    refresh: refreshDashboard,
  } = useRequest(() => getAnalyticsDashboard({ days }), {
    refreshDeps: [days],
    onSuccess: () => setLastUpdatedAt(dayjs()),
  });

  const {
    data: trendData,
    loading: trendLoading,
    refresh: refreshTrend,
  } = useRequest(() => getAnalyticsTrend({ days }), {
    refreshDeps: [days],
    onSuccess: () => setLastUpdatedAt(dayjs()),
  });

  const loading = dashboardLoading || trendLoading;

  const refreshAll = () => {
    refreshDashboard();
    refreshTrend();
    setLastUpdatedAt(dayjs());
  };

  return (
    <PageContainer
      title="运营数据看板"
      subTitle="实时监测经营结果、支付转化、履约效率与售后风险"
      extra={[
        <Space key="refresh" align="center">
          <Text type="secondary" style={{ fontSize: 12 }}>
            最后更新：{lastUpdatedAt.format('YYYY-MM-DD HH:mm:ss')}
          </Text>
          <Button icon={<ReloadOutlined />} onClick={refreshAll} loading={loading}>
            刷新
          </Button>
        </Space>,
      ]}
    >
      <div className={styles.page}>
        <HeroOverview
          days={days}
          onDaysChange={setDays}
          loading={dashboardLoading}
          data={dashboardData?.realtimeOverview}
        />

        <TrendChart
          loading={trendLoading}
          trendData={trendData}
          rangeSummary={dashboardData?.rangeSummary}
        />

        <InsightMetrics
          conversionSummary={dashboardData?.conversionSummary}
          fulfillmentSummary={dashboardData?.fulfillmentSummary}
          afterSaleEfficiencySummary={dashboardData?.afterSaleEfficiencySummary}
        />

        <SalesRankingTable
          topSellingProducts={dashboardData?.topSellingProducts}
          returnRefundRiskProducts={dashboardData?.returnRefundRiskProducts}
        />

        <AfterSalePieChart
          loading={dashboardLoading}
          statusDistribution={dashboardData?.afterSaleStatusDistribution}
          reasonDistribution={dashboardData?.afterSaleReasonDistribution}
        />
      </div>
    </PageContainer>
  );
};

export default Analytics;
