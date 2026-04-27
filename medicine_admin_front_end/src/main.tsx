/* eslint-disable react-refresh/only-export-components */
/**
 * 应用入口文件
 * 替代 UmiJS 的自动入口
 */
import React from 'react';
import ReactDOM from 'react-dom/client';
import { RouterProvider } from 'react-router-dom';
import { App, ConfigProvider, theme as antdTheme } from 'antd';
import '@ant-design/v5-patch-for-react-19';
import { HelmetProvider } from 'react-helmet-async';
import zhCN from 'antd/locale/zh_CN';
import './global.less';
import { router } from './router';

// 导入全局副作用（PWA 等）
import './global';

const rootEl = document.getElementById('root');
if (!rootEl) throw new Error('找不到 #root 元素');

const root = ReactDOM.createRoot(rootEl);
const THEME_STORAGE_KEY = 'app-nav-theme';
const THEME_CHANGE_EVENT = 'app-theme-change';

type AppThemeMode = 'light' | 'realDark';

function getStoredThemeMode(): AppThemeMode {
  try {
    return localStorage.getItem(THEME_STORAGE_KEY) === 'realDark' ? 'realDark' : 'light';
  } catch {
    return 'light';
  }
}

const RootProviders: React.FC = () => {
  const [themeMode, setThemeMode] = React.useState<AppThemeMode>(() => getStoredThemeMode());

  React.useEffect(() => {
    const syncThemeMode = () => setThemeMode(getStoredThemeMode());

    window.addEventListener(THEME_CHANGE_EVENT, syncThemeMode);
    window.addEventListener('storage', syncThemeMode);

    return () => {
      window.removeEventListener(THEME_CHANGE_EVENT, syncThemeMode);
      window.removeEventListener('storage', syncThemeMode);
    };
  }, []);

  const isDark = themeMode === 'realDark';

  return (
    <ConfigProvider
      locale={zhCN}
      theme={{
        cssVar: {},
        algorithm: isDark ? antdTheme.darkAlgorithm : antdTheme.defaultAlgorithm,
        token: {
          fontFamily: 'AlibabaSans, sans-serif',
        },
      }}
    >
      <App>
        <RouterProvider router={router} />
      </App>
    </ConfigProvider>
  );
};

root.render(
  <HelmetProvider>
    <RootProviders />
  </HelmetProvider>,
);
