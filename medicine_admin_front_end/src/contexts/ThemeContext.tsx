/**
 * 全局主题 Context
 *
 * 由 AppLayout 提供 isDark 状态，供全局组件（如 MarkdownRender）消费，
 * 避免在 cssVar 模式下通过解析 token.colorBgContainer 字符串来判断主题。
 */
import React from 'react';

interface ThemeContextValue {
  /** 当前是否为暗色模式 */
  isDark: boolean;
}

export const ThemeContext = React.createContext<ThemeContextValue>({ isDark: false });

/** 消费主题 Context 的便捷 Hook */
export const useThemeContext = (): ThemeContextValue => React.useContext(ThemeContext);
