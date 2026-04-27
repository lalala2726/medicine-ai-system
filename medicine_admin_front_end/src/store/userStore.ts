import { create } from 'zustand';
import { createJSONStorage, persist } from 'zustand/middleware';

type UserState = {
  user: API.CurrentUser | null | undefined;
};

type UserActions = {
  setUser: (user: API.CurrentUser | null | undefined) => void;
  clearUser: () => void;
};

type UserStore = UserState & UserActions;

const initialState: UserState = {
  user: undefined,
};

const storage =
  typeof window !== 'undefined' ? createJSONStorage(() => window.localStorage) : undefined;

export const useUserStore = create<UserStore>()(
  persist(
    (set) => ({
      ...initialState,
      setUser: (user) => {
        set({ user });
      },
      clearUser: () => {
        set({ user: null });
      },
    }),
    {
      name: 'user-storage',
      storage,
      skipHydration: typeof window === 'undefined',
      partialize: (state) => ({
        user: state.user,
      }),
    },
  ),
);

export const userStore = useUserStore;

type PersistHelpers = {
  hasHydrated?: () => boolean;
  rehydrate?: () => Promise<void>;
};

type UserStoreWithPersist = typeof userStore & {
  persist?: PersistHelpers;
};

export const hydrateUserStore = async () => {
  const persist = (userStore as UserStoreWithPersist).persist;
  if (persist?.hasHydrated && !persist.hasHydrated()) {
    await persist.rehydrate?.();
  }
};
