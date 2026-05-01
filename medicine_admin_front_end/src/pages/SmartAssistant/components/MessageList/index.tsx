/**
 * 消息列表组件
 *
 * - 基于 @ant-design/x 的 Bubble.List 渲染消息
 * - 支持向上滚动加载历史，加载时保持位置稳定
 * - 区分流式增长与离散新增，避免 smooth scroll 抖动
 */
import {
  LoadingOutlined,
  OpenAIOutlined,
  SearchOutlined,
  SoundOutlined,
  ToolOutlined,
} from '@ant-design/icons';
import { CircleStop, Maximize2, Minimize2 } from 'lucide-react';
import { Actions, Bubble, ThoughtChain } from '@ant-design/x';
import { Button, Flex, type GetProp, Spin, message, theme, Image } from 'antd';
import React, { useCallback, useEffect, useLayoutEffect, useMemo, useRef, useState } from 'react';
import type { ActiveToolCall, AssistantExtraEvent, ThoughtNode } from '@/store';
import { playAssistantMessageTts } from '../../_tts';
import FeedbackModal from './FeedbackModal';
import MarkdownRender from '../MarkdownRender';
import styles from './index.module.less';

type BubbleRoles = GetProp<typeof Bubble.List, 'role'>;

interface MessageItem {
  key: string;
  role: 'user' | 'ai' | 'divider';
  content: string;
  loading?: boolean;
  isFinished?: boolean;
  thoughtChain?: ThoughtNode[];
  activeToolCall?: ActiveToolCall | null;
  thinking?: string;
  extraEvents?: AssistantExtraEvent[];
  dividerProps?: Record<string, any>;
}

interface MessageListProps {
  items?: MessageItem[];
  onLoadMore?: () => Promise<void>;
  hasMore?: boolean;
  historyLoading?: boolean;
}

const ROLE_CONFIG: BubbleRoles = {
  ai: {
    placement: 'start',
    style: { maxWidth: '100%', fontSize: '15px' },
  },
  user: {
    placement: 'end',
    style: { maxWidth: '100%', fontSize: '15px' },
  },
};

/** 深度思考面板固定高度，单位 px。 */
const THINKING_PANEL_HEIGHT = 180;

const AILoadingIndicator: React.FC<{
  activeToolCall?: ActiveToolCall | null;
}> = ({ activeToolCall }) => (
  <Flex align="center" gap="small">
    <Spin size="small" />
    {activeToolCall ? (
      <Flex align="center" gap={6}>
        <ToolOutlined style={{ fontSize: 14 }} />
        <span>{activeToolCall.message || '正在调用工具...'}</span>
      </Flex>
    ) : (
      <span>正在思考...</span>
    )}
  </Flex>
);

/**
 * 深度思考面板属性。
 */
interface ThinkingPanelProps {
  /** 深度思考文本内容。 */
  content: string;
  /** 当前深度思考是否仍在流式输出。 */
  isLoading?: boolean;
  /** 正文回复是否已经开始输出。 */
  hasAnswerContent?: boolean;
}

/**
 * 固定高度的深度思考展示面板。
 *
 * @param props 组件属性。
 * @returns 深度思考展示节点。
 */
const ThinkingPanel: React.FC<ThinkingPanelProps> = ({ content, isLoading, hasAnswerContent }) => {
  const bodyRef = useRef<HTMLDivElement>(null);
  const [isCollapsed, setIsCollapsed] = useState(!isLoading);
  const autoCollapsedAfterAnswerRef = useRef(false);

  useEffect(() => {
    if (isLoading) {
      setIsCollapsed(false);
      autoCollapsedAfterAnswerRef.current = false;
    }
  }, [isLoading]);

  useEffect(() => {
    if (hasAnswerContent && !autoCollapsedAfterAnswerRef.current) {
      setIsCollapsed(true);
      autoCollapsedAfterAnswerRef.current = true;
    }
  }, [hasAnswerContent]);

  useLayoutEffect(() => {
    const body = bodyRef.current;
    if (!body || isCollapsed) {
      return;
    }
    body.scrollTop = body.scrollHeight;
  }, [content, isCollapsed]);

  /**
   * 切换深度思考面板展开状态。
   */
  const toggleCollapsed = (): void => {
    setIsCollapsed((prev) => !prev);
  };

  return (
    <section
      className={`${styles.thinkingPanel} ${isCollapsed ? styles.thinkingPanelCollapsed : ''}`}
      style={isCollapsed ? undefined : { height: THINKING_PANEL_HEIGHT }}
    >
      <div className={styles.thinkingPanelHeader}>
        <span className={styles.thinkingPanelTitle}>
          <OpenAIOutlined />
          <span>深度思考已完成</span>
        </span>
        <button
          type="button"
          className={styles.thinkingPanelToggle}
          aria-label={isCollapsed ? '展开深度思考' : '收起深度思考'}
          aria-expanded={!isCollapsed}
          onClick={toggleCollapsed}
        >
          {isCollapsed ? <Maximize2 size={14} /> : <Minimize2 size={14} />}
        </button>
      </div>
      {!isCollapsed && (
        <div ref={bodyRef} className={styles.thinkingPanelBody}>
          {content}
        </div>
      )}
    </section>
  );
};

const AIMessageContent: React.FC<{
  content: string;
  thoughtChain?: ThoughtNode[];
  activeToolCall?: ActiveToolCall | null;
  isLoading?: boolean;
  thinking?: string;
  extraEvents?: AssistantExtraEvent[];
}> = ({
  content,
  thoughtChain: _thoughtChain,
  activeToolCall,
  isLoading,
  thinking,
  extraEvents,
}) => {
  const showLoading = isLoading && !content && !thinking;
  const extraEventSummary = useMemo(() => {
    if (!extraEvents || extraEvents.length === 0) {
      return '';
    }

    const cardCount = extraEvents.filter((item) => item.type === 'card').length;
    const actionCount = extraEvents.filter((item) => item.type === 'action').length;
    const summaryParts: string[] = [];

    if (cardCount > 0) {
      summaryParts.push(`${cardCount} 个卡片`);
    }
    if (actionCount > 0) {
      summaryParts.push(`${actionCount} 个动作`);
    }

    return summaryParts.length > 0
      ? `收到 ${summaryParts.join('、')} 事件，当前前端暂未接入，已安全忽略。`
      : '';
  }, [extraEvents]);

  return (
    <div className={styles.aiMessageContent}>
      {showLoading && <AILoadingIndicator activeToolCall={activeToolCall} />}
      {thinking && (
        <ThinkingPanel
          content={thinking}
          isLoading={isLoading}
          hasAnswerContent={Boolean(content)}
        />
      )}
      {activeToolCall && !showLoading && (
        <div style={{ marginBottom: 8 }}>
          <ThoughtChain.Item
            variant="text"
            icon={<SearchOutlined />}
            title={activeToolCall.message}
            description={activeToolCall.name}
            status="loading"
          />
        </div>
      )}
      {extraEventSummary && (
        <div
          style={{
            marginBottom: 8,
            fontSize: 12,
            color: 'var(--ant-color-text-secondary)',
          }}
        >
          {extraEventSummary}
        </div>
      )}
      {content && (
        <MarkdownRender
          content={content}
          enableStreaming={!!isLoading}
          enableAnimation={!!isLoading}
        />
      )}
    </div>
  );
};

/** AI 消息底部操作栏（点赞/点踩/复制/语音） */
const AIMessageFooter: React.FC<{
  messageKey: string;
  content: string;
  onDislike?: (key: string) => void;
}> = ({ messageKey, content, onDislike }) => {
  const { token } = theme.useToken();
  const [feedback, setFeedback] = useState<'like' | 'dislike' | 'default'>('default');
  const [playing, setPlaying] = useState(false);
  const abortControllerRef = useRef<AbortController | null>(null);

  const handleFeedback = useCallback(
    (value: 'like' | 'dislike' | 'default') => {
      setFeedback(value);
      if (value === 'dislike') {
        onDislike?.(messageKey);
      }
      console.log(`[消息反馈] messageKey=${messageKey}, feedback=${value}`);
    },
    [messageKey, onDislike],
  );

  const handleVoicePlay = useCallback(async () => {
    if (playing) {
      if (abortControllerRef.current) {
        abortControllerRef.current.abort();
        abortControllerRef.current = null;
      }
      setPlaying(false);
      return;
    }

    setPlaying(true);
    const controller = new AbortController();
    abortControllerRef.current = controller;

    try {
      console.log(`[语音播放] messageKey=${messageKey}`);
      await playAssistantMessageTts(messageKey, { signal: controller.signal });
    } catch (err: any) {
      if (err.name === 'AbortError') {
        console.log('[语音播放] 已手动停止');
      } else {
        console.error('播放语音失败:', err);
        message.error(err.message || '播放失败，请重试');
      }
    } finally {
      if (abortControllerRef.current === controller) {
        setPlaying(false);
        abortControllerRef.current = null;
      }
    }
  }, [messageKey, playing]);

  // 组件卸载时清理正在播放的语音
  useEffect(() => {
    return () => {
      if (abortControllerRef.current) {
        abortControllerRef.current.abort();
        abortControllerRef.current = null;
      }
    };
  }, []);

  return (
    <Flex gap={4} align="center" className={styles.messageFooter}>
      <Actions.Copy text={content} />
      <Actions.Feedback value={feedback} onChange={handleFeedback} />
      <Button
        type="text"
        size="small"
        icon={playing ? <CircleStop size={14} /> : <SoundOutlined />}
        onClick={handleVoicePlay}
        style={{ color: token.colorTextSecondary, fontSize: 14 }}
      />
    </Flex>
  );
};

const MessageList: React.FC<MessageListProps> = ({
  items,
  onLoadMore,
  hasMore,
  historyLoading,
}) => {
  const containerRef = useRef<HTMLDivElement>(null);

  const isInitialLoadRef = useRef(true);
  const isLoadingMoreRef = useRef(false);
  const prevScrollHeightRef = useRef(0);
  const prevItemsLenRef = useRef(0);
  const prevLastItemMetaRef = useRef<{
    key: string;
    contentLength: number;
  } | null>(null);

  const isAtBottomRef = useRef(true);

  const [feedbackModalOpen, setFeedbackModalOpen] = useState(false);
  const [activeFeedbackKey, setActiveFeedbackKey] = useState<string | null>(null);
  const [feedbackLoading, setFeedbackLoading] = useState(false);

  const handleDislike = useCallback((key: string) => {
    setActiveFeedbackKey(key);
    setFeedbackModalOpen(true);
  }, []);

  const handleFeedbackSubmit = async (data: { reasons: string[]; comment: string }) => {
    setFeedbackLoading(true);
    try {
      console.log(`[反馈提交] messageKey=${activeFeedbackKey}`, data);
      await new Promise((resolve) => setTimeout(resolve, 800));
      setFeedbackModalOpen(false);
      setActiveFeedbackKey(null);
    } catch (err) {
      console.error('提交反馈失败:', err);
    } finally {
      setFeedbackLoading(false);
    }
  };

  useEffect(() => {
    const el = containerRef.current;
    if (!el) return;

    const handleScroll = () => {
      const distance = el.scrollHeight - el.scrollTop - el.clientHeight;
      isAtBottomRef.current = distance < 50;

      if (!hasMore || historyLoading || isLoadingMoreRef.current) return;
      if (el.scrollTop < 80 && onLoadMore) {
        isLoadingMoreRef.current = true;
        prevScrollHeightRef.current = el.scrollHeight;
        onLoadMore();
      }
    };

    el.addEventListener('scroll', handleScroll, { passive: true });
    isAtBottomRef.current = el.scrollHeight - el.scrollTop - el.clientHeight < 50;

    return () => el.removeEventListener('scroll', handleScroll);
  }, [hasMore, historyLoading, onLoadMore]);

  useLayoutEffect(() => {
    const el = containerRef.current;
    if (!el) return;

    const currentItems = items ?? [];
    const currentLen = currentItems.length;
    const prevLen = prevItemsLenRef.current;
    const prevLastMeta = prevLastItemMetaRef.current;
    const currentLastItem = currentItems[currentLen - 1];
    const currentLastMeta = currentLastItem
      ? {
          key: currentLastItem.key,
          contentLength: currentLastItem.content.length,
        }
      : null;

    const commitSnapshot = () => {
      prevItemsLenRef.current = currentLen;
      prevLastItemMetaRef.current = currentLastMeta;
    };

    if (currentLen === 0) {
      isInitialLoadRef.current = true;
      isLoadingMoreRef.current = false;
      prevScrollHeightRef.current = 0;
      commitSnapshot();
      return;
    }

    if (isLoadingMoreRef.current && prevScrollHeightRef.current > 0) {
      const newScrollHeight = el.scrollHeight;
      const heightDiff = newScrollHeight - prevScrollHeightRef.current;
      el.scrollTop = heightDiff;
      isLoadingMoreRef.current = false;
      prevScrollHeightRef.current = 0;
      commitSnapshot();
      return;
    }

    if (isInitialLoadRef.current && prevLen === 0) {
      isInitialLoadRef.current = false;
      el.scrollTop = el.scrollHeight;
      isAtBottomRef.current = true;
      commitSnapshot();
      return;
    }

    const isAppend = currentLen > prevLen;
    const isStreamingGrowth =
      currentLen === prevLen &&
      !!currentLastMeta &&
      !!prevLastMeta &&
      currentLastMeta.key === prevLastMeta.key &&
      currentLastMeta.contentLength > prevLastMeta.contentLength;
    const isLastItemUser = currentLastItem?.role === 'user';
    // 当发送消息时，通常会同时新增 user 消息和 ai 的 loading 消息
    const isNewSend =
      isAppend && (isLastItemUser || (currentLastItem?.role === 'ai' && currentLastItem?.loading));

    const wasAtBottom = isAtBottomRef.current;

    if (isStreamingGrowth) {
      if (wasAtBottom) {
        el.scrollTop = el.scrollHeight;
      }
      commitSnapshot();
      return;
    }

    if (isAppend) {
      if (isNewSend) {
        const lastAiMsg = [...currentItems].reverse().find((i) => i.role === 'ai');
        if (lastAiMsg) {
          // 使用 requestAnimationFrame 确保 DOM 已经渲染完毕
          requestAnimationFrame(() => {
            const aiMsgNode = el.querySelector(`.chat-msg-item-${lastAiMsg.key}`);
            if (aiMsgNode) {
              const containerRect = el.getBoundingClientRect();
              const nodeRect = aiMsgNode.getBoundingClientRect();
              // 将 AI 消息滚动到顶部并留出 20px 间距
              const targetScrollTop = el.scrollTop + nodeRect.top - containerRect.top - 20;

              el.scrollTo({ top: Math.max(0, targetScrollTop), behavior: 'smooth' });

              // 重置 isAtBottom 状态，防止流式输出时自动向下滚动，干扰用户阅读上方内容
              isAtBottomRef.current = false;
            } else {
              // 降级：如果没有找到具体节点，回退到滚动到底部
              el.scrollTop = el.scrollHeight;
              isAtBottomRef.current = true;
            }
          });

          commitSnapshot();
          return;
        }
      }

      if (wasAtBottom) {
        el.scrollTop = el.scrollHeight;
        isAtBottomRef.current = true;
      }
      commitSnapshot();
      return;
    }

    if (wasAtBottom) {
      el.scrollTop = el.scrollHeight;
    }

    commitSnapshot();
  }, [items]);

  const createAIContentRender = useCallback(
    (item: MessageItem) => () => (
      <AIMessageContent
        content={item.content}
        thoughtChain={item.thoughtChain}
        activeToolCall={item.activeToolCall}
        isLoading={item.loading}
        thinking={item.thinking}
        extraEvents={item.extraEvents}
      />
    ),
    [],
  );

  /**
   * 创建用户消息内容渲染函数。
   * 解析出 markdown 中的图片，分离图片和文本，使用自定义结构。
   */
  const createUserContentRender = useCallback(
    (item: MessageItem) => () => {
      const imgRegex = /!\[[^\]]*\]\((.*?)\)/g;
      const imageUrls: string[] = [];
      let match;
      while ((match = imgRegex.exec(item.content)) !== null) {
        imageUrls.push(match[1]);
      }

      const textContent = item.content.replace(imgRegex, '').trim();

      return (
        <div className={styles.userMessageRender}>
          {imageUrls.length > 0 && (
            <div className={styles.userImageDisplayArea}>
              <Image.PreviewGroup>
                {imageUrls.map((url, idx) => (
                  <div key={idx} className={styles.userImageWrapper}>
                    <Image
                      src={url}
                      alt={`图片-${idx}`}
                      width="100%"
                      height="100%"
                      style={{ objectFit: 'cover' }}
                      preview={{ src: url }} // Optionally strictly map preview URL
                    />
                  </div>
                ))}
              </Image.PreviewGroup>
            </div>
          )}
          {textContent && <div className={styles.userTextBubble}>{textContent}</div>}
        </div>
      );
    },
    [],
  );

  const decoratedItems = useMemo(() => {
    return (items ?? []).map((item) => ({
      key: item.key,
      role: item.role,
      className:
        item.role === 'ai'
          ? `chat-msg-item-${item.key} ${styles.aiMessageItem}`
          : `chat-msg-item-${item.key}`,
      loading: item.role === 'ai' ? false : item.loading,
      variant: (item.role === 'ai' || item.role === 'user' ? 'borderless' : undefined) as
        | 'borderless'
        | 'filled'
        | 'outlined'
        | 'shadow'
        | undefined,
      content: item.content as string,
      contentRender:
        item.role === 'ai'
          ? createAIContentRender(item)
          : item.role === 'user'
            ? createUserContentRender(item)
            : undefined,
      ...(item.role === 'divider' ? item.dividerProps : {}),
      footer:
        item.role === 'ai' && item.content && (item.isFinished ?? !item.loading)
          ? () => (
              <AIMessageFooter
                messageKey={item.key}
                content={item.content}
                onDislike={handleDislike}
              />
            )
          : undefined,
    }));
  }, [items, createAIContentRender, createUserContentRender, handleDislike]);

  return (
    <div className={styles.container} ref={containerRef}>
      {historyLoading && (
        <div className={styles.loadMoreIndicator}>
          <Spin indicator={<LoadingOutlined spin />} size="small" />
          <span>加载历史消息...</span>
        </div>
      )}
      {hasMore && !historyLoading && <div className={styles.loadMoreHint}>向上滚动加载更多</div>}
      <Bubble.List
        autoScroll={false}
        role={ROLE_CONFIG}
        items={decoratedItems}
        style={{ width: '100%' }}
      />
      <div
        style={{
          height: 'var(--assistant-message-safe-bottom, 220px)',
          flexShrink: 0,
          width: '100%',
        }}
      />

      <FeedbackModal
        open={feedbackModalOpen}
        onCancel={() => setFeedbackModalOpen(false)}
        onSubmit={handleFeedbackSubmit}
        loading={feedbackLoading}
      />
    </div>
  );
};

export default MessageList;
export type { MessageItem };
