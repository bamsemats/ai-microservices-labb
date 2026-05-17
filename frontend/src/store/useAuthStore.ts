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

const getPersistedAuth = () => {
  const userId = getSafeStorageItem('userId');
  const username = getSafeStorageItem('username');
  const role = getSafeStorageItem('role');
  
  // NOTE: Rehydrate auth from an HttpOnly refresh/session cookie (server-driven refresh endpoint)
  // rather than client-side storage. Token is null until rehydration.
  const token = null;

  if (userId && username) {
    return { token, userId, username, role };
  }
  return null;
};

export const useAuthStore = create<AuthState>((set) => {
  const persisted = getPersistedAuth();

  return {
    token: persisted?.token || null,
    userId: persisted?.userId || null,
    username: persisted?.username || null,
    role: persisted?.role || null,
    isAuthenticated: false,
    isAdmin: false,
    
    setAuth: (token, userId, username, role = 'ROLE_USER') => {
      if (!token || !userId || !username) {
        console.warn('Auth attempt failed: missing required credentials', { hasToken: !!token, hasUserId: !!userId, hasUsername: !!username });
        return false;
      }
      
      // Persist only non-sensitive credentials
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
      const persisted = getPersistedAuth();
      
      if (persisted) {
        set({ 
          token: persisted.token,
          username: persisted.username, 
          userId: persisted.userId, 
          role: persisted.role, 
          isAuthenticated: false, 
          isAdmin: false 
        });
      } else {
        set({ 
          token: null,
          username: null, 
          userId: null, 
          role: null, 
          isAuthenticated: false, 
          isAdmin: false 
        });
      }
    }
  };
});
