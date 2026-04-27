import React, { useMemo } from 'react';
import { Space, Typography, Segmented } from 'antd';
import { StatisticCard } from '@ant-design/pro-components';
import {
  CheckCircleOutlined,
  DollarCircleOutlined,
  InboxOutlined,
  RiseOutlined,
  ShoppingOutlined,
  SyncOutlined,
  WarningOutlined,
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { AnalyticsTypes } from '@/api/mall/analytics';
import { formatCurrency, formatNumber } from '../utils';
import styles from '../index.module.less';

const { Text, Title } = Typography;

const RANGE_OPTIONS: Array<{
  label: string;
  value: AnalyticsTypes.DaysRange;
}> = [
  { label: '近7天', value: 7 },
  { label: '近15天', value: 15 },
  { label: '近30天', value: 30 },
  { label: '近12周', value: 84 },
  { label: '近12月', value: 365 },
];

type MetricCardItem = {
  key: string;
  title: string;
  value?: number | string;
  formatter: (value?: number | string) => string;
  icon: React.ReactNode;
  toneClass: string;
  actionUrl?: string;
  actionText?: string;
};

interface HeroOverviewProps {
  days: AnalyticsTypes.DaysRange;
  onDaysChange: (val: AnalyticsTypes.DaysRange) => void;
  loading: boolean;
  data?: AnalyticsTypes.AnalyticsRealtimeOverviewVo;
}

const HeroOverview: React.FC<HeroOverviewProps> = ({ days, onDaysChange, loading, data }) => {
  const navigate = useNavigate();

  const revenueMetrics = useMemo<MetricCardItem[]>(
    () => [
      {
        key: 'cumulativePaidAmount',
        title: '累计成交金额',
        value: data?.cumulativePaidAmount,
        formatter: formatCurrency,
        icon: <DollarCircleOutlined />,
        toneClass: styles.blueTone,
      },
      {
        key: 'cumulativePaidOrderCount',
        title: '累计支付订单',
        value: data?.cumulativePaidOrderCount,
        formatter: formatNumber,
        icon: <CheckCircleOutlined />,
        toneClass: styles.greenTone,
      },
      {
        key: 'todayPaidAmount',
        title: '今日成交金额',
        value: data?.todayPaidAmount,
        formatter: formatCurrency,
        icon: <RiseOutlined />,
        toneClass: styles.goldTone,
      },
      {
        key: 'todayPaidOrderCount',
        title: '今日支付订单',
        value: data?.todayPaidOrderCount,
        formatter: formatNumber,
        icon: <ShoppingOutlined />,
        toneClass: styles.purpleTone,
      },
    ],
    [data],
  );

  const operationMetrics = useMemo<MetricCardItem[]>(
    () => [
      {
        key: 'pendingShipmentOrderCount',
        title: '待发货订单',
        value: data?.pendingShipmentOrderCount,
        formatter: formatNumber,
        icon: <InboxOutlined />,
        toneClass: styles.orangeTone,
        actionUrl: '/mall/order-list',
        actionText: '去处理 ➔',
      },
      {
        key: 'pendingReceiptOrderCount',
        title: '待收货订单',
        value: data?.pendingReceiptOrderCount,
        formatter: formatNumber,
        icon: <ShoppingOutlined />,
        toneClass: styles.tealTone,
      },
      {
        key: 'pendingAfterSaleCount',
        title: '待处理售后',
        value: data?.pendingAfterSaleCount,
        formatter: formatNumber,
        icon: <WarningOutlined />,
        toneClass: styles.redTone,
        actionUrl: '/mall/after-sales',
        actionText: '去处理 ➔',
      },
      {
        key: 'processingAfterSaleCount',
        title: '售后处理中',
        value: data?.processingAfterSaleCount,
        formatter: formatNumber,
        icon: <SyncOutlined />,
        toneClass: styles.skyTone,
      },
    ],
    [data],
  );

  return (
    <>
      <div className={styles.heroPanel}>
        <Space direction="vertical" size={4}>
          <Title level={4} className={styles.heroTitle}>
            核心数据实时概览
          </Title>
          <Text className={styles.heroSubtitle}>
            聚焦经营结果、支付转化、履约效率和售后质量，用固定分析周期快速查看完整业务链路。
          </Text>
        </Space>
        <Segmented
          options={RANGE_OPTIONS}
          value={days}
          onChange={(value) => onDaysChange(value as AnalyticsTypes.DaysRange)}
        />
      </div>

      <div className={styles.metricsSection}>
        <div className={styles.metricsGroup}>
          <div className={styles.groupTitle}>营收与规模</div>
          <div className={styles.metricsGrid}>
            {revenueMetrics.map((metric) => (
              <StatisticCard
                key={metric.key}
                className={`${styles.metricCard} ${metric.toneClass}`}
                statistic={{
                  title: metric.title,
                  value: metric.formatter(metric.value),
                  loading,
                  icon: <div className={styles.metricIcon}>{metric.icon}</div>,
                }}
              />
            ))}
          </div>
        </div>
        <div className={styles.metricsGroup}>
          <div className={styles.groupTitle}>履约与售后待办</div>
          <div className={styles.metricsGrid}>
            {operationMetrics.map((metric) => (
              <div key={metric.key} className={styles.metricCardWrap}>
                <StatisticCard
                  className={`${styles.metricCard} ${metric.toneClass}`}
                  statistic={{
                    title: metric.title,
                    value: metric.formatter(metric.value),
                    loading,
                    icon: <div className={styles.metricIcon}>{metric.icon}</div>,
                  }}
                />
                {metric.actionUrl && (
                  <a
                    className={styles.actionLink}
                    onClick={(e) => {
                      e.stopPropagation();
                      navigate(metric.actionUrl!);
                    }}
                  >
                    {metric.actionText}
                  </a>
                )}
              </div>
            ))}
          </div>
        </div>
      </div>
    </>
  );
};

export default HeroOverview;
