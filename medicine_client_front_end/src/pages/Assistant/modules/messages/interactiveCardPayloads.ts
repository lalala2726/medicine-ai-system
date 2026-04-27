import type { AssistantTypes } from '@/api/assistant/contract'
import { ASSISTANT_MESSAGE_TYPES } from '@/api/assistant/contract'
import {
  CHAT_MESSAGE_POSITIONS,
  CHAT_MESSAGE_TYPES,
  type ChatMessage,
  type ConsultationQuestionnaireCardSubmitPayload,
  type ConsentCardSubmitPayload,
  type SelectionCardSubmitPayload
} from './chatTypes'

interface BuildInteractiveCardPayloadOptions {
  userAvatar: string
}

interface InteractiveCardRequestPayload {
  messageType: typeof ASSISTANT_MESSAGE_TYPES.TEXT
  content: string
  cardAction?: AssistantTypes.CardActionPayload
  userMessage: ChatMessage
}

const buildTextMessage = (text: string, userAvatar: string): ChatMessage => ({
  type: CHAT_MESSAGE_TYPES.TEXT,
  content: { text },
  position: CHAT_MESSAGE_POSITIONS.RIGHT,
  user: { avatar: userAvatar }
})

/**
 * 构建同意卡点击后的发送载荷。
 */
export const buildConsentCardRequestPayload = (
  payload: ConsentCardSubmitPayload,
  { userAvatar }: BuildInteractiveCardPayloadOptions
): InteractiveCardRequestPayload => {
  const question = payload.value || payload.label

  return {
    messageType: ASSISTANT_MESSAGE_TYPES.TEXT,
    content: question,
    userMessage: buildTextMessage(payload.label, userAvatar),
    cardAction: {
      type: 'click',
      messageId: payload.messageId,
      cardUuid: payload.cardUuid,
      cardType: payload.cardType,
      cardScene: payload.cardScene,
      cardTitle: payload.cardTitle,
      action: payload.action
    }
  }
}

const buildSelectionDisplayLines = (payload: SelectionCardSubmitPayload) => {
  const selectedLabels = payload.selectedOptions.map(option => option.label)
  const trimmedCustomInput = payload.customInput.trim()

  return trimmedCustomInput ? [...selectedLabels, `其他：${trimmedCustomInput}`] : selectedLabels
}

const buildSelectionQuestionLines = (payload: SelectionCardSubmitPayload) => {
  const selectedValues = payload.selectedOptions.map(option => option.value || option.label)
  const trimmedCustomInput = payload.customInput.trim()

  return trimmedCustomInput ? [...selectedValues, trimmedCustomInput] : selectedValues
}

/**
 * 构建选择卡提交后的发送载荷。
 */
export const buildSelectionCardRequestPayload = (
  payload: SelectionCardSubmitPayload,
  { userAvatar }: BuildInteractiveCardPayloadOptions
): InteractiveCardRequestPayload => {
  const question = buildSelectionQuestionLines(payload).join(' ')

  return {
    messageType: ASSISTANT_MESSAGE_TYPES.TEXT,
    content: question,
    userMessage: buildTextMessage(buildSelectionDisplayLines(payload).join(' '), userAvatar),
    cardAction: {
      type: 'click',
      messageId: payload.messageId,
      cardUuid: payload.cardUuid,
      cardType: payload.cardType,
      cardScene: payload.cardScene,
      cardTitle: payload.cardTitle
    }
  }
}

/**
 * 构建 consultation 问卷卡提交给后端的问题文本行。
 *
 * @param payload - consultation 问卷卡提交载荷
 * @returns 提交给后端的问题文本行列表
 */
const buildConsultationQuestionnaireQuestionLines = (payload: ConsultationQuestionnaireCardSubmitPayload) => {
  return payload.answers.map(answer => {
    const selectedValues = answer.selectedOptions.map(option => option.value || option.label)
    return `${answer.question}：${selectedValues.join('、')}`
  })
}

/**
 * 构建 consultation 问卷卡在聊天气泡中的展示文本行。
 *
 * @param payload - consultation 问卷卡提交载荷
 * @returns 用户侧要展示的文本行列表
 */
const buildConsultationQuestionnaireDisplayLines = (payload: ConsultationQuestionnaireCardSubmitPayload) => {
  return payload.answers.map(answer => {
    const selectedLabels = answer.selectedOptions.map(option => option.label)
    return `${answer.question}：${selectedLabels.join('、')}`
  })
}

/**
 * 构建 consultation 问卷卡提交后的发送载荷。
 */
export const buildConsultationQuestionnaireRequestPayload = (
  payload: ConsultationQuestionnaireCardSubmitPayload,
  { userAvatar }: BuildInteractiveCardPayloadOptions
): InteractiveCardRequestPayload => {
  const questionLines = buildConsultationQuestionnaireQuestionLines(payload)
  const displayLines = buildConsultationQuestionnaireDisplayLines(payload)

  return {
    messageType: ASSISTANT_MESSAGE_TYPES.TEXT,
    content: questionLines.join('\n'),
    userMessage: buildTextMessage(displayLines.join('  \n'), userAvatar)
  }
}
