import { Popover, Space, Typography } from 'antd';
import { Bot, GitBranch, Network, Puzzle, Wrench, type LucideIcon } from 'lucide-react';
import React from 'react';
import type { AgentTraceTypes } from '@/api/agent/trace';
import styles from '../index.module.less';
import {
  renderDuration,
  renderSpanTypeTag,
  renderStatusTag,
  resolveSpanDisplayName,
  type TraceIdentityNode,
} from './traceShared';

const { Text } = Typography;

/** Span 图标配置。 */
const SPAN_ICON_META: Record<string, { icon: LucideIcon; className: string }> = {
  graph: { icon: GitBranch, className: styles.spanIconGraph },
  node: { icon: Network, className: styles.spanIconNode },
  middleware: { icon: Puzzle, className: styles.spanIconMiddleware },
  model: { icon: Bot, className: styles.spanIconModel },
  tool: { icon: Wrench, className: styles.spanIconTool },
};

export interface TraceSpanIdentityProps {
  /** Span 明细。 */
  span: TraceIdentityNode;
  /** 真实 Span 明细；树形虚拟节点用它补充 hover 详情。 */
  detailSpan?: AgentTraceTypes.SpanVo;
  /** 展示密度。 */
  variant?: 'tree' | 'detail';
  /** 是否展示状态和类型标签。 */
  showMeta?: boolean;
  /** 是否展示 Span ID。 */
  showSpanId?: boolean;
}

/**
 * 解析 Span 图标元信息。
 *
 * @param spanType Span 类型。
 * @returns Span 图标元信息。
 */
function resolveSpanIconMeta(spanType?: string) {
  return SPAN_ICON_META[spanType || ''] || SPAN_ICON_META.node;
}

/**
 * 判断当前节点是否是树形分组节点。
 *
 * @param span Span 或树节点。
 * @returns 是分组节点返回 true。
 */
function isTreeGroupNode(span: TraceIdentityNode) {
  if ('nodeType' in span && span.nodeType === 'span_group') {
    return true;
  }
  return 'children' in span && Boolean(span.children?.length) && span.spanType === 'model';
}

/**
 * 解析树形分组节点名称。
 *
 * @param span Span 或树节点。
 * @returns 分组节点展示名称。
 */
function resolveGroupDisplayName(span: TraceIdentityNode) {
  if (span.spanType === 'model') {
    return span.name || 'model';
  }
  if (span.name === 'tools') {
    return '工具';
  }
  return span.name || ('displayName' in span ? span.displayName : undefined) || '-';
}

/**
 * 从 Token 用量中读取数字。
 *
 * @param tokenUsage Token 用量对象。
 * @param key Token 字段名。
 * @returns 数字；不存在或无法转换时返回 null。
 */
function readTokenNumber(tokenUsage: AgentTraceTypes.SpanVo['tokenUsage'], key: string) {
  const value = tokenUsage?.[key];
  if (typeof value === 'number' && Number.isFinite(value)) {
    return value;
  }
  if (typeof value === 'string' && value.trim()) {
    const parsedValue = Number(value);
    return Number.isFinite(parsedValue) ? parsedValue : null;
  }
  return null;
}

/**
 * 格式化普通数值。
 *
 * @param value 原始数值。
 * @returns 展示文本。
 */
function formatNumberText(value?: number | null) {
  return typeof value === 'number' && Number.isFinite(value) ? value.toLocaleString() : '-';
}

/**
 * 格式化时间文本。
 *
 * @param value 原始时间文本。
 * @returns 展示文本；不存在时返回 null。
 */
function formatDateTimeText(value?: string) {
  return value?.trim() || null;
}

/**
 * 判断 Token 数是否有展示价值。
 *
 * @param value Token 数。
 * @returns 大于 0 时返回 true。
 */
function hasTokenValue(value?: number | null) {
  return typeof value === 'number' && Number.isFinite(value) && value > 0;
}

/**
 * 判断当前 Span 是否需要展示 Token 信息。
 *
 * @param inputTokens 输入 Token 数。
 * @param outputTokens 输出 Token 数。
 * @param totalTokens 总 Token 数。
 * @returns 存在有效 Token 时返回 true。
 */
function shouldShowTokenUsage(
  inputTokens?: number | null,
  outputTokens?: number | null,
  totalTokens?: number | null,
) {
  return [inputTokens, outputTokens, totalTokens].some(hasTokenValue);
}

/**
 * 解析 hover 展示使用的 Span。
 *
 * @param span 当前展示节点。
 * @param detailSpan 真实 Span 明细。
 * @returns hover 使用的 Span 明细。
 */
function resolvePopoverSpan(span: TraceIdentityNode, detailSpan?: AgentTraceTypes.SpanVo) {
  return detailSpan || (span as AgentTraceTypes.SpanVo);
}

/**
 * 渲染 hover 信息中的一行。
 *
 * @param label 标签。
 * @param value 内容。
 * @returns 信息行节点。
 */
function renderPopoverRow(label: string, value: React.ReactNode) {
  return (
    <div className={styles.spanPopoverRow}>
      <Text type="secondary">{label}</Text>
      <Text className={styles.spanPopoverValue}>{value}</Text>
    </div>
  );
}

/**
 * 渲染 Span hover 基础信息。
 *
 * @param span 当前展示节点。
 * @param displayName 当前展示名称。
 * @param detailSpan 真实 Span 明细。
 * @returns hover 信息节点。
 */
function renderSpanPopoverContent(
  span: TraceIdentityNode,
  displayName: string,
  detailSpan?: AgentTraceTypes.SpanVo,
) {
  const popoverSpan = resolvePopoverSpan(span, detailSpan);
  const inputTokens = readTokenNumber(popoverSpan.tokenUsage, 'input_tokens');
  const outputTokens = readTokenNumber(popoverSpan.tokenUsage, 'output_tokens');
  const totalTokens = readTokenNumber(popoverSpan.tokenUsage, 'total_tokens');
  const startedAtText = formatDateTimeText(popoverSpan.startedAt);
  const endedAtText = formatDateTimeText(popoverSpan.endedAt);
  const showTokenUsage = shouldShowTokenUsage(inputTokens, outputTokens, totalTokens);

  return (
    <div className={styles.spanPopover}>
      <div className={styles.spanPopoverTitle}>
        <Text strong>{displayName}</Text>
        {renderStatusTag(popoverSpan.status || span.status)}
      </div>
      <div className={styles.spanPopoverSection}>
        {startedAtText ? renderPopoverRow('开始时间', startedAtText) : null}
        {endedAtText ? renderPopoverRow('结束时间', endedAtText) : null}
        {renderPopoverRow('耗时', renderDuration(popoverSpan.durationMs ?? span.durationMs))}
      </div>
      {showTokenUsage ? (
        <div className={styles.spanPopoverSection}>
          <Text strong>Token 用量</Text>
          {hasTokenValue(inputTokens)
            ? renderPopoverRow('输入', formatNumberText(inputTokens))
            : null}
          {hasTokenValue(outputTokens)
            ? renderPopoverRow('输出', formatNumberText(outputTokens))
            : null}
          {hasTokenValue(totalTokens)
            ? renderPopoverRow('总计', formatNumberText(totalTokens))
            : null}
        </div>
      ) : null}
    </div>
  );
}

/**
 * Trace Span 身份展示组件。
 *
 * @param props Span 身份展示属性。
 * @returns Span 身份展示节点。
 */
const TraceSpanIdentity: React.FC<TraceSpanIdentityProps> = ({
  span,
  detailSpan,
  variant = 'tree',
  showMeta = true,
  showSpanId = false,
}) => {
  const iconMeta = resolveSpanIconMeta(span.spanType);
  const IconComponent = iconMeta.icon;
  const isDetail = variant === 'detail';
  const isGroupNode = isTreeGroupNode(span);
  const displayName = isGroupNode ? resolveGroupDisplayName(span) : resolveSpanDisplayName(span);
  const tokenText = 'tokenText' in span ? span.tokenText : undefined;
  const sourceSpanId =
    (span as AgentTraceTypes.SpanVo).spanId || (span as AgentTraceTypes.SpanTreeNode).sourceSpanId;

  const identityNode = (
    <div
      className={[
        isDetail ? styles.spanIdentityDetail : styles.spanIdentityTree,
        isGroupNode && !isDetail ? styles.spanIdentityGroup : '',
      ]
        .filter(Boolean)
        .join(' ')}
    >
      <span className={`${styles.spanIconBadge} ${iconMeta.className}`}>
        <IconComponent size={isDetail ? 16 : 14} strokeWidth={2} />
      </span>
      <div className={styles.spanIdentityContent}>
        <div className={styles.spanIdentityMain}>
          <Text
            strong={isDetail || isGroupNode}
            ellipsis={{ tooltip: false }}
            className={styles.spanIdentityName}
          >
            {displayName}
          </Text>
          {!isGroupNode && span.durationMs !== undefined && span.durationMs !== null ? (
            <Text type="secondary" className={styles.spanIdentityDuration}>
              {renderDuration(span.durationMs)}
            </Text>
          ) : null}
          {!isGroupNode && tokenText ? (
            <Text className={styles.spanTokenTag}>{tokenText}</Text>
          ) : null}
        </div>
        {showMeta && !isGroupNode ? (
          <Space size={6} wrap className={styles.spanIdentityMeta}>
            {renderSpanTypeTag(span.spanType)}
            {renderStatusTag(span.status)}
            {showSpanId && sourceSpanId ? (
              <Text copyable type="secondary" className={styles.spanIdText}>
                {sourceSpanId}
              </Text>
            ) : null}
          </Space>
        ) : null}
      </div>
    </div>
  );

  return (
    <Popover
      mouseEnterDelay={0.2}
      overlayClassName={styles.spanPopoverOverlay}
      placement={isDetail ? 'bottomLeft' : 'right'}
      content={renderSpanPopoverContent(span, displayName, detailSpan)}
    >
      {identityNode}
    </Popover>
  );
};

export default TraceSpanIdentity;
