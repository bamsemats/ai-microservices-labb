import { useEffect, useRef, useCallback } from 'react';
import { useAuthStore } from '../store/useAuthStore';
import { useChatStore, type Message } from '../store/useChatStore';

export const useWebSocket = () => {
  const socketRef = useRef<WebSocket | null>(null);
  const reconnectTimerRef = useRef<NodeJS.Timeout | null>(null);
  const mountedRef = useRef(true);
  const { token, isAuthenticated } = useAuthStore();
  const { addMessage } = useChatStore();

  const connect = useCallback(() => {
    if (!isAuthenticated || !token || socketRef.current || !mountedRef.current) return;

    const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws';
    const wsUrl = `\${protocol}://\${window.location.host}/ws/messages?token=\${token}`;
    
    const socket = new WebSocket(wsUrl);

    socket.onopen = () => {
      console.log('WebSocket Connected');
    };

    socket.onmessage = (event) => {
      try {
        const message: Message = JSON.parse(event.data);
        addMessage(message);
      } catch (err) {
        console.error('Failed to parse WebSocket message', err);
      }
    };

    socket.onclose = () => {
      console.log('WebSocket Disconnected');
      socketRef.current = null;
      if (mountedRef.current) {
        reconnectTimerRef.current = setTimeout(connect, 3000);
      }
    };

    socket.onerror = (error) => {
      console.error('WebSocket Error', error);
      socket.close();
    };

    socketRef.current = socket;
  }, [token, isAuthenticated, addMessage]);

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
