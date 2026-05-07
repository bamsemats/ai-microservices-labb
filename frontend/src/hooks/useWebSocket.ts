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
  
  const token = useAuthStore((state) => state.token);
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const addMessage = useChatStore((state) => state.addMessage);
  const setPresence = usePresenceStore((state) => state.setPresence);

  const connectRef = useRef<(() => void) | null>(null);

  const connect = useCallback(() => {
    if (!isAuthenticated || !token || socketRef.current || !mountedRef.current) return;

    const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws';
    const wsUrl = `${protocol}://${window.location.host}/ws/messages?token=${token}`;
    
    const socket = new WebSocket(wsUrl);

    socket.onopen = () => {
      console.log('WebSocket Connected');
      reconnectAttemptsRef.current = 0;
    };

    socket.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        
        if (data.type === 'UI_ADAPTATION') {
          const { theme, intensity, color, primaryColor, blurAmount, glassOpacity, glowIntensity } = data;
          if (typeof theme === 'string') {
            console.log('Applying AI UI Adaptation:', theme);
            useUIStore.getState().setTheme({ 
              theme, 
              intensity: typeof intensity === 'number' ? intensity : 0.5,
              color: typeof color === 'string' ? color : undefined,
              primaryColor: typeof primaryColor === 'string' ? primaryColor : undefined,
              blurAmount: typeof blurAmount === 'number' ? blurAmount : undefined,
              glassOpacity: typeof glassOpacity === 'number' ? glassOpacity : undefined,
              glowIntensity: typeof glowIntensity === 'number' ? glowIntensity : undefined
            });
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
          const normalizedStatus = (status || '').toString().toUpperCase();
          const statusMap: Record<string, PresenceStatus> = {
            'ONLINE': 'ONLINE',
            'OFFLINE': 'OFFLINE',
            'AWAY': 'AWAY',
            'DND': 'DND',
            'BUSY': 'DND'
          };
          if (typeof userId === 'string' && typeof username === 'string' && normalizedStatus in statusMap) {
            console.log('Received Presence Update:', userId, normalizedStatus);
            setPresence(userId, username, statusMap[normalizedStatus]);
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
      const payload = {
        type: 'CHAT_MESSAGE',
        content,
        timestamp: new Date().toISOString()
      };
      socketRef.current.send(JSON.stringify(payload));
    } else {
      console.error('WebSocket is not open');
    }
  }, []);

  return { sendMessage };
};
