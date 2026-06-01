import React, { useState, useRef, useEffect, useMemo } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { useSearchParams } from 'react-router-dom';
import { useAuthStore } from '../store/useAuthStore';
import { useChatStore, type Message } from '../store/useChatStore';
import { useFrequencyStore } from '../store/useFrequencyStore';
import { useWebSocket } from '../hooks/useWebSocket';
import MessageBubble from '../components/MessageBubble';
import MessageComposer from '../components/MessageComposer';
import ThinkingBubble from '../components/ThinkingBubble';
import { getDMChannelId } from '../utils/dmUtils';

import MainLayout from '../components/MainLayout';

const ChatPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const receiverId = searchParams.get('receiver') || 'home';
  
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
  const { frequencies } = useFrequencyStore();

  const { sendMessage: sendWs, sendTyping } = useWebSocket();

  useEffect(() => {
    let effectiveChannelId = receiverId === 'home' || receiverId === 'all' ? 'general' : receiverId;
    
    // If it's a DM (not global and not a frequency), use sorted combined ID
    if (receiverId !== 'home' && receiverId !== 'all' && !receiverId.startsWith('freq-') && userId) {
      effectiveChannelId = getDMChannelId(userId, receiverId);
    }

    setActiveChannelId(effectiveChannelId);
    if (token) fetchMessages(receiverId, userId || undefined);
  }, [receiverId, token, userId, fetchMessages, setActiveChannelId]);

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
      // Update the existing pending message to 'failed' status
      pushToStore({ ...newMsg, id: tempId, status: 'failed' });
    }
  };

  const handleTyping = (isTyping: boolean) => {
    sendTyping(activeChannelId, isTyping);
  };

  const currentFreq = frequencies.find(f => f.id === receiverId);
  const contextName = currentFreq ? currentFreq.name : (receiverId === 'home' || receiverId === 'all' ? 'general' : (receiverId === userId ? 'Me (Notes)' : receiverId));

  return (
    <MainLayout
      activeReceiver={receiverId}
      prefix={receiverId === 'all' || receiverId === 'home' ? '#' : (currentFreq ? '#' : '@')}
      contextName={contextName}
    >
      <div className="site-page page--chat">
        <div className="message-list" ref={scrollRef}>
          <AnimatePresence initial={false} mode="popLayout">
            {receiverId === 'home' && filteredMessages.length === 0 ? (
              <motion.div 
                key="welcome"
                initial={{ opacity: 0, scale: 0.9 }}
                animate={{ opacity: 1, scale: 1 }}
                exit={{ opacity: 0 }}
                className="message-empty welcome-state"
              >
                <div className="message-empty-icon">🏠</div>
                <h2>Welcome to AdaptaChat</h2>
                <p>Select a channel or direct message to start communicating across frequencies.</p>
              </motion.div>
            ) : filteredMessages.length === 0 ? (
              <motion.div 
                key="empty"
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                className="message-empty"
              >
                <div className="message-empty-icon">💬</div>
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
      </div>

      <AnimatePresence>
        <div className="toast-stack">
          {error && (
          <motion.div 
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0 }}
            className="toast toast--error"
          >
            <span className="error-message">{error}</span>
            <button className="btn btn-icon" onClick={() => setError(null)}>×</button>
          </motion.div>
        )}
        </div>
      </AnimatePresence>
    </MainLayout>
  );
};

export default ChatPage;
