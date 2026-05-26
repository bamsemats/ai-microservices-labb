import React from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { useUIStore } from '../store/useUIStore';
import { useChatStore } from '../store/useChatStore';
import ContentWidget from './ContentWidget';

const TopDrawer: React.FC = () => {
  const { injectionPanelOpen, toggleInjectionPanel } = useUIStore();
  const { injectedContent } = useChatStore();

  // Mobile only view
  if (window.innerWidth > 768) return null;

  return (
    <AnimatePresence>
      {injectionPanelOpen && (
        <>
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="drawer-overlay"
            onClick={() => toggleInjectionPanel(false)}
          />
          <motion.div
            initial={{ y: '-100%' }}
            animate={{ y: 0 }}
            exit={{ y: '-100%' }}
            transition={{ type: 'spring', stiffness: 300, damping: 30 }}
            className="top-drawer glass-panel"
          >
            <div className="drawer-header">
              <h3>Active Injections</h3>
              <button 
                onClick={() => toggleInjectionPanel(false)}
                className="close-drawer-btn"
              >
                ×
              </button>
            </div>
            
            <div className="drawer-content">
              {injectedContent.length > 0 ? (
                injectedContent.map((content, idx) => (
                  <ContentWidget key={`${content.type}-${idx}`} content={content} />
                ))
              ) : (
                <p className="drawer-empty-msg">No active signals.</p>
              )}
            </div>
          </motion.div>
        </>
      )}
    </AnimatePresence>
  );
};

export default TopDrawer;
