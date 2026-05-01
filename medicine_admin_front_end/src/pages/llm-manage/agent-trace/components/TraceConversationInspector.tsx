import { Empty, Typography } from 'antd';
import React, { useMemo } from 'react';
import type { AgentTraceTypes } from '@/api/agent/trace';
import MarkdownRender from '../../../SmartAssistant/components/MarkdownRender';
import styles from '../index.module.less';

const { Text } = Typography;

export interface TraceConversationInspectorProps {
  /** 当前选中节点的可读消息视图。 */
  messageView?: AgentTraceTypes.TraceMessageViewVo | null;
}

/**
 * 判断消息是否有可展示内容。
 *
 * @param message 可读消息。
 * @returns 有内容返回 true。
 */
function hasReadableMessageContent(message?: AgentTraceTypes.TraceMessageVo | null) {
  return Boolean(message?.content?.trim());
}

/**
 * 解析消息渲染类型。
 *
 * @param role 消息角色。
 * @returns 渲染类型。
 */
/**
 * 用户消息气泡。
 *
 * @param props 组件属性。
 * @returns 用户消息节点。
 */
const UserBubble: React.FC<{ content: string }> = ({ content }) => (
  <div className={styles.traceConversationRow} data-role="user">
    <div className={styles.traceConversationUserBubble}>{content}</div>
  </div>
);

/**
 * AI 消息内容。
 *
 * @param props 组件属性。
 * @returns AI 消息节点。
 */
const AiMessage: React.FC<{ content: string }> = ({ content }) => (
  <div className={styles.traceConversationRow} data-role="ai">
    <div className={styles.traceConversationAiContent}>
      <MarkdownRender content={content} />
    </div>
  </div>
);

/**
 * 渲染单条消息。
 *
 * @param message 可读消息。
 * @param index 消息序号。
 * @returns 消息节点。
 */
function renderTraceMessage(message: AgentTraceTypes.TraceMessageVo, index: number) {
  const messageKey =
    message.id || `${message.role || 'message'}-${message.sourceSpanId || 'span'}-${index}`;
  const content = message.content || '';
  if (message.role === 'user') {
    return <UserBubble key={messageKey} content={content} />;
  }
  if (message.role === 'ai') {
    return <AiMessage key={messageKey} content={content} />;
  }
  return null;
}

/**
 * Agent Trace 聊天式消息视图。
 *
 * @param props 组件属性。
 * @returns 聊天式消息视图节点。
 */
const TraceConversationInspector: React.FC<TraceConversationInspectorProps> = ({ messageView }) => {
  const messages = useMemo(
    () => (messageView?.messages || []).filter(hasReadableMessageContent),
    [messageView?.messages],
  );

  if (!messages.length) {
    return (
      <div className={styles.traceConversationEmpty}>
        <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="当前节点暂无可读消息" />
      </div>
    );
  }

  return (
    <div className={styles.traceConversationInspector}>
      <div className={styles.traceConversationHeader}>
        <Text strong>{messageView?.title || '消息'}</Text>
        <Text type="secondary">{messages.length.toLocaleString()} 条消息</Text>
      </div>
      <div className={styles.traceConversationBody}>
        <div className={styles.traceConversationList}>{messages.map(renderTraceMessage)}</div>
      </div>
    </div>
  );
};

export default TraceConversationInspector;
