import { create } from 'zustand';

export interface Message {
  id: string;
  senderId: string;
  senderName: string;
  receiverId?: string; // Added receiverId
  content: string;
  authorType?: 'USER' | 'BOT';
  timestamp: string;
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
  addMessage: (message: Message) => void;
  addInjectedContent: (content: InjectedContent) => void;
  setMessages: (messages: Message[]) => void;
  clearMessages: () => void;
}

export const useChatStore = create<ChatState>((set) => ({
  messages: [],
  injectedContent: [],
  addMessage: (message) => set((state) => ({ 
    messages: [...state.messages, message] 
  })),
  addInjectedContent: (content) => set((state) => ({
    injectedContent: [...state.injectedContent, content]
  })),
  setMessages: (messages) => set({ messages }),
  clearMessages: () => set({ messages: [], injectedContent: [] }),
}));
