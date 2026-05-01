import { Popup } from '@nutui/nutui-react'
import { ArrowUp, AudioLines, Camera, Keyboard, Plus, Square, X } from 'lucide-react'
import {
  useCallback,
  useEffect,
  useRef,
  useState,
  type ChangeEvent,
  type FocusEvent,
  type KeyboardEvent,
  type PointerEvent as ReactPointerEvent
} from 'react'
import { uploadFile } from '@/api/common'
import { showErrorNotify, showWarningNotify } from '@/utils/notify'
import {
  ASSISTANT_INPUT_PLACEHOLDER,
  ASSISTANT_STT_EMPTY_TEXT,
  ASSISTANT_STT_PENDING_TEXT
} from '../../modules/page/assistantPage.constants'
import { AssistantSttRecorder } from '../../modules/stt/AssistantSttRecorder'
import {
  ASSISTANT_INPUT_MODES,
  ASSISTANT_TOOLBAR_TYPES,
  type AssistantInputMode,
  type AssistantToolbarItem
} from '../../modules/shared/assistantUiConfig'
import type { AssistantConsultProductDraft } from '../../modules/shared/consultProductDraft'
import AssistantVoiceRecordingOverlay from '../AssistantVoiceRecordingOverlay'
import styles from './index.module.less'

/** 输入框自动增长允许的最大高度。 */
const ASSISTANT_INPUT_MAX_HEIGHT = 88

/** 进入取消录音状态所需的最小上滑距离。 */
const VOICE_CANCEL_THRESHOLD = 72

/** 录音拖拽反馈允许展示的最大位移。 */
const VOICE_DRAG_DISTANCE_LIMIT = 128

/** 聊天图片最多上传 5 张。 */
const ASSISTANT_IMAGE_MAX_COUNT = 5
/** 图片数量达到上限时的提示文案。 */
const ASSISTANT_IMAGE_MAX_COUNT_WARNING = `最多支持上传 ${ASSISTANT_IMAGE_MAX_COUNT} 张图片`
/** 图片上传失败时的统一提示文案。 */
const ASSISTANT_IMAGE_UPLOAD_FAILED_TEXT = '图片上传失败，请重试'
/** 图片上传进行中时的提示文案。 */
const ASSISTANT_IMAGE_UPLOAD_PENDING_TEXT = '图片上传中，请稍候'
/** 商品咨询草稿挂载时阻止上传图片的提示文案。 */
const ASSISTANT_PRODUCT_DRAFT_IMAGE_DISABLED_TEXT = '商品咨询模式不支持上传图片，请先移除商品卡片'

/** 语音录音手势的收尾类型。 */
type VoiceGestureFinishReason = 'submit' | 'cancel' | 'abort'

/** 语音录音手势状态。 */
interface VoiceGestureState {
  /** 当前是否处于录音态。 */
  isRecording: boolean
  /** 当前向上拖拽的距离。 */
  dragDistance: number
  /** 当前是否已进入取消态。 */
  isCancelArmed: boolean
}

/** Assistant 自研输入框组件属性。 */
export interface AssistantComposerProps {
  /** 当前输入框文本。 */
  value: string
  /** 当前输入模式。 */
  inputMode: AssistantInputMode
  /** 当前是否存在运行中的回复。 */
  isReplying: boolean
  /** 当前是否正在提交停止请求。 */
  isStopping: boolean
  /** 输入框占位文案。 */
  placeholder?: string
  /** 工具栏配置项。 */
  toolbarItems: AssistantToolbarItem[]
  /** 当前已上传图片 URL 列表。 */
  imageUrls: string[]
  /** 当前是否允许上传图片。 */
  imageUploadEnabled?: boolean
  /** 图片上传不可用时的提示文案。 */
  imageUploadDisabledText?: string
  /** 当前是否展示深度思考开关。 */
  showDeepThinking?: boolean
  /** 深度思考开关当前状态。 */
  deepThinking?: boolean
  /** 输入框内容更新回调。 */
  onValueChange: (value: string) => void
  /** 输入区图片 URL 列表更新回调。 */
  onImageUrlsChange: (imageUrls: string[] | ((_currentImageUrls: string[]) => string[])) => void
  /** 点击发送回调。 */
  onSend: () => void | Promise<void>
  /** 点击停止回调。 */
  onStop: () => void | Promise<void>
  /** 切换输入模式回调。 */
  onToggleInputMode: (inputMode: AssistantInputMode) => void
  /** 启动语音识别前的状态校验回调。 */
  onBeforeVoiceStart: () => boolean
  /** 语音识别完成后的最终文本提交回调。 */
  onVoiceTranscriptSubmit: (text: string) => void | Promise<void>
  /** 点击工具栏项回调。 */
  onToolbarItemClick: (item: AssistantToolbarItem) => void
  /** 深度思考状态切换回调。 */
  onDeepThinkingChange?: (enabled: boolean) => void
  /** 当前挂载在输入区上的商品咨询草稿。 */
  attachedProductDraft?: AssistantConsultProductDraft | null
  /** 移除当前商品咨询草稿。 */
  onRemoveAttachedProductDraft: () => void
  /** 输入框聚焦回调。 */
  onFocus?: (event: FocusEvent<HTMLTextAreaElement>) => void
}

/**
 * 创建语音录音手势的初始状态。
 *
 * @returns 初始语音手势状态
 */
const createInitialVoiceGestureState = (): VoiceGestureState => {
  return {
    isRecording: false,
    dragDistance: 0,
    isCancelArmed: false
  }
}

/**
 * 根据起始位置和当前指针位置，计算向上拖拽的有效距离。
 *
 * @param startY - 手势开始时的纵向坐标
 * @param currentY - 手势当前的纵向坐标
 * @returns 限制后的有效拖拽距离
 */
const resolveVoiceDragDistance = (startY: number, currentY: number) => {
  return Math.min(Math.max(startY - currentY, 0), VOICE_DRAG_DISTANCE_LIMIT)
}

/**
 * Assistant 页面自研输入区。
 * 负责文本输入、语音/键盘切换、发送按钮展示与更多工具抽屉。
 *
 * @param props - 组件属性
 * @returns 输入区节点
 */
const AssistantComposer = ({
  value,
  inputMode,
  isReplying,
  isStopping,
  placeholder = ASSISTANT_INPUT_PLACEHOLDER,
  toolbarItems,
  imageUrls,
  imageUploadEnabled = true,
  imageUploadDisabledText = '此模型不支持图片理解',
  onValueChange,
  onImageUrlsChange,
  onSend,
  onStop,
  onToggleInputMode,
  onBeforeVoiceStart,
  onVoiceTranscriptSubmit,
  onToolbarItemClick,
  attachedProductDraft,
  onRemoveAttachedProductDraft,
  onFocus
}: AssistantComposerProps) => {
  /** 当前工具栏是否处于展开状态。 */
  const [toolbarOpen, setToolbarOpen] = useState(false)
  /** 当前语音录音态的展示状态。 */
  const [voiceGestureState, setVoiceGestureState] = useState<VoiceGestureState>(createInitialVoiceGestureState)
  /** 当前语音识别浮层展示的波形高度数组。 */
  const [voiceWaveHeights, setVoiceWaveHeights] = useState<number[]>([])
  /** 当前是否仍处于语音链路准备阶段。 */
  const [voicePreparing, setVoicePreparing] = useState(false)
  /** 当前是否正在等待 STT 最终完成。 */
  const [voiceRecognitionPending, setVoiceRecognitionPending] = useState(false)
  /** 当前是否存在图片上传任务。 */
  const [imageUploading, setImageUploading] = useState(false)
  /** 文本输入框节点引用，用于聚焦和高度自适应。 */
  const textareaRef = useRef<HTMLTextAreaElement>(null)
  /** 隐藏图片文件选择输入框。 */
  const imageInputRef = useRef<HTMLInputElement>(null)
  /** 记录上一次输入模式，用于切回文本时自动聚焦。 */
  const previousInputModeRef = useRef<AssistantInputMode>(inputMode)
  /** 当前语音手势绑定的指针编号。 */
  const voicePointerIdRef = useRef<number | null>(null)
  /** 当前语音手势开始时的纵向坐标。 */
  const voiceStartYRef = useRef(0)
  /** 当前是否已进入松手取消状态。 */
  const voiceCancelArmedRef = useRef(false)
  /** 当前 STT 录音器实例。 */
  const sttRecorderRef = useRef<AssistantSttRecorder | null>(null)
  /** 当前是否仍在启动录音器。 */
  const voiceStartPendingRef = useRef(false)
  /** 是否存在可发送文本。 */
  const hasText = value.trim().length > 0
  /** 当前是否已经选择图片。 */
  const hasImages = imageUrls.length > 0
  /** 当前是否存在挂载中的商品咨询草稿。 */
  const hasAttachedProductDraft = Boolean(attachedProductDraft)
  /** 当前是否需要展示右侧主操作按钮。 */
  const shouldShowPrimaryAction = hasText || hasAttachedProductDraft || hasImages || isReplying
  /** 当前是否需要隐藏语音/文字切换按钮（仅图片不隐藏）。 */
  const shouldHideInputModeToggle = hasText || hasAttachedProductDraft || isReplying
  /** 当前图片上传功能是否处于禁用状态。 */
  const imageUploadBlocked = !imageUploadEnabled

  /**
   * 将文本框高度同步到内容高度，保持单行外观并兼容少量换行输入。
   *
   * @returns 无返回值
   */
  const syncTextareaHeight = useCallback(() => {
    const textareaElement = textareaRef.current

    if (!textareaElement) {
      return
    }

    textareaElement.style.height = '0px'
    textareaElement.style.height = `${Math.min(textareaElement.scrollHeight, ASSISTANT_INPUT_MAX_HEIGHT)}px`
  }, [])

  useEffect(() => {
    syncTextareaHeight()
  }, [syncTextareaHeight, value, inputMode])

  useEffect(() => {
    const previousInputMode = previousInputModeRef.current

    if (previousInputMode === ASSISTANT_INPUT_MODES.VOICE && inputMode === ASSISTANT_INPUT_MODES.TEXT) {
      window.requestAnimationFrame(() => {
        textareaRef.current?.focus()
      })
    }

    previousInputModeRef.current = inputMode
  }, [inputMode])

  useEffect(() => {
    if (
      inputMode !== ASSISTANT_INPUT_MODES.VOICE &&
      (voicePointerIdRef.current !== null || voiceRecognitionPending || sttRecorderRef.current)
    ) {
      sttRecorderRef.current?.cancel()
      sttRecorderRef.current = null
      voiceStartPendingRef.current = false
      setVoicePreparing(false)
      setVoiceRecognitionPending(false)
      setVoiceWaveHeights([])
      voicePointerIdRef.current = null
      voiceStartYRef.current = 0
      voiceCancelArmedRef.current = false
      setVoiceGestureState(createInitialVoiceGestureState())
    }
  }, [inputMode, voiceRecognitionPending])

  useEffect(() => {
    /**
     * 组件卸载时，兜底清理可能遗留的录音器资源。
     *
     * @returns 无返回值
     */
    return () => {
      sttRecorderRef.current?.cancel()
      sttRecorderRef.current = null
      voiceStartPendingRef.current = false
    }
  }, [])

  /**
   * 重置当前语音录音手势状态。
   *
   * @returns 无返回值
   */
  const resetVoiceGesture = useCallback(() => {
    voicePointerIdRef.current = null
    voiceStartYRef.current = 0
    voiceCancelArmedRef.current = false
    setVoiceGestureState(createInitialVoiceGestureState())
  }, [])

  /**
   * 清理当前 STT 录音器引用与波形状态。
   *
   * @returns 无返回值
   */
  const clearVoiceRecorderState = useCallback(() => {
    sttRecorderRef.current = null
    voiceStartPendingRef.current = false
    setVoicePreparing(false)
    setVoiceWaveHeights([])
  }, [])

  /**
   * 安全释放当前语音按钮的指针捕获。
   *
   * @param buttonElement - 当前按住说话按钮节点
   * @param pointerId - 需要释放的指针编号
   * @returns 无返回值
   */
  const releaseVoicePointerCapture = useCallback((buttonElement: HTMLButtonElement, pointerId: number | null) => {
    if (pointerId === null || !buttonElement.hasPointerCapture(pointerId)) {
      return
    }

    buttonElement.releasePointerCapture(pointerId)
  }, [])

  /**
   * 结束一次语音录音手势。
   *
   * @param buttonElement - 当前按住说话按钮节点
   * @param finishReason - 当前语音手势的结束类型
   * @returns 无返回值
   */
  const finishVoiceGesture = useCallback(
    (buttonElement: HTMLButtonElement, finishReason: VoiceGestureFinishReason) => {
      /** 当前录音器实例快照。 */
      const currentRecorder = sttRecorderRef.current

      releaseVoicePointerCapture(buttonElement, voicePointerIdRef.current)
      resetVoiceGesture()

      if (!currentRecorder) {
        setVoiceRecognitionPending(false)
        clearVoiceRecorderState()
        return
      }

      if (finishReason === 'submit') {
        setVoiceRecognitionPending(true)
        currentRecorder.finish()
        return
      }

      currentRecorder.cancel()
      clearVoiceRecorderState()
      setVoiceRecognitionPending(false)
    },
    [clearVoiceRecorderState, releaseVoicePointerCapture, resetVoiceGesture]
  )

  /**
   * 处理 STT 最终识别完成事件。
   *
   * @param finalText - 后端返回的最终识别文本
   * @returns 无返回值
   */
  const handleVoiceRecognitionCompleted = useCallback(
    (finalText: string) => {
      /** 规整后的最终识别文本。 */
      const normalizedText = finalText.trim()

      clearVoiceRecorderState()
      setVoiceRecognitionPending(false)

      if (!normalizedText) {
        showWarningNotify(ASSISTANT_STT_EMPTY_TEXT)
        return
      }

      onVoiceTranscriptSubmit(normalizedText)
    },
    [clearVoiceRecorderState, onVoiceTranscriptSubmit]
  )

  /**
   * 处理 STT 识别超时事件。
   *
   * @param message - 超时提示文案
   * @returns 无返回值
   */
  const handleVoiceRecognitionTimeout = useCallback(
    (message: string) => {
      clearVoiceRecorderState()
      setVoiceRecognitionPending(false)
      showWarningNotify(message)
    },
    [clearVoiceRecorderState]
  )

  /**
   * 处理 STT 识别异常事件。
   *
   * @param message - 异常提示文案
   * @returns 无返回值
   */
  const handleVoiceRecognitionError = useCallback(
    (message: string) => {
      clearVoiceRecorderState()
      setVoiceRecognitionPending(false)
      showWarningNotify(message)
    },
    [clearVoiceRecorderState]
  )

  /**
   * 处理 STT WebSocket 完成 started 确认后的状态切换。
   *
   * @returns 无返回值
   */
  const handleVoiceRecognitionStarted = useCallback(() => {
    voiceStartPendingRef.current = false
    setVoicePreparing(false)
  }, [])

  /**
   * 切换更多工具抽屉显隐状态。
   *
   * @returns 无返回值
   */
  const handleToolbarToggle = useCallback(() => {
    if (toolbarItems.length === 0) {
      return
    }

    setToolbarOpen(currentOpen => !currentOpen)
  }, [toolbarItems.length])

  /**
   * 处理输入框内容变更。
   *
   * @param nextValue - 最新输入文本
   * @returns 无返回值
   */
  const handleValueChange = useCallback(
    (nextValue: string) => {
      onValueChange(nextValue)
    },
    [onValueChange]
  )

  /**
   * 处理文本输入框键盘事件。
   * `Enter` 发送消息，`Shift + Enter` 保留换行。
   *
   * @param event - 文本框键盘事件
   * @returns 无返回值
   */
  const handleTextareaKeyDown = useCallback(
    (event: KeyboardEvent<HTMLTextAreaElement>) => {
      if (event.key !== 'Enter' || event.shiftKey || event.nativeEvent.isComposing) {
        return
      }

      event.preventDefault()
      onSend()
    },
    [onSend]
  )

  /**
   * 打开图片选择器。
   *
   * @param preferCamera - 是否优先打开相机
   * @returns 无返回值
   */
  const openImagePicker = useCallback(
    (preferCamera: boolean) => {
      if (imageUploadBlocked) {
        showWarningNotify(imageUploadDisabledText)
        return
      }

      if (imageUploading) {
        showWarningNotify(ASSISTANT_IMAGE_UPLOAD_PENDING_TEXT)
        return
      }

      if (hasAttachedProductDraft) {
        showWarningNotify(ASSISTANT_PRODUCT_DRAFT_IMAGE_DISABLED_TEXT)
        return
      }

      if (imageUrls.length >= ASSISTANT_IMAGE_MAX_COUNT) {
        showWarningNotify(ASSISTANT_IMAGE_MAX_COUNT_WARNING)
        return
      }

      const inputElement = imageInputRef.current
      if (!inputElement) {
        return
      }

      if (preferCamera) {
        inputElement.setAttribute('capture', 'environment')
      } else {
        inputElement.removeAttribute('capture')
      }
      inputElement.value = ''
      inputElement.click()
    },
    [hasAttachedProductDraft, imageUploadBlocked, imageUploadDisabledText, imageUploading, imageUrls.length]
  )

  /**
   * 处理文件选择并上传图片。
   *
   * @param event - 原生文件选择事件
   * @returns 无返回值
   */
  const handleImageInputChange = useCallback(
    (event: ChangeEvent<HTMLInputElement>) => {
      /** 当前选择文件列表。 */
      const selectedFiles = Array.from(event.target.files || [])
      event.target.value = ''

      if (imageUploadBlocked) {
        showWarningNotify(imageUploadDisabledText)
        return
      }

      if (selectedFiles.length === 0) {
        return
      }

      const remainingCount = ASSISTANT_IMAGE_MAX_COUNT - imageUrls.length
      if (remainingCount <= 0) {
        showWarningNotify(ASSISTANT_IMAGE_MAX_COUNT_WARNING)
        return
      }

      const filesToUpload = selectedFiles.slice(0, remainingCount)
      if (selectedFiles.length > remainingCount) {
        showWarningNotify(ASSISTANT_IMAGE_MAX_COUNT_WARNING)
      }

      void (async () => {
        /** 成功上传后的图片 URL 列表。 */
        const uploadedImageUrls: string[] = []

        setImageUploading(true)
        for (const file of filesToUpload) {
          try {
            const uploadResult = await uploadFile(file)
            const uploadedImageUrl = String(uploadResult.fileUrl || '').trim()
            if (!uploadedImageUrl) {
              showErrorNotify(ASSISTANT_IMAGE_UPLOAD_FAILED_TEXT)
              continue
            }
            uploadedImageUrls.push(uploadedImageUrl)
          } catch (error) {
            console.error('[AssistantComposer] image upload failed', error)
            showErrorNotify(ASSISTANT_IMAGE_UPLOAD_FAILED_TEXT)
          }
        }
        setImageUploading(false)

        if (uploadedImageUrls.length === 0) {
          return
        }

        onImageUrlsChange(currentImageUrls =>
          [...currentImageUrls, ...uploadedImageUrls].slice(0, ASSISTANT_IMAGE_MAX_COUNT)
        )
      })()
    },
    [imageUploadBlocked, imageUploadDisabledText, imageUrls.length, onImageUrlsChange]
  )

  /**
   * 移除已选中的图片。
   *
   * @param imageIndex - 待移除图片索引
   * @returns 无返回值
   */
  const handleRemoveImage = useCallback(
    (imageIndex: number) => {
      onImageUrlsChange(imageUrls.filter((_, index) => index !== imageIndex))
    },
    [imageUrls, onImageUrlsChange]
  )

  /**
   * 处理左侧相机按钮点击。
   *
   * @returns 无返回值
   */
  const handleCameraButtonClick = useCallback(() => {
    openImagePicker(true)
  }, [openImagePicker])

  /**
   * 处理工具栏项点击。
   * 先关闭抽屉，再延迟触发页面行为，避免遮罩和后续弹层叠加。
   *
   * @param item - 当前点击的工具栏项
   * @returns 无返回值
   */
  const handleToolbarItemSelection = useCallback(
    (item: AssistantToolbarItem) => {
      setToolbarOpen(false)

      if (item.type === ASSISTANT_TOOLBAR_TYPES.IMAGE) {
        window.setTimeout(() => {
          openImagePicker(false)
        }, 300)
        return
      }

      window.setTimeout(() => {
        onToolbarItemClick(item)
      }, 300)
    },
    [onToolbarItemClick, openImagePicker]
  )

  /**
   * 开始语音录音手势，并记录初始拖拽位置。
   *
   * @param event - 语音按钮指针按下事件
   * @returns 无返回值
   */
  const handleVoicePointerDown = useCallback(
    (event: ReactPointerEvent<HTMLButtonElement>) => {
      /** 当前按住说话按钮节点。 */
      const buttonElement = event.currentTarget
      /** 当前指针编号。 */
      const pointerId = event.pointerId

      if (event.pointerType === 'mouse' && event.button !== 0) {
        return
      }

      if (voiceRecognitionPending || voiceStartPendingRef.current) {
        showWarningNotify(ASSISTANT_STT_PENDING_TEXT)
        return
      }

      if (!onBeforeVoiceStart()) {
        return
      }

      event.preventDefault()
      setToolbarOpen(false)
      voicePointerIdRef.current = pointerId
      voiceStartYRef.current = event.clientY
      voiceCancelArmedRef.current = false
      voiceStartPendingRef.current = true
      setVoicePreparing(true)
      buttonElement.setPointerCapture(pointerId)
      setVoiceGestureState({
        isRecording: true,
        dragDistance: 0,
        isCancelArmed: false
      })

      /** 当前手势使用的 STT 录音器实例。 */
      const recorder = new AssistantSttRecorder({
        onStarted: handleVoiceRecognitionStarted,
        onCompleted: handleVoiceRecognitionCompleted,
        onTimeout: handleVoiceRecognitionTimeout,
        onError: handleVoiceRecognitionError,
        onWaveHeightsChange: setVoiceWaveHeights
      })

      sttRecorderRef.current = recorder

      void recorder.start().catch(() => {
        releaseVoicePointerCapture(buttonElement, pointerId)
        resetVoiceGesture()
        clearVoiceRecorderState()
        setVoiceRecognitionPending(false)
      })
    },
    [
      clearVoiceRecorderState,
      handleVoiceRecognitionStarted,
      handleVoiceRecognitionCompleted,
      handleVoiceRecognitionError,
      handleVoiceRecognitionTimeout,
      onBeforeVoiceStart,
      releaseVoicePointerCapture,
      resetVoiceGesture,
      voiceRecognitionPending
    ]
  )

  /**
   * 根据当前手势位置更新录音态的取消状态和拖拽反馈。
   *
   * @param event - 语音按钮指针移动事件
   * @returns 无返回值
   */
  const handleVoicePointerMove = useCallback((event: ReactPointerEvent<HTMLButtonElement>) => {
    if (voicePointerIdRef.current !== event.pointerId) {
      return
    }

    const dragDistance = resolveVoiceDragDistance(voiceStartYRef.current, event.clientY)
    const isCancelArmed = dragDistance >= VOICE_CANCEL_THRESHOLD

    voiceCancelArmedRef.current = isCancelArmed
    setVoiceGestureState(currentState => {
      if (
        !currentState.isRecording ||
        (currentState.dragDistance === dragDistance && currentState.isCancelArmed === isCancelArmed)
      ) {
        return currentState
      }

      return {
        ...currentState,
        dragDistance,
        isCancelArmed
      }
    })
  }, [])

  /**
   * 在松手时根据当前取消状态结束语音手势。
   *
   * @param event - 语音按钮指针抬起事件
   * @returns 无返回值
   */
  const handleVoicePointerUp = useCallback(
    (event: ReactPointerEvent<HTMLButtonElement>) => {
      if (voicePointerIdRef.current !== event.pointerId) {
        return
      }

      finishVoiceGesture(event.currentTarget, voiceCancelArmedRef.current ? 'cancel' : 'submit')
    },
    [finishVoiceGesture]
  )

  /**
   * 在浏览器中断当前指针会话时，兜底关闭录音态。
   *
   * @param event - 语音按钮指针取消事件
   * @returns 无返回值
   */
  const handleVoicePointerCancel = useCallback(
    (event: ReactPointerEvent<HTMLButtonElement>) => {
      if (voicePointerIdRef.current !== event.pointerId) {
        return
      }

      finishVoiceGesture(event.currentTarget, 'abort')
    },
    [finishVoiceGesture]
  )

  /** 当前语音按钮是否需要禁用。 */
  const voiceButtonDisabled = voiceRecognitionPending || voiceStartPendingRef.current

  return (
    <div className={styles.composerShell}>
      <input
        ref={imageInputRef}
        type='file'
        accept='image/*'
        multiple
        className={styles.hiddenImageInput}
        onChange={handleImageInputChange}
      />
      <AssistantVoiceRecordingOverlay
        visible={voiceGestureState.isRecording}
        isPreparing={voicePreparing}
        isCancelArmed={voiceGestureState.isCancelArmed}
        waveHeights={voiceWaveHeights}
      />

      {attachedProductDraft ? (
        <div className={styles.attachedProductPanel}>
          <div className={styles.attachedProductCard}>
            <div className={styles.attachedProductHeader}>
              <div className={styles.attachedProductHeaderLeft}>
                <span className={styles.attachedProductTag}>商品咨询</span>
                <span className={styles.attachedProductId}>商品ID: {attachedProductDraft.productId}</span>
              </div>
              <button
                type='button'
                className={styles.attachedProductCloseButton}
                onClick={onRemoveAttachedProductDraft}
                aria-label='移除当前咨询商品'
              >
                <X size={16} strokeWidth={2} />
              </button>
            </div>
            <div className={styles.attachedProductContent}>
              <img
                src={attachedProductDraft.image}
                alt={attachedProductDraft.name}
                className={styles.attachedProductCover}
              />
              <div className={styles.attachedProductMain}>
                <div className={styles.attachedProductName}>{attachedProductDraft.name}</div>
                <div className={styles.attachedProductSubText}>发送后会按该商品为您介绍详情、用法与注意事项</div>
                <div className={styles.attachedProductMeta}>
                  <span className={styles.attachedProductPrice}>￥{attachedProductDraft.price}</span>
                  <span className={styles.attachedProductUnit}>/{attachedProductDraft.unit}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      ) : null}

      <div id='assistant-composer-bar' className={`${styles.bar} ${hasText || hasImages ? styles.barActive : ''}`}>
        {imageUrls.length > 0 ? (
          <div className={styles.imagePreviewPanel}>
            <div className={styles.imagePreviewList}>
              {imageUrls.map((imageUrl, imageIndex) => (
                <div key={`${imageUrl}-${imageIndex}`} className={styles.imagePreviewItem}>
                  <img src={imageUrl} alt={`已上传图片${imageIndex + 1}`} className={styles.imagePreviewImage} />
                  <button
                    type='button'
                    className={styles.imagePreviewRemoveButton}
                    aria-label={`移除图片${imageIndex + 1}`}
                    onClick={() => handleRemoveImage(imageIndex)}
                  >
                    <X size={14} strokeWidth={2} />
                  </button>
                </div>
              ))}
            </div>
          </div>
        ) : null}

        {!shouldShowPrimaryAction ? (
          <button
            type='button'
            className={`${styles.iconButton} ${imageUploadBlocked ? styles.iconButtonDisabledVisual : ''}`}
            onClick={handleCameraButtonClick}
            aria-label='打开相机上传图片'
            aria-disabled={imageUploadBlocked}
          >
            <Camera size={24} strokeWidth={1.5} />
          </button>
        ) : null}

        {inputMode === ASSISTANT_INPUT_MODES.TEXT ? (
          <div className={styles.inputWrapper}>
            <textarea
              ref={textareaRef}
              rows={1}
              value={value}
              placeholder={placeholder}
              className={styles.input}
              aria-label='聊天输入框'
              onFocus={onFocus}
              onChange={event => handleValueChange(event.target.value)}
              onKeyDown={handleTextareaKeyDown}
            />
          </div>
        ) : (
          <div className={styles.voiceBtnWrapper}>
            <button
              type='button'
              className={styles.voiceHoldButton}
              aria-label='按住说话，上滑取消'
              disabled={voiceButtonDisabled}
              onPointerDown={handleVoicePointerDown}
              onPointerMove={handleVoicePointerMove}
              onPointerUp={handleVoicePointerUp}
              onPointerCancel={handleVoicePointerCancel}
              onContextMenu={event => event.preventDefault()}
            >
              {voiceRecognitionPending ? '识别中...' : '按住 说话'}
            </button>
          </div>
        )}

        {!shouldHideInputModeToggle && inputMode === ASSISTANT_INPUT_MODES.TEXT ? (
          <button
            type='button'
            className={styles.iconButton}
            onClick={() => onToggleInputMode(ASSISTANT_INPUT_MODES.VOICE)}
            aria-label='切换到语音输入'
          >
            <AudioLines size={22} strokeWidth={1.5} />
          </button>
        ) : null}

        {!shouldHideInputModeToggle && inputMode === ASSISTANT_INPUT_MODES.VOICE ? (
          <button
            type='button'
            className={styles.iconButton}
            onClick={() => onToggleInputMode(ASSISTANT_INPUT_MODES.TEXT)}
            aria-label='切换到文本输入'
          >
            <Keyboard size={22} strokeWidth={1.5} />
          </button>
        ) : null}

        {!shouldShowPrimaryAction && toolbarItems.length > 0 ? (
          <button
            type='button'
            className={`${styles.iconButton} ${styles.plusButton} ${toolbarOpen ? styles.plusButtonOpen : ''}`}
            onClick={handleToolbarToggle}
            aria-label={toolbarOpen ? '收起更多操作' : '展开更多操作'}
            aria-expanded={toolbarOpen}
          >
            <Plus size={24} strokeWidth={1.5} />
          </button>
        ) : null}

        {shouldShowPrimaryAction ? (
          <div className={styles.activeActions}>
            {toolbarItems.length > 0 && !isReplying ? (
              <button
                type='button'
                className={`${styles.iconButton} ${styles.plusButton} ${toolbarOpen ? styles.plusButtonOpen : ''}`}
                onClick={handleToolbarToggle}
                aria-label={toolbarOpen ? '收起更多操作' : '展开更多操作'}
                aria-expanded={toolbarOpen}
              >
                <Plus size={24} strokeWidth={1.5} />
              </button>
            ) : null}
            <button
              type='button'
              className={`${styles.sendButton} ${isReplying ? styles.sendButtonReplying : ''}`}
              onClick={isReplying ? onStop : onSend}
              disabled={isStopping || (imageUploading && !isReplying)}
              aria-label={isReplying ? '停止生成' : '发送消息'}
            >
              {isReplying ? (
                <Square size={12} strokeWidth={0} fill='currentColor' className={styles.stopButtonIcon} />
              ) : (
                <ArrowUp size={20} strokeWidth={2.5} />
              )}
            </button>
          </div>
        ) : null}
      </div>

      <Popup
        visible={toolbarOpen && toolbarItems.length > 0}
        position='bottom'
        round
        className={styles.drawerPopup}
        onClose={() => setToolbarOpen(false)}
        overlayStyle={{ backgroundColor: 'rgba(0, 0, 0, 0.4)' }}
        portal={typeof document !== 'undefined' ? document.body : null}
      >
        <div className={styles.drawerContent}>
          <div className={styles.drawerDragPill} />
          <div className={styles.drawerGrid}>
            {toolbarItems.map(item => (
              <button
                type='button'
                key={item.type}
                className={`${styles.drawerGridItem} ${
                  item.type === ASSISTANT_TOOLBAR_TYPES.IMAGE && imageUploadBlocked ? styles.drawerGridItemDisabled : ''
                }`}
                onClick={() => handleToolbarItemSelection(item)}
                aria-disabled={item.type === ASSISTANT_TOOLBAR_TYPES.IMAGE && imageUploadBlocked}
              >
                <div className={styles.drawerIconBox}>
                  {item.img ? <img src={item.img} alt={item.title} className={styles.drawerIconImg} /> : null}
                </div>
                <span className={styles.drawerItemTitle}>{item.title}</span>
              </button>
            ))}
          </div>
        </div>
      </Popup>
    </div>
  )
}

export default AssistantComposer
