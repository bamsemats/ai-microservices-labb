import React from 'react';
import './ThinkingBubble.css';

const ThinkingBubble: React.FC = () => {
  return (
    <div className="message-bubble bot thinking" role="status" aria-live="polite">
      <div className="sender">Adapta AI</div>
      <div className="content">
        <span className="visually-hidden">Adapta AI is thinking</span>
        <div className="dots" aria-hidden="true">
          <span></span>
          <span></span>
          <span></span>
        </div>
      </div>
    </div>
  );
};

export default ThinkingBubble;
