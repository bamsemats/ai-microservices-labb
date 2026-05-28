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
  pendingFriends: Friend[];
  fetchFriends: () => Promise<void>;
  fetchPendingFriends: () => Promise<void>;
  sendRequest: (friendId: string) => Promise<void>;
  acceptRequest: (friendId: string) => Promise<void>;
  removeFriend: (friendId: string) => Promise<void>;
}

export const useSocialStore = create<SocialState>((set, get) => ({
  friends: [],
  pendingFriends: [],
  fetchFriends: async () => {
    try {
      const response = await api.get('/friends');
      set({ friends: response.data });
    } catch (error) {
      console.error('Failed to fetch friends', error);
    }
  },
  fetchPendingFriends: async () => {
    try {
      const response = await api.get('/friends/pending');
      set({ pendingFriends: response.data });
    } catch (error) {
      console.error('Failed to fetch pending requests', error);
    }
  },
  sendRequest: async (friendId) => {
    await api.post(`/friends/request/${encodeURIComponent(friendId)}`);
    await get().fetchFriends();
    await get().fetchPendingFriends();
  },
  acceptRequest: async (friendId) => {
    await api.post(`/friends/accept/${encodeURIComponent(friendId)}`);
    await get().fetchFriends();
    await get().fetchPendingFriends();
  },
  removeFriend: async (friendId) => {
    await api.delete(`/friends/${encodeURIComponent(friendId)}`);
    set(state => ({ 
      friends: state.friends.filter(f => f.id !== friendId),
      pendingFriends: state.pendingFriends.filter(f => f.id !== friendId)
    }));
  }
}));
