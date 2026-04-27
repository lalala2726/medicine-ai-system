/**
 * 权限守卫组件
 * 替代 UmiJS 的路由级权限控制
 */
import React, { useEffect, useState } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { currentUser as queryCurrentUser } from '@/api/core/login';
import { authTokenStore, clearAuthState, hydrateAuthStores, userStore } from '@/store';
import { normalizeUserData } from '@/utils/userUtils';

const loginPath = '/user/login';

export interface AuthState {
  currentUser?: API.CurrentUser;
  loading: boolean;
  fetchUserInfo: () => Promise<API.CurrentUser | undefined>;
}

// 全局 auth 状态，供 layout 和其他组件使用
let globalAuthState: AuthState = {
  currentUser: undefined,
  loading: true,
  fetchUserInfo: async () => undefined,
};

export function getAuthState(): AuthState {
  return globalAuthState;
}

export function setAuthCurrentUser(user: API.CurrentUser | undefined) {
  globalAuthState = { ...globalAuthState, currentUser: user };
}

const AuthGuard: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const location = useLocation();
  const [loading, setLoading] = useState(true);
  const [authenticated, setAuthenticated] = useState(false);

  useEffect(() => {
    const init = async () => {
      try {
        await hydrateAuthStores();
        const { accessToken } = authTokenStore.getState();

        if (!accessToken) {
          setAuthenticated(false);
          setLoading(false);
          return;
        }

        // 尝试获取用户信息
        const fetchUserInfo = async () => {
          try {
            const { accessToken: token } = authTokenStore.getState();
            if (!token) return undefined;

            const response = await queryCurrentUser({ skipErrorHandler: true });

            if (response && typeof response === 'object' && 'code' in response) {
              if ((response as any).code === 200 && (response as any).data) {
                const normalizedUser = normalizeUserData((response as any).data);
                userStore.getState().setUser(normalizedUser);
                return normalizedUser;
              }
              if ((response as any).code === 4011) return undefined;
              return undefined;
            }

            if (response && typeof response === 'object' && (response as any).id) {
              const normalizedUser = normalizeUserData(response as any);
              userStore.getState().setUser(normalizedUser);
              return normalizedUser;
            }

            // response 可能直接就是 normalizeUserData 需要的数据
            if (response) {
              const normalizedUser = normalizeUserData(response as any);
              userStore.getState().setUser(normalizedUser);
              return normalizedUser;
            }

            return undefined;
          } catch (error: any) {
            console.error('获取用户信息失败:', error);
            if (error.response?.status === 401) {
              clearAuthState();
            }
            return undefined;
          }
        };

        const currentUser = await fetchUserInfo();

        globalAuthState = {
          currentUser,
          loading: false,
          fetchUserInfo,
        };

        setAuthenticated(!!currentUser);
        setLoading(false);
      } catch (error) {
        console.error('初始化认证状态失败:', error);
        setAuthenticated(false);
        setLoading(false);
      }
    };

    init();
  }, []);

  if (loading) {
    return null;
  }

  if (!authenticated) {
    return <Navigate to={loginPath} state={{ from: location }} replace />;
  }

  return <>{children}</>;
};

export default AuthGuard;
