import { create } from 'zustand';

export interface UITheme {
  theme: string;
  mode: 'light' | 'dark';
  intensity: number;
  adaptationEnabled: boolean;
  color?: string;
  primaryColor?: string;
  secondaryColor?: string;
  blurAmount?: number;
  glassOpacity?: number;
  glowIntensity?: number;
}

interface UIState {
  currentTheme: UITheme;
  sidebarOpen: boolean;
  setTheme: (theme: Partial<UITheme>) => void;
  resetTheme: () => void;
  toggleSidebar: (open?: boolean) => void;
}

const DEFAULT_THEME: UITheme = {
  theme: 'default',
  mode: 'dark',
  intensity: 0.5,
  adaptationEnabled: true,
};

const getPersistedUI = (): Partial<UITheme> | null => {
  const stored = localStorage.getItem('ui-theme');
  if (!stored) return null;
  try {
    return JSON.parse(stored);
  } catch {
    return null;
  }
};

export const useUIStore = create<UIState>((set) => ({
  currentTheme: { ...DEFAULT_THEME, ...getPersistedUI() },
  sidebarOpen: false,
  setTheme: (theme) => set((state) => {
    const newTheme = { ...state.currentTheme, ...theme };
    localStorage.setItem('ui-theme', JSON.stringify(newTheme));
    return { currentTheme: newTheme };
  }),
  resetTheme: () => set(() => {
    localStorage.removeItem('ui-theme');
    return { currentTheme: DEFAULT_THEME };
  }),
  toggleSidebar: (open) => set((state) => ({
    sidebarOpen: open !== undefined ? open : !state.sidebarOpen
  })),
}));
