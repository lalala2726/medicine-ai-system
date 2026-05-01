import { Empty, Space, Typography } from 'antd';
import { Bot, Network, Puzzle, Wrench, type LucideIcon } from 'lucide-react';
import React, { useMemo } from 'react';
import type { AgentTraceTypes } from '@/api/agent/trace';
import styles from '../index.module.less';
import TraceAnchorDetailLayout from './TraceAnchorDetailLayout';
import TraceModelTokenPanel from './TraceModelTokenPanel';
import TraceSpanIdentity from './TraceSpanIdentity';
import {
  renderDuration,
  renderJsonPayload,
  renderSpanTypeTag,
  renderStatusTag,
} from './traceShared';
import {
  formatModelTokenNumber,
  hasModelTokenUsage,
  resolveModelTokenUsageSummary,
} from './traceModelUtils';

const { Text } = Typography;

/** 节点执行步骤图标配置。 */
const NODE_STEP_ICON_META: Record<string, { icon: LucideIcon; className: string }> = {
  node: { icon: Network, className: styles.spanIconNode },
  model: { icon: Bot, className: styles.spanIconModel },
  middleware: { icon: Puzzle, className: styles.spanIconMiddleware },
  tool: { icon: Wrench, className: styles.spanIconTool },
};

export interface TraceNodeInspectorProps {
  /** 当前选中的节点 Span。 */
  selectedSpan: AgentTraceTypes.SpanVo;
  /** 当前选中的树节点。 */
  selectedTreeNode?: AgentTraceTypes.SpanTreeNode | null;
  /** 点击执行步骤时选中真实 Span 的回调。 */
  onSelectSpanId?: (spanId?: string | null) => void;
}

/**
 * 读取节点摘要数字。
 *
 * @param value 原始数字。
 * @returns 可展示数字。
 */
function resolveSummaryNumber(value?: number) {
  return typeof value === 'number' && Number.isFinite(value) ? value : 0;
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
 * 渲染节点头部指标。
 *
 * @param label 指标名称。
 * @param value 指标内容。
 * @returns 指标节点。
 */
function renderNodeHeaderMetric(label: string, value: React.ReactNode) {
  return (
    <div className={styles.modelHeaderMetric}>
      <Text type="secondary">{label}</Text>
      <Text strong>{value}</Text>
    </div>
  );
}

/**
 * 渲染节点摘要指标。
 *
 * @param label 指标名称。
 * @param value 指标数字。
 * @returns 摘要指标节点。
 */
function renderNodeSummaryMetric(label: string, value?: number) {
  return (
    <div className={styles.nodeSummaryMetric}>
      <Text type="secondary">{label}</Text>
      <Text strong>{resolveSummaryNumber(value).toLocaleString()}</Text>
    </div>
  );
}

/**
 * 渲染节点概览区。
 *
 * @param nodeDetail 节点详情。
 * @returns 节点概览节点。
 */
function renderNodeOverview(nodeDetail: AgentTraceTypes.NodeDetailVo) {
  const childSummary = nodeDetail.childSummary || {};
  return (
    <div className={styles.modelPanelContent}>
      <div className={styles.nodeSummaryGrid}>
        {renderNodeSummaryMetric('模型调用', childSummary.modelCount)}
        {renderNodeSummaryMetric('工具调用', childSummary.toolCount)}
        {renderNodeSummaryMetric('中间件', childSummary.middlewareCount)}
        {renderNodeSummaryMetric('异常步骤', childSummary.errorCount)}
      </div>
      <div className={styles.nodeTimeGrid}>
        <div>
          <Text type="secondary">开始时间</Text>
          <Text>{nodeDetail.startedAt || '-'}</Text>
        </div>
        <div>
          <Text type="secondary">结束时间</Text>
          <Text>{nodeDetail.endedAt || '-'}</Text>
        </div>
      </div>
    </div>
  );
}

/**
 * 解析执行步骤图标元信息。
 *
 * @param spanType Span 类型。
 * @returns 图标元信息。
 */
function resolveStepIconMeta(spanType?: string) {
  return NODE_STEP_ICON_META[spanType || ''] || NODE_STEP_ICON_META.node;
}

/**
 * 渲染单个内部执行步骤。
 *
 * @param step 执行步骤。
 * @param onSelectSpanId 选中真实 Span 的回调。
 * @returns 执行步骤节点。
 */
function renderExecutionStep(
  step: AgentTraceTypes.NodeExecutionStepVo,
  onSelectSpanId?: (spanId?: string | null) => void,
) {
  const iconMeta = resolveStepIconMeta(step.spanType);
  const IconComponent = iconMeta.icon;
  return (
    <button
      key={`${step.spanId || step.sequence || step.name}`}
      type="button"
      className={styles.nodeExecutionStep}
      onClick={() => onSelectSpanId?.(step.spanId)}
    >
      <span className={`${styles.spanIconBadge} ${iconMeta.className}`}>
        <IconComponent size={14} />
      </span>
      <div className={styles.nodeExecutionStepMain}>
        <div className={styles.nodeExecutionStepTitle}>
          <Text strong ellipsis={{ tooltip: false }}>
            {step.displayName || step.name || '-'}
          </Text>
          <Space size={6} wrap>
            {renderSpanTypeTag(step.spanType)}
            {renderStatusTag(step.status)}
          </Space>
        </div>
        <div className={styles.nodeExecutionStepMeta}>
          <Text type="secondary">{renderDuration(step.durationMs)}</Text>
          {step.tokenText ? <Text className={styles.spanTokenTag}>{step.tokenText}</Text> : null}
        </div>
      </div>
    </button>
  );
}

/**
 * 渲染节点内部执行流。
 *
 * @param steps 执行步骤列表。
 * @param onSelectSpanId 选中真实 Span 的回调。
 * @returns 内部执行流节点。
 */
function renderExecutionSteps(
  steps?: AgentTraceTypes.NodeExecutionStepVo[],
  onSelectSpanId?: (spanId?: string | null) => void,
) {
  if (!steps?.length) {
    return <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无内部执行步骤" />;
  }
  return (
    <div className={styles.nodeExecutionList}>
      {steps.map((step) => renderExecutionStep(step, onSelectSpanId))}
    </div>
  );
}

/**
 * Agent Trace 节点详情组件。
 *
 * @param props 组件属性。
 * @returns 节点详情节点。
 */
const TraceNodeInspector: React.FC<TraceNodeInspectorProps> = ({
  selectedSpan,
  selectedTreeNode,
  onSelectSpanId,
}) => {
  const nodeDetail = selectedSpan.nodeDetail;
  const tokenUsageSummary = useMemo(
    () => resolveModelTokenUsageSummary(nodeDetail?.tokenUsage),
    [nodeDetail?.tokenUsage],
  );
  const showTokenUsage = hasModelTokenUsage(tokenUsageSummary);

  if (!nodeDetail) {
    return (
      <div className={styles.emptyInspector}>
        <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="当前节点未写入节点详情" />
      </div>
    );
  }

  return (
    <TraceAnchorDetailLayout
      resetKey={selectedSpan.spanId}
      header={
        <div className={styles.modelHeader}>
          <TraceSpanIdentity
            detailSpan={selectedSpan}
            span={selectedTreeNode || selectedSpan}
            variant="detail"
            showSpanId
          />
          <div className={styles.modelHeaderMetrics}>
            {renderNodeHeaderMetric('耗时', renderDuration(nodeDetail.durationMs))}
            {showTokenUsage ? (
              <>
                {renderNodeHeaderMetric(
                  '输入 Token',
                  formatModelTokenNumber(tokenUsageSummary.inputTokens),
                )}
                {renderNodeHeaderMetric(
                  '输出 Token',
                  formatModelTokenNumber(tokenUsageSummary.outputTokens),
                )}
                {renderNodeHeaderMetric(
                  '总 Token',
                  formatModelTokenNumber(tokenUsageSummary.totalTokens),
                )}
              </>
            ) : null}
          </div>
        </div>
      }
      sections={[
        {
          key: 'overview',
          label: '节点概览',
          render: () => renderNodeOverview(nodeDetail),
        },
        {
          key: 'token',
          label: '用量',
          visible: showTokenUsage,
          render: () => <TraceModelTokenPanel summary={tokenUsageSummary} />,
        },
        {
          key: 'input',
          label: '输入',
          render: () => (
            <div className={styles.modelJsonBlock}>
              {renderJsonPayload(nodeDetail.inputPayload)}
            </div>
          ),
        },
        {
          key: 'output',
          label: '输出',
          render: () => (
            <div className={styles.modelJsonBlock}>
              {renderJsonPayload(nodeDetail.outputPayload)}
            </div>
          ),
        },
        {
          key: 'steps',
          label: '内部执行流',
          render: () => renderExecutionSteps(nodeDetail.executionSteps, onSelectSpanId),
        },
        {
          key: 'error',
          label: '错误',
          visible: hasErrorPayload(nodeDetail.errorPayload),
          render: () => (
            <div className={styles.modelJsonBlock}>
              {renderJsonPayload(nodeDetail.errorPayload)}
            </div>
          ),
        },
      ]}
    />
  );
};

export default TraceNodeInspector;
