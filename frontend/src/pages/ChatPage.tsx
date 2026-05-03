import React, { useState, useRef, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useAuthStore } from '../store/useAuthStore';
import { useChatStore, type Message, type InjectedContent } from '../store/useChatStore';
import { useWebSocket } from '../hooks/useWebSocket';
import api from '../api/axios';
import Sidebar from '../components/Sidebar';
import MessageBubble from '../components/MessageBubble';
import MessageComposer from '../components/MessageComposer';
import ContentWidget from '../components/ContentWidget';

type DisplayItem = 
  | { type: 'msg'; data: Message }
  | { type: 'content'; data: InjectedContent };

const ChatPage: React.FC = () => {
  const [receiverId, setReceiverId] = useState('all');
  const { username, userId, logout } = useAuthStore();
  const { messages, injectedContent } = useChatStore();
  useWebSocket();
  const scrollRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, [messages, injectedContent]);

  const handleSend = async (content: string) => {
    if (userId) {
      try {
        if (receiverId === 'all') {
          await api.post('/messages/broadcast', { content });
        } else {
          await api.post('/messages', {
            receiverId: receiverId ?? userId,
            content
          });
        }
      } catch (err) {
        console.error('Failed to send message', err);
      }
    }
  };

  const filteredMessages = messages.filter(msg => {
    if (receiverId === 'all') {
      return !msg.receiverId || msg.receiverId === 'all';
    }
    return (msg.senderId === userId && msg.receiverId === receiverId) || 
           (msg.senderId === receiverId && msg.receiverId === userId);
  });

  // Combine messages and injected content for display
  const displayItems: DisplayItem[] = [
    ...filteredMessages.map(m => ({ type: 'msg' as const, data: m })),
    ...injectedContent.map(c => ({ type: 'content' as const, data: c }))
  ].sort((a, b) => {
    const timeA = a.type === 'msg' ? new Date(a.data.timestamp).getTime() : a.data.timestamp;
    const timeB = b.type === 'msg' ? new Date(b.data.timestamp).getTime() : b.data.timestamp;
    return timeA - timeB;
  });

  return (
    <div className="chat-page-layout">
      <Sidebar activeReceiver={receiverId} onSelectReceiver={setReceiverId} />

      <main className="chat-main-content">
        <header className="chat-navbar glass-panel">
          <div className="active-context">
            <span className="context-prefix">{receiverId === 'all' ? '#' : '@'}</span>
            <span className="context-name">{receiverId === 'all' ? 'general' : (receiverId === userId ? 'Me (Notes)' : receiverId)}</span>
          </div>
          <div className="user-controls">
            <div className="user-badge glass-card">
              <span className="username">{username}</span>
              <div className="user-avatar">{username?.charAt(0).toUpperCase()}</div>
            </div>
            <button className="lumina-button secondary logout-btn" onClick={logout}>Logout</button>
          </div>
        </header>

        <section className="message-stream">
          <div className="message-list" ref={scrollRef}>
            <AnimatePresence initial={false} mode="popLayout">
              {displayItems.length === 0 ? (
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
            </AnimatePresence>
          </div>

          <MessageComposer 
            onSend={handleSend} 
            placeholder={`Message ${receiverId === 'all' ? '#general' : 'this frequency'}...`}
          />
        </section>
      </main>

      <style>{`
        .chat-page-layout {
          display: flex;
          width: 100vw;
          height: 100vh;
          background: radial-gradient(circle at top right, rgba(139, 92, 246, 0.05) 0%, transparent 40%),
                      radial-gradient(circle at bottom left, rgba(236, 72, 153, 0.05) 0%, transparent 40%);
        }

        .chat-main-content {
          flex: 1;
          display: flex;
          flex-direction: column;
          padding: 1rem 1rem 1rem 0;
          overflow: hidden;
        }

        .chat-navbar {
          height: 4.5rem;
          display: flex;
          justify-content: space-between;
          align-items: center;
          padding: 0 2rem;
          margin-bottom: 1rem;
          flex-shrink: 0;
        }

        .active-context {
          display: flex;
          align-items: center;
          gap: 0.5rem;
        }

        .context-prefix {
          font-weight: 700;
          color: var(--accent-primary);
          font-size: 1.25rem;
        }

        .context-name {
          font-weight: 700;
          font-size: 1.1rem;
          letter-spacing: -0.01em;
        }

        .user-controls {
          display: flex;
          align-items: center;
          gap: 1.5rem;
        }

        .user-badge {
          display: flex;
          align-items: center;
          gap: 0.75rem;
          padding: 0.5rem 0.75rem 0.5rem 1rem !important;
          border-radius: 2rem !important;
        }

        .username {
          font-size: 0.875rem;
          font-weight: 600;
          color: var(--text-primary);
        }

        .user-avatar {
          width: 2rem;
          height: 2rem;
          background: var(--accent-gradient);
          border-radius: 50%;
          display: flex;
          align-items: center;
          justify-content: center;
          font-weight: 800;
          font-size: 0.875rem;
          color: white;
          box-shadow: var(--accent-glow);
        }

        .logout-btn {
          padding: 0.5rem 1rem !important;
          font-size: 0.8125rem !important;
          border-color: rgba(244, 63, 94, 0.2) !important;
          color: var(--error) !important;
        }

        .logout-btn:hover {
          background: rgba(244, 63, 94, 0.1) !important;
          border-color: var(--error) !important;
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
          max-width: 65%;
          padding: 1rem 1.25rem;
          border-radius: 1.25rem;
          background: rgba(255, 255, 255, 0.03);
          border: 1px solid rgba(255, 255, 255, 0.05);
          align-self: flex-start;
          backdrop-filter: blur(8px);
          position: relative;
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
      `}</style>
    </div>
  );
};

export default ChatPage;
