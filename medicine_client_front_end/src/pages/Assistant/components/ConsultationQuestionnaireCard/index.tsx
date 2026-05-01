import React, { useMemo, useState, useEffect, useRef } from 'react'
import { Pencil } from 'lucide-react'
import { AssistantCardShell } from '../MessagePrimitives'
import type {
  ConsultationQuestionnaireAnswerPayload,
  ConsultationQuestionnaireCardSubmitPayload,
  ConsultationQuestionnaireQuestionContent
} from '../../modules/messages/chatTypes'
import styles from './index.module.less'

/** 问卷卡片标题文案。 */
const QUESTIONNAIRE_CARD_TITLE = '请继续补充症状'
/** 问卷卡片头部说明文案。 */
const QUESTIONNAIRE_CARD_DESCRIPTION = '按实际情况选择，也可以直接手动补充。'
/** 问卷卡片“下一步”按钮文案。 */
const QUESTIONNAIRE_NEXT_BUTTON_TEXT = '下一步'
/** 问卷卡片上一题按钮文案。 */
const QUESTIONNAIRE_PREV_BUTTON_TEXT = '上一步'
/** 问卷卡片最后一步标题文案。 */
const QUESTIONNAIRE_FINAL_QUESTION_TEXT = '还有其他需要补充的吗？'
/** 问卷卡片最后一步完成按钮文案。 */
const QUESTIONNAIRE_COMPLETE_BUTTON_TEXT = '完成并提交'
/** 问卷卡片最后一步补充按钮文案。 */
const QUESTIONNAIRE_SUPPLEMENT_BUTTON_TEXT = '继续补充'
/** 问卷卡片最后一步取消补充按钮文案。 */
const QUESTIONNAIRE_CANCEL_SUPPLEMENT_BUTTON_TEXT = '取消'
/** 问卷卡片最后一步确认提交按钮文案。 */
const QUESTIONNAIRE_CONFIRM_SUBMIT_BUTTON_TEXT = '确认提交'

/** ConsultationQuestionnaireCard 组件属性。 */
interface ConsultationQuestionnaireCardProps {
  /** 当前卡片在本地消息列表中的消息 ID。 */
  localMessageId: string
  /** 当前卡片所属的逻辑消息 ID。 */
  messageId: string
  /** 当前卡片唯一标识。 */
  cardUuid: string
  /** 问卷问题列表。 */
  questions: ConsultationQuestionnaireQuestionContent[]
  /** 提交回调。 */
  onSubmit?: (payload: ConsultationQuestionnaireCardSubmitPayload) => void
  /** 卡片是否可见，设为 false 后将先播放退出动画再回调。 */
  visible?: boolean
  /** 退出动画播放完成后的回调。 */
  onExitComplete?: () => void
}

/**
 * 诊断问卷卡组件。
 * 以“多题串行、多选、手动下一题、最后统一提交”的方式组织诊断追问。
 *
 * @param props - 组件属性
 * @returns 诊断问卷卡节点
 */
const ConsultationQuestionnaireCard: React.FC<ConsultationQuestionnaireCardProps> = ({
  localMessageId,
  messageId,
  cardUuid,
  questions,
  onSubmit,
  visible,
  onExitComplete
}) => {
  /** 当前展示的问题下标。 */
  const [currentQuestionIndex, setCurrentQuestionIndex] = useState(0)
  /** 每一题当前已选择的选项 ID 列表。 */
  const [selectedOptionIdsByQuestion, setSelectedOptionIdsByQuestion] = useState<Record<number, string[]>>({})
  /** 每一题的用户自填项内容。 */
  const [customInputsByQuestion, setCustomInputsByQuestion] = useState<Record<number, string>>({})
  /** 每一题是否展开了自填项输入框。 */
  const [showCustomInputsByQuestion, setShowCustomInputsByQuestion] = useState<Record<number, boolean>>({})
  /** 最后的补充信息内容。 */
  const [additionalInfo, setAdditionalInfo] = useState('')
  /** 最后的补充是否处于展开状态。 */
  const [isSupplementing, setIsSupplementing] = useState(false)

  /** 当前是否具备交互所需的关键标识。 */
  const canInteract = Boolean(localMessageId && messageId && cardUuid)
  /** 当前问题总数。 */
  const questionCount = questions.length
  /** 包含最后一步的总步骤数。 */
  const totalSteps = questionCount + 1
  /** 当前是否位于附加信息（最后一步）。 */
  const isFinalStep = currentQuestionIndex === questionCount

  /** 当前问题对象（如果是最后一步则为 null）。 */
  const currentQuestion = isFinalStep ? null : questions[currentQuestionIndex]
  /** 当前问题已选择的选项 ID 列表。 */
  const currentSelectedOptionIds = selectedOptionIdsByQuestion[currentQuestionIndex] || []
  /** 当前问题已填写的自定义输入。 */
  const currentCustomInput = customInputsByQuestion[currentQuestionIndex] || ''
  /** 当前问题已选择的完整选项对象列表。 */
  const currentSelectedOptions = useMemo(() => {
    return currentQuestion?.options.filter(option => currentSelectedOptionIds.includes(option.id)) || []
  }, [currentQuestion, currentSelectedOptionIds])
  /** 当前步骤提示文案。 */
  const currentStepLabel = isFinalStep ? '最后确认' : `第${currentQuestionIndex + 1}问`

  /** 当前是否允许进入下一步或提交。 */
  const canAdvance = isFinalStep
    ? true // 最后一步“补充信息”可留空直接提交
    : canInteract && (currentSelectedOptions.length > 0 || currentCustomInput.trim().length > 0)

  /** 滚动锚点，用于内容较长时滚动到顶部。 */
  const cardTopRef = useRef<HTMLDivElement>(null)
  useEffect(() => {
    if (cardTopRef.current) {
      cardTopRef.current.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
    }
  }, [currentQuestionIndex])

  /** 当前选项网格布局类名。 */
  const gridClass = useMemo(() => {
    if (!currentQuestion) return ''
    const optionLabels = currentQuestion.options.map(option => option.label) || []
    let maxVisualLength = 0

    optionLabels.forEach(label => {
      let visualLength = 0
      for (const char of label) {
        visualLength += char.charCodeAt(0) > 255 ? 1 : 0.5
      }
      if (visualLength > maxVisualLength) {
        maxVisualLength = visualLength
      }
    })

    if (maxVisualLength > 9) {
      return styles.oneCol
    }
    if (maxVisualLength > 5.5) {
      return styles.twoCols
    }
    return styles.threeCols
  }, [currentQuestion])

  /**
   * 切换当前问题某个选项的选中状态。
   *
   * @param optionId - 当前点击的选项 ID
   * @returns 无返回值
   */
  const handleOptionToggle = (optionId: string) => {
    setSelectedOptionIdsByQuestion(currentState => {
      const currentQuestionOptionIds = currentState[currentQuestionIndex] || []
      const nextQuestionOptionIds = currentQuestionOptionIds.includes(optionId)
        ? currentQuestionOptionIds.filter(id => id !== optionId)
        : [...currentQuestionOptionIds, optionId]

      return {
        ...currentState,
        [currentQuestionIndex]: nextQuestionOptionIds
      }
    })
  }

  /**
   * 构建当前问卷卡最终提交载荷。
   *
   * @returns 完整提交载荷；当前题目或历史题目缺少选择时返回 null
   */
  const buildSubmitPayload = (finalInfo: string = ''): ConsultationQuestionnaireCardSubmitPayload | null => {
    const answers: ConsultationQuestionnaireAnswerPayload[] = questions.map((questionItem, index) => {
      const selectedOptionIds = selectedOptionIdsByQuestion[index] || []
      const selectedOptions = questionItem.options.filter(option => selectedOptionIds.includes(option.id))
      const customInput = customInputsByQuestion[index]?.trim()

      if (customInput) {
        selectedOptions.push({ id: `custom-${index}`, label: customInput, value: customInput })
      }

      return {
        question: questionItem.question,
        selectedOptions
      }
    })

    if (answers.some(answer => answer.selectedOptions.length === 0)) {
      return null
    }

    if (finalInfo.trim()) {
      answers.push({
        question: '补充信息',
        selectedOptions: [{ id: 'additional-info', label: finalInfo.trim(), value: finalInfo.trim() }]
      })
    }

    return {
      localMessageId,
      messageId,
      cardUuid,
      answers
    }
  }

  /**
   * 返回上一题。
   */
  const handleBack = () => {
    if (currentQuestionIndex > 0) {
      setCurrentQuestionIndex(currentQuestionIndex - 1)
      setIsSupplementing(false)
    }
  }

  /**
   * 进入下一题或提交整张问卷卡。
   *
   * @returns 无返回值
   */
  const handleAdvance = () => {
    if (!canAdvance) {
      return
    }

    if (!isFinalStep) {
      setCurrentQuestionIndex(currentQuestionIndex + 1)
      return
    }

    const submitPayload = buildSubmitPayload('')
    if (!submitPayload) {
      return
    }

    onSubmit?.(submitPayload)
  }

  if (questionCount === 0) {
    return null
  }

  return (
    <AssistantCardShell
      className={styles.consultationQuestionnaireCard}
      visible={visible}
      onExitComplete={onExitComplete}
    >
      <div className={styles.content}>
        <div ref={cardTopRef} className={styles.header}>
          <div className={styles.headerMain}>
            <div className={styles.title}>{QUESTIONNAIRE_CARD_TITLE}</div>
            <div className={styles.description}>{QUESTIONNAIRE_CARD_DESCRIPTION}</div>
          </div>
          <div className={styles.progressBlock}>
            <div className={styles.progressLabel}>{currentStepLabel}</div>
            <div className={styles.progress}>
              {currentQuestionIndex + 1}/{totalSteps}
            </div>
          </div>
        </div>

        {!isFinalStep && currentQuestion && (
          <>
            <div className={styles.questionPanel}>
              <div className={styles.questionTag}>当前问题</div>
              <p className={styles.question}>{currentQuestion.question}</p>
            </div>

            <div className={`${styles.optionGrid} ${gridClass}`}>
              {currentQuestion.options.map(option => {
                const selected = currentSelectedOptionIds.includes(option.id)

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
            </div>

            <div className={styles.customInputToggleWrapper}>
              {!showCustomInputsByQuestion[currentQuestionIndex] ? (
                <button
                  type='button'
                  className={styles.optionItemInput}
                  disabled={!canInteract}
                  onClick={() =>
                    setShowCustomInputsByQuestion(prev => ({
                      ...prev,
                      [currentQuestionIndex]: true
                    }))
                  }
                >
                  <span className={styles.labelWrapper}>
                    <Pencil size={14} className={styles.pencilIcon} /> 输入
                  </span>
                </button>
              ) : (
                <div className={styles.customInputPanel}>
                  <div className={styles.customInputTitle}>手动补充</div>
                  <textarea
                    className={styles.customInputArea}
                    placeholder='补充其他信息'
                    value={currentCustomInput}
                    disabled={!canInteract}
                    onChange={e =>
                      setCustomInputsByQuestion(prev => ({ ...prev, [currentQuestionIndex]: e.target.value }))
                    }
                    autoFocus
                  />
                </div>
              )}
            </div>

            <div className={styles.actionRow}>
              {currentQuestionIndex > 0 && (
                <button type='button' className={styles.secondaryBtn} disabled={!canInteract} onClick={handleBack}>
                  {QUESTIONNAIRE_PREV_BUTTON_TEXT}
                </button>
              )}
              <button type='button' className={styles.submitBtn} disabled={!canAdvance} onClick={handleAdvance}>
                {QUESTIONNAIRE_NEXT_BUTTON_TEXT}
              </button>
            </div>
          </>
        )}

        {isFinalStep && (
          <>
            <div className={styles.questionPanel}>
              <div className={styles.questionTag}>最后确认</div>
              <p className={styles.question}>{QUESTIONNAIRE_FINAL_QUESTION_TEXT}</p>
            </div>

            {isSupplementing ? (
              <>
                <div className={styles.customInputPanel}>
                  <div className={styles.customInputTitle}>补充说明</div>
                  <textarea
                    className={styles.customInputArea}
                    placeholder='请输入补充信息...'
                    value={additionalInfo}
                    disabled={!canInteract}
                    onChange={e => setAdditionalInfo(e.target.value)}
                    autoFocus
                  />
                </div>
                <div className={styles.actionRow}>
                  <button
                    type='button'
                    className={styles.secondaryBtn}
                    disabled={!canInteract}
                    onClick={() => {
                      setIsSupplementing(false)
                      setAdditionalInfo('')
                    }}
                  >
                    {QUESTIONNAIRE_CANCEL_SUPPLEMENT_BUTTON_TEXT}
                  </button>
                  <button
                    type='button'
                    className={styles.submitBtn}
                    disabled={!canInteract}
                    onClick={() => {
                      const payload = buildSubmitPayload(additionalInfo)
                      if (payload) onSubmit?.(payload)
                    }}
                  >
                    {QUESTIONNAIRE_CONFIRM_SUBMIT_BUTTON_TEXT}
                  </button>
                </div>
              </>
            ) : (
              <div className={styles.actionRow}>
                {currentQuestionIndex > 0 && (
                  <button type='button' className={styles.secondaryBtn} disabled={!canInteract} onClick={handleBack}>
                    {QUESTIONNAIRE_PREV_BUTTON_TEXT}
                  </button>
                )}
                <button
                  type='button'
                  className={styles.secondaryBtn}
                  disabled={!canInteract}
                  onClick={() => setIsSupplementing(true)}
                >
                  {QUESTIONNAIRE_SUPPLEMENT_BUTTON_TEXT}
                </button>
                <button
                  type='button'
                  className={styles.submitBtn}
                  disabled={!canInteract}
                  onClick={() => {
                    const payload = buildSubmitPayload('')
                    if (payload) onSubmit?.(payload)
                  }}
                >
                  {QUESTIONNAIRE_COMPLETE_BUTTON_TEXT}
                </button>
              </div>
            )}
          </>
        )}
      </div>
    </AssistantCardShell>
  )
}

export default ConsultationQuestionnaireCard
