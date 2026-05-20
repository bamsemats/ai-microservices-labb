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
  addMessage: (message: Message) => void;
  addInjectedContent: (content: InjectedContent) => void;
  setAiStatus: (status: 'IDLE' | 'THINKING' | 'ERROR') => void;
  setMessages: (messages: Message[]) => void;
  clearMessages: () => void;
  setTyping: (username: string, channelId: string, isTyping: boolean) => void;
  markMessageRead: (messageId: string, userId: string) => void;
}

export const useChatStore = create<ChatState>((set) => ({
  messages: [],
  injectedContent: [],
  aiStatus: 'IDLE',
  typingUsers: {},
  addMessage: (message) => set((state) => {
    const existingIndex = state.messages.findIndex((m) => m.id === message.id);
    if (existingIndex !== -1) {
      const updatedMessages = [...state.messages];
      updatedMessages[existingIndex] = {
        ...updatedMessages[existingIndex],
        content: updatedMessages[existingIndex].content + message.content,
      };
      // Reset AI status once we start receiving chunks
      return { messages: updatedMessages, aiStatus: 'IDLE' };
    }
    return { messages: [...state.messages, message], aiStatus: 'IDLE' };
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
