import { Empty, Tag, Typography } from 'antd';
import React from 'react';
import ReactJson from 'react-json-view';
import type { AgentTraceTypes } from '@/api/agent/trace';

const { Text } = Typography;

/** Trace 状态展示配置。 */
const TRACE_STATUS_META: Record<string, { text: string; color: string }> = {
  running: { text: '运行中', color: 'processing' },
  success: { text: '成功', color: 'success' },
  error: { text: '异常', color: 'error' },
  cancelled: { text: '已取消', color: 'warning' },
};

/** Span 类型展示配置。 */
const SPAN_TYPE_META: Record<string, { text: string; color: string }> = {
  graph: { text: 'Graph', color: 'blue' },
  node: { text: 'Node', color: 'cyan' },
  middleware: { text: 'Middleware', color: 'purple' },
  model: { text: 'Model', color: 'orange' },
  tool: { text: 'Tool', color: 'green' },
};

/** Trace 树节点展示对象。 */
export type TraceIdentityNode = AgentTraceTypes.SpanVo | AgentTraceTypes.SpanTreeNode;

/**
 * 渲染 trace 状态标签。
 *
 * @param status 状态值。
 * @returns 状态标签节点。
 */
export function renderStatusTag(status?: string) {
  const meta = TRACE_STATUS_META[status || ''] || { text: status || '-', color: 'default' };
  return <Tag color={meta.color}>{meta.text}</Tag>;
}

/**
 * 渲染 span 类型标签。
 *
 * @param spanType Span 类型。
 * @returns Span 类型标签节点。
 */
export function renderSpanTypeTag(spanType?: string) {
  const meta = SPAN_TYPE_META[spanType || ''] || { text: spanType || '-', color: 'default' };
  return <Tag color={meta.color}>{meta.text}</Tag>;
}

/**
 * 渲染毫秒耗时文本。
 *
 * @param durationMs 耗时毫秒。
 * @returns 耗时文本。
 */
export function renderDuration(durationMs?: number) {
  if (typeof durationMs === 'number' && Number.isFinite(durationMs)) {
    return `${durationMs} ms`;
  }
  return '-';
}

/**
 * 从 Span attributes 中读取字符串属性。
 *
 * @param attributes Span 附加属性。
 * @param key 属性名称。
 * @returns 字符串属性值；不存在时返回空值。
 */
function readSpanAttributeText(attributes: AgentTraceTypes.SpanVo['attributes'], key: string) {
  const value = attributes?.[key];
  if (typeof value !== 'string') {
    return null;
  }
  const normalizedValue = value.trim();
  return normalizedValue || null;
}

/**
 * 解析 Span 在树和详情中的展示名称。
 *
 * @param span Span 明细。
 * @returns 展示名称。
 */
export function resolveSpanDisplayName(span: TraceIdentityNode) {
  if (span.spanType === 'model') {
    if ('modelName' in span && span.modelName) {
      return span.modelName;
    }
    return (
      readSpanAttributeText((span as AgentTraceTypes.SpanVo).attributes, 'model_name') ||
      readSpanAttributeText((span as AgentTraceTypes.SpanVo).attributes, 'model') ||
      readSpanAttributeText((span as AgentTraceTypes.SpanVo).attributes, 'model_id') ||
      span.name ||
      '-'
    );
  }
  if ('displayName' in span && span.displayName) {
    return span.displayName;
  }
  return span.name || '-';
}

/**
 * 根据节点 ID 查找树节点。
 *
 * @param nodes 树节点列表。
 * @param nodeId 树节点唯一标识。
 * @returns 树节点；不存在时返回 null。
 */
export function findSpanTreeNodeById(
  nodes: AgentTraceTypes.SpanTreeNode[] = [],
  nodeId?: string | null,
): AgentTraceTypes.SpanTreeNode | null {
  if (!nodeId) {
    return null;
  }
  for (const node of nodes) {
    if (node.nodeId === nodeId) {
      return node;
    }
    const matchedChild = findSpanTreeNodeById(node.children || [], nodeId);
    if (matchedChild) {
      return matchedChild;
    }
  }
  return null;
}

/**
 * 根据真实 Span ID 查找最适合选中的树节点。
 *
 * @param nodes 树节点列表。
 * @param spanId 真实 Span ID。
 * @returns 树节点；不存在时返回 null。
 */
export function findPreferredSpanTreeNodeBySourceSpanId(
  nodes: AgentTraceTypes.SpanTreeNode[] = [],
  spanId?: string | null,
): AgentTraceTypes.SpanTreeNode | null {
  if (!spanId) {
    return null;
  }
  let matchedNode: AgentTraceTypes.SpanTreeNode | null = null;
  for (const node of nodes) {
    if (node.sourceSpanId === spanId) {
      if (node.nodeType === 'model_call') {
        return node;
      }
      matchedNode = matchedNode || node;
    }
    const matchedChild = findPreferredSpanTreeNodeBySourceSpanId(node.children || [], spanId);
    if (matchedChild?.nodeType === 'model_call') {
      return matchedChild;
    }
    matchedNode = matchedNode || matchedChild;
  }
  return matchedNode;
}

/**
 * 解析详情打开后的默认选中树节点。
 *
 * @param detail Trace 详情。
 * @returns 默认选中的树节点 ID。
 */
export function resolveInitialSelectedNodeId(detail: AgentTraceTypes.DetailVo) {
  const spanTree = detail.spanTree || [];
  if (detail.rootSpanId && findSpanTreeNodeById(spanTree, detail.rootSpanId)) {
    return detail.rootSpanId;
  }
  return spanTree[0]?.nodeId ?? null;
}

/**
 * 判断值是否是可直接展示的 JSON 结构。
 *
 * @param value 原始值。
 * @returns 可展示 JSON 时返回 true。
 */
function isJsonObjectLike(value: unknown): value is Record<string, unknown> | unknown[] {
  return typeof value === 'object' && value !== null;
}

/**
 * 渲染 JSON 载荷。
 *
 * @param value 原始载荷。
 * @returns JSON 展示节点。
 */
export function renderJsonPayload(value: unknown) {
  if (value === undefined || value === null || value === '') {
    return <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无数据" />;
  }

  if (typeof value === 'string') {
    try {
      const parsed = JSON.parse(value);
      if (isJsonObjectLike(parsed)) {
        return (
          <ReactJson
            src={parsed as object}
            name={false}
            collapsed={false}
            enableClipboard
            displayDataTypes={false}
            displayObjectSize={false}
            indentWidth={2}
            collapseStringsAfterLength={120}
            theme="rjv-default"
          />
        );
      }
    } catch {
      return (
        <Text copyable style={{ whiteSpace: 'pre-wrap', wordBreak: 'break-all' }}>
          {value}
        </Text>
      );
    }
  }

  if (!isJsonObjectLike(value)) {
    return (
      <Text copyable style={{ whiteSpace: 'pre-wrap', wordBreak: 'break-all' }}>
        {String(value)}
      </Text>
    );
  }

  return (
    <ReactJson
      src={value as object}
      name={false}
      collapsed={false}
      enableClipboard
      displayDataTypes={false}
      displayObjectSize={false}
      indentWidth={2}
      collapseStringsAfterLength={120}
      theme="rjv-default"
    />
  );
}
