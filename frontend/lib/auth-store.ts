import { create } from 'zustand';

export type AuthUser = {
  id: number;
  email: string;
  name: string;
};

type AuthState = {
  user: AuthUser | null;
  accessToken: string | null;
  setSession: (user: AuthUser, accessToken: string) => void;
  clearSession: () => void;
};

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  accessToken: null,
  setSession: (user, accessToken) => {
    if (typeof window !== 'undefined') {
      window.localStorage.setItem('accessToken', accessToken);
    }
    set({ user, accessToken });
  },
  clearSession: () => {
    if (typeof window !== 'undefined') {
      window.localStorage.removeItem('accessToken');
    }
    set({ user: null, accessToken: null });
  },
}));
