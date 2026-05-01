import { Empty, Typography } from 'antd';
import type { ReactNode } from 'react';
import type { AgentTraceTypes } from '@/api/agent/trace';
import styles from '../index.module.less';
import { renderJsonPayload } from './traceShared';

const { Text } = Typography;

/** 模型 Token 用量摘要。 */
export interface ModelTokenUsageSummary {
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
 * @returns Token 数；不存在或无法转换时返回 0。
 */
export function readModelTokenNumber(
  tokenUsage: AgentTraceTypes.ModelDetailVo['tokenUsage'],
  key: string,
) {
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
 * 解析模型 Token 用量摘要。
 *
 * @param tokenUsage Token 用量对象。
 * @returns Token 用量摘要。
 */
export function resolveModelTokenUsageSummary(
  tokenUsage: AgentTraceTypes.ModelDetailVo['tokenUsage'],
): ModelTokenUsageSummary {
  const inputTokens =
    readModelTokenNumber(tokenUsage, 'input_tokens') ||
    readModelTokenNumber(tokenUsage, 'inputTokens');
  const outputTokens =
    readModelTokenNumber(tokenUsage, 'output_tokens') ||
    readModelTokenNumber(tokenUsage, 'outputTokens');
  const explicitTotalTokens =
    readModelTokenNumber(tokenUsage, 'total_tokens') ||
    readModelTokenNumber(tokenUsage, 'totalTokens');

  return {
    inputTokens,
    outputTokens,
    totalTokens: explicitTotalTokens || inputTokens + outputTokens,
  };
}

/**
 * 判断 Token 用量是否有展示价值。
 *
 * @param summary Token 用量摘要。
 * @returns 存在有效 Token 时返回 true。
 */
export function hasModelTokenUsage(summary: ModelTokenUsageSummary) {
  return summary.inputTokens > 0 || summary.outputTokens > 0 || summary.totalTokens > 0;
}

/**
 * 格式化 Token 数字。
 *
 * @param value Token 数。
 * @returns 本地化展示文本。
 */
export function formatModelTokenNumber(value: number) {
  return Number.isFinite(value) ? value.toLocaleString() : '0';
}

/**
 * 计算 Token 占比。
 *
 * @param value 当前 Token 数。
 * @param total 总 Token 数。
 * @returns 百分比，范围 0-100。
 */
export function resolveModelTokenPercent(value: number, total: number) {
  if (!Number.isFinite(value) || !Number.isFinite(total) || total <= 0) {
    return 0;
  }
  return Math.min(Math.max((value / total) * 100, 0), 100);
}

/**
 * 解析工具注册名称。
 *
 * @param tool 工具对象。
 * @returns 工具注册名称。
 */
export function resolveToolName(
  tool?: AgentTraceTypes.ModelToolVo | AgentTraceTypes.ModelToolCallVo | null,
) {
  return tool?.name?.trim() || '-';
}

/**
 * 判断值是否为空内容。
 *
 * @param value 原始值。
 * @returns 空内容返回 true。
 */
export function isEmptyModelContent(value: unknown) {
  return value === undefined || value === null || value === '';
}

/**
 * 解析消息类型展示文本。
 *
 * @param type 消息类型。
 * @returns 中文展示文本。
 */
export function resolveModelMessageTypeText(type?: string) {
  const normalizedType = type || '';
  const typeTextMap: Record<string, string> = {
    system: '系统',
    human: '用户',
    user: '用户',
    ai: 'AI',
    assistant: 'AI',
    tool: '工具',
  };
  return typeTextMap[normalizedType] || normalizedType || '-';
}

/**
 * 渲染模型消息内容。
 *
 * @param content 消息内容。
 * @returns 消息内容节点。
 */
export function renderModelMessageContent(content: unknown): ReactNode {
  if (isEmptyModelContent(content)) {
    return <Text type="secondary">暂无内容</Text>;
  }
  if (typeof content === 'string') {
    return <div className={styles.modelMessageText}>{content}</div>;
  }
  return <div className={styles.modelMessageJson}>{renderJsonPayload(content)}</div>;
}

/**
 * 渲染空态。
 *
 * @param description 空态说明。
 * @returns 空态节点。
 */
export function renderModelEmpty(description: string) {
  return <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={description} />;
}
