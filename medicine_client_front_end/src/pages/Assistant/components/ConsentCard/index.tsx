import React from 'react'
import { CircleHelp } from 'lucide-react'
import { AssistantCardShell } from '../MessagePrimitives'
import type { ConsentActionContent, ConsentCardSubmitPayload } from '../../modules/messages/chatTypes'
import styles from './index.module.less'

/** 同意卡默认标题文案。 */
const DEFAULT_CONSENT_CARD_TITLE = '请确认您的选择'
/** 同意卡头部说明文案。 */
const CONSENT_CARD_HEADER_HINT = '确认后我会继续为您处理下一步。'

interface ConsentCardProps {
  localMessageId: string
  messageId: string
  cardUuid: string
  scene?: string
  title?: string
  description?: string
  confirm: ConsentActionContent
  reject: ConsentActionContent
  onSubmit?: (payload: ConsentCardSubmitPayload) => void
  /** 卡片是否可见，设为 false 后将先播放退出动画再回调。 */
  visible?: boolean
  /** 退出动画播放完成后的回调。 */
  onExitComplete?: () => void
}

const ConsentCard: React.FC<ConsentCardProps> = ({
  localMessageId,
  messageId,
  cardUuid,
  scene,
  title,
  description,
  confirm,
  reject,
  onSubmit,
  visible,
  onExitComplete
}) => {
  const canInteract = Boolean(localMessageId && messageId && cardUuid)

  const handleActionClick = (action: ConsentActionContent) => {
    if (!canInteract) {
      return
    }

    const payload: ConsentCardSubmitPayload = {
      localMessageId,
      messageId,
      cardUuid,
      cardType: 'consent-card',
      cardScene: scene,
      cardTitle: title,
      action: action.action,
      label: action.label,
      value: action.value
    }

    onSubmit?.(payload)
  }

  return (
    <AssistantCardShell className={styles.assistantConsentCard} visible={visible} onExitComplete={onExitComplete}>
      <div className={styles.cardHeader}>
        <div className={styles.headerIconBox}>
          <CircleHelp size={14} className={styles.headerIcon} />
        </div>
        <div className={styles.headerMain}>
          <span className={styles.headerTitle}>{title || DEFAULT_CONSENT_CARD_TITLE}</span>
          <span className={styles.headerHint}>{CONSENT_CARD_HEADER_HINT}</span>
        </div>
      </div>

      <div className={styles.content}>
        {description ? (
          <div className={styles.descriptionPanel}>
            <div className={styles.descriptionLabel}>说明</div>
            <div className={styles.description}>{description}</div>
          </div>
        ) : null}
        <div className={styles.actions}>
          <button
            type='button'
            className={styles.confirmBtn}
            disabled={!canInteract}
            onClick={() => handleActionClick(confirm)}
          >
            <span>{confirm.label}</span>
          </button>
          <button
            type='button'
            className={styles.rejectBtn}
            disabled={!canInteract}
            onClick={() => handleActionClick(reject)}
          >
            <span>{reject.label}</span>
          </button>
        </div>
      </div>
    </AssistantCardShell>
  )
}

export default ConsentCard
