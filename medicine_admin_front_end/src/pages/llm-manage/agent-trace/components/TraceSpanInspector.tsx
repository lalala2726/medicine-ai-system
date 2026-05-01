import { Empty } from 'antd';
import React, { useMemo } from 'react';
import type { AgentTraceTypes } from '@/api/agent/trace';
import styles from '../index.module.less';
import TraceAnchorDetailLayout from './TraceAnchorDetailLayout';
import TraceModelInspector from './TraceModelInspector';
import TraceModelTokenPanel from './TraceModelTokenPanel';
import TraceNodeInspector from './TraceNodeInspector';
import TraceSpanIdentity from './TraceSpanIdentity';
import { renderJsonPayload } from './traceShared';

export interface TraceSpanInspectorProps {
  /** 当前选中的 Span。 */
  selectedSpan: AgentTraceTypes.SpanVo | null;
  /** 当前选中的树节点。 */
  selectedTreeNode?: AgentTraceTypes.SpanTreeNode | null;
  /** 点击内部执行步骤时选中真实 Span 的回调。 */
  onSelectSpanId?: (spanId?: string | null) => void;
}

interface TokenUsageSummary {
  /** 输入 Token 数。 */
  inputTokens: number;
  /** 输出 Token 数。 */
  outputTokens: number;
  /** 总 Token 数。 */
  totalTokens: number;
}

/**
 * 从 Token 用量中读取数字。
 *
 * @param tokenUsage Token 用量对象。
 * @param key Token 字段名。
 * @returns 数字；不存在或无法转换时返回 0。
 */
function readTokenNumber(tokenUsage: AgentTraceTypes.SpanVo['tokenUsage'], key: string) {
  const value = tokenUsage?.[key];
  if (typeof value === 'number' && Number.isFinite(value)) {
    return Math.max(value, 0);
  }
  if (typeof value === 'string' && value.trim()) {
    const parsedValue = Number(value);
    return Number.isFinite(parsedValue) ? Math.max(parsedValue, 0) : 0;
  }
  return 0;
}

/**
 * 解析 Token 用量摘要。
 *
 * @param tokenUsage Token 用量对象。
 * @returns Token 用量摘要。
 */
function resolveTokenUsageSummary(
  tokenUsage: AgentTraceTypes.SpanVo['tokenUsage'],
): TokenUsageSummary {
  const inputTokens =
    readTokenNumber(tokenUsage, 'input_tokens') || readTokenNumber(tokenUsage, 'inputTokens');
  const outputTokens =
    readTokenNumber(tokenUsage, 'output_tokens') || readTokenNumber(tokenUsage, 'outputTokens');
  const explicitTotalTokens =
    readTokenNumber(tokenUsage, 'total_tokens') || readTokenNumber(tokenUsage, 'totalTokens');
  const totalTokens = explicitTotalTokens || inputTokens + outputTokens;
  return {
    inputTokens,
    outputTokens,
    totalTokens,
  };
}

/**
 * 判断是否需要展示 Token Tab。
 *
 * @param summary Token 用量摘要。
 * @returns 有有效 Token 时返回 true。
 */
function shouldShowTokenTab(summary: TokenUsageSummary) {
  return summary.inputTokens > 0 || summary.outputTokens > 0 || summary.totalTokens > 0;
}

/**
 * 判断错误载荷是否存在。
 *
 * @param errorPayload 错误载荷。
 * @returns 存在错误时返回 true。
 */
function hasErrorPayload(errorPayload: unknown) {
  return errorPayload !== undefined && errorPayload !== null && errorPayload !== '';
}

/**
 * Trace Span 详情面板。
 *
 * @param props Span 详情属性。
 * @returns Span 详情节点。
 */
const TraceSpanInspector: React.FC<TraceSpanInspectorProps> = ({
  selectedSpan,
  selectedTreeNode,
  onSelectSpanId,
}) => {
  const tokenUsageSummary = useMemo(
    () => resolveTokenUsageSummary(selectedSpan?.tokenUsage),
    [selectedSpan?.tokenUsage],
  );
  const showTokenTab = shouldShowTokenTab(tokenUsageSummary);

  if (!selectedSpan) {
    return (
      <div className={styles.emptyInspector}>
        <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="请选择一个 Span" />
      </div>
    );
  }

  if (selectedSpan.modelDetail) {
    return <TraceModelInspector selectedSpan={selectedSpan} />;
  }

  if (selectedSpan.spanType === 'node' && selectedSpan.nodeDetail) {
    return (
      <TraceNodeInspector
        selectedSpan={selectedSpan}
        selectedTreeNode={selectedTreeNode}
        onSelectSpanId={onSelectSpanId}
      />
    );
  }

  return (
    <TraceAnchorDetailLayout
      resetKey={selectedSpan.spanId}
      header={
        <div className={styles.spanInspectorHeader}>
          <TraceSpanIdentity
            detailSpan={selectedSpan}
            span={selectedTreeNode || selectedSpan}
            variant="detail"
            showSpanId
          />
        </div>
      }
      sections={[
        {
          key: 'token',
          label: '用量',
          visible: showTokenTab,
          render: () => <TraceModelTokenPanel summary={tokenUsageSummary} />,
        },
        {
          key: 'input',
          label: '输入',
          render: () => (
            <div className={styles.modelJsonBlock}>
              {renderJsonPayload(selectedSpan.inputPayload)}
            </div>
          ),
        },
        {
          key: 'output',
          label: '输出',
          render: () => (
            <div className={styles.modelJsonBlock}>
              {renderJsonPayload(selectedSpan.outputPayload)}
            </div>
          ),
        },
        {
          key: 'attributes',
          label: '属性',
          render: () => (
            <div className={styles.modelJsonBlock}>
              {renderJsonPayload(selectedSpan.attributes)}
            </div>
          ),
        },
        {
          key: 'error',
          label: '错误',
          visible: hasErrorPayload(selectedSpan.errorPayload),
          render: () => (
            <div className={styles.modelJsonBlock}>
              {renderJsonPayload(selectedSpan.errorPayload)}
            </div>
          ),
        },
      ]}
    />
  );
};

export default TraceSpanInspector;
