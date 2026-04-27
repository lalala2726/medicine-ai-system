import { LogoutOutlined, UserOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import type { MenuProps } from 'antd';
import { App, Spin } from 'antd';
import { createStyles } from 'antd-style';
import React from 'react';
import { outLogin } from '@/api/core/login';
import { useAuthActions, useAuthTokens } from '@/hooks/useAuth';
import { useInitialState } from '@/hooks/useInitialState';
import { routePaths } from '@/router/paths';
import HeaderDropdown from '../HeaderDropdown';

export type GlobalHeaderRightProps = {
  children?: React.ReactNode;
};

export const AvatarName = () => {
  const { initialState } = useInitialState();
  const { currentUser } = initialState || {};

  // 使用与 normalizeUserData 相同的逻辑来获取显示名称
  // 优先使用 nickname，然后是 username，最后是 name
  const displayName = currentUser?.nickname || currentUser?.username || currentUser?.name || '用户';

  return <span className="anticon">{displayName}</span>;
};

const useStyles = createStyles(({ token }) => {
  return {
    action: {
      display: 'flex',
      height: '48px',
      marginLeft: 'auto',
      overflow: 'hidden',
      alignItems: 'center',
      padding: '0 8px',
      cursor: 'pointer',
      borderRadius: token.borderRadius,
      '&:hover': {
        backgroundColor: token.colorBgTextHover,
      },
    },
  };
});

export const AvatarDropdown: React.FC<GlobalHeaderRightProps> = ({ children }) => {
  const { clearAuth } = useAuthActions();
  const { accessToken } = useAuthTokens();
  const { message, modal } = App.useApp();
  const navigate = useNavigate();
  const { initialState, setInitialState } = useInitialState();

  /**
   * 构建退出登录后的登录页地址。
   *
   * @returns 带当前访问地址 redirect 参数的登录页地址。
   */
  const buildLoginRedirectPath = (): string => {
    const { search, pathname } = window.location;
    const searchParams = new URLSearchParams({
      redirect: pathname + search,
    });
    return `${routePaths.login}?${searchParams.toString()}`;
  };

  /**
   * 退出登录，并清理本地认证状态。
   *
   * @returns 无返回值。
   */
  const loginOut = async (): Promise<void> => {
    try {
      await outLogin();
    } catch (error) {
      console.error('退出登录失败:', error);
    } finally {
      if (window.location.pathname !== routePaths.login) {
        navigate(buildLoginRedirectPath(), { replace: true });
      }
      clearAuth();
      setInitialState({ currentUser: undefined });
      message.success('您已退出');
    }
  };
  const { styles } = useStyles();

  /**
   * 处理头像下拉菜单点击。
   *
   * @param event 菜单点击事件。
   * @returns 无返回值。
   */
  const onMenuClick: MenuProps['onClick'] = (event) => {
    const { key } = event;
    if (key === 'logout') {
      modal.confirm({
        title: '确认退出登录？',
        content: '退出后需要重新登录才能继续访问管理后台。',
        okText: '退出登录',
        cancelText: '取消',
        okButtonProps: { danger: true },
        onOk: loginOut,
      });
      return;
    }
    if (key === 'profile') {
      navigate(routePaths.accountProfile);
    }
  };

  const loading = (
    <span className={styles.action}>
      <Spin
        size="small"
        style={{
          marginLeft: 8,
          marginRight: 8,
        }}
      />
    </span>
  );

  const defaultUser = (
    <span className={styles.action}>
      <span className="anticon">用户</span>
    </span>
  );

  // 如果 initialState 不存在，检查是否有token
  if (!initialState) {
    if (accessToken) {
      // 有token但没有initialState，显示默认用户
      return defaultUser;
    }
    return loading;
  }

  const { currentUser } = initialState;

  // 如果 currentUser 不存在，检查是否有token
  if (!currentUser) {
    if (accessToken) {
      // 有token但没有用户数据，显示默认用户
      return defaultUser;
    }
    return loading;
  }

  // 检查用户是否有有效的显示名称
  // 使用 normalizeUserData 中的逻辑，确保至少有一个有效的显示名称
  const hasValidName = currentUser.nickname || currentUser.username;

  // 如果没有有效的显示名称，但有token，显示默认用户
  if (!hasValidName) {
    if (accessToken) {
      // 有token但没有用户数据，显示默认用户而不是加载状态
      return defaultUser;
    }
    return loading;
  }

  const menuItems = [
    {
      key: 'profile',
      icon: <UserOutlined />,
      label: '个人资料',
    },
    {
      type: 'divider' as const,
    },
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: '退出登录',
    },
  ];

  return (
    <HeaderDropdown
      menu={{
        selectedKeys: [],
        onClick: onMenuClick,
        items: menuItems,
      }}
    >
      {children}
    </HeaderDropdown>
  );
};
