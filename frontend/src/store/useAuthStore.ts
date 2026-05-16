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

export const useAuthStore = create<AuthState>((set) => {
  const initialToken = getSafeStorageItem('accessToken');
  const initialUserId = getSafeStorageItem('userId');
  const initialUsername = getSafeStorageItem('username');
  const initialRole = getSafeStorageItem('role');
  const initialIsAuthenticated = !!initialToken;

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
      
      // Persist credentials
      localStorage.setItem('accessToken', token);
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
      localStorage.removeItem('accessToken');
      localStorage.removeItem('userId');
      localStorage.removeItem('username');
      localStorage.removeItem('role');
      set({ token: null, userId: null, username: null, role: null, isAuthenticated: false, isAdmin: false });
    },

    initialize: async () => {
      const token = getSafeStorageItem('accessToken');
      const username = getSafeStorageItem('username');
      const userId = getSafeStorageItem('userId');
      const role = getSafeStorageItem('role');
      
      if (token && userId && username) {
        set({ 
          token,
          username, 
          userId, 
          role, 
          isAuthenticated: true, 
          isAdmin: role === 'ROLE_ADMIN' 
        });
      } else {
        set({ 
          token: null,
          username, 
          userId, 
          role, 
          isAuthenticated: false, 
          isAdmin: false 
        });
      }
    }
  };
});
