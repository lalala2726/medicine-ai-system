/**
 * 聊天交互 Hook
 *
 * 封装 submit / stream / stop 新协议下的消息发送、流恢复、
 * 会话切换与历史加载，页面层只负责渲染。
 */
import { message as antdMessage } from 'antd';
import { useNavigate, useParams } from 'react-router-dom';
import { useCallback, useEffect, useRef, useState } from 'react';
import { getConversation } from '@/api/agent';
import {
  attachAssistantStream,
  AssistantApiError,
  ChatTypes,
  isAssistantReplaceMessage,
  resolveAssistantFinalStatus,
  stopAssistantMessage,
  submitAssistantMessage,
} from '@/api/chat';
import {
  chatStore,
  type AssistantExtraEvent,
  type AssistantMessageStatus,
  type Message,
  useChatStore,
} from '@/store';
import { handleThoughtChainMessage } from './_thoughtChainHandler';

/** 智能助手路由基础路径 */
const BASE_CHAT_PATH = '/smart-assistant';
/** 历史消息分页大小 */
const HISTORY_PAGE_SIZE = 50;
/** 模型不支持图片理解时的统一提示文案。 */
const UNSUPPORTED_IMAGE_UNDERSTANDING_TEXT = '此模型不支持图片理解';

/**
 * 生成唯一消息 ID，避免本地新建用户消息时发生碰撞。
 *
 * @returns 随机消息 ID。
 */
function generateMessageId(): string {
  return crypto.randomUUID();
}

/**
 * 标准化后端历史消息状态。
 *
 * @param status 后端原始状态。
 * @returns 前端可识别的 AI 消息状态。
 */
function normalizeAssistantStatus(status: unknown): AssistantMessageStatus | undefined {
  if (status === 'streaming') return 'streaming';
  if (status === 'success') return 'success';
  if (status === 'cancelled') return 'cancelled';
  if (status === 'error') return 'error';
  return undefined;
}

/**
 * 将历史消息映射为前端消息结构。
 *
 * @param item 后端历史消息对象。
 * @returns 标准化后的前端消息。
 */
function normalizeHistoryMessage(item: any): Message {
  const messageUuid =
    typeof item.message_uuid === 'string'
      ? item.message_uuid
      : typeof item.id === 'string'
        ? item.id
        : undefined;
  const role = item.role === 'assistant' ? 'ai' : item.role || 'user';
  const status = role === 'ai' ? normalizeAssistantStatus(item.status) : undefined;

  return {
    ...item,
    id: item.id || messageUuid || generateMessageId(),
    message_uuid: messageUuid,
    role,
    content: typeof item.content === 'string' ? item.content : '',
    status,
    thinking: typeof item.thinking === 'string' ? item.thinking : undefined,
    isFinished:
      role === 'ai'
        ? status
          ? status !== 'streaming'
          : Boolean(item.isFinished)
        : item.isFinished,
  };
}

/**
 * 获取当前列表里最后一条 streaming 的 AI 消息。
 *
 * @param messages 消息列表。
 * @returns 最后一条 streaming AI 消息。
 */
function findLastStreamingAssistantMessage(messages: Message[]): Message | undefined {
  return [...messages].reverse().find((item) => item.role === 'ai' && item.status === 'streaming');
}

/**
 * 组装用户消息的 Markdown 文本（问题 + 图片）。
 *
 * @param question 用户问题文本。
 * @param imageUrls 用户上传图片 URL 列表。
 * @returns 可用于本地回显与历史回放的 Markdown 文本。
 */
function buildUserMessageMarkdown(question: string, imageUrls: string[]): string {
  const normalizedQuestion = question.trim();
  if (!imageUrls.length) {
    return normalizedQuestion;
  }

  const imageMarkdown = imageUrls
    .map((imageUrl, index) => `![用户上传图片${index + 1}](${imageUrl})`)
    .join('\n');

  if (normalizedQuestion) {
    return `${normalizedQuestion}\n\n${imageMarkdown}`;
  }
  return imageMarkdown;
}

/** useChat 返回的接口 */
export interface UseChatReturn {
  messages: Message[];
  content: string;
  setContent: (val: string) => void;
  /** 是否正在等待 submit 返回 */
  submitting: boolean;
  /** 是否存在已 attach 的运行中流 */
  loading: boolean;
  /** 是否已发送 stop，正在等待 end 事件 */
  stopPending: boolean;
  sendMessage: (
    text: string,
    imageUrls?: string[],
    onSuccess?: () => void,
    modelName?: string,
    reasoningEnabled?: boolean,
  ) => void;
  stopGenerating: () => void;
  conversationUuid: string | undefined;
  switchConversation: (uuid: string | null) => void;
  loadMoreHistory: () => Promise<void>;
  hasMoreHistory: boolean;
  historyLoading: boolean;
  setOnConversationCreated: (cb: ((uuid: string) => void) | null) => void;
}

/**
 * 从 /smart-assistant/* 的 splat 参数中提取会话 id。
 *
 * @param splat 路由 splat 参数。
 * @returns 解析出的会话 UUID。
 */
export function parseConversationIdFromSplat(splat?: string): string | undefined {
  if (!splat) return undefined;
  const firstSegment = splat
    .split('/')
    .map((segment) => segment.trim())
    .find(Boolean);
  return firstSegment || undefined;
}

/**
 * 根据会话 id 构建目标路径。
 *
 * @param conversationId 会话 UUID。
 * @returns 对应的页面路径。
 */
export function buildConversationPath(conversationId?: string): string {
  return conversationId ? `${BASE_CHAT_PATH}/${conversationId}` : BASE_CHAT_PATH;
}

export function useChat(): UseChatReturn {
  const navigate = useNavigate();
  const [content, setContent] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [loading, setLoading] = useState(false);
  const [stopPending, setStopPending] = useState(false);
  const { messages } = useChatStore();

  // ---------------- URL 同步 ----------------
  const params = useParams<{ conversationId?: string; '*': string }>();
  const routeConversationId = params.conversationId ?? parseConversationIdFromSplat(params['*']);

  const conversationUuidRef = useRef<string | undefined>(routeConversationId);
  const [conversationUuid, setConversationUuid] = useState<string | undefined>(routeConversationId);
  // 内部调用 navigate(replace) 时设置该标记，避免路由 effect 在过渡帧误判为外部导航
  const internalNavigationRef = useRef<{
    active: boolean;
    uuid?: string;
  }>({
    active: false,
    uuid: undefined,
  });
  const routeInitHandledRef = useRef(false);

  // ---------------- 历史分页 ----------------
  const [historyLoading, setHistoryLoading] = useState(false);
  const [hasMoreHistory, setHasMoreHistory] = useState(false);
  const historyPageRef = useRef(1);
  // 每次历史请求 +1，用于丢弃过期响应（会话切换后旧请求不可回写）
  const historyRequestIdRef = useRef(0);

  // ---------------- submit / 流控制 ----------------
  const submitRequestIdRef = useRef(0);
  const activeStreamControllerRef = useRef<AbortController | null>(null);
  // 每次发起/中断流都会递增，回调内校验 streamId，防止旧流回写 UI
  const activeStreamIdRef = useRef(0);
  // 当前运行中的 AI 消息 UUID
  const currentStreamingMessageIdRef = useRef<string | undefined>(undefined);

  // ---------------- 会话创建回调 ----------------
  const onConversationCreatedRef = useRef<((uuid: string) => void) | null>(null);

  /**
   * 注册新会话创建回调。
   *
   * @param cb 回调函数。
   */
  const setOnConversationCreated = useCallback((cb: ((uuid: string) => void) | null) => {
    onConversationCreatedRef.current = cb;
  }, []);

  /**
   * 统一更新 URL，并标记为内部导航。
   *
   * @param targetUuid 目标会话 UUID。
   * @param replace 是否替换历史记录。
   */
  const updateConversationPath = useCallback(
    (targetUuid?: string, replace = false) => {
      const targetPath = buildConversationPath(targetUuid);
      if (window.location.pathname === targetPath) {
        internalNavigationRef.current = { active: false, uuid: undefined };
        return;
      }
      internalNavigationRef.current = { active: true, uuid: targetUuid };
      navigate(targetPath, { replace });
    },
    [navigate],
  );

  /**
   * 失效所有等待中的 submit 结果。
   */
  const invalidatePendingSubmit = useCallback(() => {
    submitRequestIdRef.current += 1;
    setSubmitting(false);
  }, []);

  /**
   * 中断当前流并失效旧回调。
   */
  const abortActiveStream = useCallback(() => {
    if (activeStreamControllerRef.current) {
      activeStreamControllerRef.current.abort();
      activeStreamControllerRef.current = null;
    }
    activeStreamIdRef.current += 1;
    currentStreamingMessageIdRef.current = undefined;
    setLoading(false);
    setStopPending(false);
  }, []);

  /**
   * 重置会话相关状态。
   *
   * @param nextUuid 即将切换到的会话 UUID。
   */
  const resetConversationState = useCallback((nextUuid?: string) => {
    historyRequestIdRef.current += 1;
    conversationUuidRef.current = nextUuid;
    setConversationUuid(nextUuid);
    chatStore.getState().clearMessages();
    setContent('');
    setHasMoreHistory(false);
    setHistoryLoading(false);
    historyPageRef.current = 1;
  }, []);

  /**
   * 以 replace 或 append 的方式更新消息文本字段。
   *
   * @param messageId 目标消息 ID。
   * @param field 需要更新的字段名。
   * @param text 本次事件中的文本。
   * @param replace 是否整体覆盖。
   */
  const updateMessageText = useCallback(
    (messageId: string, field: 'content' | 'thinking', text: string, replace: boolean) => {
      const state = chatStore.getState();
      const targetMsg = state.messages.find((item) => item.id === messageId);
      if (!targetMsg) {
        return;
      }

      const nextText = replace ? text : `${targetMsg[field] || ''}${text}`;
      state.updateMessage(messageId, { [field]: nextText } as Partial<Message>);
    },
    [],
  );

  /**
   * 追加未接入协议的 card / action 事件。
   *
   * @param messageId 目标消息 ID。
   * @param type 事件类型。
   * @param payload 原始事件。
   */
  const appendExtraEvent = useCallback(
    (messageId: string, type: 'card' | 'action', payload: ChatTypes.AssistantStreamMessage) => {
      const state = chatStore.getState();
      const targetMsg = state.messages.find((item) => item.id === messageId);
      if (!targetMsg) {
        return;
      }

      const nextEvent: AssistantExtraEvent = {
        id: `${type}-${payload.timestamp || Date.now()}-${(targetMsg.extraEvents || []).length}`,
        type,
        content:
          payload.content && typeof payload.content === 'object'
            ? (payload.content as Record<string, any>)
            : null,
        meta:
          payload.meta && typeof payload.meta === 'object'
            ? (payload.meta as Record<string, any>)
            : undefined,
        timestamp: payload.timestamp,
      };

      state.updateMessage(messageId, {
        extraEvents: [...(targetMsg.extraEvents || []), nextEvent],
      });
    },
    [],
  );

  /**
   * 确保目标 AI 消息存在。
   *
   * @param messageId 目标消息 UUID。
   * @returns 可用于后续更新的消息 ID。
   */
  const ensureAssistantMessage = useCallback((messageId?: string): string | undefined => {
    if (!messageId) {
      return undefined;
    }

    const state = chatStore.getState();
    const existingMessage = state.messages.find((item) => item.id === messageId);
    if (existingMessage) {
      return messageId;
    }

    state.addMessage({
      id: messageId,
      message_uuid: messageId,
      content: '',
      role: 'ai',
      status: 'streaming',
      isFinished: false,
    });
    return messageId;
  }, []);

  /**
   * 统一完成当前运行中的 AI 消息。
   *
   * @param messageId 目标消息 ID。
   * @param payload 结束事件。
   */
  const finishStreamingMessage = useCallback(
    (messageId: string, payload: ChatTypes.AssistantStreamMessage) => {
      const state = chatStore.getState();
      const targetMessage = state.messages.find((item) => item.id === messageId);
      if (!targetMessage) {
        return;
      }

      const finalStatus = resolveAssistantFinalStatus(payload);
      const fallbackText =
        !targetMessage.content &&
        typeof payload.content.message === 'string' &&
        payload.content.message.trim()
          ? payload.content.message
          : targetMessage.content;

      state.updateMessage(messageId, {
        content: fallbackText,
        status: finalStatus,
        isFinished: true,
        activeToolCall: null,
      });
    },
    [],
  );

  /**
   * attach 到指定会话的流式输出。
   *
   * @param targetConversationUuid 目标会话 UUID。
   * @param initialMessageId 已知的消息 UUID。
   */
  const attachConversationStream = useCallback(
    (targetConversationUuid: string, initialMessageId?: string) => {
      abortActiveStream();

      const initialTargetMessageId =
        initialMessageId || findLastStreamingAssistantMessage(chatStore.getState().messages)?.id;
      currentStreamingMessageIdRef.current = ensureAssistantMessage(initialTargetMessageId);

      setLoading(true);
      setStopPending(false);

      const streamId = activeStreamIdRef.current + 1;
      activeStreamIdRef.current = streamId;
      const isCurrentStream = () => activeStreamIdRef.current === streamId;

      activeStreamControllerRef.current = attachAssistantStream(targetConversationUuid, {
        onMessage: (payload) => {
          if (!isCurrentStream()) {
            return;
          }

          const state = chatStore.getState();
          const trackedMessageId = currentStreamingMessageIdRef.current;
          const metaMessageId = payload.meta?.message_uuid;

          if (metaMessageId && trackedMessageId && trackedMessageId !== metaMessageId) {
            const trackedMessage = state.messages.find((item) => item.id === trackedMessageId);
            if (trackedMessage) {
              state.updateMessage(trackedMessageId, {
                id: metaMessageId,
                message_uuid: metaMessageId,
              });
            }
          }

          const resolvedMessageId = ensureAssistantMessage(metaMessageId || trackedMessageId);
          if (!resolvedMessageId) {
            return;
          }

          currentStreamingMessageIdRef.current = resolvedMessageId;

          if (
            payload.meta?.conversation_uuid &&
            conversationUuidRef.current !== payload.meta.conversation_uuid
          ) {
            conversationUuidRef.current = payload.meta.conversation_uuid;
            setConversationUuid(payload.meta.conversation_uuid);
          }

          const replace = isAssistantReplaceMessage(payload);
          const text = typeof payload.content.text === 'string' ? payload.content.text : '';

          if (payload.type === ChatTypes.MessageType.ANSWER && text && !payload.is_end) {
            updateMessageText(resolvedMessageId, 'content', text, replace);
          }

          if (payload.type === ChatTypes.MessageType.THINKING && text && !payload.is_end) {
            updateMessageText(resolvedMessageId, 'thinking', text, replace);
          }

          if (payload.type === ChatTypes.MessageType.CARD) {
            appendExtraEvent(resolvedMessageId, 'card', payload);
          }

          if (payload.type === ChatTypes.MessageType.ACTION) {
            appendExtraEvent(resolvedMessageId, 'action', payload);
          }

          if (
            payload.type === ChatTypes.MessageType.STATUS ||
            payload.type === ChatTypes.MessageType.FUNCTION_CALL ||
            payload.type === ChatTypes.MessageType.TOOL_RESPONSE
          ) {
            handleThoughtChainMessage(resolvedMessageId, payload);
          }

          if (payload.is_end) {
            finishStreamingMessage(resolvedMessageId, payload);
            currentStreamingMessageIdRef.current = undefined;
            activeStreamControllerRef.current = null;
            setLoading(false);
            setStopPending(false);
          }
        },
        onFinish: () => {
          if (!isCurrentStream()) {
            return;
          }

          activeStreamControllerRef.current = null;
          setLoading(false);
          setStopPending(false);
        },
        onError: (error) => {
          if (!isCurrentStream()) {
            return;
          }

          activeStreamControllerRef.current = null;
          setLoading(false);
          setStopPending(false);
          console.error('流式连接失败:', error);
        },
      });
    },
    [
      abortActiveStream,
      appendExtraEvent,
      ensureAssistantMessage,
      finishStreamingMessage,
      updateMessageText,
    ],
  );

  /**
   * 加载指定会话首页历史消息。
   *
   * @param uuid 会话 UUID。
   * @param resumeMessageId 需要优先恢复的消息 UUID。
   */
  const loadInitialHistory = useCallback(
    async (uuid: string, resumeMessageId?: string) => {
      const requestId = historyRequestIdRef.current + 1;
      historyRequestIdRef.current = requestId;
      setHistoryLoading(true);
      historyPageRef.current = 1;

      try {
        const data = await getConversation(uuid, 1, HISTORY_PAGE_SIZE);
        if (historyRequestIdRef.current !== requestId || conversationUuidRef.current !== uuid) {
          return;
        }

        const rawData = Array.isArray(data) ? data : (data as any)?.rows || [];
        const nextMessages = rawData.map(normalizeHistoryMessage);
        chatStore.getState().setMessages(nextMessages);
        setHasMoreHistory(nextMessages.length >= HISTORY_PAGE_SIZE);

        const streamingMessage =
          resumeMessageId || findLastStreamingAssistantMessage(nextMessages)?.id;
        if (streamingMessage) {
          attachConversationStream(uuid, streamingMessage);
        }
      } catch (error) {
        if (historyRequestIdRef.current === requestId && conversationUuidRef.current === uuid) {
          setHasMoreHistory(false);
        }
        console.error('加载历史消息失败:', error);
      } finally {
        if (historyRequestIdRef.current === requestId) {
          setHistoryLoading(false);
        }
      }
    },
    [attachConversationStream],
  );

  /**
   * 向上滚动加载更多历史消息。
   *
   * @returns 完成时的 Promise。
   */
  const loadMoreHistory = useCallback(async () => {
    const uuid = conversationUuidRef.current;
    if (!uuid || historyLoading || !hasMoreHistory) return;

    const requestId = historyRequestIdRef.current + 1;
    historyRequestIdRef.current = requestId;
    const nextPage = historyPageRef.current + 1;
    setHistoryLoading(true);

    try {
      const data = await getConversation(uuid, nextPage, HISTORY_PAGE_SIZE);
      if (historyRequestIdRef.current !== requestId || conversationUuidRef.current !== uuid) {
        return;
      }

      const rawData = Array.isArray(data) ? data : (data as any)?.rows || [];
      const nextMessages = rawData.map(normalizeHistoryMessage);
      if (nextMessages.length > 0) {
        if (conversationUuidRef.current !== uuid) return;
        chatStore.getState().prependMessages(nextMessages);
        historyPageRef.current = nextPage;
      }
      setHasMoreHistory(nextMessages.length >= HISTORY_PAGE_SIZE);
    } catch (error) {
      console.error('加载更多历史消息失败:', error);
    } finally {
      if (historyRequestIdRef.current === requestId) {
        setHistoryLoading(false);
      }
    }
  }, [hasMoreHistory, historyLoading]);

  /**
   * 路由 -> 会话状态同步。
   */
  useEffect(() => {
    if (!routeInitHandledRef.current) {
      routeInitHandledRef.current = true;
      if (routeConversationId) {
        resetConversationState(routeConversationId);
        void loadInitialHistory(routeConversationId);
      } else {
        chatStore.getState().clearMessages();
      }
      return;
    }

    if (internalNavigationRef.current.active) {
      const expectedPath = buildConversationPath(internalNavigationRef.current.uuid);
      if (window.location.pathname !== expectedPath) {
        internalNavigationRef.current = { active: false, uuid: undefined };
      }
    }

    if (internalNavigationRef.current.active) {
      if (routeConversationId !== internalNavigationRef.current.uuid) {
        return;
      }
      internalNavigationRef.current = { active: false, uuid: undefined };
    }

    if (routeConversationId === conversationUuidRef.current) return;

    invalidatePendingSubmit();
    abortActiveStream();
    resetConversationState(routeConversationId);
    if (routeConversationId) {
      void loadInitialHistory(routeConversationId);
    }
  }, [
    abortActiveStream,
    invalidatePendingSubmit,
    loadInitialHistory,
    resetConversationState,
    routeConversationId,
  ]);

  /**
   * 组件卸载时中断流与失效请求。
   */
  useEffect(
    () => () => {
      invalidatePendingSubmit();
      abortActiveStream();
      historyRequestIdRef.current += 1;
    },
    [abortActiveStream, invalidatePendingSubmit],
  );

  /**
   * 切换到指定会话。
   *
   * @param uuid 目标会话 UUID，null 表示新建对话。
   */
  const switchConversation = useCallback(
    (uuid: string | null) => {
      const targetUuid = uuid ?? undefined;

      if (targetUuid && targetUuid === conversationUuidRef.current) {
        return;
      }

      invalidatePendingSubmit();
      abortActiveStream();
      resetConversationState(targetUuid);

      updateConversationPath(targetUuid, false);

      if (targetUuid) {
        void loadInitialHistory(targetUuid);
      }
    },
    [
      abortActiveStream,
      invalidatePendingSubmit,
      loadInitialHistory,
      resetConversationState,
      updateConversationPath,
    ],
  );

  /**
   * 发送消息并启动 submit + attach。
   *
   * @param text 用户输入内容。
   * @param imageUrls 用户上传图片 URL 列表。
   * @param onSuccess 发送成功回调。
   * @param modelName 用户手动选择的前端自定义模型名；后端会映射为真实模型名。
   * @param reasoningEnabled 是否开启深度思考；由前端按当前模型显式传入。
   */
  const sendMessage = useCallback(
    (
      text: string,
      imageUrls: string[] = [],
      onSuccess?: () => void,
      modelName?: string,
      reasoningEnabled?: boolean,
    ) => {
      const question = text.trim();
      const normalizedImageUrls = imageUrls.map((imageUrl) => imageUrl.trim()).filter(Boolean);
      const normalizedModelName = String(modelName || '').trim();
      const normalizedReasoningEnabled = Boolean(reasoningEnabled);
      if (!question && normalizedImageUrls.length === 0) return;
      if (!normalizedModelName) {
        antdMessage.warning('请先选择聊天模型');
        return;
      }

      const sourceConversationUuid = conversationUuidRef.current;
      const submitRequestId = submitRequestIdRef.current + 1;
      submitRequestIdRef.current = submitRequestId;
      setSubmitting(true);

      void (async () => {
        try {
          const submitData = await submitAssistantMessage({
            question,
            image_urls: normalizedImageUrls.length > 0 ? normalizedImageUrls : undefined,
            conversation_uuid: sourceConversationUuid,
            model_name: normalizedModelName,
            reasoning_enabled: normalizedReasoningEnabled,
          });

          if (submitRequestIdRef.current !== submitRequestId) {
            return;
          }

          const targetConversationUuid = submitData.conversation_uuid;
          const targetMessageUuid = submitData.message_uuid;
          const isNewConversation =
            !sourceConversationUuid || sourceConversationUuid !== targetConversationUuid;

          invalidatePendingSubmit();
          abortActiveStream();

          if (isNewConversation) {
            resetConversationState(targetConversationUuid);
            conversationUuidRef.current = targetConversationUuid;
            setConversationUuid(targetConversationUuid);
            updateConversationPath(targetConversationUuid, false);
            onConversationCreatedRef.current?.(targetConversationUuid);
          }

          chatStore.getState().addMessage({
            id: generateMessageId(),
            content: buildUserMessageMarkdown(question, normalizedImageUrls),
            role: 'user',
            isFinished: true,
          });
          chatStore.getState().addMessage({
            id: targetMessageUuid,
            message_uuid: targetMessageUuid,
            content: '',
            role: 'ai',
            status: 'streaming',
            isFinished: false,
          });
          setContent('');
          onSuccess?.();

          if (!isNewConversation) {
            conversationUuidRef.current = targetConversationUuid;
            setConversationUuid(targetConversationUuid);
          }

          attachConversationStream(targetConversationUuid, targetMessageUuid);
        } catch (error) {
          if (submitRequestIdRef.current !== submitRequestId) {
            return;
          }

          invalidatePendingSubmit();

          if (
            error instanceof AssistantApiError &&
            error.code === 409 &&
            error.data &&
            typeof error.data === 'object'
          ) {
            const conflictData = error.data as ChatTypes.SubmitResponseData;
            antdMessage.info('当前会话已有回答进行中，已为你恢复连接');

            if (
              conflictData.conversation_uuid &&
              conflictData.conversation_uuid !== conversationUuidRef.current
            ) {
              abortActiveStream();
              resetConversationState(conflictData.conversation_uuid);
              updateConversationPath(conflictData.conversation_uuid, false);
              void loadInitialHistory(conflictData.conversation_uuid, conflictData.message_uuid);
              return;
            }

            if (conflictData.conversation_uuid) {
              attachConversationStream(conflictData.conversation_uuid, conflictData.message_uuid);
              return;
            }
          }

          if (error instanceof AssistantApiError) {
            const backendMessage = typeof error.message === 'string' ? error.message.trim() : '';
            if (backendMessage === UNSUPPORTED_IMAGE_UNDERSTANDING_TEXT) {
              antdMessage.warning(backendMessage);
              return;
            }
            if (backendMessage) {
              antdMessage.error(backendMessage);
              return;
            }
          }

          console.error('提交消息失败:', error);
          antdMessage.error('发送失败，请稍后重试');
        }
      })();
    },
    [
      abortActiveStream,
      attachConversationStream,
      invalidatePendingSubmit,
      loadInitialHistory,
      resetConversationState,
      updateConversationPath,
    ],
  );

  /**
   * 发送停止生成请求。
   */
  const stopGenerating = useCallback(() => {
    const targetConversationUuid = conversationUuidRef.current;
    if (!targetConversationUuid || !loading || stopPending) {
      return;
    }

    setStopPending(true);

    void (async () => {
      try {
        await stopAssistantMessage({ conversation_uuid: targetConversationUuid });
      } catch (error) {
        console.error('停止生成失败:', error);
        setStopPending(false);
        antdMessage.error('停止失败，请稍后重试');
      }
    })();
  }, [loading, stopPending]);

  return {
    messages,
    content,
    setContent,
    submitting,
    loading,
    stopPending,
    sendMessage,
    stopGenerating,
    conversationUuid,
    switchConversation,
    loadMoreHistory,
    hasMoreHistory,
    historyLoading,
    setOnConversationCreated,
  };
}
