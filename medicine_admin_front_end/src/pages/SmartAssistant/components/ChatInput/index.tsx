/**
 * ChatInput — 自定义聊天输入框组件
 *
 * 替代 @ant-design/x 的 Sender 组件 + 独立 Upload 面板。
 * 核心特性：
 *   - 输入框右侧 "+" 按钮 → Popover 弹出"上传图片"
 *   - 图片缩略图在输入框内部上方展示，带 × 删除
 *   - Enter 发送 / Shift+Enter 换行
 *   - 语音录入 / 发送 / 加载状态按钮切换
 */
import {
  ArrowUpOutlined,
  AudioOutlined,
  CloseOutlined,
  LoadingOutlined,
  PictureOutlined,
  PlusOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons';
import { Popover, Spin, message } from 'antd';
import type { UploadFile } from 'antd/es/upload/interface';
import React, { useCallback, useEffect, useLayoutEffect, useRef, useState } from 'react';
import { upload, type FileUploadTypes } from '@/api/core/file';
import styles from './index.module.less';

/** 聊天图片最多上传数量 */
const DEFAULT_MAX_IMAGE_COUNT = 5;
/** 流式输出期间回车提示文案。 */
const WAIT_FOR_STREAM_HINT = '请等待当前消息输出完成';

/** 输入框布局模式。 */
export type ChatInputLayoutMode = 'centered' | 'docked';

export interface ChatInputProps {
  /** 输入框文本（受控） */
  value: string;
  /** 文本变更回调 */
  onChange: (value: string) => void;
  /** 发送消息回调（文本 + 已上传图片 URL） */
  onSubmit: (text: string) => void;
  /** 取消/停止生成 */
  onCancel?: () => void;
  /** 是否正在加载（streaming 中） */
  loading?: boolean;
  /** 是否正在提交 */
  submitting?: boolean;
  /** 是否禁用 */
  disabled?: boolean;
  /** 占位文本 */
  placeholder?: string;
  /** 图片文件列表（受控） */
  imageFileList: UploadFile[];
  /** 图片文件列表变更回调 */
  onImageFileListChange: (fileList: UploadFile[]) => void;
  /** 最大图片数量 */
  maxImageCount?: number;
  /** 当前模型是否允许上传图片 */
  imageUploadEnabled?: boolean;
  /** 图片上传被禁用时的提示文案 */
  imageUploadDisabledMessage?: string;
  /** 是否有可发送内容（文本 + 图片），由外部传入 */
  hasContent?: boolean;
  /** 输入框布局模式 */
  layoutMode: ChatInputLayoutMode;

  // 语音录入相关
  /** 是否正在录音 */
  recording?: boolean;
  /** 录音状态变更回调 */
  onRecordingChange?: (recording: boolean) => void;
  /** STT 正在加载 */
  sttLoading?: boolean;

  // 深度思考相关
  /** 当前选中模型是否支持深度思考；为 false 时不显示按钮 */
  showDeepThinking?: boolean;
  /** 深度思考开关当前状态 */
  deepThinking?: boolean;
  /** 深度思考状态变更回调 */
  onDeepThinkingChange?: (v: boolean) => void;
}

/**
 * 获取图片文件的预览 URL。
 *
 * @param file antd UploadFile
 * @returns 可用于 img src 的 URL
 */
function getImagePreviewUrl(file: UploadFile): string | undefined {
  // 已从服务器返回的 url
  if (file.url) return file.url;
  // 服务器响应中的 fileUrl
  const response = file.response as FileUploadTypes.FileUploadVo | undefined;
  if (response?.fileUrl) return response.fileUrl;
  // 本地 blob 预览
  if (file.originFileObj) {
    return URL.createObjectURL(file.originFileObj);
  }
  return undefined;
}

const ChatInput: React.FC<ChatInputProps> = ({
  value,
  onChange,
  onSubmit,
  onCancel,
  loading = false,
  submitting = false,
  disabled = false,
  placeholder = '随便问我什么……',
  imageFileList,
  onImageFileListChange,
  maxImageCount = DEFAULT_MAX_IMAGE_COUNT,
  imageUploadEnabled = true,
  imageUploadDisabledMessage = '此模型不支持图片理解',
  hasContent = false,
  layoutMode,
  recording = false,
  onRecordingChange,
  sttLoading = false,
  showDeepThinking = false,
  deepThinking = false,
  onDeepThinkingChange,
}) => {
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [popoverOpen, setPopoverOpen] = useState(false);

  // 用 ref 保存最新的 imageFileList，在异步上传回调中读取
  const imageFileListRef = useRef(imageFileList);
  useEffect(() => {
    imageFileListRef.current = imageFileList;
  });

  // 组件加载后自动聚焦
  useEffect(() => {
    textareaRef.current?.focus();
  }, []);

  /** 自适应 textarea 高度 */
  const adjustTextareaHeight = useCallback(() => {
    const el = textareaRef.current;
    if (!el) return;
    el.style.height = 'auto';
    el.style.height = `${el.scrollHeight}px`;
  }, []);

  useLayoutEffect(() => {
    adjustTextareaHeight();
  }, [value, adjustTextareaHeight]);

  /**
   * 保持输入框焦点，避免发送后需要再次手动点击。
   */
  const focusTextarea = useCallback(() => {
    requestAnimationFrame(() => {
      textareaRef.current?.focus();
    });
  }, []);

  /** 处理发送 */
  const handleSend = useCallback(() => {
    if (disabled) {
      focusTextarea();
      return;
    }
    if (loading || submitting) {
      if (hasContent) {
        message.info(WAIT_FOR_STREAM_HINT);
      }
      focusTextarea();
      return;
    }
    if (!hasContent) {
      focusTextarea();
      return;
    }

    onSubmit(value);
    focusTextarea();
  }, [disabled, focusTextarea, hasContent, loading, onSubmit, submitting, value]);

  /** 处理键盘事件 */
  const handleKeyDown = useCallback(
    (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
      if (e.key === 'Enter' && !e.shiftKey && !e.nativeEvent.isComposing) {
        e.preventDefault();
        handleSend();
      }
    },
    [handleSend],
  );

  /** 处理文本变更 */
  const handleChange = useCallback(
    (e: React.ChangeEvent<HTMLTextAreaElement>) => {
      onChange(e.target.value);
    },
    [onChange],
  );

  /** 删除某张图片 */
  const handleRemoveImage = useCallback(
    (uid: string) => {
      onImageFileListChange(imageFileListRef.current.filter((f) => f.uid !== uid));
    },
    [onImageFileListChange],
  );

  /** 点击 Popover 中的"上传图片" */
  const handleUploadImageClick = useCallback(() => {
    if (!imageUploadEnabled) {
      setPopoverOpen(false);
      message.warning(imageUploadDisabledMessage);
      return;
    }
    setPopoverOpen(false);
    fileInputRef.current?.click();
  }, [imageUploadDisabledMessage, imageUploadEnabled]);

  /** 处理文件选择 */
  const handleFileInputChange = useCallback(
    async (e: React.ChangeEvent<HTMLInputElement>) => {
      if (!imageUploadEnabled) {
        message.warning(imageUploadDisabledMessage);
        e.target.value = '';
        return;
      }
      const { files } = e.target;
      if (!files || files.length === 0) return;

      const currentList = imageFileListRef.current;
      const remainingSlots = maxImageCount - currentList.length;
      const filesToUpload = Array.from(files).slice(0, remainingSlots);
      if (filesToUpload.length === 0) return;

      // 为每个文件创建一个 UploadFile 添加到列表（标记为上传中）
      const newFileItems: UploadFile[] = filesToUpload.map((file, index) => ({
        uid: `upload-${Date.now()}-${index}`,
        name: file.name,
        status: 'uploading' as const,
        originFileObj: file as any,
      }));

      onImageFileListChange([...currentList, ...newFileItems]);

      // 逐个上传，每完成一个立即更新列表
      for (let i = 0; i < filesToUpload.length; i++) {
        const file = filesToUpload[i];
        const uid = newFileItems[i].uid;
        try {
          const result = (await upload(file)) as FileUploadTypes.FileUploadVo;
          // 从 ref 取最新列表并更新
          const latestList = imageFileListRef.current;
          onImageFileListChange(
            latestList.map((f) =>
              f.uid === uid
                ? { ...f, status: 'done' as const, url: result.fileUrl, response: result }
                : f,
            ),
          );
        } catch {
          // 上传失败，从列表移除
          const latestList = imageFileListRef.current;
          onImageFileListChange(latestList.filter((f) => f.uid !== uid));
        }
      }

      // 重置 input 的 value 以允许重复选择同一文件
      e.target.value = '';
    },
    [imageUploadDisabledMessage, imageUploadEnabled, maxImageCount, onImageFileListChange],
  );

  /** 渲染右侧操作按钮 */
  const renderActionButton = () => {
    // 加载中/录音中 → 停止按钮
    const isBusy = submitting || sttLoading || loading || recording;

    return (
      <>
        {/* 语音按钮 (如果正在录音，则是高亮脉冲态，点击停止) */}
        {recording ? (
          <button
            type="button"
            className={styles.speechBtnRecording}
            onClick={() => onRecordingChange?.(false)}
            title="停止录音"
          >
            <AudioOutlined />
          </button>
        ) : (
          <button
            type="button"
            className={styles.speechBtn}
            onClick={() => onRecordingChange?.(true)}
            title="语音输入"
            disabled={isBusy || disabled}
          >
            <AudioOutlined />
          </button>
        )}

        {/* 发送/停止按钮 */}
        {isBusy ? (
          <button
            type="button"
            className={styles.stopBtn}
            onClick={recording ? () => onRecordingChange?.(false) : onCancel}
            title={recording ? '停止录音' : '停止生成'}
          >
            <span className={styles.stopIconInner} />
          </button>
        ) : (
          <button
            type="button"
            className={`${styles.sendBtn} ${!hasContent || disabled ? styles.sendBtnDisabled : ''}`}
            onClick={handleSend}
            disabled={!hasContent || disabled}
            title="发送"
          >
            <ArrowUpOutlined />
          </button>
        )}
      </>
    );
  };

  /** Popover 菜单内容 */
  const popoverContent = (
    <div className={styles.popoverMenu}>
      <button
        type="button"
        className={styles.popoverMenuItem}
        onClick={handleUploadImageClick}
        disabled={!imageUploadEnabled || imageFileList.length >= maxImageCount}
      >
        <PictureOutlined className={styles.popoverMenuIcon} />
        <span>上传图片</span>
      </button>
    </div>
  );

  const hasImages = imageFileList.length > 0;
  /** 输入框仅受外部显式 disabled 控制，发送中允许继续输入。 */
  const isInputDisabled = disabled;
  const chatInputWrapperClassName = `${styles.chatInputWrapper} ${
    layoutMode === 'centered' ? styles.centeredMode : styles.dockedMode
  }`;

  return (
    <div className={chatInputWrapperClassName}>
      {/* 图片预览区 */}
      {hasImages && (
        <div className={styles.imagePreviewArea}>
          {imageFileList.map((file) => {
            const previewUrl = getImagePreviewUrl(file);
            const isUploading = file.status === 'uploading';
            return (
              <div
                key={file.uid}
                className={`${styles.imagePreviewItem} ${isUploading ? styles.uploading : ''}`}
              >
                {previewUrl && <img src={previewUrl} alt={file.name} />}
                {isUploading && (
                  <div className={styles.uploadingSpinner}>
                    <Spin indicator={<LoadingOutlined />} size="small" />
                  </div>
                )}
                {!isUploading && (
                  <button
                    type="button"
                    className={styles.imageDeleteBtn}
                    onClick={() => handleRemoveImage(file.uid)}
                    title="移除图片"
                  >
                    <CloseOutlined />
                  </button>
                )}
              </div>
            );
          })}
        </div>
      )}

      {/* 输入区 */}
      <div className={styles.inputArea}>
        <textarea
          ref={textareaRef}
          className={styles.textarea}
          value={value}
          onChange={handleChange}
          onKeyDown={handleKeyDown}
          placeholder={placeholder}
          disabled={isInputDisabled}
          rows={1}
        />

        <div className={styles.bottomToolbar}>
          {/* 左侧操作区 */}
          <div className={styles.leftActions}>
            {/* "+" 按钮 */}
            <Popover
              content={popoverContent}
              trigger="click"
              placement="topLeft"
              open={imageUploadEnabled ? popoverOpen : false}
              onOpenChange={(nextOpen) => {
                if (!imageUploadEnabled && nextOpen) {
                  message.warning(imageUploadDisabledMessage);
                  setPopoverOpen(false);
                  return;
                }
                setPopoverOpen(nextOpen);
              }}
              arrow={false}
              overlayInnerStyle={{
                padding: 4,
                borderRadius: 12,
                border: 'none',
                boxShadow: '0 6px 16px 0 rgba(0, 0, 0, 0.08)',
              }}
            >
              <button
                type="button"
                className={`${styles.addBtn} ${!imageUploadEnabled ? styles.addBtnDisabledVisual : ''}`}
                aria-disabled={!imageUploadEnabled || isInputDisabled}
                disabled={isInputDisabled}
                title={imageUploadEnabled ? '更多操作' : imageUploadDisabledMessage}
              >
                <PlusOutlined />
              </button>
            </Popover>

            {/* 深度思考按钮（仅当模型支持时显示） */}
            {showDeepThinking && (
              <button
                type="button"
                className={`${styles.deepThinkingBtn} ${deepThinking ? styles.deepThinkingBtnActive : ''}`}
                onClick={() => onDeepThinkingChange?.(!deepThinking)}
                title={deepThinking ? '关闭深度思考' : '开启深度思考'}
              >
                <ThunderboltOutlined className={styles.deepThinkingIcon} />
                深度思考
              </button>
            )}
          </div>

          {/* 右侧操作区 */}
          <div className={styles.actionButtons}>
            {/* 发送 / 语音 / 加载按钮 */}
            {renderActionButton()}
          </div>
        </div>
      </div>

      {/* 隐藏的 file input */}
      <input
        ref={fileInputRef}
        type="file"
        accept="image/*"
        multiple
        style={{ display: 'none' }}
        onChange={handleFileInputChange}
      />
    </div>
  );
};

export default ChatInput;
