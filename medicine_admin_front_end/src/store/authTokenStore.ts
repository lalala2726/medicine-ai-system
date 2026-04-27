import { create } from 'zustand';
import { createJSONStorage, persist } from 'zustand/middleware';

type AuthTokenState = {
  accessToken: string | null;
  refreshToken: string | null;
};

type AuthTokenActions = {
  setTokens: (accessToken: string, refreshToken?: string | null) => void;
  setAccessToken: (accessToken: string | null) => void;
  setRefreshToken: (refreshToken: string | null) => void;
  setAuthTokens: (payload: { accessToken?: string | null; refreshToken?: string | null }) => void;
  clearTokens: () => void;
};

type AuthTokenStore = AuthTokenState & AuthTokenActions;

const initialState: AuthTokenState = {
  accessToken: null,
  refreshToken: null,
};

const storage =
  typeof window !== 'undefined' ? createJSONStorage(() => window.localStorage) : undefined;

export const useAuthTokenStore = create<AuthTokenStore>()(
  persist(
    (set) => ({
      ...initialState,
      setTokens: (accessToken, refreshToken) => {
        set((state) => ({
          accessToken,
          refreshToken:
            typeof refreshToken === 'string'
              ? refreshToken
              : refreshToken === null
                ? null
                : state.refreshToken,
        }));
      },
      setAccessToken: (accessToken) => {
        set({ accessToken: accessToken ?? null });
      },
      setRefreshToken: (refreshToken) => {
        set((state) => ({
          refreshToken:
            typeof refreshToken === 'string'
              ? refreshToken
              : refreshToken === null
                ? null
                : state.refreshToken,
        }));
      },
      setAuthTokens: ({ accessToken, refreshToken }) => {
        set((state) => ({
          accessToken: accessToken !== undefined ? (accessToken ?? null) : state.accessToken,
          refreshToken: refreshToken !== undefined ? (refreshToken ?? null) : state.refreshToken,
        }));
      },
      clearTokens: () => {
        set(initialState);
      },
    }),
    {
      name: 'auth-token-storage',
      storage,
      skipHydration: typeof window === 'undefined',
      partialize: (state) => ({
        accessToken: state.accessToken,
        refreshToken: state.refreshToken,
      }),
    },
  ),
);

export const authTokenStore = useAuthTokenStore;

type PersistHelpers = {
  hasHydrated?: () => boolean;
  rehydrate?: () => Promise<void>;
};

type AuthTokenStoreWithPersist = typeof authTokenStore & {
  persist?: PersistHelpers;
};

export const hydrateAuthTokenStore = async () => {
  const persist = (authTokenStore as AuthTokenStoreWithPersist).persist;
  if (persist?.hasHydrated && !persist.hasHydrated()) {
    await persist.rehydrate?.();
  }
};
