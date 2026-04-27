import { LockOutlined, UserOutlined } from '@ant-design/icons';
import { LoginForm, ProFormCheckbox, ProFormText } from '@ant-design/pro-components';
import { Helmet } from 'react-helmet-async';
import { useNavigate } from 'react-router-dom';
import { App, ConfigProvider, theme as antdTheme } from 'antd';
import { createStyles } from 'antd-style';
import React, { useState } from 'react';
import type { CaptchaVerificationResult } from '@/api/core/captcha';
import { currentUser, login } from '@/api/core/login';
import { ErrorBoundary, Footer } from '@/components';
import { useAuthActions } from '@/hooks/useAuth';
import { normalizeUserData } from '@/utils/userUtils';
import SliderCaptchaModal from './components/SliderCaptchaModal';
import Settings from '../../../config/defaultSettings';

/** 登录页底部展示的 ICP 备案号。 */
const LOGIN_ICP_BEIAN_TEXT = '陕ICP备2023007009号-2';

/** 工信部 ICP/IP 地址/域名信息备案管理系统地址。 */
const MIIT_BEIAN_URL = 'https://beian.miit.gov.cn/';

const useStyles = createStyles(({ token, css }, { isDark }: { isDark: boolean }) => {
  return {
    container: css`
      display: flex;
      flex-direction: column;
      height: 100vh;
      overflow: auto;
      background: ${isDark ? '#0d1117' : '#f0f2f5'};
      background-image: ${isDark
        ? 'none'
        : "url('https://mdn.alipayobjects.com/yuyan_qk0oxh/afts/img/V-_oS6r-i7wAAAAAAAAAAAAAFl94AQBr')"};
      background-size: 100% 100%;
      position: relative;
      transition: background 0.3s ease;
    `,
    content: css`
      flex: 1;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 32px 0;
      z-index: 1;
    `,
    loginCard: css`
      background: ${isDark ? 'rgba(22, 27, 34, 0.92)' : 'rgba(255, 255, 255, 0.85)'};
      backdrop-filter: blur(16px);
      -webkit-backdrop-filter: blur(16px);
      border-radius: 16px;
      box-shadow: ${isDark
        ? '0 8px 40px 0 rgba(0, 0, 0, 0.5)'
        : '0 8px 32px 0 rgba(31, 38, 135, 0.1)'};
      border: 1px solid ${isDark ? 'rgba(255, 255, 255, 0.08)' : 'rgba(255, 255, 255, 0.18)'};
      padding: 12px;
      transition:
        background 0.3s ease,
        box-shadow 0.3s ease;

      .ant-pro-form-login-container {
        padding: 24px;
      }

      .ant-pro-form-login-title {
        font-size: 28px;
        font-weight: 600;
        color: ${token.colorTextHeading};
        margin-bottom: 8px;
      }

      .ant-pro-form-login-desc {
        font-size: 14px;
        color: ${token.colorTextSecondary};
        margin-bottom: 32px;
      }

      .ant-input-affix-wrapper-lg {
        padding: 10px 15px;
        border-radius: 8px;
      }

      .ant-btn-primary {
        height: 45px;
        border-radius: 8px;
        font-size: 16px;
        font-weight: 500;
        margin-top: 12px;
      }
    `,
    warningBanner: css`
      margin-top: -12px;
      margin-bottom: 24px;
      text-align: center;
      font-size: 12px;
      color: ${token.colorError};
      background: ${isDark ? 'rgba(255, 77, 79, 0.1)' : 'rgba(255, 77, 79, 0.06)'};
      padding: 8px;
      border-radius: 6px;
      border: 1px solid ${isDark ? 'rgba(255, 77, 79, 0.25)' : 'rgba(255, 77, 79, 0.1)'};
    `,
    footer: css`
      position: relative;
      z-index: 1;
    `,
    beianLink: css`
      display: block;
      width: fit-content;
      margin: 0 auto 16px;
      color: ${token.colorTextSecondary};
      font-size: 12px;
      line-height: 20px;
      text-decoration: none;

      &:hover {
        color: ${token.colorPrimary};
      }
    `,
  };
});

/** 从 localStorage 读取暗色模式偏好 */
const THEME_STORAGE_KEY = 'app-nav-theme';

function getIsDark(): boolean {
  try {
    return localStorage.getItem(THEME_STORAGE_KEY) === 'realDark';
  } catch {
    return false;
  }
}

const Login: React.FC<{ isDark: boolean }> = ({ isDark }) => {
  const navigate = useNavigate();
  const { styles } = useStyles({ isDark });
  const { message } = App.useApp();
  const { setAuth } = useAuthActions();
  const [captchaOpen, setCaptchaOpen] = useState(false);
  const [pendingLoginValues, setPendingLoginValues] = useState<API.LoginParams | null>(null);

  const submitLogin = async (
    values: API.LoginParams,
    captchaVerificationResult: CaptchaVerificationResult,
  ) => {
    try {
      const loginResponse = await login(
        {
          ...values,
          type: 'account',
          captchaVerificationId: captchaVerificationResult.id,
        },
        {
          skipErrorHandler: true,
        },
      );

      if (!loginResponse || loginResponse.code !== 200 || !loginResponse.data?.accessToken) {
        throw new Error(loginResponse?.message || '登录失败，请重试！');
      }

      const loginResult = loginResponse.data;

      setAuth({
        accessToken: loginResult.accessToken,
        refreshToken: loginResult.refreshToken ?? null,
      });

      message.success('登录成功！');

      const fallbackUser: API.CurrentUser = {
        name: values.username,
        username: values.username,
        avatar: '/default_avatar.png',
      };

      let normalizedUser: API.CurrentUser | undefined;

      if (loginResult.user) {
        normalizedUser = normalizeUserData(loginResult.user);
      } else {
        try {
          const userResponse = await currentUser({ skipErrorHandler: true });
          const resolvedUser =
            userResponse && typeof userResponse === 'object'
              ? 'data' in userResponse
                ? (userResponse as any).data
                : userResponse
              : undefined;

          if (resolvedUser) {
            normalizedUser = normalizeUserData(resolvedUser);
          }
        } catch (error) {
          console.error('获取用户信息失败:', error);
        }
      }

      setAuth({ user: normalizedUser ?? fallbackUser });

      const urlParams = new URL(window.location.href).searchParams;
      const redirectUrl = urlParams.get('redirect') || '/';
      navigate(redirectUrl);
    } catch (error: any) {
      console.error('登录失败:', error);
      message.error(error?.message || '登录失败，请重试！');
    } finally {
      setPendingLoginValues(null);
      setCaptchaOpen(false);
    }
  };

  const handleSubmit = async (values: API.LoginParams) => {
    setPendingLoginValues(values);
    setCaptchaOpen(true);
  };

  const handleCaptchaCancel = () => {
    setCaptchaOpen(false);
    setPendingLoginValues(null);
  };

  const handleCaptchaVerified = async (captchaVerificationResult: CaptchaVerificationResult) => {
    if (!pendingLoginValues) {
      setCaptchaOpen(false);
      return;
    }
    await submitLogin(pendingLoginValues, captchaVerificationResult);
  };

  return (
    <div className={styles.container}>
      <Helmet>
        <title>{`登录 - ${Settings.title || '药智通后台管理系统'}`}</title>
      </Helmet>

      <div className={styles.content}>
        <div className={styles.loginCard}>
          <LoginForm
            logo={<img alt="logo" src="/logo.svg" style={{ width: 48, height: 48 }} />}
            title="药智通后台管理系统"
            subTitle="更高效、更现代的医药管理解决方案"
            initialValues={{ autoLogin: true }}
            onFinish={async (values) => {
              await handleSubmit(values as API.LoginParams);
            }}
            submitter={{
              searchConfig: {
                submitText: '进入系统',
              },
            }}
          >
            <div className={styles.warningBanner}>内部系统，严禁在公共计算机登录</div>
            <div style={{ marginTop: 24 }}>
              <ProFormText
                name="username"
                fieldProps={{
                  size: 'large',
                  prefix: <UserOutlined style={{ color: '#1890ff' }} />,
                }}
                placeholder="请输入用户名"
                rules={[{ required: true, message: '请输入用户名！' }]}
              />
              <ProFormText.Password
                name="password"
                fieldProps={{
                  size: 'large',
                  prefix: <LockOutlined style={{ color: '#1890ff' }} />,
                }}
                placeholder="请输入密码"
                rules={[{ required: true, message: '请输入密码！' }]}
              />
            </div>
            <div style={{ marginBottom: 24, display: 'flex', justifyContent: 'space-between' }}>
              <ProFormCheckbox noStyle name="autoLogin">
                自动登录
              </ProFormCheckbox>
              <a style={{ float: 'right' }}>忘记密码？</a>
            </div>
          </LoginForm>
        </div>
      </div>
      <div className={styles.footer}>
        <Footer />
        <a className={styles.beianLink} href={MIIT_BEIAN_URL} rel="noreferrer" target="_blank">
          {LOGIN_ICP_BEIAN_TEXT}
        </a>
      </div>
      <SliderCaptchaModal
        onCancel={handleCaptchaCancel}
        onVerified={handleCaptchaVerified}
        open={captchaOpen}
        isDark={isDark}
      />
    </div>
  );
};

const LoginWithErrorBoundary: React.FC = () => {
  const [isDark, setIsDark] = useState<boolean>(getIsDark);

  // 监听同标签/跨标签主题变更（用户在主应用切换后再回到登录页刷新会自动生效）
  React.useEffect(() => {
    const handleThemeChange = () => setIsDark(getIsDark());
    window.addEventListener('storage', handleThemeChange);
    window.addEventListener('app-theme-change', handleThemeChange);
    return () => {
      window.removeEventListener('storage', handleThemeChange);
      window.removeEventListener('app-theme-change', handleThemeChange);
    };
  }, []);

  return (
    <ErrorBoundary>
      <ConfigProvider
        theme={{
          algorithm: isDark ? antdTheme.darkAlgorithm : antdTheme.defaultAlgorithm,
          token: {
            borderRadius: 8,
          },
        }}
      >
        <App>
          <Login isDark={isDark} />
        </App>
      </ConfigProvider>
    </ErrorBoundary>
  );
};

export default LoginWithErrorBoundary;
