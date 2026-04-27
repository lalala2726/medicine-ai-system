import { useAuthStore } from '@/store/auth'
import { resolveAiAgentWebSocketUrl } from '@/utils/aiAgentUrl'

/** Assistant STT 默认波形高度。 */
const ASSISTANT_STT_DEFAULT_WAVE_HEIGHTS = Array.from({ length: 21 }, () => 8)

/** STT 音频采样率。 */
const ASSISTANT_STT_SAMPLE_RATE = 16000

/** STT analyser 的 FFT 大小。 */
const ASSISTANT_STT_ANALYSER_FFT_SIZE = 64

/** 识别静音态时使用的 RMS 阈值。 */
const ASSISTANT_STT_SILENCE_RMS_THRESHOLD = 0.02

/** AudioWorklet 处理器名称。 */
const ASSISTANT_STT_PCM_PROCESSOR_NAME = 'assistant-stt-pcm-processor'

/** 当前环境不支持麦克风采集时的兜底提示文案。 */
const ASSISTANT_STT_MEDIA_DEVICES_UNSUPPORTED_TEXT =
  '当前环境暂不支持语音识别，请使用 HTTPS、localhost 或系统浏览器后重试'

/** 当前环境不支持 AudioWorklet 时的兜底提示文案。 */
const ASSISTANT_STT_AUDIO_WORKLET_UNSUPPORTED_TEXT = '当前浏览器暂不支持语音识别，请升级浏览器后重试'

/** AudioWorklet 处理器代码，负责把 Float32 PCM 转成 16-bit PCM。 */
const ASSISTANT_STT_PCM_WORKLET_CODE = `
class AssistantSttPCMProcessor extends AudioWorkletProcessor {
  process(inputs) {
    const input = inputs[0];

    if (input && input.length > 0) {
      const float32 = input[0];
      const int16 = new Int16Array(float32.length);

      for (let index = 0; index < float32.length; index += 1) {
        const sample = Math.max(-1, Math.min(1, float32[index]));
        int16[index] = sample < 0 ? sample * 0x8000 : sample * 0x7fff;
      }

      this.port.postMessage(int16.buffer, [int16.buffer]);
    }

    return true;
  }
}

registerProcessor('${ASSISTANT_STT_PCM_PROCESSOR_NAME}', AssistantSttPCMProcessor);
`

/** STT started 消息结构。 */
interface AssistantSttStartedMessage {
  /** 消息类型。 */
  type?: 'started'
}

/** STT transcript 消息结构。 */
interface AssistantSttTranscriptMessage {
  /** 消息类型。 */
  type?: 'transcript'
  /** 当前分段是否已经结束。 */
  is_final?: boolean
  /** 识别结果载荷。 */
  result?: {
    /** 当前识别到的文本。 */
    text?: string
  }
}

/** STT timeout / error 消息结构。 */
interface AssistantSttTerminalMessage {
  /** 消息类型。 */
  type?: 'completed' | 'timeout' | 'error'
  /** 后端返回的提示文案。 */
  message?: string
}

/** STT WebSocket 消息联合类型。 */
type AssistantSttSocketMessage =
  | AssistantSttStartedMessage
  | AssistantSttTranscriptMessage
  | AssistantSttTerminalMessage

/** Assistant STT 回调集合。 */
export interface AssistantSttCallbacks {
  /** WebSocket 完成 started 确认后的回调。 */
  onStarted?: () => void
  /** 后端返回 transcript 时的回调。 */
  onTranscript?: (text: string, isFinal: boolean) => void
  /** 后端返回 completed 时的回调。 */
  onCompleted?: (finalText: string) => void
  /** 后端返回 timeout 时的回调。 */
  onTimeout?: (message: string) => void
  /** 后端返回 error 时的回调。 */
  onError?: (message: string) => void
  /** 音量波形更新时的回调。 */
  onWaveHeightsChange?: (heights: number[]) => void
}

/**
 * 解析当前环境下的 STT WebSocket 地址。
 *
 * @param accessToken - 当前登录用户 access token
 * @returns 可直接建立连接的 WebSocket 地址
 */
const resolveAssistantSttWebSocketUrl = (accessToken: string): string => {
  return resolveAiAgentWebSocketUrl('/ws/speech/stt/stream', {
    access_token: accessToken
  })
}

/**
 * 根据 analyser 数据计算当前浮层使用的波形高度。
 *
 * @param analyser - 当前音频 analyser
 * @returns 波形高度数组
 */
const resolveWaveHeightsFromAnalyser = (analyser: AnalyserNode) => {
  /** analyser 当前的频谱长度。 */
  const frequencyBinCount = analyser.frequencyBinCount
  /** 当前频谱数据缓存。 */
  const frequencyData = new Uint8Array(frequencyBinCount)
  /** 当前时域波形数据缓存。 */
  const timeDomainData = new Uint8Array(analyser.fftSize)

  analyser.getByteFrequencyData(frequencyData)
  analyser.getByteTimeDomainData(timeDomainData)

  /** 当前音频样本的 RMS 振幅。 */
  const rms = Math.sqrt(
    timeDomainData.reduce((sum, sample) => {
      const normalizedSample = (sample - 128) / 128

      return sum + normalizedSample * normalizedSample
    }, 0) / timeDomainData.length
  )

  if (rms < ASSISTANT_STT_SILENCE_RMS_THRESHOLD) {
    return ASSISTANT_STT_DEFAULT_WAVE_HEIGHTS
  }

  return ASSISTANT_STT_DEFAULT_WAVE_HEIGHTS.map((_, index) => {
    const binIndex = Math.floor((index / ASSISTANT_STT_DEFAULT_WAVE_HEIGHTS.length) * (frequencyBinCount * 0.6))
    const rawValue = frequencyData[binIndex] || 0
    const dynamicPart = (rawValue / 255) * 22
    const factor = 0.55 + index / (ASSISTANT_STT_DEFAULT_WAVE_HEIGHTS.length * 3)

    return Math.max(8, 8 + dynamicPart * factor * 1.8)
  })
}

/**
 * 解析 WebSocket 非正常关闭时的兜底错误文案。
 *
 * @param event - WebSocket 关闭事件
 * @param hasSocketError - 当前是否发生过底层 socket error
 * @returns 适合前端提示的错误文案，为空时表示无需提示
 */
const resolveSocketCloseErrorMessage = (event: CloseEvent, hasSocketError: boolean) => {
  if (hasSocketError) {
    return 'WebSocket 连接发生异常'
  }

  if (event.wasClean) {
    return ''
  }

  if (event.code === 1008) {
    return '未认证、无权限或录音流程异常'
  }

  if (event.code === 1011) {
    return '语音识别服务发生内部错误'
  }

  return '语音识别连接已中断，请稍后重试'
}

/**
 * Assistant 语音转文本录音器。
 * 负责管理 WebSocket、麦克风采集、AudioWorklet 转码和后端识别结果回调。
 */
export class AssistantSttRecorder {
  /** 当前 WebSocket 实例。 */
  private ws: WebSocket | null = null
  /** 当前 AudioContext 实例。 */
  private audioContext: AudioContext | null = null
  /** 当前麦克风媒体流。 */
  private stream: MediaStream | null = null
  /** 当前工作线程节点。 */
  private workletNode: AudioWorkletNode | null = null
  /** 当前媒体流音频源节点。 */
  private sourceNode: MediaStreamAudioSourceNode | null = null
  /** 当前波形 analyser 节点。 */
  private analyserNode: AnalyserNode | null = null
  /** 当前波形动画帧。 */
  private animationFrameId: number | null = null
  /** 当前是否已经请求 finish。 */
  private finishRequested = false
  /** 当前是否已经进入终态。 */
  private terminalSettled = false
  /** 当前是否应忽略 WebSocket 关闭事件。 */
  private shouldIgnoreSocketClose = false
  /** 当前是否发生过 socket error。 */
  private hasSocketError = false
  /** 当前已经确认完成的文本。 */
  private finalizedText = ''
  /** 当前录音器是否已经进入销毁流程。 */
  private destroyed = false

  /**
   * 创建一个 Assistant STT 录音器实例。
   *
   * @param callbacks - 录音器回调集合
   */
  constructor(private readonly callbacks: AssistantSttCallbacks) {}

  /**
   * 启动一次新的 STT 录音会话。
   *
   * @param initialText - 初始前缀文本
   * @returns 启动流程的 Promise
   */
  public async start(initialText: string = '') {
    if (this.ws || this.stream || this.audioContext) {
      return
    }

    /** 当前登录态 access token。 */
    const accessToken = useAuthStore.getState().getAccessToken()

    if (!accessToken) {
      throw new Error('未授权，请登录后再试')
    }

    this.finishRequested = false
    this.terminalSettled = false
    this.shouldIgnoreSocketClose = false
    this.hasSocketError = false
    this.destroyed = false
    this.finalizedText = initialText.trim()
    this.emitWaveHeights(ASSISTANT_STT_DEFAULT_WAVE_HEIGHTS)

    try {
      this.initializeSocket(resolveAssistantSttWebSocketUrl(accessToken))
      await this.initializeAudioPipeline()

      if (this.destroyed) {
        this.destroy()
      }
    } catch (error) {
      if (this.destroyed) {
        this.destroy()
        return
      }

      this.callbacks.onError?.(
        error instanceof Error && error.message.trim() ? error.message : '启动录音失败，请检查麦克风权限'
      )
      this.destroy()
      throw error
    }
  }

  /**
   * 结束当前录音并通知后端完成识别。
   *
   * @returns 无返回值
   */
  public finish() {
    if (this.finishRequested || this.terminalSettled || this.destroyed) {
      return
    }

    this.finishRequested = true
    this.destroyAudioPipeline()

    if (this.ws?.readyState === WebSocket.OPEN) {
      this.sendJson({
        type: 'finish'
      })
    }
  }

  /**
   * 取消当前录音，不保留任何识别结果。
   *
   * @returns 无返回值
   */
  public cancel() {
    this.shouldIgnoreSocketClose = true
    this.finishRequested = false
    this.destroyed = true
    this.destroy()
  }

  /**
   * 销毁当前录音器实例并清理所有资源。
   *
   * @returns 无返回值
   */
  public destroy() {
    this.destroyed = true
    this.destroyAudioPipeline()
    this.destroySocket()
  }

  /**
   * 初始化当前会话的 WebSocket。
   *
   * @param wsUrl - 目标 WebSocket 地址
   * @returns 无返回值
   */
  private initializeSocket(wsUrl: string) {
    /** 当前录音器使用的 WebSocket 实例。 */
    const socket = new WebSocket(wsUrl)

    this.ws = socket
    socket.binaryType = 'arraybuffer'

    socket.onopen = () => {
      if (this.destroyed) {
        socket.close()
        return
      }

      this.sendJson({
        type: 'start',
        request: {
          enable_itn: true,
          enable_punc: true,
          show_utterances: true,
          result_type: 'single'
        }
      })

      if (this.finishRequested) {
        this.sendJson({
          type: 'finish'
        })
      }
    }

    socket.onmessage = event => {
      if (this.destroyed) {
        return
      }

      this.handleSocketMessage(event.data)
    }

    socket.onerror = () => {
      this.hasSocketError = true
    }

    socket.onclose = event => {
      if (this.shouldIgnoreSocketClose || this.terminalSettled || this.destroyed) {
        this.resetSocketReferences()
        return
      }

      const errorMessage = resolveSocketCloseErrorMessage(event, this.hasSocketError)

      if (errorMessage) {
        this.callbacks.onError?.(errorMessage)
      }

      this.destroy()
    }
  }

  /**
   * 初始化当前会话的音频采集与转码链路。
   *
   * @returns 初始化完成后的 Promise
   */
  private async initializeAudioPipeline() {
    /** 当前浏览器提供的媒体设备能力。 */
    const mediaDevices = navigator.mediaDevices

    if (!mediaDevices?.getUserMedia) {
      throw new Error(ASSISTANT_STT_MEDIA_DEVICES_UNSUPPORTED_TEXT)
    }

    this.stream = await mediaDevices.getUserMedia({
      audio: true,
      video: false
    })

    if (this.destroyed) {
      this.stream.getTracks().forEach(track => {
        track.stop()
      })
      this.stream = null
      return
    }

    /** 浏览器内置 AudioContext 构造函数。 */
    const AudioContextClass =
      window.AudioContext ||
      (window as Window & typeof globalThis & { webkitAudioContext?: typeof AudioContext }).webkitAudioContext

    if (!AudioContextClass) {
      throw new Error('当前浏览器不支持 AudioContext')
    }

    this.audioContext = new AudioContextClass({
      sampleRate: ASSISTANT_STT_SAMPLE_RATE
    })

    if (!this.audioContext.audioWorklet || typeof AudioWorkletNode === 'undefined') {
      throw new Error(ASSISTANT_STT_AUDIO_WORKLET_UNSUPPORTED_TEXT)
    }

    this.sourceNode = this.audioContext.createMediaStreamSource(this.stream)
    this.analyserNode = this.audioContext.createAnalyser()
    this.analyserNode.fftSize = ASSISTANT_STT_ANALYSER_FFT_SIZE
    this.sourceNode.connect(this.analyserNode)
    this.startWaveLoop()

    /** worklet 代码的临时 Blob URL。 */
    const workletUrl = URL.createObjectURL(
      new Blob([ASSISTANT_STT_PCM_WORKLET_CODE], {
        type: 'application/javascript'
      })
    )

    try {
      await this.audioContext.audioWorklet.addModule(workletUrl)
    } finally {
      URL.revokeObjectURL(workletUrl)
    }

    if (this.destroyed) {
      return
    }

    this.workletNode = new AudioWorkletNode(this.audioContext, ASSISTANT_STT_PCM_PROCESSOR_NAME)
    this.workletNode.port.onmessage = event => {
      if (!this.destroyed && !this.finishRequested && this.ws?.readyState === WebSocket.OPEN) {
        this.ws.send(event.data as ArrayBuffer)
      }
    }

    this.analyserNode.connect(this.workletNode)
    this.workletNode.connect(this.audioContext.destination)
  }

  /**
   * 处理后端返回的 STT WebSocket 消息。
   *
   * @param rawMessage - 原始消息数据
   * @returns 无返回值
   */
  private handleSocketMessage(rawMessage: string | ArrayBuffer | Blob) {
    if (typeof rawMessage !== 'string') {
      return
    }

    try {
      const message = JSON.parse(rawMessage) as AssistantSttSocketMessage

      if (message.type === 'started') {
        this.callbacks.onStarted?.()
        return
      }

      if (message.type === 'transcript') {
        const transcriptText = message.result?.text || ''

        if (message.is_final) {
          this.finalizedText += transcriptText
          this.callbacks.onTranscript?.(this.finalizedText, true)
        } else {
          this.callbacks.onTranscript?.(this.finalizedText + transcriptText, false)
        }

        return
      }

      if (message.type === 'completed') {
        this.terminalSettled = true
        this.callbacks.onCompleted?.(this.finalizedText.trim())
        this.destroy()
        return
      }

      if (message.type === 'timeout') {
        this.terminalSettled = true
        this.callbacks.onTimeout?.(message.message || '语音识别超时，请重试')
        this.destroy()
        return
      }

      if (message.type === 'error') {
        this.terminalSettled = true
        this.callbacks.onError?.(message.message || '语音识别失败，请稍后重试')
        this.destroy()
      }
    } catch (error) {
      console.error('[AssistantSttRecorder] STT 消息解析失败', error)
    }
  }

  /**
   * 持续读取 analyser 数据并回传给录音浮层。
   *
   * @returns 无返回值
   */
  private startWaveLoop() {
    if (!this.analyserNode) {
      return
    }

    /**
     * 下一帧波形更新。
     *
     * @returns 无返回值
     */
    const updateWaveFrame = () => {
      if (!this.analyserNode) {
        return
      }

      this.emitWaveHeights(resolveWaveHeightsFromAnalyser(this.analyserNode))
      this.animationFrameId = window.requestAnimationFrame(updateWaveFrame)
    }

    updateWaveFrame()
  }

  /**
   * 回传当前波形高度数组。
   *
   * @param heights - 当前录音浮层应展示的波形高度
   * @returns 无返回值
   */
  private emitWaveHeights(heights: number[]) {
    this.callbacks.onWaveHeightsChange?.(heights)
  }

  /**
   * 清理当前录音会话的音频采集链路。
   *
   * @returns 无返回值
   */
  private destroyAudioPipeline() {
    if (this.animationFrameId !== null) {
      window.cancelAnimationFrame(this.animationFrameId)
      this.animationFrameId = null
    }

    if (this.workletNode) {
      this.workletNode.port.onmessage = null
      this.workletNode.disconnect()
      this.workletNode = null
    }

    if (this.analyserNode) {
      this.analyserNode.disconnect()
      this.analyserNode = null
    }

    if (this.sourceNode) {
      this.sourceNode.disconnect()
      this.sourceNode = null
    }

    if (this.audioContext) {
      this.audioContext.close().catch(() => undefined)
      this.audioContext = null
    }

    if (this.stream) {
      this.stream.getTracks().forEach(track => {
        track.stop()
      })
      this.stream = null
    }
  }

  /**
   * 清理当前录音会话的 WebSocket。
   *
   * @returns 无返回值
   */
  private destroySocket() {
    if (this.ws) {
      if (this.ws.readyState === WebSocket.CONNECTING) {
        return
      }

      this.ws.onopen = null
      this.ws.onmessage = null
      this.ws.onerror = null
      this.ws.onclose = null

      if (this.ws.readyState === WebSocket.OPEN) {
        this.ws.close()
      }
    }

    this.resetSocketReferences()
  }

  /**
   * 重置当前 socket 相关引用和标记位。
   *
   * @returns 无返回值
   */
  private resetSocketReferences() {
    this.ws = null
    this.hasSocketError = false
  }

  /**
   * 向后端发送一条 JSON 控制消息。
   *
   * @param data - 待发送的控制消息
   * @returns 无返回值
   */
  private sendJson(data: Record<string, unknown>) {
    if (this.ws?.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(data))
    }
  }
}
