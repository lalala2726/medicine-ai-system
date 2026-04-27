import XMarkdown from '@ant-design/x-markdown';
import { Drawer, Spin, Typography } from 'antd';
import React from 'react';

import { useThemeContext } from '@/contexts/ThemeContext';

import '@ant-design/x-markdown/themes/light.css';
import '@ant-design/x-markdown/themes/dark.css';

/**
 * 提示词预览抽屉组件属性。
 */
export interface PromptPreviewDrawerProps {
  /**
   * 抽屉是否打开。
   */
  open: boolean;
  /**
   * 是否处于加载中。
   */
  loading?: boolean;
  /**
   * 提示词业务键。
   */
  promptKey?: string;
  /**
   * 提示词版本号。
   */
  promptVersion?: number | null;
  /**
   * 提示词正文（Markdown）。
   */
  promptContent?: string;
  /**
   * 关闭抽屉回调。
   */
  onClose: () => void;
}

/**
 * 预览抽屉宽度。
 */
const PREVIEW_DRAWER_WIDTH = 980;

/**
 * 抽屉内加载区域样式。
 */
const PREVIEW_LOADING_WRAP_STYLE: React.CSSProperties = {
  display: 'flex',
  justifyContent: 'center',
  alignItems: 'center',
  height: 220,
};

/**
 * 提示词预览抽屉。
 */
const PromptPreviewDrawer: React.FC<PromptPreviewDrawerProps> = ({
  open,
  loading = false,
  promptContent,
  onClose,
}) => {
  const { isDark } = useThemeContext();
  const markdownThemeClassName = isDark ? 'x-markdown-dark' : 'x-markdown-light';

  return (
    <Drawer
      title="内容预览"
      open={open}
      onClose={onClose}
      width={PREVIEW_DRAWER_WIDTH}
      destroyOnClose
    >
      {loading ? (
        <div style={PREVIEW_LOADING_WRAP_STYLE}>
          <Spin />
        </div>
      ) : promptContent ? (
        <XMarkdown
          className={markdownThemeClassName}
          content={promptContent}
          paragraphTag="div"
          openLinksInNewTab
        />
      ) : (
        <Typography.Text type="secondary">-</Typography.Text>
      )}
    </Drawer>
  );
};

export default PromptPreviewDrawer;
