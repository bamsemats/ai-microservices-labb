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

interface ChatState {
  messages: Message[];
  addMessage: (message: Message) => void;
  setMessages: (messages: Message[]) => void;
  clearMessages: () => void;
}

export const useChatStore = create<ChatState>((set) => ({
  messages: [],
  addMessage: (message) => set((state) => ({ 
    messages: [...state.messages, message] 
  })),
  setMessages: (messages) => set({ messages }),
  clearMessages: () => set({ messages: [] }),
}));
