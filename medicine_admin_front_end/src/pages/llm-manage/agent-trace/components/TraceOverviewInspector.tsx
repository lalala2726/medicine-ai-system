import { Empty, Space, Tag, Typography } from 'antd';
import { GitBranch } from 'lucide-react';
import React, { useMemo } from 'react';
import type { AgentTraceTypes } from '@/api/agent/trace';
import styles from '../index.module.less';
import TraceAnchorDetailLayout from './TraceAnchorDetailLayout';
import TraceModelMessagesPanel from './TraceModelMessagesPanel';
import TraceModelTokenPanel from './TraceModelTokenPanel';
import TraceToolCallCard from './TraceToolCallCard';
import { renderDuration, renderJsonPayload, renderStatusTag } from './traceShared';
import {
  formatModelTokenNumber,
  hasModelTokenUsage,
  resolveModelTokenUsageSummary,
} from './traceModelUtils';

const { Text } = Typography;

export interface TraceOverviewInspectorProps {
  /** Trace 详情。 */
  detail: AgentTraceTypes.DetailVo;
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
 * 渲染顶层概览指标。
 *
 * @param label 指标名称。
 * @param value 指标内容。
 * @returns 指标节点。
 */
function renderOverviewHeaderMetric(label: string, value: React.ReactNode) {
  return (
    <div className={styles.modelHeaderMetric}>
      <Text type="secondary">{label}</Text>
      <Text strong>{value}</Text>
    </div>
  );
}

/**
 * 判断顶层输出是否存在可展示内容。
 *
 * @param output 顶层输出详情。
 * @returns 存在最终回复或工具调用时返回 true。
 */
function hasOverviewOutputContent(output?: AgentTraceTypes.OverviewOutputVo | null) {
  return Boolean(output?.finalText?.trim() || output?.toolCalls?.length);
}

/**
 * 渲染顶层输出区。
 *
 * @param output 顶层输出详情。
 * @returns 输出区节点。
 */
function renderOverviewOutput(output?: AgentTraceTypes.OverviewOutputVo | null) {
  if (!hasOverviewOutputContent(output)) {
    return <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无输出" />;
  }
  const toolCalls = output?.toolCalls || [];
  return (
    <div className={styles.modelPanelContent}>
      {output?.finalText ? (
        <section className={styles.modelPlainSection}>
          <div className={styles.modelPlainSectionHeader}>
            <Text strong>最终回复</Text>
            <Text copyable={{ text: output.finalText }} type="secondary">
              复制原文
            </Text>
          </div>
          <div className={styles.modelFinalText}>{output.finalText}</div>
        </section>
      ) : null}
      {toolCalls.length ? (
        <section className={styles.modelPlainSection}>
          <div className={styles.modelPlainSectionHeader}>
            <Text strong>工具调用</Text>
            <Text type="secondary">{toolCalls.length.toLocaleString()} 次</Text>
          </div>
          <div className={styles.modelToolCalls}>
            {toolCalls.map((call, index) => (
              <TraceToolCallCard key={call.id || `${call.name}-${index}`} call={call} />
            ))}
          </div>
        </section>
      ) : null}
    </div>
  );
}

/**
 * 顶层 Trace 概览详情组件。
 *
 * @param props 组件属性。
 * @returns 顶层 Trace 概览详情节点。
 */
const TraceOverviewInspector: React.FC<TraceOverviewInspectorProps> = ({ detail }) => {
  const overviewDetail = detail.overviewDetail;
  const tokenUsageSummary = useMemo(
    () => resolveModelTokenUsageSummary(overviewDetail?.tokenUsage),
    [overviewDetail?.tokenUsage],
  );
  const showTokenUsage = hasModelTokenUsage(tokenUsageSummary);

  if (!overviewDetail) {
    return (
      <div className={styles.emptyInspector}>
        <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="当前 Trace 未写入顶层概览数据" />
      </div>
    );
  }

  return (
    <TraceAnchorDetailLayout
      resetKey={overviewDetail.spanId || detail.traceId}
      header={
        <div className={styles.modelHeader}>
          <div className={styles.modelHeaderTitle}>
            <span className={styles.overviewHeaderIcon}>
              <GitBranch size={18} />
            </span>
            <div className={styles.modelHeaderName}>
              <Text strong>{overviewDetail.name || detail.graphName || '-'}</Text>
              <Space size={6} wrap>
                {renderStatusTag(overviewDetail.status)}
                {detail.conversationType ? <Tag>{detail.conversationType}</Tag> : null}
                {detail.entrypoint ? <Tag color="blue">{detail.entrypoint}</Tag> : null}
              </Space>
            </div>
          </div>
          <div className={styles.modelHeaderMetrics}>
            {renderOverviewHeaderMetric('耗时', renderDuration(overviewDetail.durationMs))}
            {showTokenUsage ? (
              <>
                {renderOverviewHeaderMetric(
                  '输入 Token',
                  formatModelTokenNumber(tokenUsageSummary.inputTokens),
                )}
                {renderOverviewHeaderMetric(
                  '输出 Token',
                  formatModelTokenNumber(tokenUsageSummary.outputTokens),
                )}
                {renderOverviewHeaderMetric(
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
          key: 'token',
          label: '用量',
          visible: showTokenUsage,
          render: () => <TraceModelTokenPanel summary={tokenUsageSummary} />,
        },
        {
          key: 'input',
          label: '输入',
          render: () => (
            <TraceModelMessagesPanel
              mode="input"
              systemPrompt={overviewDetail.input?.systemPrompt}
              messages={overviewDetail.input?.messages}
            />
          ),
        },
        {
          key: 'output',
          label: '输出',
          render: () => renderOverviewOutput(overviewDetail.output),
        },
        {
          key: 'attributes',
          label: '属性',
          render: () => (
            <div className={styles.modelJsonBlock}>
              {renderJsonPayload(overviewDetail.attributes)}
            </div>
          ),
        },
        {
          key: 'error',
          label: '错误',
          visible: hasErrorPayload(overviewDetail.errorPayload),
          render: () => (
            <div className={styles.modelJsonBlock}>
              {renderJsonPayload(overviewDetail.errorPayload)}
            </div>
          ),
        },
      ]}
    />
  );
};

export default TraceOverviewInspector;
