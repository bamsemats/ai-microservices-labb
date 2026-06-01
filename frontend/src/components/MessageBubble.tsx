import React from 'react';
import { motion } from 'motion/react';
import Avatar from './Avatar';

interface Message {
  id?: string;
  senderId: string;
  senderName?: string;
  content: string;
  timestamp?: string;
  authorType?: string;
  receiverId?: string;
  readBy?: string[];
  status?: 'pending' | 'sent' | 'failed';
}

interface MessageBubbleProps {
  message: Message;
  isOwn: boolean;
}

const MessageBubble: React.FC<MessageBubbleProps> = ({ message, isOwn }) => {
  return (
    <motion.div
      layout
      initial={{ opacity: 0, y: 10, scale: 0.95 }}
      animate={{ opacity: 1, y: 0, scale: 1 }}
      transition={{ 
        type: 'spring', 
        stiffness: 260, 
        damping: 20 
      }}
      className={`message-bubble ${isOwn ? 'is-own' : ''} ${message.authorType === 'BOT' ? 'bot' : ''} ${message.status || ''}`}
    >
      <div className="sender-info">
        <Avatar seed={message.senderName || message.senderId} size="sm" isBot={message.authorType === 'BOT'} className="message-avatar"/>
        <span className="sender-name">
          {message.senderName || message.senderId}
          {message.authorType === 'BOT' && <span className="badge badge-accent">AI</span>}
        </span>
      </div>
      <div className="message-text">{message.content}</div>
      {message.timestamp && (
        <div className="message-time">
          {new Date(message.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
          {isOwn && message.readBy && message.readBy.length > 0 && (
            <span className="read-status">✓ Seen</span>
          )}
          {message.status === 'pending' && <span className="status-label">...</span>}
          {message.status === 'failed' && <span className="status-label error">!</span>}
        </div>
      )}

    </motion.div>
  );
};

export default MessageBubble;
