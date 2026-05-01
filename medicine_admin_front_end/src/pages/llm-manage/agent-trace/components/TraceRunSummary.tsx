import { Descriptions, Space, Typography } from 'antd';
import React from 'react';
import type { AgentTraceTypes } from '@/api/agent/trace';
import styles from '../index.module.less';
import { renderDuration, renderStatusTag } from './traceShared';

const { Text } = Typography;

export interface TraceRunSummaryProps {
  /** Trace 详情。 */
  detail: AgentTraceTypes.DetailVo;
}

/**
 * Trace 运行概览面板。
 *
 * @param props Trace 运行概览属性。
 * @returns Trace 运行概览节点。
 */
const TraceRunSummary: React.FC<TraceRunSummaryProps> = ({ detail }) => (
  <div className={styles.traceSummaryPanel}>
    <Descriptions bordered column={2} size="small">
      <Descriptions.Item label="Trace ID" span={2}>
        <Text copyable style={{ fontFamily: 'monospace' }}>
          {detail.traceId}
        </Text>
      </Descriptions.Item>
      <Descriptions.Item label="状态">{renderStatusTag(detail.status)}</Descriptions.Item>
      <Descriptions.Item label="耗时">{renderDuration(detail.durationMs)}</Descriptions.Item>
      <Descriptions.Item label="会话 UUID" span={2}>
        <Text copyable>{detail.conversationUuid || '-'}</Text>
      </Descriptions.Item>
      <Descriptions.Item label="AI 消息 UUID" span={2}>
        <Text copyable>{detail.assistantMessageUuid || '-'}</Text>
      </Descriptions.Item>
      <Descriptions.Item label="会话类型">{detail.conversationType || '-'}</Descriptions.Item>
      <Descriptions.Item label="用户 ID">{detail.userId ?? '-'}</Descriptions.Item>
      <Descriptions.Item label="Graph">{detail.graphName || '-'}</Descriptions.Item>
      <Descriptions.Item label="入口">{detail.entrypoint || '-'}</Descriptions.Item>
      <Descriptions.Item label="开始时间">{detail.startedAt || '-'}</Descriptions.Item>
      <Descriptions.Item label="结束时间">{detail.endedAt || '-'}</Descriptions.Item>
      <Descriptions.Item label="Token" span={2}>
        <Space split="/">
          <span>{detail.inputTokens ?? 0}</span>
          <span>{detail.outputTokens ?? 0}</span>
          <span>{detail.totalTokens ?? 0}</span>
        </Space>
      </Descriptions.Item>
    </Descriptions>
  </div>
);

export default TraceRunSummary;
