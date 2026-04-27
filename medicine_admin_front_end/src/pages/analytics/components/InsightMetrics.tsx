import React, { useMemo } from 'react';
import { Typography } from 'antd';
import { ProCard } from '@ant-design/pro-components';
import { formatNumber, formatPercent, formatHours } from '../utils';
import styles from '../index.module.less';

const { Text } = Typography;

interface InsightMetricsProps {
  conversionSummary?: any;
  fulfillmentSummary?: any;
  afterSaleEfficiencySummary?: any;
}

const InsightMetrics: React.FC<InsightMetricsProps> = ({
  conversionSummary,
  fulfillmentSummary,
  afterSaleEfficiencySummary,
}) => {
  const conversionMetrics = useMemo(
    () => [
      {
        key: 'createdOrderCount',
        title: '下单订单数',
        value: formatNumber(conversionSummary?.createdOrderCount),
        hint: '按下单时间统计同一批订单',
      },
      {
        key: 'paidOrderCount',
        title: '已支付订单',
        value: formatNumber(conversionSummary?.paidOrderCount),
        hint: '同一批订单最终支付成功数量',
      },
      {
        key: 'paymentConversionRate',
        title: '支付转化率',
        value: formatPercent(conversionSummary?.paymentConversionRate),
        hint: '已支付订单 / 下单订单',
      },
      {
        key: 'pendingPaymentOrderCount',
        title: '待支付订单',
        value: formatNumber(conversionSummary?.pendingPaymentOrderCount),
        hint: '当前仍停留在待支付的订单',
      },
      {
        key: 'closedOrderCount',
        title: '已关闭订单',
        value: formatNumber(conversionSummary?.closedOrderCount),
        hint: '状态为已取消或已过期',
      },
    ],
    [conversionSummary],
  );

  const fulfillmentMetrics = useMemo(
    () => [
      {
        key: 'averageShipmentHours',
        title: '平均发货时长',
        value: formatHours(fulfillmentSummary?.averageShipmentHours),
        hint: '已发货订单：支付到发货',
      },
      {
        key: 'averageReceiptHours',
        title: '平均收货时长',
        value: formatHours(fulfillmentSummary?.averageReceiptHours),
        hint: '已收货订单：发货到收货',
      },
      {
        key: 'overdueShipmentOrderCount',
        title: '超24h未发货',
        value: formatNumber(fulfillmentSummary?.overdueShipmentOrderCount),
        hint: '已支付且仍待发货',
      },
      {
        key: 'overdueReceiptOrderCount',
        title: '超7天未收货',
        value: formatNumber(fulfillmentSummary?.overdueReceiptOrderCount),
        hint: '已发货且仍待收货',
      },
    ],
    [fulfillmentSummary],
  );

  const afterSaleEfficiencyMetrics = useMemo(
    () => [
      {
        key: 'averageAuditHours',
        title: '平均审核时长',
        value: formatHours(afterSaleEfficiencySummary?.averageAuditHours),
        hint: '售后申请到管理员审核',
      },
      {
        key: 'averageCompleteHours',
        title: '平均完结时长',
        value: formatHours(afterSaleEfficiencySummary?.averageCompleteHours),
        hint: '售后申请到最终完结',
      },
      {
        key: 'overdueAuditCount',
        title: '超24h未审核',
        value: formatNumber(afterSaleEfficiencySummary?.overdueAuditCount),
        hint: '状态仍为待审核',
      },
      {
        key: 'overdueCompleteCount',
        title: '超72h未完结',
        value: formatNumber(afterSaleEfficiencySummary?.overdueCompleteCount),
        hint: '已通过或处理中但未完结',
      },
    ],
    [afterSaleEfficiencySummary],
  );

  return (
    <div className={styles.insightSection} style={{ marginBottom: 16 }}>
      <ProCard
        className={styles.mainCard}
        headerBordered
        title="支付转化概览"
        extra={<Text type="secondary">按下单时间统计</Text>}
      >
        <div className={styles.insightList}>
          {conversionMetrics.map((item) => (
            <div key={item.key} className={styles.insightRow}>
              <div className={styles.insightInfo}>
                <Text className={styles.insightLabel}>{item.title}</Text>
                <Text type="secondary" className={styles.insightHint}>
                  {item.hint}
                </Text>
              </div>
              <span className={styles.insightValue}>{item.value}</span>
            </div>
          ))}
        </div>
      </ProCard>

      <ProCard
        className={styles.mainCard}
        headerBordered
        title="履约时效"
        extra={<Text type="secondary">超时阈值固定</Text>}
      >
        <div className={styles.insightList}>
          {fulfillmentMetrics.map((item) => (
            <div key={item.key} className={styles.insightRow}>
              <div className={styles.insightInfo}>
                <Text className={styles.insightLabel}>{item.title}</Text>
                <Text type="secondary" className={styles.insightHint}>
                  {item.hint}
                </Text>
              </div>
              <span className={styles.insightValue}>{item.value}</span>
            </div>
          ))}
        </div>
      </ProCard>

      <ProCard
        className={styles.mainCard}
        headerBordered
        title="售后处理时效"
        extra={<Text type="secondary">审核与完结效率</Text>}
      >
        <div className={styles.insightList}>
          {afterSaleEfficiencyMetrics.map((item) => (
            <div key={item.key} className={styles.insightRow}>
              <div className={styles.insightInfo}>
                <Text className={styles.insightLabel}>{item.title}</Text>
                <Text type="secondary" className={styles.insightHint}>
                  {item.hint}
                </Text>
              </div>
              <span className={styles.insightValue}>{item.value}</span>
            </div>
          ))}
        </div>
      </ProCard>
    </div>
  );
};

export default InsightMetrics;
