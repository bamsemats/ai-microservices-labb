import { create } from 'zustand';
import api from '../api/axios';

export interface Frequency {
  id: string;
  name: string;
  description?: string;
  ownerId: string;
  members: string[];
}

interface FrequencyState {
  frequencies: Frequency[];
  fetchFrequencies: () => Promise<void>;
  createFrequency: (name: string, description?: string) => Promise<Frequency>;
  joinFrequency: (id: string) => Promise<void>;
  leaveFrequency: (id: string) => Promise<void>;
  renameFrequency: (id: string, newName: string) => Promise<void>;
  inviteMember: (id: string, memberId: string) => Promise<void>;
  kickMember: (id: string, memberId: string) => Promise<void>;
}

export const useFrequencyStore = create<FrequencyState>((set, get) => ({
  frequencies: [],
  fetchFrequencies: async () => {
    try {
      const response = await api.get('/frequencies');
      set({ frequencies: response.data });
    } catch (error) {
      console.error('Failed to fetch frequencies', error);
    }
  },
  createFrequency: async (name, description) => {
    const response = await api.post('/frequencies', { name, description });
    const newFreq = response.data;
    set(state => ({ frequencies: [...state.frequencies, newFreq] }));
    return newFreq;
  },
  joinFrequency: async (id) => {
    await api.post(`/frequencies/${encodeURIComponent(id)}/join`);
    await get().fetchFrequencies();
  },
  leaveFrequency: async (id) => {
    await api.post(`/frequencies/${encodeURIComponent(id)}/leave`);
    set(state => ({ frequencies: state.frequencies.filter(f => f.id !== id) }));
  },
  renameFrequency: async (id, newName) => {
    await api.put(`/frequencies/${encodeURIComponent(id)}/rename`, { name: newName });
    await get().fetchFrequencies();
  },
  inviteMember: async (id, memberId) => {
    await api.post(`/frequencies/${encodeURIComponent(id)}/members/${encodeURIComponent(memberId)}`);
    await get().fetchFrequencies();
  },
  kickMember: async (id, memberId) => {
    await api.delete(`/frequencies/${encodeURIComponent(id)}/members/${encodeURIComponent(memberId)}`);
    await get().fetchFrequencies();
  }
}));
