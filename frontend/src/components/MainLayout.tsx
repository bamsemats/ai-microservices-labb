import React from 'react';
import { motion, AnimatePresence } from 'motion/react';
import Sidebar from './Sidebar';
import Navbar from './Navbar';
import RightPanel from './RightPanel';
import TopDrawer from './TopDrawer';
import { useUIStore } from '../store/useUIStore';

interface MainLayoutProps {
  children: React.ReactNode;
  activeReceiver?: string;
  prefix?: string;
  contextName?: string;
}

const MainLayout: React.FC<MainLayoutProps> = ({ 
  children, 
  activeReceiver, 
  prefix,
  contextName = 'AdaptaChat'
}) => {
  const { sidebarOpen, toggleSidebar } = useUIStore();

  return (
    <div className="app-shell">
      <a href="#main-content" className="sr-only sr-only-focusable skip-link">
        Skip to main content
      </a>
      <TopDrawer />
      <Sidebar 
        activeReceiver={activeReceiver} 
        className={!sidebarOpen ? 'is-closed' : ''}
      />
      
      <AnimatePresence>
        {sidebarOpen && (
          <motion.div 
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="sidebar-overlay"
            onClick={() => toggleSidebar(false)}
            aria-hidden="true"
          />
        )}
      </AnimatePresence>

      <main className="app-main" id="main-content" tabIndex={-1}>
        <Navbar prefix={prefix} contextName={contextName} />
        {children}
      </main>

      <RightPanel />
    </div>
  );
};

export default MainLayout;
