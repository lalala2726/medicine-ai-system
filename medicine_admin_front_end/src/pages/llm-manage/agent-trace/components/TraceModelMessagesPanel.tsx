import { Space, Tag, Typography } from 'antd';
import { Bot, ChevronDown, ChevronRight, MessageSquare, Wrench } from 'lucide-react';
import React, { useState } from 'react';
import type { AgentTraceTypes } from '@/api/agent/trace';
import styles from '../index.module.less';
import TraceToolCallCard from './TraceToolCallCard';
import TraceSystemPromptBlock from './TraceSystemPromptBlock';
import {
  isEmptyModelContent,
  renderModelEmpty,
  renderModelMessageContent,
  resolveModelMessageTypeText,
} from './traceModelUtils';

const { Text } = Typography;

export interface TraceModelMessagesPanelProps {
  /** 面板类型。 */
  mode: 'input' | 'output';
  /** 模型系统提示词。 */
  systemPrompt?: AgentTraceTypes.ModelSystemPromptVo | null;
  /** 模型消息列表。 */
  messages?: AgentTraceTypes.ModelMessageVo[];
  /** 模型最终文本。 */
  finalText?: string;
}

/**
 * 解析消息图标。
 *
 * @param type 消息类型。
 * @returns 消息图标节点。
 */
function renderMessageIcon(type?: string) {
  if (type === 'ai' || type === 'assistant') {
    return <Bot size={14} />;
  }
  if (type === 'tool') {
    return <Wrench size={14} />;
  }
  return <MessageSquare size={14} />;
}

/**
 * 渲染消息关联工具调用。
 *
 * @param calls 工具调用列表。
 * @returns 工具调用节点。
 */
function renderMessageToolCalls(calls?: AgentTraceTypes.ModelToolCallVo[]) {
  if (!calls?.length) {
    return null;
  }
  return (
    <div className={styles.modelMessageToolCalls}>
      {calls.map((call, index) => (
        <TraceToolCallCard key={call.id || `${call.name}-${index}`} call={call} />
      ))}
    </div>
  );
}

/**
 * 判断消息是否是工具结果消息。
 *
 * @param type 消息类型。
 * @returns 工具结果消息返回 true。
 */
function isToolMessageType(type?: string) {
  return type === 'tool';
}

/**
 * 判断消息是否是 AI 消息。
 *
 * @param type 消息类型。
 * @returns AI 消息返回 true。
 */
function isAiMessageType(type?: string) {
  return type === 'ai' || type === 'assistant';
}

/**
 * 提取消息文本内容。
 *
 * @param content 消息内容。
 * @returns 文本内容；非纯文本返回 null。
 */
function extractMessageText(content: unknown) {
  return typeof content === 'string' ? content.trim() : null;
}

/**
 * 判断是否需要跳过和最终回复重复的消息。
 *
 * @param message 消息对象。
 * @param finalText 最终回复文本。
 * @returns 重复消息返回 true。
 */
function shouldSkipDuplicateFinalMessage(
  message: AgentTraceTypes.ModelMessageVo,
  finalText?: string,
) {
  const finalTextValue = finalText?.trim();
  if (!finalTextValue || !isAiMessageType(message.type)) {
    return false;
  }
  return extractMessageText(message.content) === finalTextValue;
}

/**
 * 解析工具结果消息展示名称。
 *
 * @param message 消息对象。
 * @returns 工具结果展示名称。
 */
function resolveToolMessageName(message: AgentTraceTypes.ModelMessageVo) {
  return message.name?.trim() || '工具输出';
}

/**
 * 工具结果消息折叠卡片。
 *
 * @param props 组件属性。
 * @returns 工具结果消息节点。
 */
const TraceToolResultMessage: React.FC<{ message: AgentTraceTypes.ModelMessageVo }> = ({
  message,
}) => {
  const [expanded, setExpanded] = useState(false);
  const toolName = resolveToolMessageName(message);

  return (
    <div className={styles.modelToolResultCard}>
      <button
        type="button"
        className={styles.modelToolResultButton}
        aria-expanded={expanded}
        onClick={() => setExpanded((current) => !current)}
      >
        <span className={styles.modelToolResultIcon}>
          <Wrench size={14} />
        </span>
        <div className={styles.modelToolResultMain}>
          <div className={styles.modelToolResultTitleLine}>
            <Space size={6} wrap>
              <Tag color="green">工具</Tag>
              <Text strong>{toolName}</Text>
            </Space>
            {message.toolCallId ? (
              <Text copyable type="secondary" className={styles.modelToolResultCallId}>
                {message.toolCallId}
              </Text>
            ) : null}
          </div>
        </div>
        <span className={styles.modelToolResultChevron}>
          {expanded ? <ChevronDown size={15} /> : <ChevronRight size={15} />}
        </span>
      </button>
      {expanded ? (
        <div className={styles.modelToolResultBody}>
          {renderModelMessageContent(message.content)}
        </div>
      ) : null}
    </div>
  );
};

/**
 * 渲染单条模型消息。
 *
 * @param message 消息对象。
 * @param index 消息顺序。
 * @param finalText 最终回复文本。
 * @returns 消息节点。
 */
function renderModelMessage(
  message: AgentTraceTypes.ModelMessageVo,
  index: number,
  finalText?: string,
) {
  const toolCallsNode = renderMessageToolCalls(message.toolCalls);
  const messageKey = `${message.type || 'message'}-${index}`;
  if (isToolMessageType(message.type)) {
    return <TraceToolResultMessage key={messageKey} message={message} />;
  }
  if (shouldSkipDuplicateFinalMessage(message, finalText)) {
    return toolCallsNode ? <React.Fragment key={messageKey}>{toolCallsNode}</React.Fragment> : null;
  }
  if (isEmptyModelContent(message.content) && toolCallsNode) {
    return <React.Fragment key={messageKey}>{toolCallsNode}</React.Fragment>;
  }
  return (
    <div key={messageKey} className={styles.modelMessageItem}>
      <div className={styles.modelMessageHeader}>
        <Space size={6} wrap>
          {message.type !== 'user' && message.type !== 'human' ? (
            <span className={styles.modelMessageIcon}>{renderMessageIcon(message.type)}</span>
          ) : null}
          <Tag
            color={message.type === 'tool' ? 'green' : message.type === 'ai' ? 'orange' : 'blue'}
          >
            {resolveModelMessageTypeText(message.type)}
          </Tag>
          {message.name ? <Text type="secondary">{message.name}</Text> : null}
        </Space>
        {message.toolCallId ? (
          <Text copyable type="secondary" className={styles.modelMessageToolCallId}>
            {message.toolCallId}
          </Text>
        ) : null}
      </div>
      {renderModelMessageContent(message.content)}
      {toolCallsNode}
    </div>
  );
}

/**
 * 模型输入/输出消息面板。
 *
 * @param props 组件属性。
 * @returns 模型消息面板节点。
 */
const TraceModelMessagesPanel: React.FC<TraceModelMessagesPanelProps> = ({
  mode,
  systemPrompt,
  messages = [],
  finalText,
}) => {
  const hasFinalText = Boolean(finalText?.trim());
  const renderedMessages = messages.map((message, index) =>
    renderModelMessage(message, index, finalText),
  );
  const hasRenderedMessages = renderedMessages.some(Boolean);
  const hasVisibleContent =
    hasRenderedMessages || (mode === 'input' && systemPrompt?.content) || hasFinalText;

  if (!hasVisibleContent) {
    return <div className={styles.modelPanelContent}>{renderModelEmpty('暂无模型消息')}</div>;
  }

  return (
    <div className={styles.modelPanelContent}>
      {mode === 'input' ? (
        <section className={styles.modelPlainSection}>
          <TraceSystemPromptBlock systemPrompt={systemPrompt} />
        </section>
      ) : null}
      {hasRenderedMessages ? (
        <div className={styles.modelMessageList}>{renderedMessages}</div>
      ) : null}
      {mode === 'output' && hasFinalText ? (
        <section className={styles.modelPlainSection}>
          <div className={styles.modelPlainSectionHeader}>
            <Text strong>最终回复</Text>
            <Text copyable={{ text: finalText }} type="secondary">
              复制原文
            </Text>
          </div>
          <div className={styles.modelFinalText}>{finalText}</div>
        </section>
      ) : null}
    </div>
  );
};

export default TraceModelMessagesPanel;
