import { Button, Dialog, InfiniteLoading, Input } from '@nutui/nutui-react'
import { MessageSquarePlus } from 'lucide-react'
import { createPortal } from 'react-dom'
import { useConversationListController } from '../../modules/conversation/useConversationListController'
import styles from './index.module.less'

/**
 * 历史会话列表组件。
 * 组件只渲染 UI，所有列表和菜单行为都由 controller 管理。
 */
const ConversationList = () => {
  const {
    conversations,
    activeConversationUuid,
    conversationListError,
    conversationListLoading,
    conversationHasMore,
    historyLoading,
    isReplying,
    activeActionId,
    renameDialogOpen,
    renameValue,
    setRenameValue,
    closeActionMenu,
    closeRenameDialog,
    handleNewConversation,
    handleSelectConversation,
    handleLoadMore,
    handleRetryLoad,
    handlePressStart,
    handlePressEnd,
    handlePressMove,
    handleRenameClick,
    handleDeleteClick,
    confirmRename
  } = useConversationListController()
  /** 当前是否需要暂时锁定会话列表交互。 */
  const isConversationInteractionLocked = isReplying || historyLoading

  /**
   * 重命名弹层需要挂到 document.body。
   * 否则它会跟随抽屉容器一起被裁剪，导致弹层显示不完整。
   */
  const renameDialog = (
    <Dialog
      visible={renameDialogOpen}
      title='重命名对话'
      footer={
        <div className={styles.renameDialogFooter}>
          <Button type='default' size='large' className={styles.renameDialogCancel} onClick={closeRenameDialog}>
            取消
          </Button>
          <Button
            type='primary'
            size='large'
            className={styles.renameDialogConfirm}
            onClick={() => void confirmRename()}
          >
            确认
          </Button>
        </div>
      }
      onCancel={closeRenameDialog}
    >
      <Input
        className={styles.renameInput}
        value={renameValue}
        onChange={val => setRenameValue(val)}
        placeholder='请输入新标题'
        autoFocus
        clearable
      />
    </Dialog>
  )

  return (
    <div className={styles.container}>
      <div
        className={styles.scrollArea}
        id='scroll-container'
        onClick={() => {
          if (activeActionId) {
            closeActionMenu()
          }
        }}
      >
        <InfiniteLoading hasMore={conversationHasMore} onLoadMore={handleLoadMore} target='scroll-container'>
          {conversationListLoading && conversations.length > 0 && <div className={styles.centerState}>加载中...</div>}

          {conversations.length === 0 && !conversationListLoading ? (
            <div className={styles.centerState}>
              <p className={styles.emptyTitle}>{conversationListError ? '历史会话暂时不可用' : '还没有历史对话'}</p>
              <p className={styles.emptyText}>
                {conversationListError || '发起第一条咨询后，这里会自动出现会话记录。'}
              </p>
              {conversationListError ? (
                <Button
                  type='primary'
                  size='small'
                  className={styles.retryButton}
                  onClick={() => void handleRetryLoad()}
                >
                  重新加载
                </Button>
              ) : null}
            </div>
          ) : (
            <div className={styles.list}>
              {conversations.map(conversation => {
                const isActive = conversation.conversationUuid === activeConversationUuid
                const showMenu = activeActionId === conversation.conversationUuid

                return (
                  <div
                    key={conversation.conversationUuid}
                    className={styles.itemWrapper}
                    onTouchStart={event => handlePressStart(event, conversation.conversationUuid)}
                    onTouchEnd={handlePressEnd}
                    onTouchMove={handlePressMove}
                    onMouseDown={event => handlePressStart(event, conversation.conversationUuid)}
                    onMouseUp={handlePressEnd}
                    onMouseLeave={handlePressEnd}
                  >
                    <button
                      type='button'
                      className={`${styles.item} ${isActive ? styles.itemActive : ''}`}
                      onClick={() => handleSelectConversation(conversation.conversationUuid)}
                      disabled={isConversationInteractionLocked}
                    >
                      <span className={styles.itemTitle}>{conversation.title}</span>
                    </button>
                    {showMenu && (
                      <div className={styles.contextMenu}>
                        <button
                          type='button'
                          onClick={e => handleRenameClick(e, conversation.conversationUuid, conversation.title)}
                        >
                          重命名
                        </button>
                        <button
                          type='button'
                          onClick={e => handleDeleteClick(e, conversation.conversationUuid)}
                          className={styles.dangerAction}
                        >
                          删除
                        </button>
                      </div>
                    )}
                  </div>
                )
              })}
            </div>
          )}
        </InfiniteLoading>
      </div>

      <div className={styles.bottomActions}>
        <button
          type='button'
          className={styles.newButton}
          onClick={handleNewConversation}
          disabled={isConversationInteractionLocked}
        >
          <MessageSquarePlus size={18} className={styles.newIcon} />
          新建会话
        </button>
      </div>
      {typeof document !== 'undefined' ? createPortal(renameDialog, document.body) : renameDialog}
    </div>
  )
}

export default ConversationList
