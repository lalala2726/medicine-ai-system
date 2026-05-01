import type { ProLayoutProps } from '@ant-design/pro-components';

export type AppThemeMode = 'light' | 'realDark';

type AppSettings = ProLayoutProps & {
  pwa?: boolean;
  logo?: string;
  apiBaseURL?: string;
};

const LIGHT_BG_LAYOUT = '#f0f2f6';
const DARK_BG_LAYOUT = '#141414';
/** 默认业务 API 代理前缀，生产环境由 Nginx 或网关转发。 */
const DEFAULT_API_BASE_URL = '/api';

/**
 * 根据导航主题计算布局背景色。
 * 优先返回 antd 的布局背景 CSS 变量，fallback 到当前主题对应的固定色值，
 * 这样在浅色和暗色主题之间切换时，ProLayout 的背景可以自动同步。
 */
export const getBgLayoutColor = (navTheme: AppThemeMode = 'light') =>
  `var(--ant-color-bg-layout, ${navTheme === 'realDark' ? DARK_BG_LAYOUT : LIGHT_BG_LAYOUT})`;

/**
 * 将主题相关的布局配置合并回 settings。
 * 主要负责同步 `navTheme` 和 `token.bgLayout`，避免初始化、手动切换主题、
 * 或通过 SettingDrawer 修改主题时出现背景色未更新的问题。
 */
export const applyThemeSettings = <T extends Partial<AppSettings>>(
  settings: T,
  navTheme: AppThemeMode = 'light',
): T & Pick<AppSettings, 'navTheme' | 'token'> => ({
  ...settings,
  navTheme,
  token: {
    ...settings.token,
    bgLayout: getBgLayoutColor(navTheme),
  },
});

/**
 * @name
 */
const Settings: AppSettings = applyThemeSettings({
  // 默认的 API 基础路径
  apiBaseURL: DEFAULT_API_BASE_URL,

  // 其他默认设置...
  navTheme: 'light',
  // 拂晓蓝
  colorPrimary: '#1890ff',
  layout: 'mix',
  contentWidth: 'Fluid',
  fixedHeader: false,
  fixSiderbar: false,
  colorWeak: false,
  title: '药智通',
  pwa: true,
  logo: 'https://gw.alipayobjects.com/zos/rmsportal/KDpgvguMpGfqaHPjicRK.svg',
  iconfontUrl: '',
  // 参见ts声明，demo 见文档，通过token 修改样式
  //https://procomponents.ant.design/components/layout#%E9%80%9A%E8%BF%87-token-%E4%BF%AE%E6%94%B9%E6%A0%B7%E5%BC%8F
  token: {},
  siderMenuType: 'group',
});

export default Settings;
