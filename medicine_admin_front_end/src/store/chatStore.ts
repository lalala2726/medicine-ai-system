import { create } from 'zustand';

export interface ToolCall {
  id: string;
  description: string;
  status: 'loading' | 'success' | 'error';
}

/** 思维链子步骤（function_call / tool_response） */
export interface ThoughtStep {
  id: string;
  /** 步骤描述 */
  message: string;
  /** 工具/函数名称 */
  name?: string;
  /** 调用参数（JSON 字符串） */
  arguments?: string;
  /** 执行结果 */
  result?: string;
  status: 'loading' | 'success' | 'error';
}

/** 思维链顶层节点（status 类型消息） */
export interface ThoughtNode {
  id: string;
  /** 节点标识，如 "router"、"planner" 等 */
  node: string;
  /** 节点描述 */
  message: string;
  status: 'loading' | 'success' | 'error';
  /** 子步骤列表 */
  children: ThoughtStep[];
}

/** 独立工具调用（无父级 status 节点时使用） */
export interface ActiveToolCall {
  id: string;
  message: string;
  name?: string;
  /** 工具调用开始时间（用于确保最少显示时长） */
  startTime?: number;
}

/** AI 消息运行状态 */
export type AssistantMessageStatus = 'streaming' | 'success' | 'cancelled' | 'error';

/** 未接入协议的附加事件 */
export interface AssistantExtraEvent {
  /** 事件唯一标识 */
  id: string;
  /** 事件类型 */
  type: 'card' | 'action';
  /** 原始内容 */
  content: Record<string, any> | null;
  /** 原始元数据 */
  meta?: Record<string, any>;
  /** 事件时间戳 */
  timestamp?: number;
}

export interface Message {
  id: string;
  content: string;
  role: 'user' | 'ai' | 'divider';
  /** 后端消息 UUID */
  message_uuid?: string;
  /** AI 消息运行状态 */
  status?: AssistantMessageStatus;
  thinking?: string;
  toolCalls?: ToolCall[];
  /** 思维链数据 */
  thoughtChain?: ThoughtNode[];
  /** 独立工具调用（无父节点时，loading 中显示，完成后隐藏） */
  activeToolCall?: ActiveToolCall | null;
  /** 当前消息收到但尚未接入协议的附加事件 */
  extraEvents?: AssistantExtraEvent[];
  isFinished?: boolean;
  /** 仅针对 role='divider' 的附加配置，如 { variant: 'dashed', plain: true } */
  dividerProps?: Record<string, any>;
}

type ChatState = {
  messages: Message[];
};

type ChatActions = {
  /** 追加一条消息 */
  addMessage: (message: Message) => void;
  /** 在消息列表头部插入一批消息（用于加载历史记录） */
  prependMessages: (messages: Message[]) => void;
  /** 更新指定消息 */
  updateMessage: (id: string, updates: Partial<Message>) => void;
  /** 整体替换消息列表 */
  setMessages: (messages: Message[]) => void;
  /** 清空消息列表 */
  clearMessages: () => void;
};

type ChatStore = ChatState & ChatActions;

const initialState: ChatState = {
  messages: [],
};

export const useChatStore = create<ChatStore>()((set) => ({
  ...initialState,
  addMessage: (message) => {
    set((state) => ({
      messages: [...state.messages, message],
    }));
  },
  prependMessages: (messages) => {
    set((state) => ({
      messages: [...messages, ...state.messages],
    }));
  },
  updateMessage: (id, updates) => {
    set((state) => ({
      messages: state.messages.map((msg) => (msg.id === id ? { ...msg, ...updates } : msg)),
    }));
  },
  setMessages: (messages) => {
    set({ messages });
  },
  clearMessages: () => {
    set(initialState);
  },
}));

export const chatStore = useChatStore;
