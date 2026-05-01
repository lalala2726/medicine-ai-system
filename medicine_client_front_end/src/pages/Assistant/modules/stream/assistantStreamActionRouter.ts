import type { AssistantTypes } from '@/api/assistant/contract'

/**
 * SSE action 事件中后端可能下发的导航目标常量。
 * Router 只做事件分发，不直接操作 React 状态。
 */
export const ASSISTANT_STREAM_ACTION_TARGETS = {
  /** 打开用户订单列表选择器 */
  USER_ORDER_LIST: 'user_order_list',
  /** 打开用户售后列表选择器 */
  USER_AFTER_SALE_LIST: 'user_after_sale_list',
  /** 打开用户就诊人列表选择器 */
  USER_PATIENT_LIST: 'user_patient_list'
} as const

/** 已处理 SSE action 去重键的本机持久化存储键名。 */
const HANDLED_STREAM_ACTION_KEYS_STORAGE_KEY = 'assistant-handled-stream-action-keys'
/** 本机最多保留的已处理 SSE action 去重键数量。 */
const HANDLED_STREAM_ACTION_KEYS_LIMIT = 500
/** 已处理过的 SSE action 去重键内存缓存，避免同一页面生命周期内重复读取。 */
const handledStreamActionKeys = new Set<string>()
/** 本机持久化去重键是否已经加载到内存。 */
let handledStreamActionKeysLoaded = false

/**
 * SSE action 事件的页面回调接口。
 */
export interface AssistantStreamActionCallbacks {
  /**
   * 打开订单选择器
   * @param status - 可选的订单状态筛选值
   */
  onOpenOrderSelector: (status?: string) => void
  /**
   * 打开售后选择器
   * @param status - 可选的售后状态筛选值
   */
  onOpenAfterSaleSelector: (status?: string) => void
  /**
   * 打开就诊人选择器
   * @returns 无返回值
   */
  onOpenPatientSelector: () => void
}

/**
 * 将 action payload 序列化成去重键片段。
 *
 * @param payload - 后端 action 携带的业务参数
 * @returns 可参与去重的 payload 字符串
 */
const stringifyActionPayload = (payload?: Record<string, unknown>) => {
  return JSON.stringify(payload ?? {})
}

/**
 * 读取本机已处理过的 SSE action 去重键列表。
 *
 * @returns 已持久化的 SSE action 去重键列表
 */
const readHandledStreamActionKeys = () => {
  /** 本机保存的原始去重键 JSON 字符串。 */
  const rawActionKeys = localStorage.getItem(HANDLED_STREAM_ACTION_KEYS_STORAGE_KEY)

  if (!rawActionKeys) {
    return []
  }

  try {
    /** 从本机存储解析出的去重键候选值。 */
    const parsedActionKeys = JSON.parse(rawActionKeys)

    return Array.isArray(parsedActionKeys)
      ? parsedActionKeys.filter((actionKey): actionKey is string => typeof actionKey === 'string')
      : []
  } catch (error) {
    console.warn('[AssistantStreamActionRouter] invalid handled action keys storage', error)
    localStorage.removeItem(HANDLED_STREAM_ACTION_KEYS_STORAGE_KEY)
    return []
  }
}

/**
 * 将已处理过的 SSE action 去重键列表写入本机存储。
 *
 * @param actionKeys - 需要持久化的 SSE action 去重键列表
 * @returns 无返回值
 */
const writeHandledStreamActionKeys = (actionKeys: string[]) => {
  /** 截断后的 SSE action 去重键列表，只保留最近记录。 */
  const limitedActionKeys = actionKeys.slice(-HANDLED_STREAM_ACTION_KEYS_LIMIT)

  localStorage.setItem(HANDLED_STREAM_ACTION_KEYS_STORAGE_KEY, JSON.stringify(limitedActionKeys))
  handledStreamActionKeys.clear()
  limitedActionKeys.forEach(actionKey => handledStreamActionKeys.add(actionKey))
  handledStreamActionKeysLoaded = true
}

/**
 * 确保本机存储中的 SSE action 去重键只加载一次到内存缓存。
 *
 * @returns 无返回值
 */
const ensureHandledStreamActionKeysLoaded = () => {
  if (handledStreamActionKeysLoaded) {
    return
  }

  /** 本机已保存的 SSE action 去重键列表。 */
  const storedActionKeys = readHandledStreamActionKeys()

  handledStreamActionKeys.clear()
  storedActionKeys.forEach(actionKey => handledStreamActionKeys.add(actionKey))
  handledStreamActionKeysLoaded = true
}

/**
 * 判断指定 SSE action 去重键是否已经处理过。
 *
 * @param actionKey - 当前 SSE action 的去重键
 * @returns 当前 SSE action 是否已经处理过
 */
const hasHandledStreamActionKey = (actionKey: string) => {
  ensureHandledStreamActionKeysLoaded()

  return handledStreamActionKeys.has(actionKey)
}

/**
 * 将指定 SSE action 去重键记录为已处理。
 *
 * @param actionKey - 当前 SSE action 的去重键
 * @returns 无返回值
 */
const markStreamActionKeyHandled = (actionKey: string) => {
  ensureHandledStreamActionKeysLoaded()

  if (handledStreamActionKeys.has(actionKey)) {
    return
  }

  writeHandledStreamActionKeys([...handledStreamActionKeys, actionKey])
}

/**
 * 生成 SSE action 的一次性消费键。
 * 同一个消息中的同一条 action 重放时会得到相同 key；新消息 action 不会互相影响。
 *
 * @param event - SSE 流事件对象
 * @returns action 去重键；缺少 action 时返回空字符串
 */
const buildStreamActionKey = (event: AssistantTypes.StreamEvent) => {
  if (!event.action?.type || !event.action.target) {
    return ''
  }

  /** 当前 action 所属的消息范围。 */
  const actionScope = event.meta?.messageUuid ?? ''

  if (!actionScope) {
    return ''
  }

  return [
    event.meta?.conversationUuid ?? '',
    actionScope,
    event.action.type,
    event.action.target,
    stringifyActionPayload(event.action.payload)
  ].join('|')
}

/**
 * 判断当前 SSE action 是否可以继续路由。
 *
 * @param event - SSE 流事件对象
 * @returns 当前 action 是否未被快照标记且未重复消费
 */
const canRouteStreamAction = (event: AssistantTypes.StreamEvent) => {
  if (event.meta?.snapshot === true) {
    return false
  }

  const actionKey = buildStreamActionKey(event)
  if (!actionKey) {
    return false
  }

  if (hasHandledStreamActionKey(actionKey)) {
    return false
  }

  markStreamActionKeyHandled(actionKey)
  return true
}

/**
 * 将 Assistant SSE action 事件路由到具体的 UI 回调。
 * 当前只处理 `navigate` 类型的动作。
 *
 * @param event - SSE 流事件对象
 * @param callbacks - action 回调函数集合
 * @returns 无返回值
 */
export function routeAssistantStreamAction(
  event: AssistantTypes.StreamEvent,
  callbacks: AssistantStreamActionCallbacks
) {
  if (event.action?.type !== 'navigate' || !event.action.target) {
    return
  }

  if (!canRouteStreamAction(event)) {
    return
  }

  switch (event.action.target) {
    case ASSISTANT_STREAM_ACTION_TARGETS.USER_ORDER_LIST:
      callbacks.onOpenOrderSelector(event.action.payload?.orderStatus as string | undefined)
      break
    case ASSISTANT_STREAM_ACTION_TARGETS.USER_AFTER_SALE_LIST:
      callbacks.onOpenAfterSaleSelector(event.action.payload?.afterSaleStatus as string | undefined)
      break
    case ASSISTANT_STREAM_ACTION_TARGETS.USER_PATIENT_LIST:
      callbacks.onOpenPatientSelector()
      break
  }
}
