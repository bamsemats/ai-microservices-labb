import { create } from 'zustand';

export interface Message {
  id: string;
  senderId: string;
  senderName: string;
  receiverId?: string;
  channelId?: string;
  content: string;
  authorType?: 'USER' | 'BOT';
  timestamp: string;
  readBy?: string[];
}

export interface InjectedContent {
  type: 'CONTENT_INJECTION';
  contentType: string;
  data: Record<string, string>;
  timestamp: number;
}

interface ChatState {
  messages: Message[];
  injectedContent: InjectedContent[];
  aiStatus: 'IDLE' | 'THINKING' | 'ERROR';
  typingUsers: Record<string, string[]>; // channelId -> usernames
  activeChannelId: string;
  setActiveChannelId: (id: string) => void;
  fetchMessages: (channelId: string) => Promise<void>;
  sendMessage: (message: Message) => void;
  addMessage: (message: Message) => void;
  addInjectedContent: (content: InjectedContent) => void;
  setAiStatus: (status: 'IDLE' | 'THINKING' | 'ERROR') => void;
  setMessages: (messages: Message[]) => void;
  clearMessages: () => void;
  setTyping: (username: string, channelId: string, isTyping: boolean) => void;
  markMessageRead: (messageId: string, userId: string) => void;
}

import api from '../api/axios';

export const useChatStore = create<ChatState>((set, get) => ({
  messages: [],
  injectedContent: [],
  aiStatus: 'IDLE',
  typingUsers: {},
  activeChannelId: 'home',
  setActiveChannelId: (id) => set({ activeChannelId: id }),
  fetchMessages: async (channelId) => {
    try {
      const isGlobal = channelId === 'home' || channelId === 'all';
      const params = isGlobal ? { channelId: 'general' } : { channelId };
      const response = await api.get('/messages', { params });
      set({ messages: response.data, injectedContent: [] });
    } catch (error) {
      console.error('Failed to fetch messages', error);
    }
  },
  sendMessage: (message) => {
    get().addMessage(message);
  },
  addMessage: (message) => set((state) => {
    const existingIndex = state.messages.findIndex((m) => m.id === message.id);
    const shouldResetAiStatus = message.authorType === 'BOT';

    if (existingIndex !== -1) {
      const updatedMessages = [...state.messages];
      updatedMessages[existingIndex] = {
        ...updatedMessages[existingIndex],
        ...message,
      };
      // Reset AI status once we start receiving messages from the bot
      return { 
        messages: updatedMessages, 
        aiStatus: shouldResetAiStatus ? 'IDLE' : state.aiStatus 
      };
    }

    return { 
      messages: [...state.messages, message], 
      aiStatus: shouldResetAiStatus ? 'IDLE' : state.aiStatus 
    };
  }),
  addInjectedContent: (content) => set((state) => ({
    injectedContent: [...state.injectedContent, content]
  })),
  setAiStatus: (status) => set({ aiStatus: status }),
  setMessages: (messages) => set({ messages, injectedContent: [] }),
  clearMessages: () => set({ messages: [], injectedContent: [] }),
  setTyping: (username, channelId, isTyping) => set((state) => {
    const channelTyping = state.typingUsers[channelId] || [];
    const newChannelTyping = isTyping 
      ? Array.from(new Set([...channelTyping, username]))
      : channelTyping.filter(u => u !== username);
    
    return {
      typingUsers: {
        ...state.typingUsers,
        [channelId]: newChannelTyping
      }
    };
  }),
  markMessageRead: (messageId, userId) => set((state) => ({
    messages: state.messages.map(m => 
      m.id === messageId 
        ? { ...m, readBy: Array.from(new Set([...(m.readBy || []), userId])) }
        : m
    )
  }))
}));
