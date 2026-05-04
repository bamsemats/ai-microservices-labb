import { useEffect, useRef, useCallback } from 'react';
import { useAuthStore } from '../store/useAuthStore';
import { useChatStore, type Message } from '../store/useChatStore';
import { useUIStore } from '../store/useUIStore';
import { usePresenceStore } from '../store/usePresenceStore';

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
    const wsUrl = `${protocol}://${window.location.host}/ws/messages?token=${token}`;
    
    const socket = new WebSocket(wsUrl);

    socket.onopen = () => {
      console.log('WebSocket Connected');
    };

    socket.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        
        if (data.type === 'UI_ADAPTATION') {
          console.log('Applying AI UI Adaptation:', data.theme);
          useUIStore.getState().setTheme({
            theme: data.theme,
            intensity: data.intensity,
            color: data.color
          });
        } else if (data.type === 'CONTENT_INJECTION') {
          console.log('Received Content Injection:', data.contentType);
          useChatStore.getState().addInjectedContent(data);
        } else if (data.type === 'PRESENCE_UPDATE') {
          console.log('Received Presence Update:', data.userId, data.status);
          setPresence(data.userId, data.username, data.status);
        } else {
          const message: Message = data;
          addMessage(message);
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
  }, [token, isAuthenticated, addMessage]);

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
