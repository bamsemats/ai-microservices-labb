import React, { useState, useRef, useEffect, useMemo } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { useAuthStore } from '../store/useAuthStore';
import { useChatStore, type Message } from '../store/useChatStore';
import { useWebSocket } from '../hooks/useWebSocket';
import MessageBubble from '../components/MessageBubble';
import MessageComposer from '../components/MessageComposer';
import ThinkingBubble from '../components/ThinkingBubble';

import MainLayout from '../components/MainLayout';

const ChatPage: React.FC = () => {
  const [receiverId, setReceiverId] = useState('home');
  const [error, setError] = useState<string | null>(null);
  const scrollRef = useRef<HTMLDivElement>(null);
  
  const { userId, token } = useAuthStore();
  const { 
    messages, 
    fetchMessages, 
    sendMessage: pushToStore, 
    activeChannelId, 
    setActiveChannelId,
    aiStatus
  } = useChatStore();

  const { sendMessage: sendWs, sendTyping } = useWebSocket();

  useEffect(() => {
    const effectiveChannelId = receiverId === 'home' || receiverId === 'all' ? 'general' : receiverId;
    setActiveChannelId(effectiveChannelId);
    if (token) fetchMessages(receiverId);
  }, [receiverId, token, fetchMessages, setActiveChannelId]);

  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, [messages, aiStatus]);

  const filteredMessages = useMemo(() => {
    return messages.filter(m => m.channelId === activeChannelId);
  }, [messages, activeChannelId]);

  const handleSendMessage = (content: string) => {
    if (!content.trim()) return;
    
    const tempId = `temp-${Date.now()}`;
    const newMsg: Message = {
      id: tempId,
      senderId: userId || 'anonymous',
      senderName: useAuthStore.getState().displayName || useAuthStore.getState().username || 'Me',
      receiverId: receiverId === 'home' ? 'all' : receiverId,
      channelId: activeChannelId,
      content,
      timestamp: new Date().toISOString(),
      authorType: 'USER',
      status: 'pending'
    };

    pushToStore(newMsg);
    const sent = sendWs(newMsg);
    if (!sent) {
      pushToStore({ ...newMsg, status: 'failed' });
    }
  };

  const handleTyping = (isTyping: boolean) => {
    sendTyping(activeChannelId, isTyping);
  };

  return (
    <MainLayout
      activeReceiver={receiverId}
      onSelectReceiver={setReceiverId}
      prefix={receiverId === 'all' || receiverId === 'home' ? '#' : '@'}
      contextName={receiverId === 'home' || receiverId === 'all' ? 'general' : (receiverId === userId ? 'Me (Notes)' : receiverId)}
    >
      <section className="message-stream">
        <div className="message-list" ref={scrollRef}>
          <AnimatePresence initial={false} mode="popLayout">
            {receiverId === 'home' && filteredMessages.length === 0 ? (
              <motion.div 
                key="welcome"
                initial={{ opacity: 0, scale: 0.9 }}
                animate={{ opacity: 1, scale: 1 }}
                exit={{ opacity: 0 }}
                className="empty-state welcome-state"
              >
                <div className="empty-icon">🏠</div>
                <h2>Welcome to AdaptaChat</h2>
                <p>Select a channel or direct message to start communicating across frequencies.</p>
              </motion.div>
            ) : filteredMessages.length === 0 ? (
              <motion.div 
                key="empty"
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                className="empty-state"
              >
                <div className="empty-icon">💬</div>
                <p>No messages in this frequency yet. Start the broadcast.</p>
              </motion.div>
            ) : (
              filteredMessages.map((msg, idx) => (
                <MessageBubble 
                  key={msg.id || `msg-${idx}`} 
                  message={msg} 
                  isOwn={msg.senderId === userId} 
                />
              ))
            )}
            {aiStatus === 'THINKING' && (
              <motion.div
                key="thinking"
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, scale: 0.95 }}
              >
                <ThinkingBubble />
              </motion.div>
            )}
          </AnimatePresence>
        </div>

        <MessageComposer 
          onSend={handleSendMessage} 
          onTyping={handleTyping}
        />
      </section>

      <AnimatePresence>
        {error && (
          <motion.div 
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0 }}
            className="error-toast"
          >
            <span className="error-message">{error}</span>
            <button className="close-toast" onClick={() => setError(null)}>×</button>
          </motion.div>
        )}
      </AnimatePresence>
    </MainLayout>
  );
};

export default ChatPage;
