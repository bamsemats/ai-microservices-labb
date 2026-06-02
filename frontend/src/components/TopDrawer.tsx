import React from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { useUIStore } from '../store/useUIStore';
import { useChatStore } from '../store/useChatStore';
import ContentWidget from './ContentWidget';

const TopDrawer: React.FC = () => {
  const { injectionPanelOpen, toggleInjectionPanel } = useUIStore();
  const { injectedContent } = useChatStore();
  const [isMobile, setIsMobile] = React.useState(typeof window !== 'undefined' ? window.innerWidth <= 768 : false);

  React.useEffect(() => {
    const handleResize = () => setIsMobile(window.innerWidth <= 768);
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  // Mobile only view
  if (!isMobile) return null;

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
            role="dialog"
            aria-modal="true"
            aria-labelledby="active-injections-title"
          >
            <div className="drawer-header">
              <h3 id="active-injections-title">Active Injections</h3>
              <button 
                type="button"
                onClick={() => toggleInjectionPanel(false)}
                className="close-drawer-btn"
                aria-label="Close Active Injections"
                title="Close"
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
