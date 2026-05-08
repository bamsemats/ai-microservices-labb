import { create } from 'zustand';

export interface UITheme {
  theme: string;
  intensity: number;
  color?: string;
  primaryColor?: string;
  blurAmount?: number;
  glassOpacity?: number;
  glowIntensity?: number;
}

interface UIState {
  currentTheme: UITheme;
  setTheme: (theme: Partial<UITheme>) => void;
}

export const useUIStore = create<UIState>((set) => ({
  currentTheme: {
    theme: 'default',
    intensity: 0.5,
  },
  setTheme: (theme) => set((state) => ({
    currentTheme: { ...state.currentTheme, ...theme }
  })),
}));
