import React, { useState, useRef, useEffect } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import api from '../api/axios';

const FeedbackWidget: React.FC = () => {
  const [isOpen, setIsOpen] = useState(false);
  const [rating, setRating] = useState(5);
  const [comment, setComment] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitted, setSubmitted] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const autoCloseTimerRef = useRef<NodeJS.Timeout | null>(null);

  useEffect(() => {
    return () => {
      if (autoCloseTimerRef.current) {
        clearTimeout(autoCloseTimerRef.current);
      }
    };
  }, []);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubmitting(true);
    setError(null);

    try {
      await api.post('/feedback', { rating, comment });
      setSubmitted(true);
      
      if (autoCloseTimerRef.current) {
        clearTimeout(autoCloseTimerRef.current);
      }
      
      autoCloseTimerRef.current = setTimeout(() => {
        setIsOpen(false);
        setSubmitted(false);
        setComment('');
        setRating(5);
      }, 3000);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to submit feedback. Please try again later.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="feedback-widget-container">
      <AnimatePresence>
        {isOpen && (
          <motion.div
            initial={{ opacity: 0, y: 20, scale: 0.9 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 20, scale: 0.9 }}
            className="feedback-modal glass-panel"
          >
            {submitted ? (
              <div className="feedback-success">
                <span className="icon">✨</span>
                <h3>Thank you!</h3>
                <p>Your feedback helps us improve AdaptaChat.</p>
              </div>
            ) : (
              <form onSubmit={handleSubmit}>
                <h3>Share your thoughts</h3>
                <div className="rating-selector">
                  {[1, 2, 3, 4, 5].map((num) => (
                    <button
                      key={num}
                      type="button"
                      className={`rating-btn ${rating >= num ? 'active' : ''}`}
                      onClick={() => setRating(num)}
                      aria-label={`Rate ${num} stars`}
                    >
                      ★
                    </button>
                  ))}
                </div>
                <textarea
                  placeholder="Tell us what you think..."
                  value={comment}
                  onChange={(e) => setComment(e.target.value)}
                  required
                  maxLength={2000}
                />
                {error && <p className="feedback-error">{error}</p>}
                <div className="modal-actions">
                  <button type="button" className="lumina-button secondary" onClick={() => setIsOpen(false)}>Cancel</button>
                  <button type="submit" className="lumina-button primary" disabled={isSubmitting}>
                    {isSubmitting ? 'Sending...' : 'Submit'}
                  </button>
                </div>
              </form>
            )}
          </motion.div>
        )}
      </AnimatePresence>

      <motion.button
        whileHover={{ scale: 1.1 }}
        whileTap={{ scale: 0.9 }}
        className="feedback-trigger-btn"
        onClick={() => setIsOpen(!isOpen)}
        aria-label="Give feedback"
      >
        <span className="icon">💬</span>
      </motion.button>
    </div>
  );
};

export default FeedbackWidget;
