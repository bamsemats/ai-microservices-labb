import React, { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';

interface MessageComposerProps {
  onSend: (content: string) => void;
  placeholder?: string;
}

const AI_SUGGESTIONS = [
  "Summarize our talk",
  "What's the status?",
  "Tell me a joke",
  "Help me with code"
];

const MessageComposer: React.FC<MessageComposerProps> = ({ onSend, placeholder }) => {
  const [value, setValue] = useState('');
  const [isFocused, setIsFocused] = useState(false);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (value.trim()) {
      onSend(value);
      setValue('');
    }
  };

  const handleSuggestionClick = (suggestion: string) => {
    setValue(suggestion);
  };

  return (
    <div className="composer-container">
      <AnimatePresence>
        {isFocused && (
          <motion.div 
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: 10 }}
            className="ai-suggestions-bar"
          >
            {AI_SUGGESTIONS.map((suggestion, index) => (
              <button 
                key={index} 
                className="suggestion-chip"
                onClick={() => handleSuggestionClick(suggestion)}
              >
                <span className="sparkle">✨</span> {suggestion}
              </button>
            ))}
          </motion.div>
        )}
      </AnimatePresence>
      
      <form 
        className={`composer-form ${isFocused ? 'focused' : ''}`} 
        onSubmit={handleSubmit}
      >
        <div className="input-wrapper glass-panel">
          <input
            type="text"
            placeholder={placeholder || "Type a message..."}
            value={value}
            onChange={(e) => setValue(e.target.value)}
            onFocus={() => setIsFocused(true)}
            onBlur={() => setTimeout(() => setIsFocused(false), 200)}
          />
          <button 
            type="submit" 
            className="lumina-button"
            disabled={!value.trim()}
          >
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
              <line x1="22" y1="2" x2="11" y2="13"></line>
              <polygon points="22 2 15 22 11 13 2 9 22 2"></polygon>
            </svg>
          </button>
        </div>
      </form>

      <style>{`
        .composer-container {
          padding: 1.5rem 2rem;
          background: linear-gradient(to top, var(--bg-dark) 60%, transparent);
          position: relative;
          z-index: 10;
        }

        .ai-suggestions-bar {
          display: flex;
          gap: 0.75rem;
          margin-bottom: 1rem;
          padding: 0 0.5rem;
          overflow-x: auto;
          scrollbar-width: none;
        }

        .ai-suggestions-bar::-webkit-scrollbar {
          display: none;
        }

        .suggestion-chip {
          background: rgba(255, 255, 255, 0.05);
          border: 1px solid rgba(255, 255, 255, 0.1);
          color: var(--text-secondary);
          padding: 0.4rem 0.8rem;
          border-radius: 2rem;
          font-size: 0.8125rem;
          white-space: nowrap;
          cursor: pointer;
          transition: all 0.2s ease;
          display: flex;
          align-items: center;
          gap: 0.4rem;
        }

        .suggestion-chip:hover {
          background: rgba(139, 92, 246, 0.1);
          border-color: rgba(139, 92, 246, 0.3);
          color: var(--text-primary);
          transform: translateY(-1px);
        }

        .sparkle {
          color: var(--accent-tertiary);
          font-size: 0.9rem;
        }

        .input-wrapper {
          display: flex;
          align-items: center;
          padding: 0.5rem;
          gap: 0.5rem;
          transition: all 0.3s ease;
          background: rgba(11, 19, 38, 0.5);
        }

        .input-wrapper input {
          background: transparent !important;
          border: none !important;
          box-shadow: none !important;
          flex: 1;
        }

        .composer-form.focused .input-wrapper {
          border-color: rgba(139, 92, 246, 0.4) !important;
          box-shadow: 0 0 30px rgba(139, 92, 246, 0.15), var(--accent-glow);
          background: rgba(11, 19, 38, 0.8);
        }

        .composer-form button {
          height: 2.75rem;
          width: 2.75rem;
          padding: 0 !important;
          flex-shrink: 0;
        }

        .composer-form button:disabled {
          opacity: 0.3;
          cursor: not-allowed;
          filter: grayscale(1);
        }
      `}</style>
    </div>
  );
};

export default MessageComposer;
