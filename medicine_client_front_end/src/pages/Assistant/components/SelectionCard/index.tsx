import React, { useCallback, useMemo, useState } from 'react'
import { ListChecks, Edit2 } from 'lucide-react'
import { AssistantCardShell } from '../MessagePrimitives'
import type { SelectionCardSubmitPayload, SelectionOptionContent } from '../../modules/messages/chatTypes'
import styles from './index.module.less'

/** 选择卡默认标题文案。 */
const DEFAULT_SELECTION_CARD_TITLE = '请选择'
/** 选择卡默认提交按钮文案。 */
const DEFAULT_SELECTION_CARD_SUBMIT_TEXT = '确认选择'

/** SelectionCard 组件属性。 */
interface SelectionCardProps {
  /** 当前卡片在本地消息列表中的消息 ID。 */
  localMessageId: string
  /** 当前卡片所属的逻辑消息 ID。 */
  messageId: string
  /** 当前卡片唯一标识。 */
  cardUuid: string
  /** 卡片场景标识。 */
  scene?: string
  /** 卡片标题。 */
  title?: string
  /** 卡片说明。 */
  description?: string
  /** 卡片选择模式。 */
  selectionMode: 'single' | 'multiple'
  /** 提交按钮文案。 */
  submitText?: string
  /** 是否允许用户补充输入。 */
  allowCustomInput: boolean
  /** 自定义输入框占位文案。 */
  customInputPlaceholder?: string
  /** 可选项列表。 */
  options: SelectionOptionContent[]
  /** 提交回调。 */
  onSubmit?: (payload: SelectionCardSubmitPayload) => void
  /** 卡片是否可见，设为 false 后将先播放退出动画再回调。 */
  visible?: boolean
  /** 退出动画播放完成后的回调。 */
  onExitComplete?: () => void
}

/**
 * 选择卡组件。
 *
 * @param props - 组件属性
 * @returns 选择卡节点
 */
const SelectionCard: React.FC<SelectionCardProps> = ({
  localMessageId,
  messageId,
  cardUuid,
  scene,
  title,
  description,
  selectionMode,
  submitText = DEFAULT_SELECTION_CARD_SUBMIT_TEXT,
  allowCustomInput,
  customInputPlaceholder,
  options,
  onSubmit,
  visible,
  onExitComplete
}) => {
  const [selectedOptionIds, setSelectedOptionIds] = useState<string[]>([])
  const [customInput, setCustomInput] = useState('')
  const [showInputArea, setShowInputArea] = useState(false)

  const selectedOptions = useMemo(
    () => options.filter(option => selectedOptionIds.includes(option.id)),
    [options, selectedOptionIds]
  )
  /** 当前选择卡的头部说明文案。 */
  const selectionCardHeaderHint = useMemo(() => {
    if (selectionMode === 'multiple' && allowCustomInput) {
      return '支持多选，也可以手动补充。'
    }
    if (selectionMode === 'multiple') {
      return '可按实际情况选择多个选项。'
    }
    if (allowCustomInput) {
      return '请选择一项，也可以手动补充。'
    }
    return '请选择最符合当前情况的选项。'
  }, [allowCustomInput, selectionMode])

  // 根据选项字符的视觉长度，动态决定显示为 3列、2列 还是 1列
  const gridClass = useMemo(() => {
    if (options.length === 0 && !allowCustomInput) return styles.oneCol

    let maxVisualLen = 0
    const allLabels = options.map(opt => opt.label)
    if (allowCustomInput) allLabels.push('输入')

    allLabels.forEach(label => {
      let len = 0
      for (let i = 0; i < label.length; i++) {
        // 非ASCII字符（如中文）算1，数字字母符号算0.5
        len += label.charCodeAt(i) > 255 ? 1 : 0.5
      }
      if (len > maxVisualLen) maxVisualLen = len
    })

    if (maxVisualLen > 9) return styles.oneCol
    if (maxVisualLen > 5.5) return styles.twoCols
    return styles.threeCols
  }, [options, allowCustomInput])

  const canInteract = Boolean(localMessageId && messageId && cardUuid)
  const canSubmit = canInteract && (selectedOptions.length > 0 || (showInputArea && customInput.trim().length > 0))

  /**
   * 按指定内容提交当前选择卡。
   *
   * @param nextSelectedOptions - 本次要提交的已选选项
   * @param nextCustomInput - 本次要提交的自定义输入
   * @returns 当前是否已触发提交
   */
  const submitSelectionCard = useCallback(
    (nextSelectedOptions: SelectionOptionContent[], nextCustomInput = '') => {
      const trimmedCustomInput = nextCustomInput.trim()
      const canSubmitCurrent = canInteract && (nextSelectedOptions.length > 0 || trimmedCustomInput.length > 0)

      if (!canSubmitCurrent) {
        return false
      }

      onSubmit?.({
        localMessageId,
        messageId,
        cardUuid,
        cardType: 'selection-card',
        cardScene: scene,
        cardTitle: title,
        selectionMode,
        selectedOptions: nextSelectedOptions,
        customInput: trimmedCustomInput
      })

      return true
    },
    [canInteract, cardUuid, localMessageId, messageId, onSubmit, scene, selectionMode, title]
  )

  if (options.length === 0 && !allowCustomInput) {
    return null
  }

  /**
   * 切换普通选项的选中状态。
   *
   * @param optionId - 当前点击的选项 ID
   * @returns 无返回值
   */
  const handleOptionToggle = (optionId: string) => {
    if (selectionMode === 'single') {
      const nextSelectedOptionIds = selectedOptionIds[0] === optionId ? [] : [optionId]
      /** 当前单选点击后将要提交的选项列表。 */
      const nextSelectedOptions = options.filter(option => nextSelectedOptionIds.includes(option.id))

      setShowInputArea(false)
      setCustomInput('')
      setSelectedOptionIds(nextSelectedOptionIds)
      submitSelectionCard(nextSelectedOptions)
      return
    }

    setSelectedOptionIds(current => {
      return current.includes(optionId) ? current.filter(id => id !== optionId) : [...current, optionId]
    })
  }

  /**
   * 切换自定义输入区的显隐状态。
   *
   * @returns 无返回值
   */
  const handleCustomToggle = () => {
    if (selectionMode === 'single') {
      setSelectedOptionIds([])
      setShowInputArea(prev => !prev)
    } else {
      setShowInputArea(prev => !prev)
    }
  }

  /**
   * 提交当前选择卡。
   *
   * @returns 无返回值
   */
  const handleSubmit = () => {
    const didSubmit = submitSelectionCard(selectedOptions, showInputArea ? customInput : '')

    if (!didSubmit) {
      return
    }

    if (showInputArea) {
      setCustomInput('')
      setShowInputArea(false)
    }
  }

  return (
    <AssistantCardShell className={styles.assistantSelectionCard} visible={visible} onExitComplete={onExitComplete}>
      <div className={styles.cardHeader}>
        <div className={styles.headerIconBox}>
          <ListChecks size={14} className={styles.headerIcon} />
        </div>
        <div className={styles.headerMain}>
          <span className={styles.headerTitle}>{title || DEFAULT_SELECTION_CARD_TITLE}</span>
          <span className={styles.headerHint}>{selectionCardHeaderHint}</span>
        </div>
      </div>

      <div className={styles.content}>
        {description && (
          <div className={styles.descriptionPanel}>
            <div className={styles.descriptionLabel}>选择说明</div>
            <p className={styles.description}>{description}</p>
          </div>
        )}

        <div className={`${styles.optionGrid} ${gridClass}`}>
          {options.map(option => {
            const selected = selectedOptionIds.includes(option.id)
            return (
              <button
                key={option.id}
                type='button'
                disabled={!canInteract}
                className={`${styles.optionItem} ${selected ? styles.active : ''}`}
                onClick={() => handleOptionToggle(option.id)}
              >
                <span className={styles.label}>{option.label}</span>
              </button>
            )
          })}

          {allowCustomInput && (
            <button
              type='button'
              disabled={!canInteract}
              className={`${styles.optionItem} ${styles.customBtnItem} ${showInputArea ? styles.active : ''}`}
              onClick={handleCustomToggle}
            >
              <Edit2 size={12} className={styles.customIcon} />
              <span className={styles.label}>输入</span>
            </button>
          )}
        </div>

        {showInputArea && (
          <div className={styles.inputPanel}>
            <div className={styles.inputTitle}>手动补充</div>
            <div className={styles.inputWrapper}>
              <input
                autoFocus
                className={styles.input}
                disabled={!canInteract}
                value={customInput}
                placeholder={customInputPlaceholder || '请输入补充信息...'}
                onChange={e => setCustomInput(e.target.value)}
                onKeyDown={e => e.key === 'Enter' && handleSubmit()}
              />
            </div>
          </div>
        )}

        <button type='button' className={styles.submitBtn} disabled={!canSubmit} onClick={handleSubmit}>
          {submitText}
        </button>
      </div>
    </AssistantCardShell>
  )
}

export default SelectionCard
