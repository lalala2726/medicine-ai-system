/**
 * 思维链消息处理器
 *
 * 负责将后端 SSE 推送的 status / function_call / tool_response 消息
 * 转换为前端 ThoughtChain 组件所需的数据结构。
 *
 * 该模块为纯函数，不依赖 React，便于单元测试。
 */
import { ChatTypes } from '@/api/chat';
import { chatStore, type ThoughtNode, type ThoughtStep } from '@/store';

// ======================== 常量 ========================

/** 独立工具调用结束后的最短展示时长（毫秒） */
const MIN_TOOL_DISPLAY_TIME = 1000;

// ======================== 内部工具函数 ========================

/**
 * 根据 parent_node 在思维链中查找匹配的父节点索引
 * 从后向前遍历，返回最后一个 node 字段与 parentNode 相等的索引；
 * 若未指定 parentNode 或未找到匹配项，fallback 到最后一个节点。
 */
function findParentNodeIndex(chain: ThoughtNode[], parentNode?: string | null): number {
  const fallback = chain.length - 1;
  if (!parentNode) return fallback;

  for (let i = chain.length - 1; i >= 0; i--) {
    if (chain[i].node === parentNode) {
      return i;
    }
  }
  return fallback;
}

/**
 * 在子步骤列表中查找需要更新的 loading 步骤索引
 * 优先根据 name 精确匹配，否则 fallback 到最后一个 loading 步骤。
 */
function findLoadingStepIndex(children: ThoughtStep[], name?: string | null): number {
  // 有 name 时优先精确匹配
  if (name) {
    for (let i = children.length - 1; i >= 0; i--) {
      if (children[i].name === name && children[i].status === 'loading') {
        return i;
      }
    }
  }
  // fallback：最后一个 loading 步骤
  for (let i = children.length - 1; i >= 0; i--) {
    if (children[i].status === 'loading') {
      return i;
    }
  }
  return -1;
}

// ======================== 各消息类型处理器 ========================

/**
 * 处理 status 类型消息 —— 管理顶层思维节点的 start / end
 */
function handleStatusMessage(
  aiMsgId: string,
  content: ChatTypes.Content,
  chain: ThoughtNode[],
): void {
  if (content.state === 'start') {
    // 新增一个顶层思维节点
    chain.push({
      id: `${aiMsgId}-node-${Date.now()}`,
      node: content.node ?? 'unknown',
      message: content.message ?? '处理中...',
      status: 'loading',
      children: [],
    });
  } else if (content.state === 'end') {
    // 从后往前查找匹配 node 的 loading 节点，将其标记为 success
    for (let i = chain.length - 1; i >= 0; i--) {
      if (chain[i].node === content.node && chain[i].status === 'loading') {
        chain[i] = { ...chain[i], status: 'success' };
        break;
      }
    }
  }
}

/**
 * 处理独立工具调用（思维链为空时 function_call 直接显示为顶层加载态）
 * @returns true 表示已处理完毕（独立工具调用场景），调用方可直接 return
 */
function handleStandaloneToolCall(
  aiMsgId: string,
  content: ChatTypes.Content,
  state: ReturnType<typeof chatStore.getState>,
): boolean {
  if (content.state === 'start') {
    state.updateMessage(aiMsgId, {
      activeToolCall: {
        id: `${aiMsgId}-tool-${Date.now()}`,
        message: content.message ?? '调用工具...',
        name: content.name ?? undefined,
        startTime: Date.now(),
      },
    });
  } else if (content.state === 'end') {
    // 保证已显示至少 MIN_TOOL_DISPLAY_TIME 毫秒再移除
    const currentMsg = state.messages.find((m) => m.id === aiMsgId);
    const startTime = currentMsg?.activeToolCall?.startTime;
    if (startTime) {
      const remaining = Math.max(0, MIN_TOOL_DISPLAY_TIME - (Date.now() - startTime));
      setTimeout(() => state.updateMessage(aiMsgId, { activeToolCall: null }), remaining);
    } else {
      state.updateMessage(aiMsgId, { activeToolCall: null });
    }
  }
  return true;
}

/**
 * 处理 function_call 类型消息 —— 在对应父节点下新增 / 完成子步骤
 */
function handleFunctionCallMessage(
  aiMsgId: string,
  content: ChatTypes.Content,
  chain: ThoughtNode[],
): void {
  const parentIdx = findParentNodeIndex(chain, content.parent_node);
  const parentNode = chain[parentIdx];

  if (content.state === 'start') {
    // 追加新的 loading 子步骤
    const newStep: ThoughtStep = {
      id: `${aiMsgId}-step-${Date.now()}`,
      message: content.message ?? '调用工具...',
      name: content.name ?? undefined,
      arguments: content.arguments ?? undefined,
      status: 'loading',
    };
    chain[parentIdx] = {
      ...parentNode,
      children: [...parentNode.children, newStep],
    };
  } else if (content.state === 'end') {
    // 将匹配的 loading 子步骤标记为 success
    const children = [...parentNode.children];
    const targetIdx = findLoadingStepIndex(children, content.name);
    if (targetIdx !== -1) {
      children[targetIdx] = {
        ...children[targetIdx],
        status: 'success',
        result: content.result ?? undefined,
      };
      chain[parentIdx] = { ...parentNode, children };
    }
  }
}

/**
 * 处理 tool_response 类型消息 —— 更新对应父节点下最后一个子步骤的结果
 */
function handleToolResponseMessage(content: ChatTypes.Content, chain: ThoughtNode[]): void {
  if (chain.length === 0) return;

  const parentIdx = findParentNodeIndex(chain, content.parent_node);
  const parentNode = chain[parentIdx];
  const children = [...parentNode.children];

  if (children.length > 0) {
    const lastIdx = children.length - 1;
    children[lastIdx] = {
      ...children[lastIdx],
      result: content.result ?? content.text ?? undefined,
    };
    chain[parentIdx] = { ...parentNode, children };
  }
}

// ======================== 导出主入口 ========================

/**
 * 处理 SSE 消息中的思维链数据
 *
 * 根据消息类型（status / function_call / tool_response）分发到对应处理器，
 * 更新目标 AI 消息上的 thoughtChain 字段。
 *
 * @param aiMsgId 当前 AI 消息的 id
 * @param data    后端推送的完整 SSE 消息体
 */
export function handleThoughtChainMessage(
  aiMsgId: string,
  data: ChatTypes.AssistantStreamMessage,
): void {
  const { type, content } = data;

  // 只处理与思维链相关的三种消息类型
  if (
    type !== ChatTypes.MessageType.STATUS &&
    type !== ChatTypes.MessageType.FUNCTION_CALL &&
    type !== ChatTypes.MessageType.TOOL_RESPONSE
  ) {
    return;
  }

  const state = chatStore.getState();
  const msg = state.messages.find((m) => m.id === aiMsgId);
  if (!msg) return;

  // 浅拷贝思维链，确保 immutable 更新
  const chain: ThoughtNode[] = [...(msg.thoughtChain ?? [])];

  // ---------- 按类型分发处理 ----------

  if (type === ChatTypes.MessageType.STATUS) {
    handleStatusMessage(aiMsgId, content, chain);
  } else if (type === ChatTypes.MessageType.FUNCTION_CALL) {
    // 思维链为空时按独立工具调用处理，不写入 chain
    if (chain.length === 0) {
      const handled = handleStandaloneToolCall(aiMsgId, content, state);
      if (handled) return;
    }
    handleFunctionCallMessage(aiMsgId, content, chain);
  } else if (type === ChatTypes.MessageType.TOOL_RESPONSE) {
    handleToolResponseMessage(content, chain);
  }

  // 思维链只更新消息内容，不改写消息最终运行状态，
  // 避免覆盖 streaming / cancelled / error 等协议态。
  state.updateMessage(aiMsgId, { thoughtChain: chain });
}
