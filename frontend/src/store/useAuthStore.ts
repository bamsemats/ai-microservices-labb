import { create } from 'zustand';

interface AuthState {
  token: string | null;
  userId: string | null;
  username: string | null;
  isAuthenticated: boolean;
  setAuth: (token: string, userId: string, username: string) => void;
  logout: () => void;
}

const getSafeStorageItem = (key: string) => {
  const value = localStorage.getItem(key);
  if (value === 'undefined' || value === 'null' || value === null) return null;
  return value;
};

export const useAuthStore = create<AuthState>((set) => ({
  token: null, // Keep only in memory
  userId: getSafeStorageItem('userId'),
  username: getSafeStorageItem('username'),
  isAuthenticated: !!getSafeStorageItem('username'),
  setAuth: (token, userId, username) => {
    if (!token || !userId || !username) {
      return;
    }
    // We don't store token in localStorage anymore for security
    localStorage.setItem('userId', userId);
    localStorage.setItem('username', username);
    set({ token, userId, username, isAuthenticated: true });
  },
  logout: () => {
    localStorage.removeItem('userId');
    localStorage.removeItem('username');
    set({ token: null, userId: null, username: null, isAuthenticated: false });
  },
}));
