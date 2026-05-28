import { create } from 'zustand';

export interface UITheme {
  theme: string;
  mode: 'light' | 'dark';
  intensity: number;
  baseIntensity: number; // Unmutated user preference
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
  injectionPanelOpen: boolean;
  setTheme: (theme: Partial<UITheme>) => void;
  resetTheme: () => void;
  toggleSidebar: (open?: boolean) => void;
  toggleInjectionPanel: (open?: boolean) => void;
}

const DEFAULT_THEME: UITheme = {
  theme: 'default',
  mode: 'dark',
  intensity: 0.5,
  baseIntensity: 0.5,
  adaptationEnabled: true,
};

const getPersistedUI = (): Partial<UITheme> | null => {
  const stored = localStorage.getItem('ui-theme');
  if (!stored) return null;
  try {
    const parsed = JSON.parse(stored);
    // Migration: ensure baseIntensity exists if intensity does
    if (parsed.intensity !== undefined && parsed.baseIntensity === undefined) {
      parsed.baseIntensity = parsed.intensity;
    }
    return parsed;
  } catch {
    return null;
  }
};

export const useUIStore = create<UIState>((set) => ({
  currentTheme: { ...DEFAULT_THEME, ...getPersistedUI() },
  sidebarOpen: typeof window !== 'undefined' ? window.innerWidth > 768 : false,
  injectionPanelOpen: false,
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
  toggleInjectionPanel: (open) => set((state) => ({
    injectionPanelOpen: open !== undefined ? open : !state.injectionPanelOpen
  })),
}));
