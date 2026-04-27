import {
  ArrowRightOutlined,
  CloseOutlined,
  ReloadOutlined,
  SafetyCertificateOutlined,
} from '@ant-design/icons';
import { Button, Spin } from 'antd';
import { createStyles } from 'antd-style';
import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import {
  checkSliderCaptcha,
  getSliderCaptcha,
  type CaptchaVerificationResult,
  type SliderCaptchaChallenge,
  type SliderCaptchaCheckPayload,
  type SliderCaptchaTrackPayload,
  type SliderCaptchaTrackPoint,
} from '@/api/core/captcha';

/**
 * 滑块按钮宽度。
 */
const HANDLE_WIDTH = 62;

/**
 * 滑块按钮高度。
 */
const HANDLE_HEIGHT = 48;

/**
 * 验证码预览最大宽度。
 */
const PREVIEW_WIDTH = 360;

/**
 * 校验失败后刷新前的等待时间（毫秒）。
 */
const RELOAD_DELAY_MS = 720;

/**
 * 组件状态枚举。
 */
type SliderCaptchaStage = 'idle' | 'loading' | 'checking' | 'error' | 'success';

/**
 * 拖动上下文。
 */
interface DragContext {
  /** 指针 ID */
  pointerId: number;
  /** 拖动开始时的页面 X 坐标 */
  startPageX: number;
  /** 拖动开始时的时间戳 */
  startTime: number;
  /** 拖动起始时的滑块偏移 */
  startHandleOffset: number;
  /** 已采集的轨迹点 */
  trackList: SliderCaptchaTrackPoint[];
}

/**
 * 尺寸测量结果。
 */
interface CaptchaMetrics {
  /** 背景图渲染宽度 */
  previewWidth: number;
  /** 背景图渲染高度 */
  previewHeight: number;
  /** 拼图块渲染宽度 */
  pieceWidth: number;
  /** 拼图块渲染高度 */
  pieceHeight: number;
  /** 拼图块最大位移 */
  maxPieceOffset: number;
}

/**
 * 组件属性。
 */
interface SliderCaptchaModalProps {
  /** 是否打开弹层 */
  open: boolean;
  /** 关闭弹层回调 */
  onCancel: () => void;
  /** 校验成功回调 */
  onVerified: (result: CaptchaVerificationResult) => void;
  /** 是否为暗色模式 */
  isDark?: boolean;
  /** 弹层层级 */
  zIndex?: number;
}

/**
 * 创建验证码弹层样式。
 */
const useStyles = createStyles(
  ({ token, css }, { isDark, zIndex }: { isDark: boolean; zIndex: number }) => {
    return {
      mask: css`
        position: fixed;
        inset: 0;
        z-index: ${zIndex};
        display: flex;
        align-items: center;
        justify-content: center;
        padding: 24px;
        background: rgba(0, 0, 0, 0.45);
      `,
      panel: css`
        width: min(420px, 100%);
        position: relative;
        overflow: hidden;
        border-radius: 18px;
        border: 1px solid ${isDark ? 'rgba(255, 255, 255, 0.08)' : 'rgba(221, 230, 240, 0.9)'};
        background: ${isDark ? '#1f1f1f' : 'rgba(255, 255, 255, 0.98)'};
        box-shadow: ${isDark
          ? '0 16px 40px rgba(0, 0, 0, 0.4)'
          : '0 16px 40px rgba(15, 33, 56, 0.18)'};
      `,
      header: css`
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 12px;
        padding: 18px 18px 12px;
      `,
      headerLeft: css`
        display: flex;
        gap: 10px;
        align-items: center;
      `,
      badge: css`
        width: 36px;
        height: 36px;
        border-radius: 10px;
        display: inline-flex;
        align-items: center;
        justify-content: center;
        color: ${token.colorPrimary};
        font-size: 18px;
        background: rgba(24, 144, 255, 0.1);
        border: 1px solid rgba(24, 144, 255, 0.14);
      `,
      titleWrap: css`
        display: flex;
        flex-direction: column;
        gap: 2px;
      `,
      title: css`
        margin: 0;
        color: ${token.colorTextHeading};
        font-size: 18px;
        line-height: 1.2;
        font-weight: 600;
      `,
      subtitle: css`
        margin: 0;
        color: ${token.colorTextSecondary};
        font-size: 12px;
        line-height: 1.5;
      `,
      headerActions: css`
        display: flex;
        gap: 8px;
      `,
      actionButton: css`
        width: 34px;
        height: 34px;
        border: none;
        border-radius: 10px;
        color: ${token.colorTextSecondary};
        background: ${token.colorFillTertiary};
        box-shadow: inset 0 0 0 1px rgba(0, 0, 0, 0.04);

        &:hover {
          color: ${token.colorPrimary};
          background: ${token.colorFillSecondary};
        }
      `,
      body: css`
        position: relative;
        padding: 0 18px 18px;
      `,
      statusBar: css`
        margin-bottom: 12px;
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 12px;
      `,
      statusText: css`
        font-size: 13px;
        color: ${token.colorTextSecondary};
        line-height: 1.5;
      `,
      previewShell: css`
        border-radius: 12px;
      `,
      previewFrame: css`
        position: relative;
        width: 100%;
        overflow: hidden;
        border-radius: 12px;
        background: ${token.colorFillAlter};
        border: 1px solid ${token.colorBorderSecondary};
      `,
      previewImage: css`
        width: 100%;
        height: 100%;
        object-fit: cover;
        display: block;
        user-select: none;
        -webkit-user-drag: none;
      `,
      pieceImage: css`
        width: auto;
        height: 100%;
        display: block;
        user-select: none;
        -webkit-user-drag: none;
      `,
      pieceLayer: css`
        position: absolute;
        inset: 0 auto 0 0;
        pointer-events: none;
        user-select: none;
      `,
      previewOverlay: css`
        position: absolute;
        inset: 0;
        pointer-events: none;
        box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.08);
      `,
      sliderArea: css`
        margin: 16px auto 0;
        max-width: ${PREVIEW_WIDTH}px;
      `,
      sliderTrack: css`
        position: relative;
        height: ${HANDLE_HEIGHT}px;
        border-radius: ${HANDLE_HEIGHT / 2}px;
        overflow: hidden;
        background: ${token.colorFillAlter};
        border: 1px solid ${token.colorBorderSecondary};
      `,
      sliderFill: css`
        position: absolute;
        inset: 0 auto 0 0;
        border-radius: ${HANDLE_HEIGHT / 2}px;
        background: rgba(24, 144, 255, 0.12);
      `,
      sliderHint: css`
        position: absolute;
        inset: 0;
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 8px;
        font-size: 13px;
        color: ${token.colorTextSecondary};
        pointer-events: none;
      `,
      sliderHandle: css`
        position: absolute;
        top: -1px;
        left: -1px;
        width: ${HANDLE_WIDTH}px;
        height: ${HANDLE_HEIGHT}px;
        border: 1px solid rgba(24, 144, 255, 0.2);
        border-radius: ${HANDLE_HEIGHT / 2}px;
        display: inline-flex;
        align-items: center;
        justify-content: center;
        color: white;
        font-size: 16px;
        cursor: grab;
        background: ${token.colorPrimary};
        box-shadow: 0 6px 16px rgba(24, 144, 255, 0.2);
        transition: box-shadow 0.2s ease;

        &:active {
          cursor: grabbing;
        }

        &:disabled {
          cursor: not-allowed;
          opacity: 0.7;
        }
      `,
      successText: css`
        color: ${isDark ? '#49aa19' : '#1f9d55'};
      `,
      errorText: css`
        color: ${isDark ? '#e6453f' : '#d94f41'};
      `,
      loadingCover: css`
        position: absolute;
        inset: 0;
        display: flex;
        align-items: center;
        justify-content: center;
        background: ${isDark ? 'rgba(0, 0, 0, 0.76)' : 'rgba(255, 255, 255, 0.76)'};
        z-index: 2;
      `,
    };
  },
);

/**
 * 限制数值在指定区间内。
 *
 * @param value 当前值
 * @param min 最小值
 * @param max 最大值
 * @returns 限制后的值
 */
function clamp(value: number, min: number, max: number): number {
  return Math.min(Math.max(value, min), max);
}

/**
 * 提取页面坐标。
 *
 * @param event 指针事件
 * @returns 页面坐标
 */
function getPagePoint(event: PointerEvent | React.PointerEvent<HTMLElement>): {
  x: number;
  y: number;
} {
  return {
    x: window.scrollX + event.clientX,
    y: window.scrollY + event.clientY,
  };
}

/**
 * 生成轨迹点。
 *
 * @param event 指针事件
 * @param startTime 拖动开始时间
 * @param type 轨迹点类型
 * @returns 轨迹点
 */
function buildTrackPoint(
  event: PointerEvent | React.PointerEvent<HTMLElement>,
  startTime: number,
  type: SliderCaptchaTrackPoint['type'],
): SliderCaptchaTrackPoint {
  const pagePoint = getPagePoint(event);
  return {
    x: pagePoint.x,
    y: pagePoint.y,
    t: Date.now() - startTime,
    type,
  };
}

/**
 * 自定义滑块验证码弹层。
 *
 * @param props 组件属性
 * @returns React 节点
 */
const SliderCaptchaModal: React.FC<SliderCaptchaModalProps> = ({
  open,
  onCancel,
  onVerified,
  isDark = false,
  zIndex = 1400,
}) => {
  const { styles, cx } = useStyles({ isDark, zIndex });
  const previewRef = useRef<HTMLDivElement | null>(null);
  const pieceImageRef = useRef<HTMLImageElement | null>(null);
  const dragContextRef = useRef<DragContext | null>(null);
  const reloadTimerRef = useRef<number | null>(null);
  const handlePointerMoveRef = useRef<(event: PointerEvent) => void>(() => {});
  const handlePointerUpRef = useRef<(event: PointerEvent) => void | Promise<void>>(() => {});
  const pieceOffsetRef = useRef(0);
  const handleOffsetRef = useRef(0);
  const [challenge, setChallenge] = useState<SliderCaptchaChallenge | null>(null);
  const [metrics, setMetrics] = useState<CaptchaMetrics | null>(null);
  const [loadedImageCount, setLoadedImageCount] = useState(0);
  const [pieceOffset, setPieceOffset] = useState(0);
  const [handleOffset, setHandleOffset] = useState(0);
  const [stage, setStage] = useState<SliderCaptchaStage>('idle');
  const [statusMessage, setStatusMessage] = useState('拖动下方按钮，将拼图移动到正确位置');

  /**
   * 当前是否处于加载或校验状态。
   */
  const blocked = stage === 'loading' || stage === 'checking';

  /**
   * 当前背景图纵横比。
   */
  const previewAspectRatio = useMemo(() => {
    if (!challenge?.backgroundImageWidth || !challenge?.backgroundImageHeight) {
      return 5 / 3;
    }
    return challenge.backgroundImageWidth / challenge.backgroundImageHeight;
  }, [challenge?.backgroundImageHeight, challenge?.backgroundImageWidth]);

  /**
   * 重置拖动状态。
   */
  const resetDragState = useCallback(() => {
    dragContextRef.current = null;
    pieceOffsetRef.current = 0;
    handleOffsetRef.current = 0;
    setPieceOffset(0);
    setHandleOffset(0);
  }, []);

  /**
   * 清理自动刷新定时器。
   */
  const clearReloadTimer = useCallback(() => {
    if (reloadTimerRef.current !== null) {
      window.clearTimeout(reloadTimerRef.current);
      reloadTimerRef.current = null;
    }
  }, []);

  /**
   * 处理拖动过程。
   */
  const handlePointerMove = useCallback((event: PointerEvent) => {
    handlePointerMoveRef.current(event);
  }, []);

  /**
   * 处理拖动结束。
   */
  const handlePointerUp = useCallback((event: PointerEvent) => {
    void handlePointerUpRef.current(event);
  }, []);

  /**
   * 卸载拖动监听器。
   */
  const detachPointerListeners = useCallback(() => {
    document.removeEventListener('pointermove', handlePointerMove);
    document.removeEventListener('pointerup', handlePointerUp);
    document.removeEventListener('pointercancel', handlePointerUp);
  }, [handlePointerMove, handlePointerUp]);

  /**
   * 重新测量验证码区域尺寸。
   */
  const measureCaptchaMetrics = useCallback(() => {
    const previewElement = previewRef.current;
    const pieceImageElement = pieceImageRef.current;
    if (!previewElement || !pieceImageElement) {
      return;
    }
    const nextPreviewWidth = previewElement.clientWidth;
    const nextPreviewHeight = previewElement.clientHeight;
    const nextPieceWidth = pieceImageElement.clientWidth;
    const nextPieceHeight = pieceImageElement.clientHeight;
    if (!nextPreviewWidth || !nextPreviewHeight || !nextPieceWidth || !nextPieceHeight) {
      return;
    }
    setMetrics({
      previewWidth: nextPreviewWidth,
      previewHeight: nextPreviewHeight,
      pieceWidth: nextPieceWidth,
      pieceHeight: nextPieceHeight,
      maxPieceOffset: Math.max(nextPreviewWidth - nextPieceWidth, 0),
    });
  }, []);

  /**
   * 拉取新的 challenge。
   */
  const loadChallenge = useCallback(async () => {
    clearReloadTimer();
    detachPointerListeners();
    resetDragState();
    setChallenge(null);
    setMetrics(null);
    setLoadedImageCount(0);
    setStage('loading');
    setStatusMessage('正在加载新的安全校验拼图...');
    const response = await getSliderCaptcha();
    if (response.code !== 200 || !response.data) {
      setStage('error');
      setStatusMessage(response.msg || '验证码加载失败，请点击刷新后重试');
      return;
    }
    setChallenge(response.data);
    setStage('idle');
    setStatusMessage('拖动下方按钮，将拼图移动到正确位置');
  }, [clearReloadTimer, detachPointerListeners, resetDragState]);

  /**
   * 提交滑块轨迹到后端。
   *
   * @param trackPayload 轨迹数据
   */
  const submitTrack = useCallback(
    async (trackPayload: SliderCaptchaTrackPayload) => {
      if (!challenge) {
        return;
      }
      setStage('checking');
      setStatusMessage('正在校验你的拖动轨迹，请稍候...');
      const response = await checkSliderCaptcha({
        id: challenge.id,
        data: trackPayload,
      } satisfies SliderCaptchaCheckPayload);
      if (response.code === 200 && response.data?.id) {
        setStage('success');
        setStatusMessage('校验成功，正在继续执行...');
        onVerified(response.data);
        return;
      }
      setStage('error');
      setStatusMessage(response.msg || '校验失败，正在刷新新的拼图...');
      clearReloadTimer();
      reloadTimerRef.current = window.setTimeout(() => {
        void loadChallenge();
      }, RELOAD_DELAY_MS);
    },
    [challenge, clearReloadTimer, loadChallenge, onVerified],
  );

  useEffect(() => {
    handlePointerMoveRef.current = (event: PointerEvent) => {
      const dragContext = dragContextRef.current;
      if (!dragContext || !metrics) {
        return;
      }
      if (event.pointerId !== dragContext.pointerId) {
        return;
      }
      const pagePoint = getPagePoint(event);
      const deltaX = pagePoint.x - dragContext.startPageX;
      const nextHandleOffset = clamp(
        dragContext.startHandleOffset + deltaX,
        0,
        metrics.maxPieceOffset,
      );
      const nextPieceOffset = nextHandleOffset;
      handleOffsetRef.current = nextHandleOffset;
      pieceOffsetRef.current = nextPieceOffset;
      setHandleOffset(nextHandleOffset);
      setPieceOffset(nextPieceOffset);
      dragContext.trackList.push(buildTrackPoint(event, dragContext.startTime, 'move'));
    };
  }, [metrics]);

  useEffect(() => {
    handlePointerUpRef.current = async (event: PointerEvent) => {
      const dragContext = dragContextRef.current;
      if (!dragContext || !metrics || !challenge) {
        return;
      }
      if (event.pointerId !== dragContext.pointerId) {
        return;
      }
      const currentTrackPoint = buildTrackPoint(event, dragContext.startTime, 'up');
      dragContext.trackList.push(currentTrackPoint);
      dragContextRef.current = null;
      detachPointerListeners();
      const trackPayload: SliderCaptchaTrackPayload = {
        bgImageWidth: Math.round(metrics.previewWidth),
        bgImageHeight: Math.round(metrics.previewHeight),
        templateImageWidth: Math.round(metrics.pieceWidth),
        templateImageHeight: Math.round(metrics.pieceHeight),
        startTime: dragContext.startTime,
        stopTime: dragContext.startTime + currentTrackPoint.t,
        left: Math.round(pieceOffsetRef.current),
        top: 0,
        trackList: dragContext.trackList,
        data: challenge.data,
      };
      await submitTrack(trackPayload);
    };
  }, [challenge, detachPointerListeners, metrics, submitTrack]);

  /**
   * 开始拖动滑块。
   *
   * @param event 指针按下事件
   */
  const handlePointerDown = useCallback(
    (event: React.PointerEvent<HTMLButtonElement>) => {
      if (!challenge || !metrics || blocked) {
        return;
      }
      event.preventDefault();
      clearReloadTimer();
      const startTime = Date.now();
      dragContextRef.current = {
        pointerId: event.pointerId,
        startPageX: getPagePoint(event).x,
        startTime,
        startHandleOffset: handleOffsetRef.current,
        trackList: [buildTrackPoint(event, startTime, 'down')],
      };
      setStage('idle');
      setStatusMessage('继续拖动，完成后系统会自动校验');
      document.addEventListener('pointermove', handlePointerMove);
      document.addEventListener('pointerup', handlePointerUp);
      document.addEventListener('pointercancel', handlePointerUp);
    },
    [blocked, challenge, clearReloadTimer, handlePointerMove, handlePointerUp, metrics],
  );

  /**
   * 图片加载完成后的处理逻辑。
   */
  const handleImageLoaded = useCallback(() => {
    setLoadedImageCount((currentCount) => currentCount + 1);
  }, []);

  /**
   * 刷新 challenge。
   */
  const handleRefresh = useCallback(() => {
    void loadChallenge();
  }, [loadChallenge]);

  /**
   * 关闭弹层。
   */
  const handleClose = useCallback(() => {
    clearReloadTimer();
    detachPointerListeners();
    resetDragState();
    onCancel();
  }, [clearReloadTimer, detachPointerListeners, onCancel, resetDragState]);

  useEffect(() => {
    if (!open) {
      clearReloadTimer();
      detachPointerListeners();
      resetDragState();
      setChallenge(null);
      setMetrics(null);
      setLoadedImageCount(0);
      setStage('idle');
      setStatusMessage('拖动下方按钮，将拼图移动到正确位置');
      return;
    }
    void loadChallenge();
    return () => {
      clearReloadTimer();
      detachPointerListeners();
    };
  }, [clearReloadTimer, detachPointerListeners, loadChallenge, open, resetDragState]);

  useEffect(() => {
    if (!open || loadedImageCount < 2) {
      return;
    }
    measureCaptchaMetrics();
    const previewElement = previewRef.current;
    if (!previewElement || typeof ResizeObserver === 'undefined') {
      return;
    }
    const resizeObserver = new ResizeObserver(() => {
      measureCaptchaMetrics();
    });
    resizeObserver.observe(previewElement);
    return () => {
      resizeObserver.disconnect();
    };
  }, [loadedImageCount, measureCaptchaMetrics, open]);

  if (!open) {
    return null;
  }

  const modalNode = (
    <div className={styles.mask} onClick={handleClose}>
      <div className={styles.panel} onClick={(event) => event.stopPropagation()}>
        <div className={styles.header}>
          <div className={styles.headerLeft}>
            <div className={styles.badge}>
              <SafetyCertificateOutlined />
            </div>
            <div className={styles.titleWrap}>
              <h3 className={styles.title}>滑块验证</h3>
              <p className={styles.subtitle}>拖动滑块完成拼图后继续操作</p>
            </div>
          </div>
          <div className={styles.headerActions}>
            <Button
              aria-label="刷新验证码"
              className={styles.actionButton}
              icon={<ReloadOutlined />}
              onClick={handleRefresh}
              type="text"
            />
            <Button
              aria-label="关闭验证码"
              className={styles.actionButton}
              icon={<CloseOutlined />}
              onClick={handleClose}
              type="text"
            />
          </div>
        </div>

        <div className={styles.body}>
          <div className={styles.statusBar}>
            <div
              className={cx(
                styles.statusText,
                stage === 'success' && styles.successText,
                stage === 'error' && styles.errorText,
              )}
            >
              {statusMessage}
            </div>
          </div>

          <div className={styles.previewShell}>
            <div
              className={styles.previewFrame}
              ref={previewRef}
              style={{
                aspectRatio: `${previewAspectRatio}`,
                maxWidth: PREVIEW_WIDTH,
                margin: '0 auto',
              }}
            >
              {challenge ? (
                <>
                  <img
                    alt="验证码背景图"
                    className={styles.previewImage}
                    onLoad={handleImageLoaded}
                    src={challenge.backgroundImage}
                  />
                  <div
                    className={styles.pieceLayer}
                    style={{
                      transform: `translate3d(${pieceOffset}px, 0, 0)`,
                    }}
                  >
                    <img
                      alt="验证码拼图块"
                      className={styles.pieceImage}
                      onLoad={handleImageLoaded}
                      ref={pieceImageRef}
                      src={challenge.templateImage}
                    />
                  </div>
                  <div className={styles.previewOverlay} />
                </>
              ) : null}

              {stage === 'loading' ? (
                <div className={styles.loadingCover}>
                  <Spin size="large" />
                </div>
              ) : null}
            </div>

            <div className={styles.sliderArea}>
              <div className={styles.sliderTrack}>
                <div
                  className={styles.sliderFill}
                  style={{
                    width: `${handleOffset + HANDLE_WIDTH * 0.78}px`,
                  }}
                />
                <div className={styles.sliderHint}>
                  <span>按住滑块，平滑拖动完成拼图</span>
                </div>
                <button
                  className={styles.sliderHandle}
                  disabled={blocked || !challenge || !metrics}
                  onPointerDown={handlePointerDown}
                  style={{
                    transform: `translate3d(${handleOffset}px, 0, 0)`,
                  }}
                  type="button"
                >
                  <ArrowRightOutlined />
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );

  if (typeof document === 'undefined') {
    return modalNode;
  }

  return createPortal(modalNode, document.body);
};

export default SliderCaptchaModal;
