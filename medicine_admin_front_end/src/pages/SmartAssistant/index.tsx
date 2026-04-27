/**
 * 智能助手页面
 *
 * 页面职责：布局 + 组合子组件，不包含业务逻辑。
 * - 聊天逻辑 → _useChat.ts
 * - 思维链处理 → _thoughtChainHandler.ts
 *
 * 渲染策略：
 * - WelcomeScreen 和 MessageList 始终保持挂载，用 CSS opacity 切换，
 *   避免条件渲染导致的 DOM 卸载/挂载抖动。
 * - 新会话 SSE 过程中不触发路由跳转，等 SSE 结束后再安全更新 URL，
 *   防止路由切换导致组件重新挂载、AI 回复中断。
 */
import { XProvider } from '@ant-design/x';
import zhCN_X from '@ant-design/x/locale/zh_CN';
import { Flex, Spin, Typography, theme, message } from 'antd';
import type { UploadFile } from 'antd/es/upload/interface';
import zhCN from 'antd/locale/zh_CN';
import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useThemeContext } from '@/contexts/ThemeContext';
import type { FileUploadTypes } from '@/api/core/file';
import { getAdminAssistantConfig, type SystemModelTypes } from '@/api/llm-manage/systemModels';
import { useUserStore } from '@/store/userStore';
import { useChat } from './_useChat';
import { SttRecorder } from './_stt';
import ChatInput from './components/ChatInput';
import ConversationList, { type ConversationListHandle } from './components/ConversationList';
import ModelSelector from './components/ModelSelector';
import type { MessageItem } from './components/MessageList';
import MessageList from './components/MessageList';
import styles from './index.module.less';

const { Text, Title } = Typography;
/** 聊天图片最多上传 5 张。 */
const MAX_CHAT_IMAGE_COUNT = 5;
/** 管理端聊天模型选择本地存储前缀。 */
const SMART_ASSISTANT_MODEL_SELECTION_STORAGE_PREFIX = 'smart-assistant:model-selection';
/** 未配置聊天展示模型时的提示文案。 */
const MODEL_CONFIG_REQUIRED_HINT = '请先到系统模型配置 > 管理端 > 配置展示模型';
/** 未配置聊天展示模型时的占位文案。 */
const MODEL_SELECTOR_EMPTY_LABEL = '未配置模型';
/** 模型不支持图片理解时的统一提示文案。 */
const UNSUPPORTED_IMAGE_UPLOAD_HINT = '此模型不支持图片理解';

/** 新会话欢迎文案。 */
const FRESH_CONVERSATION_TITLE = '开始一个新对话，告诉我你想处理什么。';

/** 新对话欢迎引导屏 */
const WelcomeScreen: React.FC = () => (
  <div className={styles.welcomeScreen}>
    <Title level={3} className={styles.welcomeTitle}>
      {FRESH_CONVERSATION_TITLE}
    </Title>
  </div>
);

/**
 * 将 store 中的 Message[] 转换为 MessageList 组件所需的 MessageItem[]
 */
function toMessageItems(messages: ReturnType<typeof useChat>['messages']): MessageItem[] {
  return messages.map(
    ({
      id,
      content,
      role,
      status,
      thoughtChain,
      activeToolCall,
      isFinished,
      thinking,
      extraEvents,
      dividerProps,
    }) => ({
      key: id,
      role,
      content,
      loading: status === 'streaming' || (role === 'ai' && isFinished === false),
      isFinished,
      thoughtChain,
      activeToolCall,
      thinking,
      extraEvents,
      dividerProps,
    }),
  );
}

/**
 * 构建管理端聊天模型选择的本地存储 key。
 *
 * @param user 当前登录用户。
 * @returns 当前用户对应的本地存储 key。
 */
function buildModelSelectionStorageKey(user: API.CurrentUser | null | undefined): string {
  const userIdentity = String(user?.userid || user?.id || 'anonymous').trim() || 'anonymous';
  return `${SMART_ASSISTANT_MODEL_SELECTION_STORAGE_PREFIX}:${userIdentity}`;
}

/**
 * 读取本地缓存的展示模型名称。
 *
 * @param storageKey 本地存储 key。
 * @returns 已缓存的展示模型名称；不存在时返回 undefined。
 */
function readCachedSelectedModelName(storageKey: string): string | undefined {
  if (typeof window === 'undefined') {
    return undefined;
  }
  const cachedModelName = window.localStorage.getItem(storageKey);
  return cachedModelName ? cachedModelName : undefined;
}

/**
 * 写入当前选中的展示模型名称。
 *
 * @param storageKey 本地存储 key。
 * @param customModelName 当前选中的展示模型名称。
 * @returns 无返回值。
 */
function writeCachedSelectedModelName(storageKey: string, customModelName: string): void {
  if (typeof window === 'undefined') {
    return;
  }
  window.localStorage.setItem(storageKey, customModelName);
}

/**
 * 清理当前用户缓存的展示模型名称。
 *
 * @param storageKey 本地存储 key。
 * @returns 无返回值。
 */
function clearCachedSelectedModelName(storageKey: string): void {
  if (typeof window === 'undefined') {
    return;
  }
  window.localStorage.removeItem(storageKey);
}

/**
 * 解析当前应默认选中的展示模型。
 *
 * @param modelOptions 当前可选展示模型列表。
 * @param cachedModelName 本地缓存的展示模型名称。
 * @returns 默认选中的展示模型；无可选项时返回 null。
 */
function resolveDefaultSelectedModel(
  modelOptions: SystemModelTypes.AdminAssistantChatDisplayModel[],
  cachedModelName?: string,
): SystemModelTypes.AdminAssistantChatDisplayModel | null {
  const cachedModel = modelOptions.find((item) => item.customModelName === cachedModelName);
  if (cachedModel) {
    return cachedModel;
  }
  return modelOptions[0] || null;
}

const SmartAssistantPage: React.FC = () => {
  const { isDark } = useThemeContext();
  const currentUser = useUserStore((state) => state.user);
  const {
    messages,
    content,
    setContent,
    submitting,
    loading,
    sendMessage,
    stopGenerating,
    conversationUuid,
    switchConversation,
    loadMoreHistory,
    hasMoreHistory,
    historyLoading,
    setOnConversationCreated,
  } = useChat();
  const { token } = theme.useToken();
  const pageContainerClassName = `${styles.pageContainer} ${isDark ? styles.dark : ''}`;
  const [recording, setRecording] = useState(false);
  const [sttLoading, setSttLoading] = useState(false);
  const [imageFileList, setImageFileList] = useState<UploadFile[]>([]);
  const sttRecorderRef = useRef<SttRecorder | null>(null);
  const contentRef = useRef(content);
  const modelSelectionStorageKey = buildModelSelectionStorageKey(currentUser);

  // -------- 模型选择状态 --------
  const [modelOptions, setModelOptions] = useState<
    SystemModelTypes.AdminAssistantChatDisplayModel[]
  >([]);
  const [modelOptionsLoading, setModelOptionsLoading] = useState(false);
  const [selectedModel, setSelectedModel] =
    useState<SystemModelTypes.AdminAssistantChatDisplayModel | null>(null);
  const [deepThinking, setDeepThinking] = useState(false);
  const hasAvailableModels = modelOptions.length > 0;

  /** 当前所选模型是否支持深度思考。 */
  const showDeepThinking = Boolean(selectedModel?.supportReasoning);

  /** 加载管理端聊天界面可选展示模型列表。 */
  useEffect(() => {
    let cancelled = false;
    setModelOptionsLoading(true);
    getAdminAssistantConfig()
      .then((config) => {
        if (cancelled) return;
        const list = config?.chatDisplayModels ?? [];
        const cachedModelName = readCachedSelectedModelName(modelSelectionStorageKey);
        const defaultSelectedModel = resolveDefaultSelectedModel(list, cachedModelName);
        setModelOptions(list);
        setSelectedModel(defaultSelectedModel);
        setDeepThinking(false);
        if (defaultSelectedModel?.customModelName) {
          writeCachedSelectedModelName(
            modelSelectionStorageKey,
            defaultSelectedModel.customModelName,
          );
        } else {
          clearCachedSelectedModelName(modelSelectionStorageKey);
        }
      })
      .catch(() => {
        /* 加载失败不阻塞页面 */
      })
      .finally(() => {
        if (!cancelled) setModelOptionsLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [modelSelectionStorageKey]);

  /**
   * 切换模型时，若新模型不支持深度思考则自动关闭。
   *
   * @param opt 新选中的模型选项。
   */
  const handleModelChange = useCallback(
    (opt: SystemModelTypes.AdminAssistantChatDisplayModel) => {
      if (!opt.supportVision && imageFileList.length > 0) {
        setImageFileList([]);
        message.warning(UNSUPPORTED_IMAGE_UPLOAD_HINT);
      }
      setSelectedModel(opt);
      setDeepThinking(false);
      writeCachedSelectedModelName(modelSelectionStorageKey, opt.customModelName);
    },
    [imageFileList.length, modelSelectionStorageKey],
  );

  // 在 effect 中同步 ref，避免在渲染阶段直接写入 ref（违反 react-hooks/refs 规则）
  useEffect(() => {
    contentRef.current = content;
  });

  // 语音输入状态发生变化时
  const handleRecordingChange = useCallback(
    (nextRecording: boolean) => {
      if (nextRecording) {
        setSttLoading(true);
        console.log('[语音输入] 开始');

        // 结束上一次的（如果有）
        if (sttRecorderRef.current) {
          sttRecorderRef.current.stop();
        }

        // 实例化新的录音器
        const recorder = new SttRecorder({
          onStarted: (maxDuration) => {
            console.log(`[语音输入] 已连接，最大时长 ${maxDuration} 秒`);
            setSttLoading(false);
            setRecording(true);
          },
          onTranscript: (text, _isFinal) => {
            // 将识别结果追加到当前输入框中
            // 由于内容可能是基于当前状态增量的，我们直接替换内容
            // 依赖 _stt 内部维护 finalizedText 和增量逻辑
            setContent(text);
          },
          onCompleted: () => {
            console.log('[语音输入] 识别完成');
            setRecording(false);
            setSttLoading(false);
          },
          onTimeout: (msg) => {
            message.warning(msg);
            setRecording(false);
            setSttLoading(false);
          },
          onError: (msg) => {
            message.error(msg);
            setRecording(false);
            setSttLoading(false);
          },
        });

        sttRecorderRef.current = recorder;
        // 把输入框当前的内容传进去作为前缀，这样识别的文本会追加在后面
        recorder.start(contentRef.current);
      } else {
        console.log('[语音输入] 结束');
        setRecording(false);
        setSttLoading(false);
        if (sttRecorderRef.current) {
          sttRecorderRef.current.stop();
          sttRecorderRef.current = null;
        }
      }
    },
    [setContent],
  );

  // 组件卸载时清理录音资源
  useEffect(() => {
    return () => {
      if (sttRecorderRef.current) {
        sttRecorderRef.current.stop();
        sttRecorderRef.current = null;
      }
    };
  }, []);

  /** 会话列表 ref，用于在新会话创建后刷新列表 */
  const conversationListRef = useRef<ConversationListHandle>(null);

  /** 注册新会话创建回调：拿到 conversation_uuid 后立即本地插入“新聊天” */
  useEffect(() => {
    setOnConversationCreated((uuid) => {
      conversationListRef.current?.addCreatedConversation(uuid);
    });
    return () => setOnConversationCreated(null);
  }, [setOnConversationCreated]);

  /** 将 store messages 映射为 MessageList 需要的数据格式 */
  const items = useMemo(() => toMessageItems(messages), [messages]);

  /**
   * 是否为真正的新会话：
   * 无会话 UUID、无消息、且未处于历史加载中。
   */
  const isFreshConversation = !conversationUuid && messages.length === 0 && !historyLoading;

  /**
   * 是否使用居中输入框布局。
   * 首次点击发送后，立即切换到底部布局，保证位移动画能马上发生。
   */
  const useCenteredLayout = isFreshConversation && !submitting;

  /**
   * 是否显示加载过渡态：
   * 切换到已有会话时，messages 清空后历史消息还在加载中，
   * 此时展示居中 Spin 防止空白闪屏。
   */
  const showTransitionLoading = historyLoading && items.length === 0;

  /** 会话切换处理 */
  const handleConversationChange = useCallback(
    (uuid: string | null) => {
      switchConversation(uuid);
    },
    [switchConversation],
  );

  /**
   * 提取当前已上传成功的图片 URL 列表。
   */
  const selectedImageUrls = useMemo(
    () =>
      imageFileList
        .map((file) => {
          if (file.url) {
            return file.url;
          }
          const response = file.response as FileUploadTypes.FileUploadVo | undefined;
          return response?.fileUrl;
        })
        .filter((imageUrl): imageUrl is string => Boolean(imageUrl)),
    [imageFileList],
  );

  /**
   * 统一处理发送（文本 + 图片）。
   *
   * @param nextContent 输入框文本。
   */
  const handleSubmitMessage = useCallback(
    (nextContent: string) => {
      if (!selectedModel?.customModelName) {
        message.warning(MODEL_CONFIG_REQUIRED_HINT);
        return;
      }
      sendMessage(
        nextContent,
        selectedImageUrls,
        () => {
          setImageFileList([]);
        },
        selectedModel.customModelName,
        selectedModel.supportReasoning ? deepThinking : false,
      );
    },
    [selectedImageUrls, sendMessage, selectedModel, deepThinking],
  );

  return (
    <XProvider locale={{ ...zhCN, ...zhCN_X }}>
      <div className={pageContainerClassName}>
        <div className={styles.card}>
          <Flex className={styles.layout}>
            {/* ---- 左侧会话列表 ---- */}
            <div
              className={styles.sidebar}
              style={{ borderRight: `1px solid ${token.colorBorderSecondary}` }}
            >
              <ConversationList
                ref={conversationListRef}
                style={{ height: '100%', borderRadius: 0, width: 250 }}
                activeKey={conversationUuid}
                onConversationChange={handleConversationChange}
              />
            </div>

            {/* ---- 右侧聊天区域 ---- */}
            <div className={styles.chatArea}>
              <div className={styles.chatHeader}>
                <ModelSelector
                  options={modelOptions}
                  value={selectedModel?.customModelName}
                  onChange={handleModelChange}
                  loading={modelOptionsLoading}
                  disabled={!hasAvailableModels}
                  emptyLabel={MODEL_SELECTOR_EMPTY_LABEL}
                />
              </div>

              {/*
               * chatContent 是相对定位容器，两个面板绝对定位叠放。
               * 切换时仅改变 opacity，两个面板始终保持挂载，避免 DOM 抖动。
               */}
              <div className={styles.chatContent}>
                {/* 会话切换中的加载遮罩 */}
                {showTransitionLoading && (
                  <div
                    className={styles.transitionMask}
                    style={{ background: token.colorBgContainer }}
                  >
                    <Spin size="large" />
                  </div>
                )}
                {/* 欢迎屏面板 */}
                <div className={useCenteredLayout ? styles.paneVisible : styles.paneHidden}>
                  <WelcomeScreen />
                </div>
                {/* 消息列表面板 */}
                <div className={useCenteredLayout ? styles.paneHidden : styles.paneVisible}>
                  <MessageList
                    items={items}
                    onLoadMore={loadMoreHistory}
                    hasMore={hasMoreHistory}
                    historyLoading={historyLoading}
                  />
                </div>
              </div>

              <Flex
                vertical
                align="center"
                className={`${styles.senderContainer} ${
                  useCenteredLayout ? styles.senderContainerCentered : styles.senderContainerDocked
                }`}
              >
                <ChatInput
                  value={content}
                  onChange={setContent}
                  onSubmit={handleSubmitMessage}
                  onCancel={stopGenerating}
                  layoutMode={useCenteredLayout ? 'centered' : 'docked'}
                  loading={loading}
                  submitting={submitting}
                  disabled={!hasAvailableModels}
                  placeholder={hasAvailableModels ? '随便问我什么……' : '请先配置聊天模型'}
                  imageFileList={imageFileList}
                  onImageFileListChange={setImageFileList}
                  maxImageCount={MAX_CHAT_IMAGE_COUNT}
                  imageUploadEnabled={Boolean(selectedModel?.supportVision)}
                  imageUploadDisabledMessage={UNSUPPORTED_IMAGE_UPLOAD_HINT}
                  hasContent={
                    hasAvailableModels && (!!content.trim() || selectedImageUrls.length > 0)
                  }
                  recording={recording}
                  onRecordingChange={handleRecordingChange}
                  sttLoading={sttLoading}
                  showDeepThinking={hasAvailableModels && showDeepThinking}
                  deepThinking={deepThinking}
                  onDeepThinkingChange={setDeepThinking}
                />
                <Text
                  type={hasAvailableModels ? 'secondary' : 'warning'}
                  className={`${styles.hintText} ${
                    useCenteredLayout ? styles.hintTextHidden : styles.hintTextVisible
                  } ${!hasAvailableModels ? styles.hintTextWarning : ''}`}
                >
                  {hasAvailableModels
                    ? 'AI 也会犯错，请仔细甄别回答内容'
                    : MODEL_CONFIG_REQUIRED_HINT}
                </Text>
              </Flex>
            </div>
          </Flex>
        </div>
      </div>
    </XProvider>
  );
};

export default SmartAssistantPage;
