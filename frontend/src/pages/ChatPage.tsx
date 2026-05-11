import React, { useState, useRef, useEffect } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { useAuthStore } from '../store/useAuthStore';
import { useChatStore, type Message, type InjectedContent } from '../store/useChatStore';
import { useWebSocket } from '../hooks/useWebSocket';
import api from '../api/axios';
import Sidebar from '../components/Sidebar';
import MessageBubble from '../components/MessageBubble';
import MessageComposer from '../components/MessageComposer';
import ContentWidget from '../components/ContentWidget';
import ThinkingBubble from '../components/ThinkingBubble';
import Navbar from '../components/Navbar';

type DisplayItem = 
  | { type: 'msg'; data: Message }
  | { type: 'content'; data: InjectedContent };

const ChatPage: React.FC = () => {
  const [receiverId, setReceiverId] = useState('home');
  const [error, setError] = useState<string | null>(null);
  const { userId, isAdmin } = useAuthStore();
  const { messages, injectedContent, aiStatus, typingUsers } = useChatStore();
  const { sendTyping, sendReadReceipt } = useWebSocket();
  const scrollRef = useRef<HTMLDivElement>(null);

  const activeChannelId = receiverId === 'home' ? 'general' : receiverId;
  const currentTypingUsers = typingUsers[activeChannelId] || [];

  const filteredMessages = messages.filter(msg => {
    if (receiverId === 'all' || receiverId === 'home') {
      return !msg.receiverId || msg.receiverId === 'all' || msg.channelId === 'general';
    }
    return (msg.senderId === userId && msg.receiverId === receiverId) || 
           (msg.senderId === receiverId && msg.receiverId === userId);
  });

  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
    
    // Read receipt logic: mark visible messages as read when they arrive
    const unreadMessages = filteredMessages.filter(m => m.senderId !== userId && !(m.readBy || []).includes(userId || ''));
    unreadMessages.forEach(m => {
      if (m.id) sendReadReceipt(m.id, m.channelId || activeChannelId);
    });
  }, [messages, injectedContent, aiStatus, receiverId, activeChannelId, filteredMessages, sendReadReceipt, userId]);

  const handleSend = async (content: string) => {
    setError(null);
    try {
      if (receiverId === 'all' || receiverId === 'home') {
        const target = receiverId === 'home' ? 'general' : receiverId;
        if (!isAdmin) {
          setError(`Only admins can broadcast to #${target}.`);
          return;
        }
        await api.post('/messages/broadcast', { content });
      } else {
        await api.post('/messages', {
          receiverId,
          content,
          channelId: activeChannelId
        });
      }
    } catch (err) {
      setError("Failed to transmit frequency. Target may be offline.");
      console.error('Failed to send message', err);
    }
  };

  const handleTyping = (isTyping: boolean) => {
    sendTyping(activeChannelId, isTyping);
  };

  const filteredInjectedContent = (receiverId === 'all' || receiverId === 'home') ? injectedContent : [];

  // Combine messages and injected content for display
  const displayItems: DisplayItem[] = [
    ...filteredMessages.map(m => ({ type: 'msg' as const, data: m })),
    ...filteredInjectedContent.map(c => ({ type: 'content' as const, data: c }))
  ].sort((a, b) => {
    const timeA = a.type === 'msg' ? new Date(a.data.timestamp).getTime() : a.data.timestamp;
    const timeB = b.type === 'msg' ? new Date(b.data.timestamp).getTime() : b.data.timestamp;
    return timeA - timeB;
  });

  return (
    <div className="chat-page-layout">
      <Sidebar activeReceiver={receiverId} onSelectReceiver={setReceiverId} />

      <main className="chat-main-content">
        <Navbar 
          prefix={receiverId === 'all' || receiverId === 'home' ? '#' : '@'}
          contextName={receiverId === 'home' || receiverId === 'all' ? 'general' : (receiverId === userId ? 'Me (Notes)' : receiverId)}
        />

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
              ) : displayItems.length === 0 ? (
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
                displayItems.map((item, idx) => (
                  item.type === 'msg' ? (
                    <MessageBubble 
                      key={item.data.id || `msg-${idx}`} 
                      message={item.data} 
                      isOwn={item.data.senderId === userId} 
                    />
                  ) : (
                    <ContentWidget 
                      key={`content-${idx}`} 
                      content={item.data} 
                    />
                  )
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

          <AnimatePresence>
            {currentTypingUsers.length > 0 && (
              <motion.div 
                initial={{ opacity: 0, y: 5 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0 }}
                className="typing-indicator-wrapper"
              >
                <div className="typing-dots">
                  <span></span><span></span><span></span>
                </div>
                <span className="typing-text">
                  {currentTypingUsers.length === 1 
                    ? `${currentTypingUsers[0]} is typing...` 
                    : `${currentTypingUsers.length} users are typing...`}
                </span>
              </motion.div>
            )}
          </AnimatePresence>

          <MessageComposer 
            onSend={handleSend} 
            onTyping={handleTyping}
            placeholder={`Message ${receiverId === 'home' ? 'general' : (receiverId === 'all' ? '#general' : 'this frequency')}...`}
            disabled={(receiverId === 'all' || receiverId === 'home') && !isAdmin}
          />
        </section>
      </main>

      <AnimatePresence>
        {error && (
          <motion.div 
            initial={{ opacity: 0, y: 50 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: 50 }}
            className="error-toast glass-panel"
          >
            <span className="error-icon">⚠️</span>
            <span className="error-message">{error}</span>
            <button className="close-toast" onClick={() => setError(null)}>×</button>
          </motion.div>
        )}
      </AnimatePresence>

      <style>{`
        .chat-page-layout {
          display: flex;
          width: 100vw;
          height: 100vh;
        }

        .chat-main-content {
          flex: 1;
          display: flex;
          flex-direction: column;
          overflow: hidden;
          background: radial-gradient(circle at top right, rgba(99, 102, 241, 0.05) 0%, transparent 40%),
                      radial-gradient(circle at bottom left, rgba(244, 63, 92, 0.05) 0%, transparent 40%);
        }

        .message-stream {
          flex: 1;
          display: flex;
          flex-direction: column;
          overflow: hidden;
          position: relative;
        }

        .message-list {
          flex: 1;
          padding: 1rem 2rem;
          overflow-y: auto;
          display: flex;
          flex-direction: column;
          gap: 1rem;
        }

        .empty-state {
          flex: 1;
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: center;
          opacity: 0.5;
        }

        .empty-icon {
          font-size: 4rem;
          margin-bottom: 1rem;
        }

        .message-bubble {
          max-width: 75%;
          padding: 1rem 1.25rem;
          border-radius: 1.25rem;
          background: var(--glass-bg);
          backdrop-filter: blur(var(--glass-blur-amount));
          border: 1px solid var(--glass-border);
          align-self: flex-start;
          position: relative;
          box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
        }

        .message-bubble.own {
          align-self: flex-end;
          background: var(--accent-gradient);
          border: none;
          color: white;
          box-shadow: var(--accent-glow);
        }

        .sender-info {
          font-size: 0.75rem;
          font-weight: 700;
          margin-bottom: 0.5rem;
          color: var(--accent-tertiary);
          display: flex;
          align-items: center;
          gap: 0.5rem;
        }

        .message-bubble.own .sender-info {
          color: rgba(255, 255, 255, 0.8);
        }

        .bot-tag {
          background: var(--accent-tertiary);
          color: var(--bg-dark);
          padding: 0.1rem 0.3rem;
          border-radius: 4px;
          font-size: 0.65rem;
        }

        .bubble-content {
          font-size: 1rem;
          line-height: 1.5;
        }

        .message-time {
          font-size: 0.65rem;
          opacity: 0.5;
          margin-top: 0.5rem;
          text-align: right;
        }

        .typing-indicator-wrapper {
          padding: 0.5rem 2rem;
          display: flex;
          align-items: center;
          gap: 0.75rem;
          font-size: 0.75rem;
          color: var(--text-secondary);
          font-weight: 500;
        }

        .typing-dots {
          display: flex;
          gap: 3px;
        }

        .typing-dots span {
          width: 4px;
          height: 4px;
          background: var(--accent-primary);
          border-radius: 50%;
          animation: typing-bounce 1.4s infinite ease-in-out both;
        }

        .typing-dots span:nth-child(1) { animation-delay: -0.32s; }
        .typing-dots span:nth-child(2) { animation-delay: -0.16s; }

        @keyframes typing-bounce {
          0%, 80%, 100% { transform: scale(0); }
          40% { transform: scale(1); }
        }
      `}</style>
    </div>
  );
};

export default ChatPage;
