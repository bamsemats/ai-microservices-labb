import React from 'react';
import './ThinkingBubble.css';

const ThinkingBubble: React.FC = () => {
  return (
    <div className="message-bubble bot thinking">
      <div className="sender">Adapta AI</div>
      <div className="content">
        <div className="dots">
          <span></span>
          <span></span>
          <span></span>
        </div>
      </div>
    </div>
  );
};

export default ThinkingBubble;
