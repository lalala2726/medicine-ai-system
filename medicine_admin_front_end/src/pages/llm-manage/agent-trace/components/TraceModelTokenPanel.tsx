import { Typography } from 'antd';
import React from 'react';
import styles from '../index.module.less';
import {
  formatModelTokenNumber,
  resolveModelTokenPercent,
  type ModelTokenUsageSummary,
} from './traceModelUtils';

const { Text } = Typography;

export interface TraceModelTokenPanelProps {
  /** Token 用量摘要。 */
  summary: ModelTokenUsageSummary;
}

/**
 * 渲染模型 Token 指标块。
 *
 * @param label 指标名称。
 * @param value 指标数值。
 * @returns Token 指标节点。
 */
function renderModelTokenMetric(label: string, value: number) {
  return (
    <div className={styles.tokenMetric}>
      <Text type="secondary">{label}</Text>
      <Text strong className={styles.tokenMetricValue}>
        {formatModelTokenNumber(value)}
      </Text>
    </div>
  );
}

/**
 * 渲染模型 Token 占比行。
 *
 * @param label 指标名称。
 * @param value 当前 Token 数。
 * @param total 总 Token 数。
 * @returns Token 占比行节点。
 */
function renderModelTokenBreakdownRow(label: string, value: number, total: number) {
  const percent = resolveModelTokenPercent(value, total);
  return (
    <div className={styles.tokenBreakdownRow}>
      <Text type="secondary">{label}</Text>
      <div className={styles.tokenBreakdownBar}>
        <span style={{ width: `${percent}%` }} />
      </div>
      <Text className={styles.tokenBreakdownValue}>
        {formatModelTokenNumber(value)} / {percent.toFixed(1)}%
      </Text>
    </div>
  );
}

/**
 * 模型 Token 用量面板。
 *
 * @param props 组件属性。
 * @returns Token 用量面板节点。
 */
const TraceModelTokenPanel: React.FC<TraceModelTokenPanelProps> = ({ summary }) => (
  <div className={styles.modelPanelContent}>
    <div className={styles.tokenMetricGrid}>
      {renderModelTokenMetric('输入 Token', summary.inputTokens)}
      {renderModelTokenMetric('输出 Token', summary.outputTokens)}
      {renderModelTokenMetric('总 Token', summary.totalTokens)}
    </div>
    <div className={styles.tokenBreakdown}>
      <Text strong>Token 占比</Text>
      {renderModelTokenBreakdownRow('输入', summary.inputTokens, summary.totalTokens)}
      {renderModelTokenBreakdownRow('输出', summary.outputTokens, summary.totalTokens)}
    </div>
  </div>
);

export default TraceModelTokenPanel;
