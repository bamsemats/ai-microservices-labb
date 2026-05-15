import React, { useState, useRef, useEffect } from 'react';
import { motion, AnimatePresence } from 'motion/react';

interface MessageComposerProps {
  onSend: (content: string) => void;
  onTyping?: (isTyping: boolean) => void;
  placeholder?: string;
  disabled?: boolean;
}

const AI_SUGGESTIONS = [
  "Summarize our talk",
  "What's the status?",
  "Tell me a joke",
  "Help me with code"
];

const MessageComposer: React.FC<MessageComposerProps> = ({ onSend, onTyping, placeholder, disabled }) => {
  const [value, setValue] = useState('');
  const [isFocused, setIsFocused] = useState(false);
  const typingTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const isTypingRef = useRef(false);

  useEffect(() => {
    return () => {
      if (typingTimeoutRef.current) clearTimeout(typingTimeoutRef.current);
      if (isTypingRef.current && onTyping) {
        onTyping(false);
      }
    };
  }, [onTyping]);

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const newValue = e.target.value;
    setValue(newValue);

    if (onTyping && !disabled) {
      if (!isTypingRef.current && newValue.trim().length > 0) {
        isTypingRef.current = true;
        onTyping(true);
      } else if (isTypingRef.current && newValue.trim().length === 0) {
        isTypingRef.current = false;
        onTyping(false);
      }

      if (typingTimeoutRef.current) clearTimeout(typingTimeoutRef.current);
      
      if (isTypingRef.current) {
        typingTimeoutRef.current = setTimeout(() => {
          isTypingRef.current = false;
          onTyping(false);
        }, 3000);
      }
    }
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const trimmed = value.trim();
    if (trimmed && !disabled) {
      if (isTypingRef.current && onTyping) {
        isTypingRef.current = false;
        onTyping(false);
        if (typingTimeoutRef.current) clearTimeout(typingTimeoutRef.current);
      }
      onSend(trimmed);
      setValue('');
    }
  };

  const handleSuggestionClick = (suggestion: string) => {
    if (!disabled) {
      setValue(suggestion);
    }
  };

  return (
    <div className="composer-container">
      <AnimatePresence>
        {isFocused && !disabled && (
          <motion.div 
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: 10 }}
            className="ai-suggestions-bar"
            role="group"
            aria-label="AI message suggestions"
          >
            {AI_SUGGESTIONS.map((suggestion, index) => (
              <button 
                key={index} 
                className="suggestion-chip"
                onClick={() => handleSuggestionClick(suggestion)}
                aria-label={`Use suggestion: ${suggestion}`}
              >
                <span className="sparkle" aria-hidden="true">✨</span> {suggestion}
              </button>
            ))}
          </motion.div>
        )}
      </AnimatePresence>
      
      <form 
        className={`composer-form ${isFocused ? 'focused' : ''} ${disabled ? 'disabled' : ''}`} 
        onSubmit={handleSubmit}
        role="search"
        aria-label="Message composer"
      >
        <div className="input-wrapper glass-panel">
          <input
            type="text"
            placeholder={disabled ? "Broadcast disabled (Admin only)" : (placeholder || "Type a message...")}
            value={value}
            onChange={handleInputChange}
            onFocus={() => setIsFocused(true)}
            onBlur={() => setTimeout(() => setIsFocused(false), 200)}
            disabled={disabled}
            aria-label="Message content"
            aria-required="true"
          />
          <button 
            type="submit" 
            className="lumina-button"
            disabled={!value.trim() || disabled}
            aria-label="Send message"
          >
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
              <line x1="22" y1="2" x2="11" y2="13"></line>
              <polygon points="22 2 15 22 11 13 2 9 22 2"></polygon>
            </svg>
          </button>
        </div>
      </form>
    </div>
  );
};

export default MessageComposer;
