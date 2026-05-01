import { Empty, Space, Tag, Typography } from 'antd';
import { Bot } from 'lucide-react';
import React, { useMemo } from 'react';
import type { AgentTraceTypes } from '@/api/agent/trace';
import styles from '../index.module.less';
import TraceAnchorDetailLayout from './TraceAnchorDetailLayout';
import TraceModelAttributesPanel from './TraceModelAttributesPanel';
import TraceModelMessagesPanel from './TraceModelMessagesPanel';
import TraceModelTokenPanel from './TraceModelTokenPanel';
import TraceModelToolsPanel from './TraceModelToolsPanel';
import { renderDuration, renderStatusTag } from './traceShared';
import {
  formatModelTokenNumber,
  hasModelTokenUsage,
  resolveModelTokenUsageSummary,
  type ModelTokenUsageSummary,
} from './traceModelUtils';

const { Text } = Typography;

interface TraceModelInspectorProps {
  /** 当前选中的模型 Span。 */
  selectedSpan: AgentTraceTypes.SpanVo;
}

/**
 * 渲染模型顶部指标。
 *
 * @param label 指标名称。
 * @param value 指标内容。
 * @returns 指标节点。
 */
function renderModelHeaderMetric(label: string, value: React.ReactNode) {
  return (
    <div className={styles.modelHeaderMetric}>
      <Text type="secondary">{label}</Text>
      <Text strong>{value}</Text>
    </div>
  );
}

/**
 * 渲染模型 Token 顶部指标。
 *
 * @param summary Token 用量摘要。
 * @returns Token 指标节点。
 */
function renderModelHeaderTokenMetrics(summary: ModelTokenUsageSummary) {
  if (!hasModelTokenUsage(summary)) {
    return null;
  }
  return (
    <>
      {renderModelHeaderMetric('输入 Token', formatModelTokenNumber(summary.inputTokens))}
      {renderModelHeaderMetric('输出 Token', formatModelTokenNumber(summary.outputTokens))}
      {renderModelHeaderMetric('总 Token', formatModelTokenNumber(summary.totalTokens))}
    </>
  );
}

/**
 * 模型调用结构化详情组件。
 *
 * @param props 组件属性。
 * @returns 模型调用详情节点。
 */
const TraceModelInspector: React.FC<TraceModelInspectorProps> = ({ selectedSpan }) => {
  const modelDetail = selectedSpan.modelDetail;
  const tokenUsageSummary = useMemo(
    () => resolveModelTokenUsageSummary(modelDetail?.tokenUsage || selectedSpan.tokenUsage),
    [modelDetail?.tokenUsage, selectedSpan.tokenUsage],
  );
  const showTokenPanel = hasModelTokenUsage(tokenUsageSummary);

  if (!modelDetail) {
    return (
      <div className={styles.emptyInspector}>
        <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无模型详情" />
      </div>
    );
  }

  return (
    <TraceAnchorDetailLayout
      resetKey={selectedSpan.spanId}
      header={
        <div className={styles.modelHeader}>
          <div className={styles.modelHeaderTitle}>
            <span className={styles.modelHeaderIcon}>
              <Bot size={18} />
            </span>
            <div className={styles.modelHeaderName}>
              <Text strong>{modelDetail.modelName || selectedSpan.name || '-'}</Text>
              <Space size={6} wrap>
                {renderStatusTag(selectedSpan.status)}
                {modelDetail.modelClass ? <Tag>{modelDetail.modelClass}</Tag> : null}
                {modelDetail.slot ? <Tag color="blue">{modelDetail.slot}</Tag> : null}
                {modelDetail.finishReason ? (
                  <Tag color="geekblue">{modelDetail.finishReason}</Tag>
                ) : null}
              </Space>
            </div>
          </div>
          <div className={styles.modelHeaderMetrics}>
            {renderModelHeaderMetric('耗时', renderDuration(selectedSpan.durationMs))}
            {renderModelHeaderTokenMetrics(tokenUsageSummary)}
          </div>
        </div>
      }
      sections={[
        {
          key: 'token',
          label: '用量',
          visible: showTokenPanel,
          render: () => <TraceModelTokenPanel summary={tokenUsageSummary} />,
        },
        {
          key: 'tools',
          label: '工具',
          render: () => (
            <TraceModelToolsPanel
              tools={modelDetail.availableTools}
              toolCalls={modelDetail.toolCalls}
            />
          ),
        },
        {
          key: 'input',
          label: '输入',
          render: () => (
            <TraceModelMessagesPanel
              mode="input"
              systemPrompt={modelDetail.systemPrompt}
              messages={modelDetail.inputMessages}
            />
          ),
        },
        {
          key: 'output',
          label: '输出',
          render: () => (
            <TraceModelMessagesPanel
              mode="output"
              messages={modelDetail.outputMessages}
              finalText={modelDetail.finalText}
            />
          ),
        },
        {
          key: 'attributes',
          label: '属性',
          render: () => (
            <TraceModelAttributesPanel selectedSpan={selectedSpan} modelDetail={modelDetail} />
          ),
        },
      ]}
    />
  );
};

export default TraceModelInspector;
