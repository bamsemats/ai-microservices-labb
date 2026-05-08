import { create } from 'zustand';

export interface UITheme {
  theme: string;
  mode: 'light' | 'dark';
  intensity: number;
  color?: string;
  primaryColor?: string;
  blurAmount?: number;
  glassOpacity?: number;
  glowIntensity?: number;
}

interface UIState {
  currentTheme: UITheme;
  sidebarOpen: boolean;
  setTheme: (theme: Partial<UITheme>) => void;
  toggleSidebar: (open?: boolean) => void;
}

export const useUIStore = create<UIState>((set) => ({
  currentTheme: {
    theme: 'default',
    mode: 'dark',
    intensity: 0.5,
  },
  sidebarOpen: false,
  setTheme: (theme) => set((state) => ({
    currentTheme: { ...state.currentTheme, ...theme }
  })),
  toggleSidebar: (open) => set((state) => ({
    sidebarOpen: open !== undefined ? open : !state.sidebarOpen
  })),
}));
