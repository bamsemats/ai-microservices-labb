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

export interface AiStatusEvent {
  type: 'AI_STATUS';
  status: 'THINKING' | 'COMPLETED' | 'ERROR';
  channelId: string;
  userId?: string;
}

interface ChatState {
  messages: Message[];
  injectedContent: InjectedContent[];
  aiStatus: 'IDLE' | 'THINKING' | 'ERROR';
  addMessage: (message: Message) => void;
  addInjectedContent: (content: InjectedContent) => void;
  setAiStatus: (status: 'IDLE' | 'THINKING' | 'ERROR') => void;
  setMessages: (messages: Message[]) => void;
  clearMessages: () => void;
}

export const useChatStore = create<ChatState>((set) => ({
  messages: [],
  injectedContent: [],
  aiStatus: 'IDLE',
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
}));
