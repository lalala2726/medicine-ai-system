import React, { useState } from 'react'
import styles from './index.module.less'
import AssistantComposer from './components/AssistantComposer'
import AssistantMessageList from './components/AssistantMessageList'
import OrderSelector from './components/OrderSelector'
import AfterSaleSelector from './components/AfterSaleSelector'
import PatientSelector from './components/PatientSelector'
import { AssistantTtsProvider } from './modules/tts/AssistantTtsProvider'
import { Menu, MessageSquarePlus, ChevronRight, Check } from 'lucide-react'
import { ASSISTANT_HISTORY_LOADING_TEXT, ASSISTANT_PAGE_TITLE } from './modules/page/assistantPage.constants'
import { useAssistantPageController } from './modules/page/useAssistantPageController'

/**
 * AI 智能助手聊天页面组件
 * 页面本身只负责组装头部、聊天容器和各类弹层。
 */
const AIConsult: React.FC = () => {
  const {
    historyLoading,
    historyError,
    messageListRef,
    messages,
    messageCallbacks,
    composerProps,
    orderSelectorProps,
    afterSaleSelectorProps,
    patientSelectorProps,
    handleMenuButtonClick,
    handleNewSessionClick
  } = useAssistantPageController()

  const [modelSelectorVisible, setModelSelectorVisible] = useState(false)

  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <div className={styles.leftAction}>
          <button
            type='button'
            className={styles.menuButton}
            onClick={handleMenuButtonClick}
            aria-label='打开历史对话抽屉'
          >
            <Menu size={20} />
          </button>
          <div className={styles.titleInfo}>
            <div className={styles.title}>{ASSISTANT_PAGE_TITLE}</div>
            {composerProps.showDeepThinking ? (
              <div className={styles.modelSelector} onClick={() => setModelSelectorVisible(!modelSelectorVisible)}>
                {composerProps.deepThinking ? '深度思考' : '快速思考'}{' '}
                <ChevronRight
                  size={12}
                  className={`${styles.chevron} ${modelSelectorVisible ? styles.chevronUp : ''}`}
                />
              </div>
            ) : null}
          </div>

          {modelSelectorVisible && composerProps.showDeepThinking ? (
            <>
              <div className={styles.modelSelectorOverlay} onClick={() => setModelSelectorVisible(false)} />
              <div className={styles.modelSelectorDropdown}>
                <div
                  className={styles.modelOption}
                  onClick={() => {
                    composerProps.onDeepThinkingChange?.(false)
                    setModelSelectorVisible(false)
                  }}
                >
                  <div className={styles.modelOptionCheck}>
                    {!composerProps.deepThinking && <Check size={16} color='#111827' />}
                  </div>
                  <div className={styles.modelOptionText}>
                    <div className={styles.modelOptionTitle}>快速思考</div>
                    <div className={styles.modelOptionDesc}>立即回答</div>
                  </div>
                </div>
                <div className={styles.modelOptionDivider} />
                <div
                  className={styles.modelOption}
                  onClick={() => {
                    composerProps.onDeepThinkingChange?.(true)
                    setModelSelectorVisible(false)
                  }}
                >
                  <div className={styles.modelOptionCheck}>
                    {composerProps.deepThinking && <Check size={16} color='#111827' />}
                  </div>
                  <div className={styles.modelOptionText}>
                    <div className={styles.modelOptionTitle}>深度思考</div>
                    <div className={styles.modelOptionDesc}>思考更充分，回答更优质</div>
                  </div>
                </div>
              </div>
            </>
          ) : null}
        </div>
        <div className={styles.rightActions}>
          <button type='button' className={styles.iconButton} onClick={handleNewSessionClick} aria-label='新会话'>
            <MessageSquarePlus size={20} />
          </button>
        </div>
      </header>
      {historyLoading ? <div className={styles.historyStatus}>{ASSISTANT_HISTORY_LOADING_TEXT}</div> : null}
      {!historyLoading && historyError ? <div className={styles.historyError}>{historyError}</div> : null}
      <AssistantTtsProvider messages={messages}>
        <div className={styles.chatContainer}>
          <AssistantMessageList messages={messages} containerRef={messageListRef} callbacks={messageCallbacks} />
          <AssistantComposer {...composerProps} />
        </div>
      </AssistantTtsProvider>
      <OrderSelector {...orderSelectorProps} />
      <AfterSaleSelector {...afterSaleSelectorProps} />
      <PatientSelector {...patientSelectorProps} />
    </div>
  )
}

export default AIConsult
