import requestClient from '@/request/requestClient'
import { rawRequest } from '@/request/rawRequestClient'
import { resolveAiAgentHttpUrl } from '@/utils/aiAgentUrl'
import { sseRequest } from '@/utils/sse'
import { logServerError, normalizeServerErrorMessage } from '@/utils/serverError'
import {
  ASSISTANT_CARD_TYPES,
  ASSISTANT_MESSAGE_TYPES,
  ASSISTANT_STREAM_EVENT_TYPES,
  type AssistantTypes
} from './contract'

/** 会话列表每页默认条数 */
export const CONVERSATION_PAGE_SIZE = 20

/** 聊天历史记录每页条数 */
const HISTORY_PAGE_SIZE = 50

/** TTS 接口缺少 message_uuid 时的错误提示。 */
const ASSISTANT_TTS_MESSAGE_UUID_REQUIRED_TEXT = 'message_uuid 不能为空'

/** TTS 接口失败时的通用错误提示。 */
const ASSISTANT_TTS_REQUEST_FAILED_TEXT = '语音播放请求失败，请稍后重试'

/** submit 接口失败时的通用错误提示。 */
const ASSISTANT_CHAT_SUBMIT_REQUEST_FAILED_TEXT = '发送消息失败，请稍后重试'

/** stop 接口失败时的通用错误提示。 */
const ASSISTANT_CHAT_STOP_REQUEST_FAILED_TEXT = '停止回复失败，请稍后重试'

/** submit 接口会话冲突的业务状态码。 */
const ASSISTANT_CHAT_CONFLICT_CODE = 409

/** 客户端聊天输入区能力。 */
export interface AssistantChatCapability {
  /** 当前是否允许上传图片。 */
  imageUploadEnabled: boolean
  /** 图片上传禁用时的提示文案。 */
  imageUploadDisabledMessage?: string
  /** 当前是否允许开启深度思考。 */
  reasoningToggleEnabled: boolean
  /** 深度思考禁用时的提示文案。 */
  reasoningToggleDisabledMessage?: string
}

/**
 * 解析原始响应中的错误提示文案。
 *
 * @param response - 原始 HTTP 响应
 * @returns 适合前端提示的错误文案
 */
const resolveRawResponseErrorMessage = async (response: Response) => {
  const contentType = response.headers.get('content-type') || ''

  if (contentType.includes('application/json')) {
    try {
      const rawError = (await response.json()) as { message?: string }

      if (typeof rawError.message === 'string' && rawError.message.trim()) {
        return normalizeServerErrorMessage(rawError.message, ASSISTANT_TTS_REQUEST_FAILED_TEXT)
      }
    } catch {
      // ignore parse failure and fall through to default message
    }
  }

  return normalizeServerErrorMessage(ASSISTANT_TTS_REQUEST_FAILED_TEXT, ASSISTANT_TTS_REQUEST_FAILED_TEXT)
}

/** 后端分页响应的原始数据结构（兼容 snake_case 和 camelCase）。 */
interface RawPageResponse<T> {
  /** 当前页数据列表。 */
  rows?: T[]
  /** 总记录数。 */
  total?: number
  /** 当前页码（snake_case）。 */
  page_num?: number
  /** 每页条数（snake_case）。 */
  page_size?: number
  /** 当前页码（camelCase）。 */
  pageNum?: number
  /** 每页条数（camelCase）。 */
  pageSize?: number
}

/** 后端返回的会话条目原始数据结构。 */
interface RawConversationItem {
  /** 会话 UUID（snake_case）。 */
  conversation_uuid?: string
  /** 会话 UUID（camelCase）。 */
  conversationUuid?: string
  /** 会话标题。 */
  title?: string
}

/** 重命名/删除会话接口返回的原始数据结构。 */
interface RawConversationMutationData {
  /** 会话 UUID（snake_case）。 */
  conversation_uuid?: string
  /** 会话 UUID（camelCase）。 */
  conversationUuid?: string
  /** 会话标题。 */
  title?: string
}

/** 后端返回的卡片载荷原始数据结构。 */
interface RawAssistantCardPayload {
  /** 卡片类型标识。 */
  type?: string
  /** 卡片唯一标识（snake_case）。 */
  card_uuid?: string
  /** 卡片业务数据。 */
  data?: Record<string, unknown>
}

/** 后端 SSE 持久化消息的原始数据结构。 */
interface RawAssistantMessage {
  /** 消息唯一标识。 */
  id?: string
  /** 独立卡片事件所属消息 ID（snake_case）。 */
  source_message_id?: string
  /** 独立卡片事件所属消息 ID（camelCase）。 */
  sourceMessageId?: string
  /** 卡片唯一标识（snake_case）。 */
  card_uuid?: string
  /** 消息发送方角色。 */
  role?: AssistantTypes.Role
  /** 消息类型。 */
  message_type?: AssistantTypes.MessageType
  /** 文本消息内容。 */
  content?: string
  /** 卡片消息载荷。 */
  card?: RawAssistantCardPayload
  /** AI 思考过程文本。 */
  thinking?: string
  /** 消息最终状态。 */
  status?: AssistantTypes.MessageStatus
  /** 所属会话 UUID。 */
  conversation_uuid?: string
  /** 创建时间。 */
  created_at?: string
}

/** 后端返回的历史卡片原始数据结构。 */
interface RawHistoryCard {
  /** 卡片唯一标识（snake_case）。 */
  card_uuid?: string
  /** 卡片类型标识。 */
  type?: string
  /** 卡片业务数据。 */
  data?: Record<string, unknown>
}

/** 后端返回的历史消息原始数据结构。 */
interface RawHistoryMessage {
  /** 消息唯一标识。 */
  id?: string
  /** 消息发送方角色。 */
  role?: AssistantTypes.Role
  /** 文本消息内容。 */
  content?: string
  /** AI 思考过程文本。 */
  thinking?: string
  /** 消息最终状态。 */
  status?: AssistantTypes.MessageStatus
  /** 历史卡片数组。 */
  cards?: RawHistoryCard[]
  /** 所属会话 UUID。 */
  conversation_uuid?: string
  /** 创建时间。 */
  created_at?: string
}

/** 后端 SSE 流式事件中的内容（思考过程、回答文本等）。 */
interface RawAssistantStreamContent {
  /** 本次增量文本。 */
  text?: string
  /** 当前节点标识。 */
  node?: string
  /** 父节点标识。 */
  parent_node?: string | null
  /** 当前节点状态。 */
  state?: string | null
  /** 后端附带的提示消息。 */
  message?: string | null
  /** 节点执行结果。 */
  result?: string | null
  /** 工具名或节点名。 */
  name?: string | null
  /** 工具调用参数。 */
  arguments?: string | null
}

/** 后端 SSE 流式事件中的元数据（会话 ID、消息 ID）。 */
interface RawAssistantStreamMeta {
  /** 当前流所属会话 UUID。 */
  conversation_uuid?: string
  /** 当前 AI 消息 UUID。 */
  message_uuid?: string
  /** 独立卡片事件对应的卡片 UUID。 */
  card_uuid?: string
  /** 当前运行状态。 */
  run_status?: string
  /** 当前事件是否属于快照恢复。 */
  snapshot?: boolean
  /** 当前事件是否需要前端执行 replace 覆盖。 */
  replace?: boolean
}

/** 后端 SSE 流式事件中的 action 指令（如导航到订单列表）。 */
interface RawAssistantStreamAction {
  /** 动作类型。 */
  type?: string
  /** 动作目标。 */
  target?: string
  /** 动作业务参数。 */
  payload?: Record<string, unknown>
  /** 动作优先级。 */
  priority?: number
}

/** 后端 SSE 流式推送的完整事件原始数据结构。 */
interface RawAssistantStreamEvent {
  /** 事件类型。 */
  type?: string
  /** 事件内容。 */
  content?: RawAssistantStreamContent
  /** 事件元信息。 */
  meta?: RawAssistantStreamMeta
  /** 动作指令。 */
  action?: RawAssistantStreamAction
  /** 独立卡片数据。 */
  card?: RawAssistantCardPayload
  /** 单条持久化消息。 */
  message?: RawAssistantMessage
  /** 多条持久化消息。 */
  messages?: RawAssistantMessage[]
  /** 是否为本次流的结束事件。 */
  is_end?: boolean
  /** 事件时间戳。 */
  timestamp?: number
}

/** 后端通用 JSON 包裹响应。 */
interface RawAssistantResponseEnvelope<T> {
  /** 业务状态码。 */
  code?: number
  /** 后端返回的提示文案。 */
  message?: string
  /** 业务数据。 */
  data?: T
  /** 服务端时间戳。 */
  timestamp?: number
}

/** 客户端聊天输入区能力的原始响应结构。 */
interface RawAssistantChatCapabilityData {
  /** 当前是否允许上传图片（snake_case）。 */
  image_upload_enabled?: boolean
  /** 图片上传禁用时的提示文案（snake_case）。 */
  image_upload_disabled_message?: string
  /** 当前是否允许上传图片（camelCase）。 */
  imageUploadEnabled?: boolean
  /** 图片上传禁用时的提示文案（camelCase）。 */
  imageUploadDisabledMessage?: string
  /** 当前是否允许开启深度思考（snake_case）。 */
  reasoning_toggle_enabled?: boolean
  /** 深度思考禁用时的提示文案（snake_case）。 */
  reasoning_toggle_disabled_message?: string
  /** 当前是否允许开启深度思考（camelCase）。 */
  reasoningToggleEnabled?: boolean
  /** 深度思考禁用时的提示文案（camelCase）。 */
  reasoningToggleDisabledMessage?: string
}

/** submit / stop 接口返回的原始运行结果。 */
interface RawAssistantChatRunData {
  /** 会话 UUID（snake_case）。 */
  conversation_uuid?: string
  /** 会话 UUID（camelCase）。 */
  conversationUuid?: string
  /** 消息 UUID（snake_case）。 */
  message_uuid?: string
  /** 消息 UUID（camelCase）。 */
  messageUuid?: string
  /** 后端运行状态（snake_case）。 */
  run_status?: string
  /** 后端运行状态（camelCase）。 */
  runStatus?: string
  /** stop 接口是否已接受停止请求（snake_case）。 */
  stop_requested?: boolean
  /** stop 接口是否已接受停止请求（camelCase）。 */
  stopRequested?: boolean
}

/** 分页会话列表的前端结果。 */
interface ConversationListPageResult {
  /** 当前页会话条目。 */
  items: AssistantTypes.ConversationItem[]
  /** 会话总数。 */
  total: number
  /** 是否还有下一页。 */
  hasMore: boolean
}

/** submit 接口中的卡片消息载荷。 */
export interface AssistantChatSubmitCardPayload {
  /** 卡片类型标识。 */
  type: string
  /** 卡片业务数据。 */
  data: Record<string, unknown>
}

/** submit 接口的请求参数。 */
export interface AssistantChatSubmitOptions {
  /** 本次发送的消息类型。 */
  messageType: AssistantTypes.MessageType
  /** 文本消息正文，仅 text 类型需要传递。 */
  content?: string
  /** 本次发送附带的图片 URL 列表。 */
  imageUrls?: string[]
  /** 本次发送附带的卡片消息载荷。 */
  card?: AssistantChatSubmitCardPayload
  /** 当前会话 UUID，不传则由后端创建新会话。 */
  conversationUuid?: string | null
  /** 当前点击交互卡后附带的动作数据。 */
  cardAction?: AssistantTypes.CardActionPayload
  /** 当前轮是否开启深度思考。 */
  reasoningEnabled?: boolean
  /** 当前请求的中断信号。 */
  signal?: AbortSignal
}

/** stream attach 接口的请求参数。 */
export interface AssistantChatAttachStreamOptions {
  /** 要 attach 的会话 UUID。 */
  conversationUuid: string
  /** 断点恢复使用的 SSE 事件 ID。 */
  lastEventId?: string
  /** 当前请求的中断信号。 */
  signal?: AbortSignal
  /** 收到 SSE 事件时的回调。 */
  onEvent?: (event: AssistantTypes.StreamEvent) => void
  /** SSE 连接关闭时的回调。 */
  onClose?: () => void
  /** SSE 请求错误时的回调。 */
  onError?: (error: unknown) => void
}

/** stop 接口的请求参数。 */
export interface AssistantChatStopOptions {
  /** 要停止的会话 UUID。 */
  conversationUuid: string
  /** 当前请求的中断信号。 */
  signal?: AbortSignal
}

/** submit/stop 接口通用的运行结果。 */
export interface AssistantChatRunResult {
  /** 会话 UUID。 */
  conversationUuid: string
  /** 当前运行中的消息 UUID。 */
  messageUuid: string
  /** 后端返回的运行状态。 */
  runStatus: string
}

/** submit 成功结果。 */
export interface AssistantChatSubmitSuccessResult extends AssistantChatRunResult {
  /** 当前 submit 请求已成功创建或恢复本轮运行。 */
  type: 'submitted'
}

/** submit 冲突结果。 */
export interface AssistantChatSubmitConflictResult extends AssistantChatRunResult {
  /** 当前 submit 请求命中了单会话单活冲突。 */
  type: 'conflict'
  /** 后端返回的冲突提示文案。 */
  message: string
}

/** submit 接口的规范化结果。 */
export type AssistantChatSubmitResult = AssistantChatSubmitSuccessResult | AssistantChatSubmitConflictResult

/** stop 接口的规范化结果。 */
export interface AssistantChatStopResult extends AssistantChatRunResult {
  /** 后端是否已接受停止请求。 */
  stopRequested: boolean
}

const getRecord = (value: unknown): Record<string, unknown> | null => {
  return value && typeof value === 'object' && !Array.isArray(value) ? (value as Record<string, unknown>) : null
}

const getArray = <T = unknown>(value: unknown): T[] => {
  return Array.isArray(value) ? (value as T[]) : []
}

const getString = (value: unknown) => {
  if (typeof value === 'string' && value.trim()) {
    return value.trim()
  }

  if (typeof value === 'number' && Number.isFinite(value)) {
    return String(value)
  }

  return undefined
}

/**
 * 读取卡片协议中的正整数数量字段。
 *
 * @param value - 原始数量值
 * @returns 规范化后的正整数，非法时返回 undefined
 */
const getPositiveInteger = (value: unknown) => {
  const rawNumber =
    typeof value === 'number' ? value : typeof value === 'string' && value.trim() ? Number(value.trim()) : Number.NaN

  if (!Number.isFinite(rawNumber)) {
    return undefined
  }

  const normalizedNumber = Math.trunc(rawNumber)
  return normalizedNumber > 0 ? normalizedNumber : undefined
}

/**
 * 读取卡片协议中的布尔标记字段。
 *
 * @param value - 原始布尔标记值
 * @returns 规范化后的布尔值，非法时返回 undefined
 */
const getBooleanFlag = (value: unknown) => {
  if (typeof value === 'boolean') {
    return value
  }

  if (typeof value === 'number' && Number.isFinite(value)) {
    if (value === 1) {
      return true
    }

    if (value === 0) {
      return false
    }
  }

  if (typeof value === 'string') {
    const normalizedValue = value.trim().toLowerCase()

    if (normalizedValue === '1' || normalizedValue === 'true') {
      return true
    }

    if (normalizedValue === '0' || normalizedValue === 'false') {
      return false
    }
  }

  return undefined
}

const normalizeProductCardItem = (value: unknown): AssistantTypes.ProductCardItem | null => {
  const record = getRecord(value)

  if (!record) {
    return null
  }

  const id = getString(record.id)
  const name = getString(record.name)
  const image = getString(record.image)
  const price = getString(record.price)

  if (!id || !name || !image || !price) {
    return null
  }

  return {
    id,
    name,
    image,
    price
  }
}

/**
 * 规范化商品卡 data 字段。
 * 历史消息允许 products 为空数组，实时卡片是否可渲染由调用方再决定。
 *
 * @param value - 卡片 data 原始数据
 * @returns 规范化后的商品卡数据，完全无效时返回 null
 */
const normalizeProductCardData = (value: unknown): AssistantTypes.ProductCardData | null => {
  const data = getRecord(value)
  const products = getArray(data?.products)
    .map(normalizeProductCardItem)
    .filter((item): item is AssistantTypes.ProductCardItem => item !== null)
  const title = getString(data?.title)

  if (!title && products.length === 0) {
    return null
  }

  return {
    title,
    products
  }
}

/**
 * 规范化商品咨询卡 data 字段。
 *
 * @param value - 卡片 data 原始数据
 * @returns 规范化后的商品咨询卡数据，缺少核心商品信息时返回 null
 */
const normalizeConsultProductCardData = (value: unknown): AssistantTypes.ConsultProductCardData | null => {
  const data = getRecord(value)
  const title = getString(data?.title)
  const product = normalizeProductCardItem(data?.product)

  if (!product) {
    return null
  }

  return {
    title,
    product: {
      id: product.id,
      name: product.name,
      image: product.image,
      price: product.price
    }
  }
}

/**
 * 规范化商品购买卡中的单个商品条目。
 *
 * @param value - 原始商品条目
 * @returns 规范化后的购买商品条目，缺少必要字段时返回 null
 */
const normalizeProductPurchaseCardItem = (value: unknown): AssistantTypes.ProductPurchaseCardItem | null => {
  const record = getRecord(value)

  if (!record) {
    return null
  }

  const id = getString(record.id)
  const name = getString(record.name)
  const image = getString(record.image)
  const price = getString(record.price)
  const quantity = getPositiveInteger(record.quantity)

  if (!id || !name || !image || !price || !quantity) {
    return null
  }

  return {
    id,
    name,
    image,
    price,
    quantity
  }
}

/**
 * 规范化商品购买卡 data 字段。
 * 总价完全以服务端下发结果为准，前端不做计算和纠偏。
 *
 * @param value - 卡片 data 原始数据
 * @returns 规范化后的商品购买卡数据，缺少关键字段时返回 null
 */
const normalizeProductPurchaseCardData = (value: unknown): AssistantTypes.ProductPurchaseCardData | null => {
  const data = getRecord(value)
  const products = getArray(data?.products)
    .map(normalizeProductPurchaseCardItem)
    .filter((item): item is AssistantTypes.ProductPurchaseCardItem => item !== null)
  const title = getString(data?.title)
  const totalPrice = getString(data?.total_price)

  if (!totalPrice || products.length === 0) {
    return null
  }

  return {
    title,
    products,
    totalPrice
  }
}

/**
 * 规范化订单卡中的首个商品预览信息。
 *
 * @param value - 原始商品预览对象
 * @returns 规范化后的首个商品预览信息，无效时返回 null
 */
const normalizeOrderCardPreviewProductData = (value: unknown): AssistantTypes.OrderCardPreviewProductData | null => {
  const record = getRecord(value)
  const productId = getString(record?.product_id) ?? getString(record?.productId)
  const productName = getString(record?.product_name) ?? getString(record?.productName)
  const imageUrl = getString(record?.image_url) ?? getString(record?.imageUrl)

  if (!productId || !productName || !imageUrl) {
    return null
  }

  return {
    productId,
    productName,
    imageUrl
  }
}

/**
 * 规范化订单卡 data 字段。
 *
 * @param value - 卡片 data 原始数据
 * @returns 规范化后的订单卡数据，缺少关键字段时返回 null
 */
const normalizeOrderCardData = (value: unknown): AssistantTypes.OrderCardData | null => {
  const data = getRecord(value)
  const orderNo = getString(data?.order_no) ?? getString(data?.orderNo)
  const orderStatus = getString(data?.order_status) ?? getString(data?.orderStatus)
  const orderStatusText = getString(data?.order_status_text) ?? getString(data?.orderStatusText)
  const previewProduct = normalizeOrderCardPreviewProductData(data?.preview_product ?? data?.previewProduct)
  const productCount = getPositiveInteger(data?.product_count ?? data?.productCount)
  const payAmount = getString(data?.pay_amount) ?? getString(data?.payAmount)
  const totalAmount = getString(data?.total_amount) ?? getString(data?.totalAmount)
  const createTime = getString(data?.create_time) ?? getString(data?.createTime)

  if (
    !orderNo ||
    !orderStatus ||
    !orderStatusText ||
    !previewProduct ||
    !productCount ||
    !payAmount ||
    !totalAmount ||
    !createTime
  ) {
    return null
  }

  return {
    orderNo,
    orderStatus,
    orderStatusText,
    previewProduct,
    productCount,
    payAmount,
    totalAmount,
    createTime
  }
}

/**
 * 规范化售后卡中的商品信息。
 *
 * @param value - 原始商品信息对象
 * @returns 规范化后的商品信息，无效时返回 null
 */
const normalizeAfterSaleCardProductInfoData = (value: unknown): AssistantTypes.AfterSaleCardProductInfoData | null => {
  const record = getRecord(value)
  const productName = getString(record?.product_name) ?? getString(record?.productName)
  const productImage = getString(record?.product_image) ?? getString(record?.productImage)

  if (!productName || !productImage) {
    return null
  }

  return {
    productName,
    productImage
  }
}

/**
 * 规范化售后卡 data 字段。
 *
 * @param value - 卡片 data 原始数据
 * @returns 规范化后的售后卡数据，缺少关键字段时返回 null
 */
const normalizeAfterSaleCardData = (value: unknown): AssistantTypes.AfterSaleCardData | null => {
  const data = getRecord(value)
  const afterSaleNo = getString(data?.after_sale_no) ?? getString(data?.afterSaleNo)
  const orderNo = getString(data?.order_no) ?? getString(data?.orderNo)
  const afterSaleType = getString(data?.after_sale_type) ?? getString(data?.afterSaleType)
  const afterSaleTypeText = getString(data?.after_sale_type_text) ?? getString(data?.afterSaleTypeText)
  const afterSaleStatus = getString(data?.after_sale_status) ?? getString(data?.afterSaleStatus)
  const afterSaleStatusText = getString(data?.after_sale_status_text) ?? getString(data?.afterSaleStatusText)
  const refundAmount = getString(data?.refund_amount) ?? getString(data?.refundAmount)
  const applyReasonName = getString(data?.apply_reason_name) ?? getString(data?.applyReasonName)
  const applyTime = getString(data?.apply_time) ?? getString(data?.applyTime)
  const productInfo = normalizeAfterSaleCardProductInfoData(data?.product_info ?? data?.productInfo)

  if (
    !afterSaleNo ||
    !orderNo ||
    !afterSaleType ||
    !afterSaleTypeText ||
    !afterSaleStatus ||
    !afterSaleStatusText ||
    !refundAmount ||
    !applyReasonName ||
    !applyTime ||
    !productInfo
  ) {
    return null
  }

  return {
    afterSaleNo,
    orderNo,
    afterSaleType,
    afterSaleTypeText,
    afterSaleStatus,
    afterSaleStatusText,
    refundAmount,
    applyReasonName,
    applyTime,
    productInfo
  }
}

/**
 * 读取就诊人卡中的性别编码字段。
 *
 * @param value - 原始性别编码
 * @returns 规范化后的性别编码，非法时返回 undefined
 */
const getPatientGenderValue = (value: unknown) => {
  const normalizedGender = getPositiveInteger(value)

  if (normalizedGender !== 1 && normalizedGender !== 2) {
    return undefined
  }

  return normalizedGender
}

/**
 * 规范化就诊人卡 data 字段。
 *
 * @param value - 卡片 data 原始数据
 * @returns 规范化后的就诊人卡数据，缺少关键字段时返回 null
 */
const normalizePatientCardData = (value: unknown): AssistantTypes.PatientCardData | null => {
  const data = getRecord(value)
  const patientId = getString(data?.patient_id) ?? getString(data?.patientId)
  const name = getString(data?.name)
  const gender = getPatientGenderValue(data?.gender)
  const genderText = getString(data?.gender_text) ?? getString(data?.genderText) ?? (gender === 1 ? '男' : '女')
  const birthDate = getString(data?.birth_date) ?? getString(data?.birthDate)
  const relationship = getString(data?.relationship)
  const isDefault = getBooleanFlag(data?.is_default ?? data?.isDefault)
  const allergy = getString(data?.allergy)
  const pastMedicalHistory = getString(data?.past_medical_history) ?? getString(data?.pastMedicalHistory)
  const chronicDisease = getString(data?.chronic_disease) ?? getString(data?.chronicDisease)
  const longTermMedications = getString(data?.long_term_medications) ?? getString(data?.longTermMedications)

  if (!patientId || !name || !gender || !genderText || !birthDate || !relationship || typeof isDefault !== 'boolean') {
    return null
  }

  return {
    patientId,
    name,
    gender,
    genderText,
    birthDate,
    relationship,
    isDefault,
    allergy,
    pastMedicalHistory,
    chronicDisease,
    longTermMedications
  }
}

/**
 * 规范化同意卡中的按钮动作。
 *
 * @param value - 原始动作对象
 * @returns 规范化后的动作，无效时返回 null
 */
const normalizeConsentAction = (value: unknown): AssistantTypes.ConsentAction | null => {
  const record = getRecord(value)

  if (!record) {
    return null
  }

  const label = getString(record.label)

  if (!label) {
    return null
  }

  return {
    label,
    value: getString(record.value) ?? label
  }
}

/**
 * 规范化同意卡 data 字段。
 *
 * @param value - 卡片 data 原始数据
 * @returns 规范化后的同意卡数据，缺少关键字段时返回 null
 */
const normalizeConsentCardData = (value: unknown): AssistantTypes.ConsentCardData | null => {
  const data = getRecord(value)
  const scene = getString(data?.scene)
  const title = getString(data?.title)
  const description = getString(data?.description)
  const confirm = normalizeConsentAction(data?.confirm)
  const reject = normalizeConsentAction(data?.reject)

  if (!confirm || !reject) {
    return null
  }

  return {
    scene,
    title,
    description,
    confirm,
    reject
  }
}

/**
 * 规范化选择卡 data 字段。
 *
 * @param value - 卡片 data 原始数据
 * @returns 规范化后的选择卡数据，完全无效时返回 null
 */
const normalizeSelectionCardData = (value: unknown): AssistantTypes.SelectionCardData | null => {
  const data = getRecord(value)
  const scene = getString(data?.scene)
  const title = getString(data?.title)
  const options = getArray(data?.options)
    .map(option => getString(option))
    .filter((item): item is string => Boolean(item))

  if (options.length === 0) {
    return null
  }

  return {
    scene,
    title,
    options
  }
}

/**
 * 规范化 consultation 问卷卡中的单个问题。
 *
 * @param value - 原始问题数据
 * @returns 规范化后的问卷问题，缺少关键字段时返回 null
 */
const normalizeConsultationQuestionnaireQuestionData = (
  value: unknown
): AssistantTypes.ConsultationQuestionnaireQuestionData | null => {
  const record = getRecord(value)
  const question = getString(record?.question)
  const options = getArray(record?.options)
    .map(option => getString(option))
    .filter((item): item is string => Boolean(item))

  if (!question || options.length === 0) {
    return null
  }

  return {
    question,
    options
  }
}

/**
 * 规范化 consultation 问卷卡 data 字段。
 *
 * @param value - 卡片 data 原始数据
 * @returns 规范化后的 consultation 问卷卡数据，完全无效时返回 null
 */
const normalizeConsultationQuestionnaireCardData = (
  value: unknown
): AssistantTypes.ConsultationQuestionnaireCardData | null => {
  const data = getRecord(value)
  const questions = getArray(data?.questions)
    .map(normalizeConsultationQuestionnaireQuestionData)
    .filter((item): item is AssistantTypes.ConsultationQuestionnaireQuestionData => item !== null)

  if (questions.length === 0) {
    return null
  }

  return {
    questions
  }
}

const normalizeProductCardPayload = (card: RawAssistantCardPayload): AssistantTypes.ProductCardPayload | undefined => {
  if (card.type !== ASSISTANT_CARD_TYPES.PRODUCT_CARD) {
    return undefined
  }

  const data = normalizeProductCardData(card.data)

  if (!data || data.products.length === 0) {
    return undefined
  }

  return {
    type: ASSISTANT_CARD_TYPES.PRODUCT_CARD,
    data
  }
}

/**
 * 将原始商品咨询卡载荷规范化为前端使用的结构。
 *
 * @param card - 原始卡片载荷
 * @returns 规范化后的商品咨询卡载荷，缺少关键字段时返回 undefined
 */
const normalizeConsultProductCardPayload = (
  card: RawAssistantCardPayload
): AssistantTypes.ConsultProductCardPayload | undefined => {
  if (card.type !== ASSISTANT_CARD_TYPES.CONSULT_PRODUCT_CARD) {
    return undefined
  }

  const data = normalizeConsultProductCardData(card.data)

  if (!data) {
    return undefined
  }

  return {
    type: ASSISTANT_CARD_TYPES.CONSULT_PRODUCT_CARD,
    data
  }
}

/**
 * 将原始商品购买卡载荷规范化为前端使用的结构。
 *
 * @param card - 原始卡片载荷
 * @returns 规范化后的商品购买卡载荷，缺少关键字段时返回 undefined
 */
const normalizeProductPurchaseCardPayload = (
  card: RawAssistantCardPayload
): AssistantTypes.ProductPurchaseCardPayload | undefined => {
  if (card.type !== ASSISTANT_CARD_TYPES.PRODUCT_PURCHASE_CARD) {
    return undefined
  }

  const data = normalizeProductPurchaseCardData(card.data)

  if (!data) {
    return undefined
  }

  return {
    type: ASSISTANT_CARD_TYPES.PRODUCT_PURCHASE_CARD,
    data
  }
}

/**
 * 将原始订单卡载荷规范化为前端使用的结构。
 *
 * @param card - 原始卡片载荷
 * @returns 规范化后的订单卡载荷，缺少关键字段时返回 undefined
 */
const normalizeOrderCardPayload = (card: RawAssistantCardPayload): AssistantTypes.OrderCardPayload | undefined => {
  if (card.type !== ASSISTANT_CARD_TYPES.ORDER_CARD) {
    return undefined
  }

  const data = normalizeOrderCardData(card.data)

  if (!data) {
    return undefined
  }

  return {
    type: ASSISTANT_CARD_TYPES.ORDER_CARD,
    data
  }
}

/**
 * 将原始售后卡载荷规范化为前端使用的结构。
 *
 * @param card - 原始卡片载荷
 * @returns 规范化后的售后卡载荷，缺少关键字段时返回 undefined
 */
const normalizeAfterSaleCardPayload = (
  card: RawAssistantCardPayload
): AssistantTypes.AfterSaleCardPayload | undefined => {
  if (card.type !== ASSISTANT_CARD_TYPES.AFTER_SALE_CARD) {
    return undefined
  }

  const data = normalizeAfterSaleCardData(card.data)

  if (!data) {
    return undefined
  }

  return {
    type: ASSISTANT_CARD_TYPES.AFTER_SALE_CARD,
    data
  }
}

/**
 * 将原始就诊人卡载荷规范化为前端使用的结构。
 *
 * @param card - 原始卡片载荷
 * @returns 规范化后的就诊人卡载荷，缺少关键字段时返回 undefined
 */
const normalizePatientCardPayload = (card: RawAssistantCardPayload): AssistantTypes.PatientCardPayload | undefined => {
  if (card.type !== ASSISTANT_CARD_TYPES.PATIENT_CARD) {
    return undefined
  }

  const data = normalizePatientCardData(card.data)

  if (!data) {
    return undefined
  }

  return {
    type: ASSISTANT_CARD_TYPES.PATIENT_CARD,
    data
  }
}

/**
 * 将原始同意卡载荷规范化为前端使用的结构。
 *
 * @param card - 原始卡片载荷
 * @returns 规范化后的同意卡载荷，缺少关键字段时返回 undefined
 */
const normalizeConsentCardPayload = (card: RawAssistantCardPayload): AssistantTypes.ConsentCardPayload | undefined => {
  if (card.type !== ASSISTANT_CARD_TYPES.CONSENT_CARD) {
    return undefined
  }

  const data = normalizeConsentCardData(card.data)

  if (!data) {
    return undefined
  }

  return {
    type: ASSISTANT_CARD_TYPES.CONSENT_CARD,
    data
  }
}

/**
 * 将原始选择卡载荷规范化为前端使用的结构。
 *
 * @param card - 原始卡片载荷
 * @returns 规范化后的选择卡载荷，缺少关键字段时返回 undefined
 */
const normalizeSelectionCardPayload = (
  card: RawAssistantCardPayload
): AssistantTypes.SelectionCardPayload | undefined => {
  if (card.type !== ASSISTANT_CARD_TYPES.SELECTION_CARD) {
    return undefined
  }

  const data = normalizeSelectionCardData(card.data)

  if (!data) {
    return undefined
  }

  return {
    type: ASSISTANT_CARD_TYPES.SELECTION_CARD,
    data
  }
}

/**
 * 将原始 consultation 问卷卡载荷规范化为前端使用的结构。
 *
 * @param card - 原始卡片载荷
 * @returns 规范化后的 consultation 问卷卡载荷，缺少关键字段时返回 undefined
 */
const normalizeConsultationQuestionnaireCardPayload = (
  card: RawAssistantCardPayload
): AssistantTypes.ConsultationQuestionnaireCardPayload | undefined => {
  if (card.type !== ASSISTANT_CARD_TYPES.CONSULTATION_QUESTIONNAIRE_CARD) {
    return undefined
  }

  const data = normalizeConsultationQuestionnaireCardData(card.data)

  if (!data) {
    return undefined
  }

  return {
    type: ASSISTANT_CARD_TYPES.CONSULTATION_QUESTIONNAIRE_CARD,
    data
  }
}

/**
 * 将后端原始会话条目规范化为前端类型。
 *
 * @param item - 后端返回的原始会话条目
 * @returns 规范化后的 ConversationItem，缺少 UUID 时返回 null
 */
const normalizeConversationItem = (item: RawConversationItem): AssistantTypes.ConversationItem | null => {
  const conversationUuid = item.conversation_uuid ?? item.conversationUuid

  if (!conversationUuid) {
    return null
  }

  return {
    conversationUuid,
    title: item.title?.trim() || '新对话'
  }
}

/**
 * 规范化会话变更接口返回的会话 UUID。
 *
 * @param data - 后端返回的原始数据
 * @returns 有效的会话 UUID，无效时返回 null
 */
const normalizeConversationUuid = (data: RawConversationMutationData): string | null => {
  const conversationUuid = data.conversation_uuid ?? data.conversationUuid

  return typeof conversationUuid === 'string' && conversationUuid.trim() ? conversationUuid.trim() : null
}

/**
 * 解析 submit / stop 接口返回的运行结果。
 *
 * @param data - 后端返回的原始运行结果
 * @returns 规范化后的运行结果，缺少关键字段时返回 null
 */
const normalizeAssistantChatRunData = (data?: RawAssistantChatRunData): AssistantChatRunResult | null => {
  if (!data) {
    return null
  }

  const conversationUuid = getString(data.conversation_uuid) ?? getString(data.conversationUuid)
  const messageUuid = getString(data.message_uuid) ?? getString(data.messageUuid)
  const runStatus = getString(data.run_status) ?? getString(data.runStatus)

  if (!conversationUuid || !messageUuid || !runStatus) {
    return null
  }

  return {
    conversationUuid,
    messageUuid,
    runStatus
  }
}

/**
 * 解析原始 Response 中的通用 JSON 包裹结构。
 *
 * @param response - 原始 HTTP 响应
 * @param fallbackMessage - 解析失败时的兜底错误文案
 * @returns 通用 JSON 包裹对象
 */
const parseAssistantResponseEnvelope = async <T>(
  response: Response,
  fallbackMessage: string
): Promise<RawAssistantResponseEnvelope<T>> => {
  try {
    return (await response.json()) as RawAssistantResponseEnvelope<T>
  } catch (error) {
    logServerError('ai', `${response.status} ${response.url}`, error)
    throw new Error(normalizeServerErrorMessage(fallbackMessage, fallbackMessage))
  }
}

/**
 * 将后端原始卡片数据规范化为前端类型。
 *
 * @param card - 后端返回的原始卡片数据
 * @returns 规范化后的 CardPayload，缺少 type 时返回 undefined
 */
const normalizeCardPayload = (card?: RawAssistantCardPayload): AssistantTypes.CardPayload | undefined => {
  if (!card?.type) {
    return undefined
  }

  switch (card.type) {
    case ASSISTANT_CARD_TYPES.PRODUCT_CARD:
      return normalizeProductCardPayload(card)
    case ASSISTANT_CARD_TYPES.CONSULT_PRODUCT_CARD:
      return normalizeConsultProductCardPayload(card)
    case ASSISTANT_CARD_TYPES.PRODUCT_PURCHASE_CARD:
      return normalizeProductPurchaseCardPayload(card)
    case ASSISTANT_CARD_TYPES.ORDER_CARD:
      return normalizeOrderCardPayload(card)
    case ASSISTANT_CARD_TYPES.AFTER_SALE_CARD:
      return normalizeAfterSaleCardPayload(card)
    case ASSISTANT_CARD_TYPES.PATIENT_CARD:
      return normalizePatientCardPayload(card)
    case ASSISTANT_CARD_TYPES.CONSENT_CARD:
      return normalizeConsentCardPayload(card)
    case ASSISTANT_CARD_TYPES.CONSULTATION_QUESTIONNAIRE_CARD:
      return normalizeConsultationQuestionnaireCardPayload(card)
    case ASSISTANT_CARD_TYPES.SELECTION_CARD:
      return normalizeSelectionCardPayload(card)
    default:
      return undefined
  }
}

/**
 * 将后端 SSE 持久化消息规范化为前端类型。
 *
 * @param message - 后端返回的原始消息
 * @returns 规范化后的 Message，缺少必要字段时返回 null
 */
const normalizeAssistantMessage = (message: RawAssistantMessage): AssistantTypes.Message | null => {
  if (!message.id || !message.role) {
    return null
  }

  const card = normalizeCardPayload(message.card)
  const messageType = message.message_type ?? (card ? ASSISTANT_MESSAGE_TYPES.CARD : ASSISTANT_MESSAGE_TYPES.TEXT)

  if (messageType === ASSISTANT_MESSAGE_TYPES.CARD && !card) {
    return null
  }

  return {
    id: message.id,
    sourceMessageId: getString(message.source_message_id) ?? getString(message.sourceMessageId),
    cardUuid: getString(message.card_uuid) ?? getString(message.card?.card_uuid),
    role: message.role,
    messageType,
    content: messageType === ASSISTANT_MESSAGE_TYPES.TEXT ? (message.content ?? '') : undefined,
    card,
    thinking: message.thinking,
    status: message.status,
    conversationUuid: message.conversation_uuid,
    createdAt: message.created_at
  }
}

/**
 * 将后端历史卡片规范化为历史渲染需要的结构。
 * 商品卡会额外走产品数据校验，其他卡片仅保留通用 data 结构。
 *
 * @param card - 历史接口返回的原始卡片
 * @returns 规范化后的历史卡片，关键字段缺失时返回 null
 */
const normalizeHistoryCard = (card: RawHistoryCard): AssistantTypes.HistoryCard | null => {
  const cardUuid = getString(card.card_uuid)

  if (!cardUuid || !card.type) {
    return null
  }

  if (card.type === ASSISTANT_CARD_TYPES.PRODUCT_CARD) {
    const data = normalizeProductCardData(card.data)

    if (!data) {
      return null
    }

    return {
      cardUuid,
      type: ASSISTANT_CARD_TYPES.PRODUCT_CARD,
      data
    }
  }

  if (card.type === ASSISTANT_CARD_TYPES.CONSULT_PRODUCT_CARD) {
    const data = normalizeConsultProductCardData(card.data)

    if (!data) {
      return null
    }

    return {
      cardUuid,
      type: ASSISTANT_CARD_TYPES.CONSULT_PRODUCT_CARD,
      data
    }
  }

  if (card.type === ASSISTANT_CARD_TYPES.PRODUCT_PURCHASE_CARD) {
    const data = normalizeProductPurchaseCardData(card.data)

    if (!data) {
      return null
    }

    return {
      cardUuid,
      type: ASSISTANT_CARD_TYPES.PRODUCT_PURCHASE_CARD,
      data
    }
  }

  if (card.type === ASSISTANT_CARD_TYPES.ORDER_CARD) {
    const data = normalizeOrderCardData(card.data)

    if (!data) {
      return null
    }

    return {
      cardUuid,
      type: ASSISTANT_CARD_TYPES.ORDER_CARD,
      data
    }
  }

  if (card.type === ASSISTANT_CARD_TYPES.AFTER_SALE_CARD) {
    const data = normalizeAfterSaleCardData(card.data)

    if (!data) {
      return null
    }

    return {
      cardUuid,
      type: ASSISTANT_CARD_TYPES.AFTER_SALE_CARD,
      data
    }
  }

  if (card.type === ASSISTANT_CARD_TYPES.PATIENT_CARD) {
    const data = normalizePatientCardData(card.data)

    if (!data) {
      return null
    }

    return {
      cardUuid,
      type: ASSISTANT_CARD_TYPES.PATIENT_CARD,
      data
    }
  }

  if (card.type === ASSISTANT_CARD_TYPES.CONSENT_CARD) {
    const data = normalizeConsentCardData(card.data)

    if (!data) {
      return null
    }

    return {
      cardUuid,
      type: ASSISTANT_CARD_TYPES.CONSENT_CARD,
      data
    }
  }

  if (card.type === ASSISTANT_CARD_TYPES.CONSULTATION_QUESTIONNAIRE_CARD) {
    const data = normalizeConsultationQuestionnaireCardData(card.data)

    if (!data) {
      return null
    }

    return {
      cardUuid,
      type: ASSISTANT_CARD_TYPES.CONSULTATION_QUESTIONNAIRE_CARD,
      data
    }
  }

  if (card.type === ASSISTANT_CARD_TYPES.SELECTION_CARD) {
    const data = normalizeSelectionCardData(card.data)

    if (!data) {
      return null
    }

    return {
      cardUuid,
      type: ASSISTANT_CARD_TYPES.SELECTION_CARD,
      data
    }
  }

  const data = getRecord(card.data)

  if (!data) {
    return null
  }

  return {
    cardUuid,
    type: card.type,
    data
  }
}

/**
 * 将后端历史消息规范化为历史渲染使用的结构。
 *
 * @param message - 历史接口返回的原始消息
 * @returns 规范化后的 HistoryMessage，缺少必要字段时返回 null
 */
const normalizeHistoryMessage = (message: RawHistoryMessage): AssistantTypes.HistoryMessage | null => {
  const id = getString(message.id)

  if (!id || !message.role) {
    return null
  }

  const cards = message.cards
    ?.map(normalizeHistoryCard)
    .filter((item): item is AssistantTypes.HistoryCard => item !== null)

  return {
    id,
    role: message.role,
    content: message.content ?? '',
    thinking: message.thinking,
    status: message.status,
    cards: cards?.length ? cards : undefined,
    conversationUuid: message.conversation_uuid,
    createdAt: message.created_at
  }
}

/**
 * 将后端原始 SSE 事件规范化为前端 StreamEvent。
 *
 * @param event - 后端推送的原始 SSE 事件
 * @returns 规范化后的 StreamEvent
 */
const normalizeStreamEvent = (event: RawAssistantStreamEvent): AssistantTypes.StreamEvent => {
  const message = event.message ? normalizeAssistantMessage(event.message) : null
  const messages = event.messages
    ?.map(normalizeAssistantMessage)
    .filter((item): item is AssistantTypes.Message => item !== null)

  return {
    type: event.type ?? ASSISTANT_STREAM_EVENT_TYPES.UNKNOWN,
    content: event.content
      ? {
          text: event.content.text,
          node: event.content.node,
          parentNode: event.content.parent_node,
          state: event.content.state,
          message: event.content.message,
          result: event.content.result,
          name: event.content.name,
          arguments: event.content.arguments
        }
      : undefined,
    meta: event.meta
      ? {
          conversationUuid: event.meta.conversation_uuid,
          messageUuid: event.meta.message_uuid,
          cardUuid: event.meta.card_uuid,
          runStatus: event.meta.run_status,
          snapshot: Boolean(event.meta.snapshot),
          replace: Boolean(event.meta.replace)
        }
      : undefined,
    action:
      event.action?.type && event.action?.target
        ? {
            type: event.action.type,
            target: event.action.target,
            payload: event.action.payload,
            priority: event.action.priority
          }
        : undefined,
    card: normalizeCardPayload(event.card),
    message: message ?? undefined,
    messages: messages?.length ? messages : undefined,
    isEnd: Boolean(event.is_end),
    timestamp: event.timestamp
  }
}

/**
 * 分页获取会话列表。
 *
 * @param pageNum - 页码（从 1 开始）
 * @param pageSize - 每页条数，默认 `CONVERSATION_PAGE_SIZE`
 * @returns 包含 items、total、hasMore 的分页结果
 */
export const fetchConversationListPage = async (
  pageNum: number,
  pageSize: number = CONVERSATION_PAGE_SIZE
): Promise<ConversationListPageResult> => {
  const response = await requestClient.get<RawPageResponse<RawConversationItem>>(
    resolveAiAgentHttpUrl('/client/assistant/conversation/list'),
    {
      params: {
        page_num: pageNum,
        page_size: pageSize
      }
    }
  )

  const total = response.total ?? 0
  const items = (response.rows ?? [])
    .map(normalizeConversationItem)
    .filter((item): item is AssistantTypes.ConversationItem => item !== null)

  const hasMore = pageNum * pageSize < total && items.length === pageSize

  return {
    items,
    total,
    hasMore
  }
}

/**
 * 调用后端接口修改当前登录用户的助手会话标题。
 *
 * @param conversationUuid - 会话 UUID
 * @param newTitle - 新标题
 * @returns 后端确认后的会话信息
 */
export const renameConversation = async (
  conversationUuid: string,
  newTitle: string
): Promise<AssistantTypes.ConversationItem> => {
  const normalizedConversationUuid = conversationUuid.trim()
  const normalizedTitle = newTitle.trim()

  if (!normalizedConversationUuid) {
    throw new Error('会话标识不能为空')
  }

  if (!normalizedTitle) {
    throw new Error('会话标题不能为空')
  }

  const response = await requestClient.put<RawConversationMutationData>(
    resolveAiAgentHttpUrl(`/client/assistant/conversation/${encodeURIComponent(normalizedConversationUuid)}`),
    {
      title: normalizedTitle
    }
  )

  const conversation = normalizeConversationItem(response)

  if (!conversation) {
    throw new Error('会话标题更新成功，但返回数据无效')
  }

  return conversation
}

/**
 * 调用后端接口逻辑删除当前登录用户的助手会话。
 *
 * @param conversationUuid - 会话 UUID
 * @returns 后端确认删除的会话 UUID
 */
export const deleteConversation = async (conversationUuid: string): Promise<string> => {
  const normalizedConversationUuid = conversationUuid.trim()

  if (!normalizedConversationUuid) {
    throw new Error('会话标识不能为空')
  }

  const response = await requestClient.delete<RawConversationMutationData>(
    resolveAiAgentHttpUrl(`/client/assistant/conversation/${encodeURIComponent(normalizedConversationUuid)}`)
  )

  const deletedConversationUuid = normalizeConversationUuid(response)

  if (!deletedConversationUuid) {
    throw new Error('会话删除成功，但返回数据无效')
  }

  return deletedConversationUuid
}

/**
 * 获取单页会话历史记录。
 *
 * @param conversationUuid - 会话 UUID
 * @param pageNum - 页码
 * @param pageSize - 每页条数
 * @returns 后端原始分页响应
 */
const getConversationHistoryPage = (conversationUuid: string, pageNum: number, pageSize: number) => {
  return requestClient.get<RawPageResponse<RawHistoryMessage>>(
    resolveAiAgentHttpUrl(`/client/assistant/history/${conversationUuid}`),
    {
      params: {
        page_num: pageNum,
        page_size: pageSize
      }
    }
  )
}

/**
 * 获取指定会话的全部历史消息。
 *
 * @param conversationUuid - 会话 UUID
 * @returns 规范化后的历史消息数组（从旧到新）
 */
export const getAssistantConversationHistory = async (conversationUuid: string) => {
  const firstPage = await getConversationHistoryPage(conversationUuid, 1, HISTORY_PAGE_SIZE)
  const total = firstPage.total ?? 0
  const totalPages = Math.max(1, Math.ceil(total / HISTORY_PAGE_SIZE))
  const pages = new Map<number, RawPageResponse<RawHistoryMessage>>([[1, firstPage]])

  if (totalPages > 1) {
    const restPages = await Promise.all(
      Array.from({ length: totalPages - 1 }, (_, index) =>
        getConversationHistoryPage(conversationUuid, index + 2, HISTORY_PAGE_SIZE)
      )
    )

    restPages.forEach((page, index) => {
      pages.set(index + 2, page)
    })
  }

  return Array.from(pages.entries())
    .sort(([pageNumA], [pageNumB]) => pageNumA - pageNumB)
    .flatMap(([, page]) => page.rows ?? [])
    .map(normalizeHistoryMessage)
    .filter((message): message is AssistantTypes.HistoryMessage => message !== null)
}

/**
 * 提交一轮新的客户端助手提问。
 *
 * @param options - submit 接口请求参数
 * @returns 规范化后的 submit 结果；遇到 409 时返回 conflict 结果而不是抛错
 */
export const submitAssistantChat = async ({
  messageType,
  content,
  imageUrls,
  card,
  conversationUuid,
  cardAction,
  reasoningEnabled,
  signal
}: AssistantChatSubmitOptions): Promise<AssistantChatSubmitResult> => {
  /** 规整后的图片 URL 列表。 */
  const normalizedImageUrls = (imageUrls || []).map(item => item.trim()).filter(Boolean)

  /** submit 接口原始 HTTP 响应。 */
  const response = await rawRequest({
    url: resolveAiAgentHttpUrl('/client/assistant/chat/submit'),
    method: 'POST',
    body: {
      message_type: messageType,
      ...(messageType === ASSISTANT_MESSAGE_TYPES.TEXT && content?.trim() ? { content: content.trim() } : {}),
      ...(normalizedImageUrls.length > 0 ? { image_urls: normalizedImageUrls } : {}),
      ...(card ? { card } : {}),
      ...(typeof reasoningEnabled === 'boolean' ? { reasoning_enabled: reasoningEnabled } : {}),
      ...(cardAction
        ? {
            card_action: {
              type: cardAction.type,
              message_id: cardAction.messageId,
              card_uuid: cardAction.cardUuid,
              ...(cardAction.cardType?.trim() ? { card_type: cardAction.cardType.trim() } : {}),
              ...(cardAction.cardScene?.trim() ? { card_scene: cardAction.cardScene.trim() } : {}),
              ...(cardAction.cardTitle?.trim() ? { card_title: cardAction.cardTitle.trim() } : {}),
              ...(cardAction.action?.trim() ? { action: cardAction.action.trim() } : {})
            }
          }
        : {}),
      ...(conversationUuid?.trim() ? { conversation_uuid: conversationUuid.trim() } : {})
    },
    signal
  })
  /** submit 接口的业务包裹响应。 */
  const envelope = await parseAssistantResponseEnvelope<RawAssistantChatRunData>(
    response,
    ASSISTANT_CHAT_SUBMIT_REQUEST_FAILED_TEXT
  )
  /** 规范化后的运行结果。 */
  const runData = normalizeAssistantChatRunData(envelope.data)
  /** 后端返回的业务状态码。 */
  const businessCode = typeof envelope.code === 'number' ? envelope.code : response.status
  /** 后端返回的提示文案。 */
  const responseMessage = normalizeServerErrorMessage(
    getString(envelope.message),
    ASSISTANT_CHAT_SUBMIT_REQUEST_FAILED_TEXT
  )

  if (businessCode === ASSISTANT_CHAT_CONFLICT_CODE) {
    if (!runData) {
      logServerError('ai', `POST ${response.url}`, envelope)
      throw new Error(responseMessage)
    }

    return {
      type: 'conflict',
      ...runData,
      message: responseMessage
    }
  }

  if (!response.ok || businessCode !== 200) {
    logServerError('ai', `POST ${response.url}`, envelope)
    throw new Error(responseMessage)
  }

  if (!runData) {
    throw new Error('消息发送成功，但返回的运行信息无效')
  }

  return {
    type: 'submitted',
    ...runData
  }
}

/**
 * 获取客户端聊天输入区能力。
 *
 * @returns 客户端聊天输入区能力结果。
 */
export const getAssistantChatCapability = async (): Promise<AssistantChatCapability> => {
  const response = await requestClient.get<RawAssistantChatCapabilityData>(
    resolveAiAgentHttpUrl('/client/assistant/chat/capability')
  )

  return {
    imageUploadEnabled:
      typeof response?.imageUploadEnabled === 'boolean'
        ? response.imageUploadEnabled
        : Boolean(response?.image_upload_enabled),
    imageUploadDisabledMessage:
      getString(response?.imageUploadDisabledMessage) ??
      getString(response?.image_upload_disabled_message) ??
      undefined,
    reasoningToggleEnabled:
      typeof response?.reasoningToggleEnabled === 'boolean'
        ? response.reasoningToggleEnabled
        : Boolean(response?.reasoning_toggle_enabled),
    reasoningToggleDisabledMessage:
      getString(response?.reasoningToggleDisabledMessage) ??
      getString(response?.reasoning_toggle_disabled_message) ??
      undefined
  }
}

/**
 * attach 到某个已存在的客户端助手会话流。
 *
 * @param options - stream 接口请求参数
 * @returns AbortController 实例，可用于断开当前 attach 连接
 */
export const attachAssistantChatStream = ({
  conversationUuid,
  lastEventId,
  signal,
  onEvent,
  onClose,
  onError
}: AssistantChatAttachStreamOptions) => {
  /** 规整后的会话 UUID。 */
  const normalizedConversationUuid = conversationUuid.trim()

  if (!normalizedConversationUuid) {
    throw new Error('会话标识不能为空')
  }

  return sseRequest<AssistantTypes.StreamEvent>({
    url: resolveAiAgentHttpUrl(
      `/client/assistant/chat/stream?conversation_uuid=${encodeURIComponent(normalizedConversationUuid)}`
    ),
    method: 'GET',
    headers: lastEventId?.trim() ? { 'Last-Event-ID': lastEventId.trim() } : undefined,
    signal,
    maxRetries: 3,
    parseMessage: raw => normalizeStreamEvent(JSON.parse(raw) as RawAssistantStreamEvent),
    onMessage: onEvent,
    onClose,
    onError
  })
}

/**
 * 请求后端停止当前会话的 AI 生成任务。
 *
 * @param options - stop 接口请求参数
 * @returns 规范化后的 stop 结果
 */
export const stopAssistantChat = async ({
  conversationUuid,
  signal
}: AssistantChatStopOptions): Promise<AssistantChatStopResult> => {
  /** 规整后的会话 UUID。 */
  const normalizedConversationUuid = conversationUuid.trim()

  if (!normalizedConversationUuid) {
    throw new Error('会话标识不能为空')
  }

  /** stop 接口原始 HTTP 响应。 */
  const response = await rawRequest({
    url: resolveAiAgentHttpUrl('/client/assistant/chat/stop'),
    method: 'POST',
    body: {
      conversation_uuid: normalizedConversationUuid
    },
    signal
  })
  /** stop 接口的业务包裹响应。 */
  const envelope = await parseAssistantResponseEnvelope<RawAssistantChatRunData>(
    response,
    ASSISTANT_CHAT_STOP_REQUEST_FAILED_TEXT
  )
  /** 规范化后的运行结果。 */
  const runData = normalizeAssistantChatRunData(envelope.data)
  /** 后端返回的业务状态码。 */
  const businessCode = typeof envelope.code === 'number' ? envelope.code : response.status
  /** 后端返回的提示文案。 */
  const responseMessage = normalizeServerErrorMessage(
    getString(envelope.message),
    ASSISTANT_CHAT_STOP_REQUEST_FAILED_TEXT
  )

  if (!response.ok || businessCode !== 200) {
    logServerError('ai', `POST ${response.url}`, envelope)
    throw new Error(responseMessage)
  }

  if (!runData) {
    throw new Error('停止请求已提交，但返回的运行信息无效')
  }

  return {
    ...runData,
    stopRequested: Boolean(envelope.data?.stop_requested ?? envelope.data?.stopRequested)
  }
}

/** Assistant TTS 音频流请求参数。 */
interface AssistantMessageTtsStreamOptions {
  /** 需要转语音的 AI 消息 UUID。 */
  messageUuid: string
  /** 当前请求的中断信号。 */
  signal?: AbortSignal
}

/**
 * 请求某条 AI 消息的 TTS 音频流。
 *
 * @param options - TTS 音频流请求参数
 * @returns 原始音频流响应
 */
export const streamAssistantMessageTts = async ({
  messageUuid,
  signal
}: AssistantMessageTtsStreamOptions): Promise<Response> => {
  /** 归一化后的消息 UUID。 */
  const normalizedMessageUuid = messageUuid.trim()

  if (!normalizedMessageUuid) {
    throw new Error(ASSISTANT_TTS_MESSAGE_UUID_REQUIRED_TEXT)
  }

  const response = await rawRequest({
    url: resolveAiAgentHttpUrl('/client/assistant/message/tts/stream'),
    method: 'POST',
    body: {
      message_uuid: normalizedMessageUuid
    },
    signal
  })

  if (!response.ok) {
    const errorMessage = await resolveRawResponseErrorMessage(response)
    logServerError('ai', `POST ${response.url}`, {
      status: response.status,
      message: errorMessage
    })
    throw new Error(errorMessage)
  }

  return response
}
