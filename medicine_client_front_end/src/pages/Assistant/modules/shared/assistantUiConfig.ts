import { createElement, type ElementType } from 'react'
import { renderToStaticMarkup } from 'react-dom/server'
import { Image as ImageIcon, Order as OrderIcon, Service as ServiceIcon } from '@nutui/icons-react'
import { UserRound } from 'lucide-react'

type ValueOf<T> = T[keyof T]

/**
 * Assistant 入口页会用到的工具栏类型常量。
 * 这组值属于页面 UI 配置，不属于后端协议。
 */
export const ASSISTANT_TOOLBAR_TYPES = {
  IMAGE: 'image',
  ORDER: 'order',
  AFTER_SALE: 'after-sale',
  PATIENT: 'patient'
} as const

/** Assistant 工具栏类型值联合。 */
export type AssistantToolbarType = ValueOf<typeof ASSISTANT_TOOLBAR_TYPES>

/** AI 助手默认头像路径 */
export const ASSISTANT_AVATAR = '/logo.png'

/** 用户默认头像路径（用户未设置头像时兜底） */
export const DEFAULT_USER_AVATAR = '/images/default-avatar.png'

/** Assistant 输入模式值。 */
export const ASSISTANT_INPUT_MODES = {
  TEXT: 'text',
  VOICE: 'voice'
} as const

/** Assistant 输入模式联合类型。 */
export type AssistantInputMode = ValueOf<typeof ASSISTANT_INPUT_MODES>

/** Assistant 工具栏项类型。 */
export interface AssistantToolbarItem {
  type: AssistantToolbarType
  title: string
  icon?: string
  img?: string
}

/**
 * 将图标组件渲染为静态 SVG 字符串。
 *
 * @param iconComponent - 目标图标组件
 * @param props - 图标渲染属性
 * @returns SVG 字符串
 */
const renderToolbarIconSvg = (iconComponent: ElementType, props: Record<string, unknown>) => {
  return renderToStaticMarkup(createElement(iconComponent, props))
}

/**
 * 工具栏配置
 * 将 NutUI 图标组件渲染为 data URI SVG 图片，供 Assistant 页面工具栏使用。
 */
const imageIconSvg = renderToolbarIconSvg(ImageIcon, { color: '#333' })
const orderIconSvg = renderToolbarIconSvg(OrderIcon, { color: '#333' })
const afterSaleIconSvg = renderToolbarIconSvg(ServiceIcon, { color: '#333' })
const patientIconSvg = renderToolbarIconSvg(UserRound, { color: '#333', size: 18, strokeWidth: 1.75 })

/** Assistant 底部工具栏配置（图片、订单、售后、就诊人）。 */
export const ASSISTANT_TOOLBAR_ITEMS: AssistantToolbarItem[] = [
  {
    type: ASSISTANT_TOOLBAR_TYPES.IMAGE,
    title: '图片',
    img: `data:image/svg+xml;utf8,${encodeURIComponent(imageIconSvg)}`
  },
  {
    type: ASSISTANT_TOOLBAR_TYPES.ORDER,
    title: '订单',
    img: `data:image/svg+xml;utf8,${encodeURIComponent(orderIconSvg)}`
  },
  {
    type: ASSISTANT_TOOLBAR_TYPES.AFTER_SALE,
    title: '售后',
    img: `data:image/svg+xml;utf8,${encodeURIComponent(afterSaleIconSvg)}`
  },
  {
    type: ASSISTANT_TOOLBAR_TYPES.PATIENT,
    title: '就诊人',
    img: `data:image/svg+xml;utf8,${encodeURIComponent(patientIconSvg)}`
  }
]
