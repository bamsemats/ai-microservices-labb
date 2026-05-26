import { create } from 'zustand';
import api from '../api/axios';

export interface Friend {
  id: string;
  username: string;
  displayName?: string;
  enabled: boolean;
}

interface SocialState {
  friends: Friend[];
  fetchFriends: () => Promise<void>;
  sendRequest: (friendId: string) => Promise<void>;
  acceptRequest: (friendId: string) => Promise<void>;
  removeFriend: (friendId: string) => Promise<void>;
}

export const useSocialStore = create<SocialState>((set, get) => ({
  friends: [],
  fetchFriends: async () => {
    try {
      const response = await api.get('/friends');
      set({ friends: response.data });
    } catch (error) {
      console.error('Failed to fetch friends', error);
    }
  },
  sendRequest: async (friendId) => {
    await api.post(`/friends/request/${encodeURIComponent(friendId)}`);
    await get().fetchFriends();
  },
  acceptRequest: async (friendId) => {
    await api.post(`/friends/accept/${encodeURIComponent(friendId)}`);
    await get().fetchFriends();
  },
  removeFriend: async (friendId) => {
    await api.delete(`/friends/${encodeURIComponent(friendId)}`);
    set(state => ({ friends: state.friends.filter(f => f.id !== friendId) }));
  }
}));
