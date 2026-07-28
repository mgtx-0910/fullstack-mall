import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { CurrentUser } from '@/services/types';

type AuthState = {
  token?: string;
  currentUser?: CurrentUser;
  setAuth: (payload: { token: string; currentUser: CurrentUser }) => void;
  setCurrentUser: (currentUser?: CurrentUser) => void;
  clearAuth: () => void;
};

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      token: undefined,
      currentUser: undefined,
      setAuth: ({ token, currentUser }) => set({ token, currentUser }),
      setCurrentUser: (currentUser) => set({ currentUser }),
      clearAuth: () => set({ token: undefined, currentUser: undefined }),
    }),
    {
      name: 'fullstack-mall-admin-auth',
      partialize: (state) => ({ token: state.token, currentUser: state.currentUser }),
    },
  ),
);
