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
      
      // Since token is not in localStorage, we can't fully restore authenticated state 
      // here without a refresh token flow or cookie. For now, we follow the mandate 
      // to remove token from localStorage and only set isAuthenticated if all are present.
      // If we had a token here (e.g. from an initial in-memory state), we'd check it.
      const { token } = get();
      
      if (token && username && userId) {
        console.log('Restoring session for user:', username);
        set({ isAuthenticated: true, isAdmin: get().role === 'ROLE_ADMIN' });
      } else {
        // If we don't have a token, we aren't truly authenticated in this tab yet
        // but we might want to keep the username/userId for UX if the server 
        // will provide a token via HttpOnly cookie.
        if (!token) {
          set({ isAuthenticated: false });
        }
      }
    }
  };
});
