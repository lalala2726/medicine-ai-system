import React from 'react'
import { cjk } from '@streamdown/cjk'
import { code } from '@streamdown/code'
import { createMathPlugin } from '@streamdown/math'
import { mermaid } from '@streamdown/mermaid'
import { Streamdown, type ControlsConfig, type StreamdownProps } from 'streamdown'
import './index.css'

/** MarkdownRenderer 组件属性。 */
interface MarkdownRendererProps {
  /** 待渲染的 Markdown 文本内容。 */
  content: string
  /** Streamdown 渲染模式。 */
  mode?: StreamdownProps['mode']
  /** Streamdown 控制项配置。 */
  controls?: ControlsConfig
  /** 是否展示代码块行号。 */
  lineNumbers?: StreamdownProps['lineNumbers']
  /** 根节点附加类名。 */
  className?: string
}

/** MarkdownRenderer 默认使用的 Streamdown 渲染模式。 */
const DEFAULT_MARKDOWN_RENDERER_MODE: NonNullable<StreamdownProps['mode']> = 'static'

/** MarkdownRenderer 默认使用的 Shiki 主题配置。 */
const DEFAULT_MARKDOWN_RENDERER_SHIKI_THEME: NonNullable<StreamdownProps['shikiTheme']> = [
  'github-light',
  'github-dark'
]

/** MarkdownRenderer 默认使用的 Streamdown 插件配置。 */
const DEFAULT_MARKDOWN_RENDERER_PLUGINS: NonNullable<StreamdownProps['plugins']> = {
  code,
  mermaid,
  cjk,
  math: createMathPlugin({
    singleDollarTextMath: true
  })
}

/** MarkdownRenderer 默认使用的 Streamdown 控制项配置。 */
const DEFAULT_MARKDOWN_RENDERER_CONTROLS: ControlsConfig = {
  table: false
}

/** MarkdownRenderer 默认是否展示代码块行号。移动端默认隐藏，避免占用阅读宽度。 */
const DEFAULT_MARKDOWN_RENDERER_LINE_NUMBERS: NonNullable<StreamdownProps['lineNumbers']> = false

/**
 * 组合 Markdown 根节点样式类名。
 *
 * @param className - 外部传入的附加类名
 * @returns 组合后的类名字符串
 */
const resolveMarkdownRendererClassName = (className?: string) => {
  return className ? `markdown-body ${className}` : 'markdown-body'
}

/**
 * 统一的 Markdown 文本渲染组件。
 * 全量使用 Streamdown 渲染，避免项目内存在多套 Markdown 解析实现。
 *
 * @param props - 组件属性
 * @returns Markdown 渲染节点
 */
const MarkdownRenderer: React.FC<MarkdownRendererProps> = ({
  content,
  mode = DEFAULT_MARKDOWN_RENDERER_MODE,
  controls = DEFAULT_MARKDOWN_RENDERER_CONTROLS,
  lineNumbers = DEFAULT_MARKDOWN_RENDERER_LINE_NUMBERS,
  className
}) => {
  return (
    <Streamdown
      className={resolveMarkdownRendererClassName(className)}
      mode={mode}
      controls={controls}
      lineNumbers={lineNumbers}
      plugins={DEFAULT_MARKDOWN_RENDERER_PLUGINS}
      shikiTheme={DEFAULT_MARKDOWN_RENDERER_SHIKI_THEME}
    >
      {content}
    </Streamdown>
  )
}

export default MarkdownRenderer
