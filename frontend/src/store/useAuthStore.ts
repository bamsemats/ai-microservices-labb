import { create } from 'zustand';

interface AuthState {
  token: string | null;
  userId: string | null;
  username: string | null;
  role: string | null;
  isAuthenticated: boolean;
  isAdmin: boolean;
  setAuth: (token: string, userId: string, username: string, role?: string) => boolean;
  logout: () => void;
  initialize: () => Promise<void>;
}

const getSafeStorageItem = (key: string) => {
  const value = localStorage.getItem(key);
  if (value === 'undefined' || value === 'null' || value === null) return null;
  return value;
};

export const useAuthStore = create<AuthState>((set, get) => ({
  token: null, // Keep only in memory
  userId: getSafeStorageItem('userId'),
  username: getSafeStorageItem('username'),
  role: getSafeStorageItem('role'),
  isAuthenticated: !!getSafeStorageItem('username'),
  isAdmin: getSafeStorageItem('role') === 'ROLE_ADMIN',
  
  setAuth: (token, userId, username, role = 'ROLE_USER') => {
    if (!token || !userId || !username) {
      console.warn('Auth attempt failed: missing required credentials', { hasToken: !!token, hasUserId: !!userId, hasUsername: !!username });
      return false;
    }
    // We don't store token in localStorage for security
    localStorage.setItem('userId', userId);
    localStorage.setItem('username', username);
    localStorage.setItem('role', role);
    set({ token, userId, username, role, isAuthenticated: true, isAdmin: role === 'ROLE_ADMIN' });
    return true;
  },
  
  logout: () => {
    localStorage.removeItem('userId');
    localStorage.removeItem('username');
    localStorage.removeItem('role');
    set({ token: null, userId: null, username: null, role: null, isAuthenticated: false, isAdmin: false });
  },

  initialize: async () => {
    const { isAuthenticated, token } = get();
    if (isAuthenticated && !token) {
      console.log('Detected active session without token. Attempting recovery...');
      // In a real app, we would call a /refresh endpoint here.
      // For this lab, we'll force logout if we can't recover, 
      // as the token is only in memory and lost on refresh.
      // Alternatively, we could use a HttpOnly cookie for the token.
      get().logout();
      window.location.href = '/login';
    }
  }
}));
