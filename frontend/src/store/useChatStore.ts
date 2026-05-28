import { create } from 'zustand';
import api from '../api/axios';

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
  status?: 'pending' | 'sent' | 'failed';
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
    // Standardize incoming global channel IDs to match frontend state
    const normalizedArrival = {
      ...message,
      channelId: (message.channelId === 'home' || message.channelId === 'all') ? 'general' : message.channelId
    };

    // 1. Exact ID match (covers server echoes of unique message IDs)
    let existingIndex = state.messages.findIndex((m) => m.id === normalizedArrival.id);

    // 2. Optimistic match (covers server echoes of our own messages that got a real ID)
    if (existingIndex === -1 && normalizedArrival.authorType !== 'BOT') {
      existingIndex = state.messages.findIndex((m) => 
        m.status === 'pending' && 
        m.content === normalizedArrival.content && 
        m.senderId === normalizedArrival.senderId &&
        Math.abs(new Date(m.timestamp).getTime() - new Date(normalizedArrival.timestamp).getTime()) < 5000
      );
    }

    const shouldResetAiStatus = normalizedArrival.authorType === 'BOT';

    if (existingIndex !== -1) {
      const updatedMessages = [...state.messages];
      updatedMessages[existingIndex] = {
        ...updatedMessages[existingIndex],
        ...normalizedArrival,
        status: 'sent' // Mark as sent if it was pending
      };
      return { 
        messages: updatedMessages.sort((a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime()), 
        aiStatus: shouldResetAiStatus ? 'IDLE' : state.aiStatus 
      };
    }

    return { 
      messages: [...state.messages, normalizedArrival].sort((a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime()), 
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
