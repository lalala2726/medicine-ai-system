import { AiEditor, type AiEditorOptions } from 'aieditor';
import 'aieditor/dist/style.css';

import React, { forwardRef, type HTMLAttributes, useEffect, useMemo, useRef } from 'react';
import { upload } from '@/api/core/file';
import { useThemeContext } from '@/contexts/ThemeContext';
import './index.css';

/**
 * 富文本编辑器支持的内容格式。
 */
export type RichTextContentFormat = 'html' | 'markdown';

/**
 * 富文本编辑器允许透传的增强配置。
 * 这些配置不会覆盖封装层托管的主题、AI、上传与内容同步逻辑。
 */
export type RichTextEditorOptions = Omit<
  AiEditorOptions,
  | 'ai'
  | 'attachment'
  | 'content'
  | 'contentIsMarkdown'
  | 'editable'
  | 'element'
  | 'image'
  | 'onChange'
  | 'textSelectionBubbleMenu'
  | 'theme'
  | 'uploader'
  | 'video'
>;

/**
 * 富文本编辑器组件属性。
 */
export interface RichTextEditorProps extends Omit<HTMLAttributes<HTMLDivElement>, 'onChange'> {
  /** 占位提示文案 */
  placeholder?: string;
  /** 默认内容 */
  defaultValue?: string;
  /** 受控内容 */
  value?: string;
  /** 内容变化回调 */
  onChange?: (value: string) => void;
  /** 编辑器高度 */
  height?: number | string;
  /** 是否允许编辑 */
  editable?: boolean;
  /** 内容格式 */
  contentFormat?: RichTextContentFormat;
  /** 是否开启本地内容暂存 */
  contentRetention?: boolean;
  /** 编辑器初始化完成回调 */
  onReady?: (instance: AiEditor) => void;
  /** 编辑器增强配置 */
  options?: RichTextEditorOptions;
}

/**
 * 默认工具栏配置。
 */
const DEFAULT_TOOLBAR_KEYS: NonNullable<AiEditorOptions['toolbarKeys']> = [
  'undo',
  'redo',
  'brush',
  'eraser',
  '|',
  'heading',
  'font-family',
  'font-size',
  '|',
  'bold',
  'italic',
  'underline',
  'strike',
  'link',
  'code',
  'subscript',
  'superscript',
  'hr',
  'todo',
  'emoji',
  '|',
  'highlight',
  'font-color',
  '|',
  'align',
  'line-height',
  '|',
  'bullet-list',
  'ordered-list',
  'indent-decrease',
  'indent-increase',
  'break',
  '|',
  'image',
  'video',
  'attachment',
  'quote',
  'code-block',
  'table',
  '|',
  'source-code',
  'printer',
  'fullscreen',
];

/**
 * 默认文本选择气泡菜单配置。
 */
const DEFAULT_TEXT_SELECTION_BUBBLE_ITEMS = ['bold', 'italic', 'underline', 'strike', 'code'];

/**
 * 需要强制移除的 AI 相关工具栏 key。
 */
const AI_TOOLBAR_KEYS = new Set(['ai']);

/**
 * 将上传成功结果转换为 aieditor 需要的结构。
 * @param data 上传成功数据。
 * @returns 上传成功响应。
 */
function buildUploadSuccess<T extends Record<string, unknown>>(data: T) {
  return { errorCode: 0, data } as const;
}

/**
 * 将上传失败结果转换为 aieditor 需要的结构。
 * @param message 错误文案。
 * @returns 上传失败响应。
 */
function buildUploadFailure(message = 'upload failed') {
  return { errorCode: 500, message } as const;
}

/**
 * 过滤工具栏中的 AI 能力入口。
 * @param toolbarKeys 原始工具栏配置。
 * @returns 清洗后的工具栏配置。
 */
function sanitizeToolbarKeys(
  toolbarKeys: NonNullable<AiEditorOptions['toolbarKeys']>,
): NonNullable<AiEditorOptions['toolbarKeys']> {
  return toolbarKeys.reduce<NonNullable<AiEditorOptions['toolbarKeys']>>((result, item) => {
    if (typeof item === 'string') {
      if (AI_TOOLBAR_KEYS.has(item)) {
        return result;
      }
      result.push(item);
      return result;
    }

    if (
      item &&
      typeof item === 'object' &&
      'toolbarKeys' in item &&
      Array.isArray(item.toolbarKeys)
    ) {
      result.push({
        ...item,
        toolbarKeys: sanitizeToolbarKeys(item.toolbarKeys),
      });
      return result;
    }

    result.push(item);
    return result;
  }, []);
}

/**
 * 构建图片上传适配器。
 * @param file 图片文件。
 * @returns 上传结果。
 */
async function uploadImage(file: File) {
  try {
    const result = await upload(file);
    const url = result?.fileUrl;

    if (!url) {
      return buildUploadFailure('no image url');
    }

    return buildUploadSuccess({ src: url });
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : 'image upload error';
    return buildUploadFailure(errorMessage);
  }
}

/**
 * 构建视频上传适配器。
 * @param file 视频文件。
 * @returns 上传结果。
 */
async function uploadVideo(file: File) {
  try {
    const result = await upload(file);
    const url = result?.fileUrl;

    if (!url) {
      return buildUploadFailure('no video url');
    }

    return buildUploadSuccess({ src: url });
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : 'video upload error';
    return buildUploadFailure(errorMessage);
  }
}

/**
 * 构建附件上传适配器。
 * @param file 附件文件。
 * @returns 上传结果。
 */
async function uploadAttachment(file: File) {
  try {
    const result = await upload(file);
    const url = result?.fileUrl;
    const fileName = result?.fileName || file.name;

    if (!url) {
      return buildUploadFailure('no attachment url');
    }

    return buildUploadSuccess({ href: url, fileName });
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : 'attachment upload error';
    return buildUploadFailure(errorMessage);
  }
}

/**
 * 获取编辑器当前内容。
 * @param editor 编辑器实例。
 * @param contentFormat 内容格式。
 * @returns 当前内容字符串。
 */
function getEditorContent(editor: AiEditor, contentFormat: RichTextContentFormat): string {
  return contentFormat === 'markdown' ? String(editor.getMarkdown() || '') : editor.getHtml();
}

/**
 * 将内容写入编辑器。
 * @param editor 编辑器实例。
 * @param content 内容字符串。
 * @param contentFormat 内容格式。
 * @returns 无返回值。
 */
function setEditorContent(
  editor: AiEditor,
  content: string,
  contentFormat: RichTextContentFormat,
): void {
  if (contentFormat === 'markdown') {
    editor.setMarkdownContent(content);
    return;
  }

  editor.setContent(content);
}

/**
 * 富文本编辑器组件。
 * @param props 组件属性。
 * @param ref 容器节点引用。
 * @returns 富文本编辑器节点。
 */
export const RichTextEditor = forwardRef<HTMLDivElement, RichTextEditorProps>(
  function RichTextEditor(
    {
      placeholder = '',
      defaultValue,
      value,
      onChange,
      height = 600,
      editable = true,
      contentFormat = 'markdown',
      contentRetention = false,
      onReady,
      options,
      style,
      className,
      ...props
    },
    ref,
  ) {
    const { isDark } = useThemeContext();
    const hostRef = useRef<HTMLDivElement>(null);
    const editorRef = useRef<AiEditor | null>(null);
    const onChangeRef = useRef<typeof onChange>(onChange);
    const onReadyRef = useRef<typeof onReady>(onReady);
    const currentThemeRef = useRef<'dark' | 'light'>('light');

    /**
     * 当前编辑器主题。
     */
    const currentTheme = isDark ? 'dark' : 'light';

    /**
     * 清洗后的工具栏配置。
     */
    const toolbarKeys = useMemo(
      () => sanitizeToolbarKeys(options?.toolbarKeys || DEFAULT_TOOLBAR_KEYS),
      [options?.toolbarKeys],
    );

    /**
     * 工具栏额外排除项。
     */
    const toolbarExcludeKeys = useMemo(
      () => Array.from(new Set([...(options?.toolbarExcludeKeys || []), 'ai'])),
      [options?.toolbarExcludeKeys],
    );

    /**
     * 允许透传的编辑器增强配置。
     */
    const editorOptions = useMemo<RichTextEditorOptions>(() => ({ ...(options || {}) }), [options]);

    useEffect(() => {
      if (!ref) {
        return;
      }

      if (typeof ref === 'function') {
        ref(hostRef.current);
        return;
      }

      ref.current = hostRef.current;
    }, [ref]);

    useEffect(() => {
      onChangeRef.current = onChange;
    }, [onChange]);

    useEffect(() => {
      onReadyRef.current = onReady;
    }, [onReady]);

    useEffect(() => {
      currentThemeRef.current = currentTheme;
    }, [currentTheme]);

    useEffect(() => {
      if (!hostRef.current) {
        return;
      }

      const initialContent = value ?? defaultValue ?? '';

      editorRef.current = new AiEditor({
        ...editorOptions,
        element: hostRef.current,
        content: initialContent,
        contentIsMarkdown: contentFormat === 'markdown',
        contentRetention,
        editable,
        placeholder,
        theme: currentThemeRef.current,
        toolbarKeys,
        toolbarExcludeKeys,
        textSelectionBubbleMenu: {
          enable: true,
          items: DEFAULT_TEXT_SELECTION_BUBBLE_ITEMS,
        },
        image: {
          uploader: uploadImage,
        },
        video: {
          uploader: uploadVideo,
        },
        attachment: {
          uploader: uploadAttachment,
        },
        onChange: (editor) => {
          const nextValue = getEditorContent(editor, contentFormat);
          onChangeRef.current?.(nextValue);
        },
      });

      onReadyRef.current?.(editorRef.current);

      return () => {
        if (!editorRef.current) {
          return;
        }

        editorRef.current.destroy();
        editorRef.current = null;
      };
    }, [
      contentFormat,
      contentRetention,
      defaultValue,
      editorOptions,
      placeholder,
      toolbarExcludeKeys,
      toolbarKeys,
    ]);

    useEffect(() => {
      if (!editorRef.current) {
        return;
      }

      editorRef.current.changeTheme(currentTheme);
    }, [currentTheme]);

    useEffect(() => {
      if (!editorRef.current) {
        return;
      }

      editorRef.current.setEditable(editable);
    }, [editable]);

    useEffect(() => {
      if (!editorRef.current || value === undefined) {
        return;
      }

      const currentContent = getEditorContent(editorRef.current, contentFormat);

      if (value === currentContent) {
        return;
      }

      setEditorContent(editorRef.current, value, contentFormat);
    }, [contentFormat, value]);

    /**
     * 归一化后的编辑器高度。
     */
    const normalizedHeight = typeof height === 'number' ? `${height}px` : height;

    return (
      <div
        ref={hostRef}
        className={`ai-editor-host ${className || ''}`.trim()}
        style={{
          height: normalizedHeight,
          ...style,
        }}
        {...props}
      />
    );
  },
);

RichTextEditor.displayName = 'RichTextEditor';

export default RichTextEditor;
