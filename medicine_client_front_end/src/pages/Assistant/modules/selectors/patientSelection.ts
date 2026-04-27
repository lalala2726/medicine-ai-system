import type { patientProfileTypes } from '@/api/patientProfile'
import { ASSISTANT_CARD_TYPES, ASSISTANT_MESSAGE_TYPES } from '@/api/assistant/contract'
import { CHAT_MESSAGE_POSITIONS, CHAT_MESSAGE_TYPES, type ChatMessage } from '../messages/chatTypes'
import type { AssistantChatSubmitCardPayload } from '@/api/assistant/agent'
import { resolvePatientGenderText } from '@/utils/patientProfile'

/** 就诊人选择后，发送给流式会话层的完整载荷。 */
export interface PatientSelectionPayload {
  messageType: typeof ASSISTANT_MESSAGE_TYPES.CARD
  card: AssistantChatSubmitCardPayload
  userMessage: ChatMessage
}

/** 就诊人选择载荷构建所需的参数。 */
interface BuildPatientSelectionPayloadOptions {
  patient: patientProfileTypes.PatientProfileListVo
  userAvatar: string
}

/**
 * 将就诊人数据整理成聊天卡片消息与本地回显消息。
 *
 * @param options - 载荷构建参数
 * @returns 发送给 Assistant 流式层的就诊人卡选择载荷
 */
export function buildPatientSelectionPayload({
  patient,
  userAvatar
}: BuildPatientSelectionPayloadOptions): PatientSelectionPayload {
  /** 当前就诊人唯一标识。 */
  const patientId = String(patient.id ?? '')
  /** 当前就诊人姓名。 */
  const patientName = patient.name || ''
  /** 当前就诊人性别编码。 */
  const gender = patient.gender || 1
  /** 当前就诊人性别展示文案。 */
  const genderText = resolvePatientGenderText(patient.gender)
  /** 当前就诊人出生日期。 */
  const birthDate = patient.birthDate || ''
  /** 当前就诊人与账户的关系。 */
  const relationship = patient.relationship || ''
  /** 当前就诊人是否为默认就诊人。 */
  const isDefault = patient.isDefault === 1
  /** 当前就诊人完整提交卡片载荷。 */
  const card: AssistantChatSubmitCardPayload = {
    type: ASSISTANT_CARD_TYPES.PATIENT_CARD,
    data: {
      patient_id: patientId,
      name: patientName,
      gender,
      gender_text: genderText,
      birth_date: birthDate,
      relationship,
      is_default: isDefault ? 1 : 0,
      allergy: patient.allergy || '',
      past_medical_history: patient.pastMedicalHistory || '',
      chronic_disease: patient.chronicDisease || '',
      long_term_medications: patient.longTermMedications || ''
    }
  }

  /** 用户发送到聊天区的本地就诊人卡回显消息。 */
  const userMessage: ChatMessage = {
    type: CHAT_MESSAGE_TYPES.PATIENT_CARD,
    content: {
      patientCard: {
        patientId,
        name: patientName,
        gender,
        genderText,
        birthDate,
        relationship,
        isDefault,
        allergy: patient.allergy || undefined,
        pastMedicalHistory: patient.pastMedicalHistory || undefined,
        chronicDisease: patient.chronicDisease || undefined,
        longTermMedications: patient.longTermMedications || undefined
      }
    },
    position: CHAT_MESSAGE_POSITIONS.RIGHT,
    user: { avatar: userAvatar }
  }

  return {
    messageType: ASSISTANT_MESSAGE_TYPES.CARD,
    card,
    userMessage
  }
}
