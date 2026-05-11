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

export const useAuthStore = create<AuthState>((set, get) => {
  const initialToken = null; // Token is now in-memory only
  const initialUserId = getSafeStorageItem('userId');
  const initialUsername = getSafeStorageItem('username');
  const initialRole = getSafeStorageItem('role');
  const initialIsAuthenticated = false; // Cannot be authenticated without in-memory token on start

  return {
    token: initialToken,
    userId: initialUserId,
    username: initialUsername,
    role: initialRole,
    isAuthenticated: initialIsAuthenticated,
    isAdmin: initialIsAuthenticated && initialRole === 'ROLE_ADMIN',
    
    setAuth: (token, userId, username, role = 'ROLE_USER') => {
      if (!token || !userId || !username) {
        console.warn('Auth attempt failed: missing required credentials', { hasToken: !!token, hasUserId: !!userId, hasUsername: !!username });
        return false;
      }
      
      // Persist only non-sensitive data
      localStorage.setItem('userId', userId);
      localStorage.setItem('username', username);
      localStorage.setItem('role', role);
      
      set({ 
        token, 
        userId, 
        username, 
        role, 
        isAuthenticated: true, 
        isAdmin: role === 'ROLE_ADMIN' 
      });
      return true;
    },
    
    logout: () => {
      localStorage.removeItem('userId');
      localStorage.removeItem('username');
      localStorage.removeItem('role');
      set({ token: null, userId: null, username: null, role: null, isAuthenticated: false, isAdmin: false });
    },

    initialize: async () => {
      const username = getSafeStorageItem('username');
      const userId = getSafeStorageItem('userId');
      const role = getSafeStorageItem('role');
      
      // Hydrate basic user info from storage for UX (e.g. showing name in sidebar)
      // but explicitly set isAuthenticated: false since we don't store JWTs in localStorage.
      // Full authentication requires a fresh login or refresh token flow.
      set({ 
        username, 
        userId, 
        role, 
        isAuthenticated: false, 
        isAdmin: role === 'ROLE_ADMIN' 
      });
    }
  };
});
