export * from './authTokenStore';
export * from './chatStore';
export * from './userStore';

import { authTokenStore, hydrateAuthTokenStore } from './authTokenStore';
import { hydrateUserStore, userStore } from './userStore';

export const hydrateAuthStores = async () => {
  await Promise.all([hydrateAuthTokenStore(), hydrateUserStore()]);
};

export const clearAuthState = () => {
  authTokenStore.getState().clearTokens();
  userStore.getState().clearUser();
};

export const getAuthStateSnapshot = () => {
  const tokenState = authTokenStore.getState();
  const userState = userStore.getState();
  return {
    accessToken: tokenState.accessToken,
    refreshToken: tokenState.refreshToken,
    user: userState.user,
  };
};
