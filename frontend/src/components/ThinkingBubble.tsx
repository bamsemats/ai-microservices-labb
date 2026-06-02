import React from 'react';
import './ThinkingBubble.css';

const ThinkingBubble: React.FC = () => {
  return (
    <div className="message-bubble is-thinking" role="status" aria-live="polite">
      <div className="message-sender">Adapta AI</div>
      <div className="message-text">
        <span className="visually-hidden">Adapta AI is thinking</span>
        <div className="typing-dots" aria-hidden="true">
          <span></span>
          <span></span>
          <span></span>
        </div>
      </div>
    </div>
  );
};

export default ThinkingBubble;
