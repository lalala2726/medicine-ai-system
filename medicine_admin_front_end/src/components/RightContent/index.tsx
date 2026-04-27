import { MoonOutlined, QuestionCircleOutlined, SunOutlined } from '@ant-design/icons';
import React from 'react';

export * from './AvatarDropdown';

export type SiderTheme = 'light' | 'dark';

export const SelectLang: React.FC = () => {
  return (
    <div
      style={{
        padding: 4,
      }}
    >
      Lang
    </div>
  );
};

export const Question: React.FC = () => {
  return (
    <a
      href="https://pro.ant.design/docs/getting-started"
      target="_blank"
      rel="noreferrer"
      style={{
        display: 'inline-flex',
        padding: '4px',
        fontSize: '18px',
        color: 'inherit',
      }}
    >
      <QuestionCircleOutlined />
    </a>
  );
};

/** 亮/暗主题切换按钮 */
export const ThemeToggle: React.FC<{
  isDark: boolean;
  onToggle: () => void;
}> = ({ isDark, onToggle }) => {
  return (
    <span
      onClick={onToggle}
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '4px',
        fontSize: '18px',
        color: 'inherit',
        cursor: 'pointer',
      }}
      title={isDark ? '切换到亮色模式' : '切换到暗色模式'}
    >
      {isDark ? <SunOutlined /> : <MoonOutlined />}
    </span>
  );
};
