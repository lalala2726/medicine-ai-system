/**
 * 全局页面加载组件
 *
 * 使用统一的旋转动画，支持两种模式：
 * - fullscreen: 全局居中覆盖整个视口，用于权限守卫 / 初始加载
 * - 内容区: 仅填充父容器，用于路由懒加载（不遮挡侧边栏和顶部栏）
 *
 * 初始加载时（Ant Design CSS 变量未注入）通过读取 localStorage 主题偏好来确定背景色。
 */
import React from 'react';
import styles from './index.module.less';

const THEME_STORAGE_KEY = 'app-nav-theme';

interface PageLoaderProps {
  /** 提示文字，默认"加载中…" */
  text?: string;
  /** 是否全屏覆盖（默认 false，仅填充父容器） */
  fullscreen?: boolean;
}

const PageLoader: React.FC<PageLoaderProps> = ({ text = '加载中…', fullscreen = false }) => {
  // CSS 变量可能还没注入（首次刷新），通过 localStorage 判断主题作为 fallback
  const isDark = localStorage.getItem(THEME_STORAGE_KEY) === 'realDark';

  const wrapperClass = fullscreen ? styles.fullscreen : styles.content;

  return (
    <div
      className={wrapperClass}
      style={{
        background: isDark ? '#141414' : '#ffffff',
        color: isDark ? 'rgba(255,255,255,0.45)' : undefined,
      }}
    >
      <div className={styles.loader} />
      {text && <span className={styles.text}>{text}</span>}
    </div>
  );
};

export default PageLoader;
