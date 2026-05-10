import React from 'react';
import { motion } from 'motion/react';

interface Message {
  id?: string;
  senderId: string;
  senderName?: string;
  content: string;
  timestamp?: string;
  authorType?: string;
  receiverId?: string;
  readBy?: string[];
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
      className={`message-bubble ${isOwn ? 'own' : ''}`}
    >
      <div className="sender-info">
        {message.senderName || message.senderId}
        {message.authorType === 'BOT' && <span className="bot-tag">AI</span>}
      </div>
      <div className="bubble-content">{message.content}</div>
      {message.timestamp && (
        <div className="message-time">
          {new Date(message.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
          {isOwn && message.readBy && message.readBy.length > 0 && (
            <span className="read-status" style={{ marginLeft: '0.5rem', opacity: 0.8 }}>✓ Seen</span>
          )}
        </div>
      )}
    </motion.div>
  );
};

export default MessageBubble;
