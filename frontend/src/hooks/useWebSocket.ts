import { useEffect, useRef, useCallback } from 'react';
import { useAuthStore } from '../store/useAuthStore';
import { useChatStore, type Message } from '../store/useChatStore';
import { useUIStore } from '../store/useUIStore';
import { usePresenceStore } from '../store/usePresenceStore';

type AiStatus = 'IDLE' | 'THINKING' | 'ERROR';

export const useWebSocket = () => {
  const socketRef = useRef<WebSocket | null>(null);
  const reconnectTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const mountedRef = useRef(true);
  const { token, isAuthenticated } = useAuthStore();
  const { addMessage } = useChatStore();
  const { setPresence } = usePresenceStore();

  const connectRef = useRef<(() => void) | null>(null);

  const connect = useCallback(() => {
    if (!isAuthenticated || !token || socketRef.current || !mountedRef.current) return;

    const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws';
    const wsUrl = `${protocol}://${window.location.host}/ws/messages`;
    
    const socket = new WebSocket(wsUrl);

    socket.onopen = () => {
      console.log('WebSocket Connected');
    };

    socket.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        
        if (data.type === 'UI_ADAPTATION') {
          const { theme, intensity, color } = data;
          if (typeof theme === 'string' && typeof intensity === 'number' && typeof color === 'string') {
            console.log('Applying AI UI Adaptation:', theme);
            useUIStore.getState().setTheme({ theme, intensity, color });
          }
        } else if (data.type === 'CONTENT_INJECTION') {
          if (data.contentType && data.data) {
            console.log('Received Content Injection:', data.contentType);
            useChatStore.getState().addInjectedContent(data);
          }
        } else if (data.type === 'AI_STATUS') {
          const status = data.status;
          const allowedStatuses = ['IDLE', 'THINKING', 'ERROR', 'COMPLETED'];
          if (allowedStatuses.includes(status)) {
            console.log('Received AI Status:', status);
            const mappedStatus: AiStatus = status === 'COMPLETED' ? 'IDLE' : status;
            useChatStore.getState().setAiStatus(mappedStatus);
          }
        } else if (data.type === 'PRESENCE_UPDATE') {
          const { userId, username, status } = data;
          const allowedStatuses = ['online', 'offline', 'away', 'busy'];
          if (typeof userId === 'string' && typeof username === 'string' && allowedStatuses.includes(status)) {
            console.log('Received Presence Update:', userId, status);
            setPresence(userId, username, status as any); // Cast is safe now after validation
          }
        } else {
          const isValidMessage = (m: any): m is Message => {
            return typeof m.id === 'string' &&
                   typeof m.senderId === 'string' &&
                   typeof m.content === 'string' &&
                   typeof m.channelId === 'string';
          };
          if (isValidMessage(data)) {
            addMessage(data);
          }
        }
      } catch (err) {
        console.error('Failed to parse WebSocket message', err);
      }
    };

    socket.onclose = () => {
      console.log('WebSocket Disconnected');
      socketRef.current = null;
      if (mountedRef.current && connectRef.current) {
        reconnectTimerRef.current = setTimeout(connectRef.current, 3000);
      }
    };

    socket.onerror = (error) => {
      console.error('WebSocket Error', error);
      socket.close();
    };

    socketRef.current = socket;
  }, [token, isAuthenticated, addMessage, setPresence]);

  useEffect(() => {
    connectRef.current = connect;
  }, [connect]);

  useEffect(() => {
    mountedRef.current = true;
    connect();
    return () => {
      mountedRef.current = false;
      if (reconnectTimerRef.current) {
        clearTimeout(reconnectTimerRef.current);
      }
      if (socketRef.current) {
        socketRef.current.close();
        socketRef.current = null;
      }
    };
  }, [connect]);

  const sendMessage = useCallback((content: string) => {
    if (socketRef.current?.readyState === WebSocket.OPEN) {
      socketRef.current.send(content);
    } else {
      console.error('WebSocket is not open');
    }
  }, []);

  return { sendMessage };
};
