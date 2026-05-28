import { create } from 'zustand';

interface AuthState {
  token: string | null;
  refreshToken: string | null;
  userId: string | null;
  username: string | null;
  displayName: string | null;
  role: string | null;
  isAuthenticated: boolean;
  isAdmin: boolean;
  setAuth: (token: string, userId: string, username: string, role?: string, displayName?: string | null, refreshToken?: string) => boolean;
  setDisplayName: (displayName: string | null) => void;
  logout: () => void;
  initialize: () => Promise<void>;
}

const getSafeStorageItem = (key: string) => {
  const value = localStorage.getItem(key);
  if (value === 'undefined' || value === 'null' || value === null) return null;
  return value;
};

const getPersistedAuth = () => {
  const token = getSafeStorageItem('accessToken');
  const refreshToken = getSafeStorageItem('refreshToken');
  const userId = getSafeStorageItem('userId');
  const username = getSafeStorageItem('username');
  const displayName = getSafeStorageItem('displayName');
  const role = getSafeStorageItem('role');
  
  if (token && userId && username) {
    return { token, refreshToken, userId, username, displayName, role };
  }
  return null;
};

export const useAuthStore = create<AuthState>((set) => {
  const persisted = getPersistedAuth();

  return {
    token: persisted?.token || null,
    refreshToken: persisted?.refreshToken || null,
    userId: persisted?.userId || null,
    username: persisted?.username || null,
    displayName: persisted?.displayName || null,
    role: persisted?.role || null,
    isAuthenticated: !!persisted?.token,
    isAdmin: persisted?.role === 'ROLE_ADMIN',
    
    setAuth: (token, userId, username, role = 'ROLE_USER', displayName = null, refreshToken = '') => {
      if (!token || !userId || !username) {
        console.warn('Auth attempt failed: missing required credentials', { hasToken: !!token, hasUserId: !!userId, hasUsername: !!username });
        return false;
      }
      
      // Persist credentials
      localStorage.setItem('accessToken', token);
      if (refreshToken) localStorage.setItem('refreshToken', refreshToken);
      localStorage.setItem('userId', userId);
      localStorage.setItem('username', username);
      if (displayName) localStorage.setItem('displayName', displayName);
      localStorage.setItem('role', role);
      
      set({ 
        token, 
        refreshToken: refreshToken || null,
        userId, 
        username, 
        displayName,
        role, 
        isAuthenticated: true, 
        isAdmin: role === 'ROLE_ADMIN' 
      });
      return true;
    },

    setDisplayName: (displayName) => {
      if (displayName) {
        localStorage.setItem('displayName', displayName);
      } else {
        localStorage.removeItem('displayName');
      }
      set({ displayName });
    },
    
    logout: () => {
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('userId');
      localStorage.removeItem('username');
      localStorage.removeItem('displayName');
      localStorage.removeItem('role');
      set({ token: null, refreshToken: null, userId: null, username: null, displayName: null, role: null, isAuthenticated: false, isAdmin: false });
    },

    initialize: async () => {
      const persisted = getPersistedAuth();
      
      if (persisted) {
        set({ 
          token: persisted.token,
          refreshToken: persisted.refreshToken,
          username: persisted.username, 
          userId: persisted.userId, 
          displayName: persisted.displayName,
          role: persisted.role, 
          isAuthenticated: true, 
          isAdmin: persisted.role === 'ROLE_ADMIN' 
        });
      } else {
        set({ 
          token: null,
          refreshToken: null,
          username: null, 
          userId: null, 
          displayName: null,
          role: null, 
          isAuthenticated: false, 
          isAdmin: false 
        });
      }
    }
  };
});
