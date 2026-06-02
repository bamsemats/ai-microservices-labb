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
  channelId?: string;
  data: Record<string, string>;
  timestamp: number;
}

interface ChatState {
  messages: Message[];
  injectedContentByChannel: Record<string, InjectedContent[]>;
  aiStatus: 'IDLE' | 'THINKING' | 'ERROR';
  typingUsers: Record<string, string[]>; // channelId -> usernames
  activeChannelId: string;
  setActiveChannelId: (id: string) => void;
  fetchMessages: (channelId: string, currentUserId?: string) => Promise<void>;
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
  injectedContentByChannel: {},
  aiStatus: 'IDLE',
  typingUsers: {},
  activeChannelId: 'home',
  setActiveChannelId: (id) => set({ activeChannelId: id }),
  fetchMessages: async (channelId, currentUserId?) => {
    try {
      const isGlobal = channelId === 'home' || channelId === 'all';
      const params: Record<string, string | undefined> = {};
      
      const effectiveId = isGlobal ? 'general' : channelId;

      if (isGlobal) {
        params.channelId = 'general';
      } else if (currentUserId && channelId !== 'general' && !channelId.startsWith('freq-')) {
        // Heuristic for DM: if it's not a known frequency, treat as DM to this user
        params.receiverId = channelId;
      } else {
        params.channelId = channelId;
      }

      const response = await api.get<Message[]>('/messages', { params });
      
      set((state) => ({
        messages: response.data,
        injectedContentByChannel: {
          ...state.injectedContentByChannel,
          [effectiveId]: [] // Reset only the active channel's injections on explicit fetch
        }
      }));
    } catch (error) {
      console.error('Failed to fetch messages', error);
    }
  },
  sendMessage: (message) => {
    get().addMessage(message);
  },
  addMessage: (message) => set((state) => {
    // Standardize incoming global channel IDs to match frontend state
    const normalizedArrival: Message = {
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
      const existingMsg = updatedMessages[existingIndex];
      
      // Merge fields but preserve 'failed' status unless the incoming payload explicitly indicates success
      const newStatus = (normalizedArrival.status === 'sent' || !normalizedArrival.status) 
        ? (existingMsg.status === 'failed' ? 'failed' : 'sent')
        : normalizedArrival.status;

      updatedMessages[existingIndex] = {
        ...existingMsg,
        ...normalizedArrival,
        status: newStatus
      };
      
      return { 
        messages: updatedMessages, 
        aiStatus: shouldResetAiStatus ? 'IDLE' : state.aiStatus 
      };
    }

    // Binary search to find correct insertion index for ordered messages
    const newMessages = [...state.messages];
    const arrivalTime = new Date(normalizedArrival.timestamp).getTime();
    
    let low = 0;
    let high = newMessages.length;
    
    while (low < high) {
      const mid = (low + high) >>> 1;
      if (new Date(newMessages[mid].timestamp).getTime() < arrivalTime) {
        low = mid + 1;
      } else {
        high = mid;
      }
    }
    
    newMessages.splice(low, 0, normalizedArrival);

    return { 
      messages: newMessages, 
      aiStatus: shouldResetAiStatus ? 'IDLE' : state.aiStatus 
    };
  }),
  addInjectedContent: (content) => set((state) => {
    const rawId = content.channelId || '__global__';
    const channelId = (rawId === 'home' || rawId === 'all') ? 'general' : rawId;
    const currentList = state.injectedContentByChannel[channelId] || [];
    return {
      injectedContentByChannel: {
        ...state.injectedContentByChannel,
        [channelId]: [...currentList, content]
      }
    };
  }),
  setAiStatus: (status) => set({ aiStatus: status }),
  setMessages: (messages) => set({ messages }),
  clearMessages: () => set({ messages: [], injectedContentByChannel: {} }),
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
