import React from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { useUIStore } from '../store/useUIStore';
import { useChatStore } from '../store/useChatStore';
import ContentWidget from './ContentWidget';

const RightPanel: React.FC = () => {
  const { injectionPanelOpen } = useUIStore();
  const { injectedContent } = useChatStore();

  // Desktop only view
  if (window.innerWidth <= 768) return null;

  return (
    <AnimatePresence>
      {injectionPanelOpen && (
        <motion.aside
          initial={{ width: 0, opacity: 0 }}
          animate={{ width: '350px', opacity: 1 }}
          exit={{ width: 0, opacity: 0 }}
          transition={{ type: 'spring', stiffness: 300, damping: 30 }}
          className="injection-panel glass-panel"
        >
          <div className="panel-header">
            <h3>
              Signal Injections
            </h3>
          </div>
          
          <div className="panel-content">
            {injectedContent.length > 0 ? (
              injectedContent.map((content, idx) => (
                <ContentWidget key={`${content.type}-${idx}`} content={content} />
              ))
            ) : (
              <div className="empty-panel">
                <span className="empty-panel-icon">📡</span>
                <p className="empty-panel-text">No active signal detected in current conversation.</p>
              </div>
            )}
          </div>
        </motion.aside>
      )}
    </AnimatePresence>
  );
};

export default RightPanel;
