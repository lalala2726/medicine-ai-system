import React from 'react'
import type { PatientCardContent } from '../../modules/messages/chatTypes'
import { AssistantBubble, AssistantCardShell } from '../MessagePrimitives'
import { calculatePatientAge } from '@/utils/patientProfile'
import styles from './index.module.less'

/** 就诊人卡气泡语义类型。 */
type PatientCardTone = 'assistant' | 'user'

/** 就诊人卡组件属性。 */
interface PatientCardProps {
  /** 就诊人卡内容。 */
  content: PatientCardContent
  /** 卡片布局语义。 */
  tone?: PatientCardTone
}

/**
 * Assistant 就诊人卡组件。
 *
 * @param props - 组件属性
 * @returns 就诊人卡节点
 */
const PatientCard: React.FC<PatientCardProps> = ({ content, tone = 'assistant' }) => {
  /** 当前就诊人年龄。 */
  const patientAge = calculatePatientAge(content.birthDate)
  /** 当前卡片容器样式。 */
  const cardClassName = [styles.patientCard, tone === 'user' ? styles.patientCardUser : styles.patientCardAssistant]
    .filter(Boolean)
    .join(' ')

  /** 当前卡片主体。 */
  const cardContent = (
    <div className={cardClassName}>
      <div className={styles.header}>
        <div className={styles.headerLeft}>
          <span className={styles.name}>{content.name}</span>
          <span className={styles.relationship}>{content.relationship}</span>
          {content.isDefault ? <span className={styles.defaultTag}>默认</span> : null}
        </div>
        <span className={styles.gender}>{content.genderText}</span>
      </div>

      <div className={styles.metaBox}>
        <div className={styles.metaRow}>
          <span className={styles.metaLabel}>年龄</span>
          <span className={styles.metaValue}>{patientAge > 0 ? `${patientAge}岁` : '未知'}</span>
        </div>
        <div className={styles.metaRow}>
          <span className={styles.metaLabel}>出生日期</span>
          <span className={styles.metaValue}>{content.birthDate}</span>
        </div>
      </div>
    </div>
  )

  if (tone === 'user') {
    return (
      <AssistantBubble tone='user' className={styles.userBubble}>
        {cardContent}
      </AssistantBubble>
    )
  }

  return <AssistantCardShell className={styles.assistantCardShell}>{cardContent}</AssistantCardShell>
}

export default PatientCard
