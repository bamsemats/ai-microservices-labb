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
  token: getSafeStorageItem('token'),
  userId: getSafeStorageItem('userId'),
  username: getSafeStorageItem('username'),
  isAuthenticated: !!getSafeStorageItem('token'),
  setAuth: (token, userId, username) => {
    if (!token || !userId || !username) {
      console.error('useAuthStore: Attempted to set auth with missing data', { token, userId, username });
      return;
    }
    console.log('useAuthStore: Setting auth', { userId, username });
    localStorage.setItem('token', token);
    localStorage.setItem('userId', userId);
    localStorage.setItem('username', username);
    set({ token, userId, username, isAuthenticated: true });
  },
  logout: () => {
    console.log('useAuthStore: Logging out');
    localStorage.removeItem('token');
    localStorage.removeItem('userId');
    localStorage.removeItem('username');
    set({ token: null, userId: null, username: null, isAuthenticated: false });
  },
}));
