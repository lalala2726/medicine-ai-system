import { getActiveAccessToken } from '@/utils/request/authRefresh';

/** 语音识别 WebSocket 错误对应的服务来源。 */
const STT_SERVICE_LABEL = 'AI 服务器';

/**
 * 记录语音识别请求错误来源。
 *
 * @param detail 错误诊断详情。
 */
function logSttRequestError(detail: Record<string, unknown>): void {
  console.error(`[请求错误][${STT_SERVICE_LABEL}]`, detail);
}

export interface SttCallbacks {
  onStarted?: (maxDuration: number) => void;
  onTranscript?: (text: string, isFinal: boolean) => void;
  onCompleted?: () => void;
  onTimeout?: (msg: string) => void;
  onError?: (msg: string) => void;
}

/** AudioWorklet 处理器代码，运行在独立线程中将 Float32 转为 16-bit PCM */
const PCM_WORKLET_CODE = `
class PCMProcessor extends AudioWorkletProcessor {
  process(inputs) {
    const input = inputs[0];
    if (input && input.length > 0) {
      const float32 = input[0];
      const int16 = new Int16Array(float32.length);
      for (let i = 0; i < float32.length; i++) {
        const s = Math.max(-1, Math.min(1, float32[i]));
        int16[i] = s < 0 ? s * 0x8000 : s * 0x7fff;
      }
      this.port.postMessage(int16.buffer, [int16.buffer]);
    }
    return true;
  }
}
registerProcessor('pcm-processor', PCMProcessor);
`;

export class SttRecorder {
  private ws: WebSocket | null = null;
  private audioContext: AudioContext | null = null;
  private stream: MediaStream | null = null;
  private workletNode: AudioWorkletNode | null = null;
  private source: MediaStreamAudioSourceNode | null = null;
  private isRecording = false;
  private finalizedText = '';

  constructor(private callbacks: SttCallbacks) {}

  /**
   * 开始语音识别录音。
   *
   * @param currentText 当前输入框已有文本。
   * @returns 启动完成时的 Promise。
   */
  public async start(currentText: string = ''): Promise<void> {
    if (this.isRecording) return;
    this.isRecording = true;
    this.finalizedText = currentText;

    try {
      const token = await getActiveAccessToken({ refreshWhenExpired: true });
      if (!token) throw new Error('未授权，请登录后再试');

      // 使用原生 WebSocket 连接
      const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';

      // 注意：由于我们在本地开发或者代理环境中，可能需要连接到特定的 AI 后端域。
      // 这里根据要求，直接使用前端域名代理或直接连后端。
      // 如果后端在同一域名但路径不同，或者跨域，我们先构建 URL，并用 query 传 token。
      const host = window.location.host;
      // 根据文档，连接地址为 /ws/speech/stt/stream，并通过 query 传 token
      // 为了支持 vite 开发环境的代理或者未来的网关，我们先拼接当前 host。
      // 如果是在 vite 开发环境下，可以在 vite.config.ts 配置代理，但根据最新要求直接用下面的方式连。
      // 我们将其指向当前 origin 并代理，或者直接连 localhost:8000。
      // 根据文档示例：ws://localhost:8000/ws/speech/stt/stream?access_token=...
      // 为了代码灵活性，我们在开发环境可以直接连目标，生产环境连当前域。这里先按照相对当前域或者通过环境变量配置处理。
      // 如果需要连本地 8000：
      const isDev = process.env.NODE_ENV === 'development';
      const wsHost = isDev ? 'localhost:8000' : host;

      const wsUrl = `${protocol}//${wsHost}/ws/speech/stt/stream?access_token=${encodeURIComponent(token)}`;

      this.ws = new WebSocket(wsUrl);
      this.ws.binaryType = 'arraybuffer';

      this.ws.onopen = () => {
        // 连接建立后先发 start 包
        this.sendJson({
          type: 'start',
          request: {
            enable_itn: true,
            enable_punc: true,
            show_utterances: true,
            result_type: 'single', // 单句模式
          },
        });
      };

      this.ws.onmessage = (event) => {
        try {
          const msg = JSON.parse(event.data as string);

          if (msg.type === 'started') {
            this.callbacks.onStarted?.(msg.max_duration_seconds);
          } else if (msg.type === 'transcript') {
            const text = msg.result?.text || '';
            if (msg.is_final) {
              this.finalizedText += text;
              this.callbacks.onTranscript?.(this.finalizedText, true);
            } else {
              this.callbacks.onTranscript?.(this.finalizedText + text, false);
            }
          } else if (msg.type === 'completed') {
            this.callbacks.onCompleted?.();
            this.stop();
          } else if (msg.type === 'timeout') {
            this.callbacks.onTimeout?.(msg.message);
            this.stop();
          } else if (msg.type === 'error') {
            logSttRequestError({
              stage: 'message',
              message: msg.message,
              response: msg,
            });
            this.callbacks.onError?.(msg.message);
            this.stop();
          }
        } catch (e) {
          console.error('STT 消息解析失败:', e);
        }
      };

      this.ws.onclose = (event) => {
        if (!event.wasClean) {
          logSttRequestError({
            stage: 'close',
            code: event.code,
            reason: event.reason,
          });
          if (event.code === 1008) {
            this.callbacks.onError?.('未认证/无权限/协议顺序错误 (1008)');
          } else if (event.code === 1011) {
            this.callbacks.onError?.('服务端STT内部错误 (1011)');
          } else {
            // this.callbacks.onError?.(`连接意外关闭 (Code: ${event.code})`);
          }
        }
        this.stop();
      };

      this.ws.onerror = () => {
        logSttRequestError({
          stage: 'websocket-error',
        });
        this.callbacks.onError?.('WebSocket 连接发生异常');
        this.stop();
      };

      // 获取麦克风权限并初始化 AudioContext (16kHz, mono)
      this.stream = await navigator.mediaDevices.getUserMedia({ audio: true, video: false });

      const AudioContextClass = window.AudioContext || (window as any).webkitAudioContext;
      this.audioContext = new AudioContextClass({ sampleRate: 16000 });

      this.source = this.audioContext.createMediaStreamSource(this.stream);

      // 使用 AudioWorklet 在独立线程中处理音频，避免主线程阻塞
      const workletBlob = new Blob([PCM_WORKLET_CODE], { type: 'application/javascript' });
      const workletUrl = URL.createObjectURL(workletBlob);
      try {
        await this.audioContext.audioWorklet.addModule(workletUrl);
      } finally {
        URL.revokeObjectURL(workletUrl);
      }

      this.workletNode = new AudioWorkletNode(this.audioContext, 'pcm-processor');
      this.workletNode.port.onmessage = (e: MessageEvent) => {
        if (!this.isRecording || this.ws?.readyState !== WebSocket.OPEN) return;
        this.ws.send(e.data as ArrayBuffer);
      };

      this.source.connect(this.workletNode);
      // 连接到 destination 保持音频图活跃（静音输出）
      this.workletNode.connect(this.audioContext.destination);
    } catch (err: any) {
      logSttRequestError({
        stage: 'start',
        message: err.message,
        error: err,
      });
      this.callbacks.onError?.(err.message || '启动录音失败，请检查麦克风权限');
      this.stop();
    }
  }

  /**
   * 向 STT WebSocket 发送 JSON 消息。
   *
   * @param data 需要发送的消息对象。
   */
  private sendJson(data: any): void {
    if (this.ws?.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(data));
    }
  }

  /**
   * 停止录音并释放本地音频资源。
   */
  public stop(): void {
    if (!this.isRecording) return;
    this.isRecording = false;

    // 发送 finish 包
    if (this.ws?.readyState === WebSocket.OPEN) {
      this.sendJson({ type: 'finish' });
    }

    if (this.workletNode) {
      this.workletNode.port.onmessage = null;
      this.workletNode.disconnect();
      this.workletNode = null;
    }
    if (this.source) {
      this.source.disconnect();
      this.source = null;
    }
    if (this.audioContext) {
      this.audioContext.close();
      this.audioContext = null;
    }
    if (this.stream) {
      this.stream.getTracks().forEach((track) => track.stop());
      this.stream = null;
    }

    // 不主动关闭 WS，等待后端完成处理后自动关闭
  }
}
