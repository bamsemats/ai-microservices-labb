import { useEffect, useRef, useCallback } from 'react';
import { useAuthStore } from '../store/useAuthStore';
import { useChatStore, type Message } from '../store/useChatStore';
import { useUIStore } from '../store/useUIStore';
import { usePresenceStore, type PresenceStatus } from '../store/usePresenceStore';

type AiStatus = 'IDLE' | 'THINKING' | 'ERROR';

export const useWebSocket = () => {
  const socketRef = useRef<WebSocket | null>(null);
  const reconnectTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const reconnectAttemptsRef = useRef(0);
  const mountedRef = useRef(true);
  
  const { token, isAuthenticated } = useAuthStore((state) => ({ 
    token: state.token, 
    isAuthenticated: state.isAuthenticated 
  }));
  const addMessage = useChatStore((state) => state.addMessage);
  const setPresence = usePresenceStore((state) => state.setPresence);

  const connectRef = useRef<(() => void) | null>(null);

  const connect = useCallback(() => {
    if (!isAuthenticated || !token || socketRef.current || !mountedRef.current) return;

    const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws';
    const wsUrl = `${protocol}://${window.location.host}/ws/messages`;
    
    const socket = new WebSocket(wsUrl);

    socket.onopen = () => {
      console.log('WebSocket Connected');
      reconnectAttemptsRef.current = 0;
      if (token) {
        socket.send(JSON.stringify({ type: 'auth', token }));
      }
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
          const statusMap: Record<string, PresenceStatus> = {
            'online': 'ONLINE',
            'offline': 'OFFLINE',
            'away': 'AWAY',
            'busy': 'DND'
          };
          if (typeof userId === 'string' && typeof username === 'string' && status in statusMap) {
            console.log('Received Presence Update:', userId, status);
            setPresence(userId, username, statusMap[status]);
          }
        } else {
          const isValidMessage = (m: unknown): m is Message => {
            const msg = m as Record<string, unknown>;
            return !!msg &&
                   typeof msg.id === 'string' &&
                   typeof msg.senderId === 'string' &&
                   typeof msg.senderName === 'string' &&
                   typeof msg.content === 'string' &&
                   (typeof msg.timestamp === 'number' || (typeof msg.timestamp === 'string' && !isNaN(Date.parse(msg.timestamp))));
          };
          if (isValidMessage(data)) {
            addMessage(data);
          }
        }
      } catch (err) {
        console.error('Failed to parse WebSocket message', err);
      }
    };

    socket.onclose = (event) => {
      console.log(`WebSocket Disconnected (Code: ${event.code})`);
      
      if (socketRef.current === socket) {
        socketRef.current = null;

        // Bailing for auth-related codes (1008 or 4xxx custom)
        if (event.code === 1008 || (event.code >= 4000 && event.code < 5000)) {
          console.error('WebSocket closed due to authentication/policy violation. Stopping reconnect.');
          if (reconnectTimerRef.current) {
            clearTimeout(reconnectTimerRef.current);
            reconnectTimerRef.current = null;
          }
          return;
        }

        if (mountedRef.current && connectRef.current) {
          const delay = Math.min(1000 * Math.pow(2, reconnectAttemptsRef.current), 30000);
          console.log(`Scheduling reconnect in ${delay}ms (Attempt: ${reconnectAttemptsRef.current + 1})`);
          reconnectTimerRef.current = setTimeout(() => {
            reconnectAttemptsRef.current++;
            connectRef.current?.();
          }, delay);
        }
      }
    };

    socket.onerror = (error) => {
      console.error('WebSocket Error', error);
      // Let onclose handle the logic
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
