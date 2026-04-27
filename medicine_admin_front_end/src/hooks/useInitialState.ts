/**
 * useInitialState hook
 * 替代 UmiJS 的 useModel('@@initialState')
 * 使用 zustand 的 userStore 作为底层存储
 */
import { useCallback, useMemo } from 'react';
import { userStore } from '@/store';
import { getAuthState, setAuthCurrentUser } from '@/layouts/AuthGuard';

interface InitialState {
  currentUser?: API.CurrentUser | null;
  settings?: any;
  loading?: boolean;
  fetchUserInfo?: () => Promise<API.CurrentUser | undefined>;
}

/**
 * 替代 useModel('@@initialState')
 */
export function useInitialState() {
  const user = userStore((s) => s.user);
  const authState = getAuthState();

  /**
   * 当前初始化状态快照。
   */
  const initialState: InitialState = useMemo(
    () => ({
      currentUser: user || authState.currentUser,
      loading: authState.loading,
      fetchUserInfo: authState.fetchUserInfo,
    }),
    [authState.currentUser, authState.fetchUserInfo, authState.loading, user],
  );

  /**
   * 更新初始化状态。
   *
   * @param updater 更新参数或更新函数。
   * @returns 无返回值。
   */
  const setInitialState = useCallback(
    (updater: Partial<InitialState> | ((prev: InitialState) => Partial<InitialState>)) => {
      const currentAuthState = getAuthState();
      const currentState: InitialState = {
        currentUser: userStore.getState().user || currentAuthState.currentUser,
        loading: currentAuthState.loading,
        fetchUserInfo: currentAuthState.fetchUserInfo,
      };
      const newState = typeof updater === 'function' ? updater(currentState) : updater;
      // 允许显式传入 undefined/null 来清理当前用户状态。
      if (Object.prototype.hasOwnProperty.call(newState, 'currentUser')) {
        userStore.getState().setUser(newState.currentUser);
        setAuthCurrentUser(newState.currentUser ?? undefined);
      }
    },
    [],
  );

  return { initialState, setInitialState };
}
