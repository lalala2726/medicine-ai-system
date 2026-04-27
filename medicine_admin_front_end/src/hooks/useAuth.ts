import { useCallback, useMemo } from 'react';
import {
  authTokenStore,
  clearAuthState,
  getAuthStateSnapshot,
  useAuthTokenStore,
  userStore,
  useUserStore,
} from '@/store';

export const useAuth = () => {
  const accessToken = useAuthTokenStore((state) => state.accessToken);
  const refreshToken = useAuthTokenStore((state) => state.refreshToken);
  const user = useUserStore((state) => state.user);

  return useMemo(
    () => ({
      accessToken,
      refreshToken,
      user,
    }),
    [accessToken, refreshToken, user],
  );
};

export const useAuthTokens = () => {
  const accessToken = useAuthTokenStore((state) => state.accessToken);
  const refreshToken = useAuthTokenStore((state) => state.refreshToken);

  return useMemo(
    () => ({
      accessToken,
      refreshToken,
    }),
    [accessToken, refreshToken],
  );
};

export const useAuthActions = () => {
  const setTokens = useAuthTokenStore((state) => state.setTokens);
  const setAccessToken = useAuthTokenStore((state) => state.setAccessToken);
  const setRefreshToken = useAuthTokenStore((state) => state.setRefreshToken);
  const setAuthTokens = useAuthTokenStore((state) => state.setAuthTokens);
  const clearTokens = useAuthTokenStore((state) => state.clearTokens);
  const setUser = useUserStore((state) => state.setUser);
  const clearUser = useUserStore((state) => state.clearUser);

  const setAuth = useCallback(
    (payload: {
      accessToken?: string | null;
      refreshToken?: string | null;
      user?: API.CurrentUser | null | undefined;
    }) => {
      const { accessToken, refreshToken, user } = payload;
      if (accessToken !== undefined || refreshToken !== undefined) {
        setAuthTokens({
          accessToken,
          refreshToken,
        });
      }
      if (user !== undefined) {
        setUser(user);
      }
    },
    [setAuthTokens, setUser],
  );

  const clearAuth = useCallback(() => {
    clearTokens();
    clearUser();
  }, [clearTokens, clearUser]);

  return useMemo(
    () => ({
      setTokens,
      setAccessToken,
      setRefreshToken,
      setUser,
      setAuth,
      clearAuth,
      clearTokens,
      clearUser,
    }),
    [
      setTokens,
      setAccessToken,
      setRefreshToken,
      setUser,
      setAuth,
      clearAuth,
      clearTokens,
      clearUser,
    ],
  );
};

export const getAuthState = () => {
  return getAuthStateSnapshot();
};

export const getAuthTokenState = () => {
  return authTokenStore.getState();
};

export const getUserState = () => {
  return userStore.getState();
};

export const clearAuthSnapshot = () => {
  clearAuthState();
};
