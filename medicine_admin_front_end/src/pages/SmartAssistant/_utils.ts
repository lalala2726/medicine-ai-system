/**
 * SmartAssistant 模块通用工具函数
 */
import React from 'react';
import { useThemeContext } from '@/contexts/ThemeContext';

/**
 * Markdown 主题 Hook
 *
 * 源自 ThemeContext，配合 AppLayout 提供的 isDark 信号判断当前是亮色还是暗色。
 * 避免在 cssVar 模式下通过解析 token.colorBgContainer 字符串判断亮度失效。
 *
 * @returns [className] - 'x-markdown-light' | 'x-markdown-dark'
 */
export const useMarkdownTheme = (): [string] => {
  const { isDark } = useThemeContext();

  const className = React.useMemo(() => {
    return isDark ? 'x-markdown-dark' : 'x-markdown-light';
  }, [isDark]);

  return [className];
};

/**
 * 图表主题 Hook
 *
 * 配合 isDark 信号判断当前图表应使用的渲染主题名。
 *
 * @returns 'dark' | 'default'
 */
export const useChartTheme = (): 'dark' | 'default' => {
  const { isDark } = useThemeContext();

  return React.useMemo(() => {
    return isDark ? 'dark' : 'default';
  }, [isDark]);
};
