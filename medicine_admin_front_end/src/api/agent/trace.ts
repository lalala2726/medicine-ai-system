import { requestClient } from '@/utils/request';
import type { PageRequest, TableDataResult } from '@/types';

export namespace AgentTraceTypes {
  /** Agent Trace 运行状态。 */
  export type TraceStatus = 'running' | 'success' | 'error' | 'cancelled';

  /** Agent Trace span 类型。 */
  export type SpanType = 'graph' | 'node' | 'middleware' | 'model' | 'tool';

  /** Agent Trace 树节点类型。 */
  export type SpanTreeNodeType = 'span_group' | 'span' | 'model_call';

  /** Agent Trace 列表查询参数。 */
  export interface RunListRequest extends PageRequest {
    /** Trace 唯一标识。 */
    traceId?: string;
    /** 会话 UUID。 */
    conversationUuid?: string;
    /** AI 消息 UUID。 */
    assistantMessageUuid?: string;
    /** 用户 ID。 */
    userId?: number;
    /** 会话类型：admin/client。 */
    conversationType?: string;
    /** 运行状态。 */
    status?: TraceStatus;
    /** Graph 名称。 */
    graphName?: string;
    /** 开始时间范围起点。 */
    startTime?: string;
    /** 开始时间范围终点。 */
    endTime?: string;
  }

  /** Agent Trace 列表项。 */
  export interface RunListVo {
    /** Trace 唯一标识。 */
    traceId?: string;
    /** 会话 UUID。 */
    conversationUuid?: string;
    /** AI 消息 UUID。 */
    assistantMessageUuid?: string;
    /** 用户 ID。 */
    userId?: number;
    /** 会话类型：admin/client。 */
    conversationType?: string;
    /** Graph 名称。 */
    graphName?: string;
    /** 入口标识。 */
    entrypoint?: string;
    /** 运行状态。 */
    status?: TraceStatus;
    /** 开始时间。 */
    startedAt?: string;
    /** 结束时间。 */
    endedAt?: string;
    /** 总耗时毫秒。 */
    durationMs?: number;
    /** 输入 token 数。 */
    inputTokens?: number;
    /** 输出 token 数。 */
    outputTokens?: number;
    /** 总 token 数。 */
    totalTokens?: number;
    /** 用户最新输入文本。 */
    inputText?: string;
    /** AI 最终输出文本。 */
    outputText?: string;
  }

  /** Agent Trace span 明细。 */
  export interface SpanVo {
    /** Span 唯一标识。 */
    spanId?: string;
    /** 父 Span 唯一标识。 */
    parentSpanId?: string;
    /** Span 类型。 */
    spanType?: SpanType;
    /** Span 名称。 */
    name?: string;
    /** Span 状态。 */
    status?: TraceStatus;
    /** 开始时间。 */
    startedAt?: string;
    /** 结束时间。 */
    endedAt?: string;
    /** 耗时毫秒。 */
    durationMs?: number;
    /** 输入载荷。 */
    inputPayload?: unknown;
    /** 输出载荷。 */
    outputPayload?: unknown;
    /** 附加属性。 */
    attributes?: Record<string, unknown> | null;
    /** Token 用量。 */
    tokenUsage?: Record<string, unknown> | null;
    /** 模型调用详情。 */
    modelDetail?: ModelDetailVo | null;
    /** 节点执行详情。 */
    nodeDetail?: NodeDetailVo | null;
    /** 错误载荷。 */
    errorPayload?: unknown;
    /** Trace 内顺序号。 */
    sequence?: number;
  }

  /** Agent Trace 节点执行详情。 */
  export interface NodeDetailVo {
    /** 节点名称。 */
    nodeName?: string;
    /** 节点状态。 */
    status?: TraceStatus | string;
    /** 开始时间。 */
    startedAt?: string;
    /** 结束时间。 */
    endedAt?: string;
    /** 耗时毫秒。 */
    durationMs?: number;
    /** 当前节点子树汇总 Token 用量。 */
    tokenUsage?: Record<string, unknown> | null;
    /** 当前节点输入载荷。 */
    inputPayload?: unknown;
    /** 当前节点输出载荷。 */
    outputPayload?: unknown;
    /** 当前节点错误载荷或子节点错误摘要。 */
    errorPayload?: unknown;
    /** 当前节点子树摘要。 */
    childSummary?: NodeChildSummaryVo | null;
    /** 当前节点内部执行步骤。 */
    executionSteps?: NodeExecutionStepVo[];
    /** 节点可读消息视图。 */
    messageView?: TraceMessageViewVo | null;
  }

  /** Agent Trace 节点子树摘要。 */
  export interface NodeChildSummaryVo {
    /** 模型调用数量。 */
    modelCount?: number;
    /** 工具调用数量。 */
    toolCount?: number;
    /** 中间件调用数量。 */
    middlewareCount?: number;
    /** 异常步骤数量。 */
    errorCount?: number;
  }

  /** Agent Trace 节点内部执行步骤。 */
  export interface NodeExecutionStepVo {
    /** Span 唯一标识。 */
    spanId?: string;
    /** 父 Span 唯一标识。 */
    parentSpanId?: string;
    /** Span 类型。 */
    spanType?: SpanType | string;
    /** 原始名称。 */
    name?: string;
    /** 展示名称。 */
    displayName?: string;
    /** 执行状态。 */
    status?: TraceStatus | string;
    /** 耗时毫秒。 */
    durationMs?: number;
    /** Token 展示文本。 */
    tokenText?: string;
    /** Token 用量。 */
    tokenUsage?: Record<string, unknown> | null;
    /** Trace 内顺序号。 */
    sequence?: number;
  }

  /** Agent Trace 顶层概览详情。 */
  export interface OverviewDetailVo {
    /** Root Span 唯一标识。 */
    spanId?: string;
    /** Trace 展示名称。 */
    name?: string;
    /** Trace 状态。 */
    status?: TraceStatus | string;
    /** 开始时间。 */
    startedAt?: string;
    /** 结束时间。 */
    endedAt?: string;
    /** 耗时毫秒。 */
    durationMs?: number;
    /** Token 用量。 */
    tokenUsage?: Record<string, unknown> | null;
    /** 顶层输入。 */
    input?: OverviewInputVo | null;
    /** 顶层输出。 */
    output?: OverviewOutputVo | null;
    /** 顶层属性。 */
    attributes?: Record<string, unknown> | null;
    /** 错误载荷。 */
    errorPayload?: unknown;
    /** 顶层 Trace 可读消息视图。 */
    messageView?: TraceMessageViewVo | null;
  }

  /** Agent Trace 顶层输入。 */
  export interface OverviewInputVo {
    /** 系统提示词。 */
    systemPrompt?: ModelSystemPromptVo | null;
    /** 用户视角输入消息列表。 */
    messages?: ModelMessageVo[];
  }

  /** Agent Trace 顶层输出。 */
  export interface OverviewOutputVo {
    /** 最终回复文本。 */
    finalText?: string;
    /** 整轮 Trace 关联的工具调用列表。 */
    toolCalls?: ModelToolCallVo[];
  }

  /** Agent Trace 模型调用详情。 */
  export interface ModelDetailVo {
    /** 模型名称。 */
    modelName?: string;
    /** 模型类名。 */
    modelClass?: string;
    /** 业务模型槽位。 */
    slot?: string;
    /** 模型调用设置。 */
    settings?: unknown;
    /** 模型结束原因。 */
    finishReason?: string;
    /** Token 用量。 */
    tokenUsage?: Record<string, unknown> | null;
    /** 系统提示词。 */
    systemPrompt?: ModelSystemPromptVo | null;
    /** 当前模型可见工具列表。 */
    availableTools?: ModelToolVo[];
    /** 模型输入消息列表。 */
    inputMessages?: ModelMessageVo[];
    /** 模型输出消息列表。 */
    outputMessages?: ModelMessageVo[];
    /** 模型发起的工具调用列表。 */
    toolCalls?: ModelToolCallVo[];
    /** 模型最终文本。 */
    finalText?: string;
    /** 模型可读消息视图。 */
    messageView?: TraceMessageViewVo | null;
  }

  /** Agent Trace 可读消息视图。 */
  export interface TraceMessageViewVo {
    /** 消息视图标题。 */
    title?: string;
    /** 用户与 AI 可读消息列表。 */
    messages?: TraceMessageVo[];
  }

  /** Agent Trace 可读消息。 */
  export interface TraceMessageVo {
    /** 消息唯一标识。 */
    id?: string;
    /** 消息角色。 */
    role?: 'user' | 'ai';
    /** 消息文本内容。 */
    content?: string;
    /** 消息来源 Span ID。 */
    sourceSpanId?: string;
    /** 消息来源 Span 顺序号。 */
    sequence?: number;
  }

  /** Agent Trace 模型系统提示词。 */
  export interface ModelSystemPromptVo {
    /** 提示词内容。 */
    content?: string;
    /** 渲染模式。 */
    renderMode?: string;
  }

  /** Agent Trace 模型可见工具。 */
  export interface ModelToolVo {
    /** 工具注册名称。 */
    name?: string;
    /** 工具展示名称。 */
    displayName?: string;
    /** 工具描述。 */
    description?: string;
    /** 工具参数 JSON Schema。 */
    argsSchema?: unknown;
    /** 本轮模型是否调用过该工具。 */
    called?: boolean;
    /** 工具调用记录。 */
    calls?: ModelToolCallVo[];
  }

  /** Agent Trace 模型消息。 */
  export interface ModelMessageVo {
    /** 消息类型。 */
    type?: string;
    /** 消息内容。 */
    content?: unknown;
    /** 消息名称。 */
    name?: string;
    /** 工具调用 ID。 */
    toolCallId?: string;
    /** 消息携带的工具调用列表。 */
    toolCalls?: ModelToolCallVo[];
    /** 响应元数据。 */
    responseMetadata?: unknown;
  }

  /** Agent Trace 模型工具调用。 */
  export interface ModelToolCallVo {
    /** 工具调用 ID。 */
    id?: string;
    /** 工具注册名称。 */
    name?: string;
    /** 工具展示名称。 */
    displayName?: string;
    /** 工具调用参数。 */
    arguments?: unknown;
    /** 工具执行状态。 */
    status?: TraceStatus | string;
    /** 工具执行耗时毫秒。 */
    durationMs?: number;
    /** 工具返回结果。 */
    outputPayload?: unknown;
    /** 工具错误载荷。 */
    errorPayload?: unknown;
  }

  /** Agent Trace span 树形展示节点。 */
  export interface SpanTreeNode {
    /** 前端树节点唯一标识。 */
    nodeId?: string;
    /** 真实 Span 唯一标识。 */
    sourceSpanId?: string;
    /** 父级树节点唯一标识。 */
    parentNodeId?: string;
    /** 树节点类型。 */
    nodeType?: SpanTreeNodeType;
    /** Span 类型。 */
    spanType?: SpanType;
    /** 原始名称。 */
    name?: string;
    /** 前端展示名称。 */
    displayName?: string;
    /** 模型名称。 */
    modelName?: string;
    /** Span 状态。 */
    status?: TraceStatus;
    /** 耗时毫秒。 */
    durationMs?: number;
    /** Token 展示文本。 */
    tokenText?: string;
    /** Trace 内顺序号。 */
    sequence?: number;
    /** 子树节点。 */
    children?: SpanTreeNode[];
  }

  /** Agent Trace 详情。 */
  export interface DetailVo extends RunListVo {
    /** 根 graph span ID。 */
    rootSpanId?: string;
    /** 错误载荷。 */
    errorPayload?: unknown;
    /** 顶层 Trace 概览详情。 */
    overviewDetail?: OverviewDetailVo | null;
    /** Span 明细列表。 */
    spans?: SpanVo[];
    /** Span 树形展示节点列表。 */
    spanTree?: SpanTreeNode[];
  }
}

/**
 * 分页查询 Agent Trace 运行列表。
 *
 * @param params 查询参数。
 * @returns Trace 运行分页列表。
 */
export async function listAgentTraceRuns(params?: AgentTraceTypes.RunListRequest) {
  return requestClient.get<TableDataResult<AgentTraceTypes.RunListVo>>('/agent/trace/list', {
    params,
  });
}

/**
 * 查询 Agent Trace 详情。
 *
 * @param traceId Trace 唯一标识。
 * @returns Trace 详情。
 */
export async function getAgentTraceDetail(traceId: string) {
  return requestClient.get<AgentTraceTypes.DetailVo>(`/agent/trace/${encodeURIComponent(traceId)}`);
}

/**
 * 删除 Agent Trace。
 *
 * @param traceId Trace 唯一标识。
 * @returns 删除结果。
 */
export async function deleteAgentTrace(traceId: string) {
  return requestClient.delete<void>(`/agent/trace/${encodeURIComponent(traceId)}`);
}
