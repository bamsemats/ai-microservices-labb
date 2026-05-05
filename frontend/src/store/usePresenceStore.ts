import { create } from 'zustand';

export type PresenceStatus = 'ONLINE' | 'AWAY' | 'DND' | 'OFFLINE';

interface UserPresence {
  userId: string;
  username: string;
  status: PresenceStatus;
  lastUpdated: number;
}

interface PresenceState {
  presences: Record<string, UserPresence>;
  setPresence: (userId: string, username: string, status: PresenceStatus) => void;
  getPresence: (userId: string) => PresenceStatus;
  fetchPresences: (token: string) => Promise<void>;
}

export const usePresenceStore = create<PresenceState>((set, get) => ({
  presences: {},
  setPresence: (userId, username, status) => set((state) => ({
    presences: {
      ...state.presences,
      [userId]: {
        userId,
        username,
        status,
        lastUpdated: Date.now(),
      },
    },
  })),
  getPresence: (userId) => get().presences[userId]?.status || 'OFFLINE',
  fetchPresences: async (token) => {
    try {
      const response = await fetch('/api/users/presence', {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });
      if (response.ok) {
        const data = await response.json();
        const presences: Record<string, UserPresence> = {};
        data.forEach((item: { userId: string; username: string; status: PresenceStatus }) => {
          presences[item.userId] = {
            userId: item.userId,
            username: item.username,
            status: item.status,
            lastUpdated: Date.now(),
          };
        });
        set({ presences });
      }
    } catch (err) {
      console.error('Failed to fetch presences', err);
    }
  },
}));
