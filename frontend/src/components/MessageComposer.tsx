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
  const onTypingRef = useRef(onTyping);

  useEffect(() => {
    onTypingRef.current = onTyping;
  }, [onTyping]);

  useEffect(() => {
    return () => {
      if (typingTimeoutRef.current) clearTimeout(typingTimeoutRef.current);
      if (isTypingRef.current && onTypingRef.current) {
        onTypingRef.current(false);
      }
    };
  }, []);

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const newValue = e.target.value;
    setValue(newValue);

    if (onTypingRef.current && !disabled) {
      if (!isTypingRef.current && newValue.trim().length > 0) {
        isTypingRef.current = true;
        onTypingRef.current(true);
      } else if (isTypingRef.current && newValue.trim().length === 0) {
        isTypingRef.current = false;
        onTypingRef.current(false);
      }

      if (typingTimeoutRef.current) clearTimeout(typingTimeoutRef.current);
      
      if (isTypingRef.current) {
        typingTimeoutRef.current = setTimeout(() => {
          isTypingRef.current = false;
          if (onTypingRef.current) onTypingRef.current(false);
        }, 3000);
      }
    }
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const trimmed = value.trim();
    if (trimmed && !disabled) {
      if (isTypingRef.current && onTypingRef.current) {
        isTypingRef.current = false;
        onTypingRef.current(false);
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
      <form 
        className={`composer-form ${isFocused ? 'focused' : ''} ${disabled ? 'disabled' : ''}`} 
        onSubmit={handleSubmit}
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
            required
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
