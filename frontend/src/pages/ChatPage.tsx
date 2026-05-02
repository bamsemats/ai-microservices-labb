import React, { useState, useRef, useEffect } from 'react';
import { useAuthStore } from '../store/useAuthStore';
import { useChatStore } from '../store/useChatStore';
import { useWebSocket } from '../hooks/useWebSocket';
import api from '../api/axios';

const ChatPage: React.FC = () => {
  const [inputValue, setInputValue] = useState('');
  const [receiverId, setReceiverId] = useState('all'); // Default to broadcast or similar
  const { username, userId, logout } = useAuthStore();
  const { messages } = useChatStore();
  useWebSocket(); // Just initialize connection
  const scrollRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, [messages]);

  const handleSend = async (e: React.FormEvent) => {
    e.preventDefault();
    if (inputValue.trim() && userId) {
      try {
        if (receiverId === 'all') {
          await api.post('/messages/broadcast', {
            content: inputValue
          });
        } else {
          await api.post('/messages', {
            receiverId: receiverId ?? userId,
            content: inputValue
          });
        }
        setInputValue('');
      } catch (err) {
        console.error('Failed to send message', err);
      }
    } else if (!userId) {
      console.error('User is not authenticated');
    }
  };

  return (
    <div className="chat-container">
      <header className="chat-header">
        <h1>MicroChat</h1>
        <div className="user-info">
          <span>{username}</span>
          <button onClick={logout}>Logout</button>
        </div>
      </header>

      <main className="chat-main">
        <aside className="sidebar">
          <h2>Channels</h2>
          <div 
            className={`channel-item \${receiverId === 'all' ? 'active' : ''}`}
            onClick={() => setReceiverId('all')}
          >
            # general
          </div>
          <h2 style={{ marginTop: '2rem' }}>Direct Messages</h2>
          <div 
            className={`channel-item \${receiverId === userId ? 'active' : ''}`}
            onClick={() => setReceiverId(userId || '')}
          >
            @ Me (Notes)
          </div>
        </aside>

        <section className="message-area">
          <div className="message-list" ref={scrollRef}>
            {messages.filter(msg => {
              if (receiverId === 'all') {
                return !msg.receiverId || msg.receiverId === 'all';
              }
              // Direct message context: either I sent it to the receiver, or the receiver sent it to me
              return (msg.senderId === userId && msg.receiverId === receiverId) || 
                     (msg.senderId === receiverId && msg.receiverId === userId);
            }).length === 0 ? (
              <p style={{ textAlign: 'center', color: '#64748b', marginTop: '2rem' }}>
                No messages yet. Say hello!
              </p>
            ) : (
              messages.filter(msg => {
                if (receiverId === 'all') {
                  return !msg.receiverId || msg.receiverId === 'all';
                }
                return (msg.senderId === userId && msg.receiverId === receiverId) || 
                       (msg.senderId === receiverId && msg.receiverId === userId);
              }).map((msg, idx) => (
                <div 
                  key={msg.id || idx} 
                  className={`message-item \${msg.senderId === userId ? 'own' : ''}`}
                >
                  <div className="sender">
                    {msg.senderName || msg.senderId}
                    {msg.authorType === 'BOT' && <span className="bot-badge">BOT</span>}
                  </div>
                  <div className="content">{msg.content}</div>
                </div>
              ))
            )}
          </div>

          <div className="message-input-container">
            <form className="message-input-form" onSubmit={handleSend}>
              <input
                type="text"
                placeholder="Type a message..."
                value={inputValue}
                onChange={(e) => setInputValue(e.target.value)}
              />
              <button type="submit">Send</button>
            </form>
          </div>
        </section>
      </main>
    </div>
  );
};

export default ChatPage;
