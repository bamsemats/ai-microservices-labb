import { useEffect, useRef, useCallback } from 'react';
import { useAuthStore } from '../store/useAuthStore';
import { useChatStore, type Message } from '../store/useChatStore';

export const useWebSocket = () => {
  const socketRef = useRef<WebSocket | null>(null);
  const { token, isAuthenticated } = useAuthStore();
  const { addMessage } = useChatStore();

  const connect = useCallback(() => {
    if (!isAuthenticated || !token || socketRef.current) return;

    // Use absolute URL for WebSocket if needed, or relative if proxied
    // Vite proxy handles /api, but we might need a separate one for /ws
    const wsUrl = `ws://${window.location.host}/ws/messages?token=${token}`;
    
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
      // Simple reconnect logic
      setTimeout(connect, 3000);
    };

    socket.onerror = (error) => {
      console.error('WebSocket Error', error);
      socket.close();
    };

    socketRef.current = socket;
  }, [token, isAuthenticated, addMessage]);

  useEffect(() => {
    connect();
    return () => {
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
