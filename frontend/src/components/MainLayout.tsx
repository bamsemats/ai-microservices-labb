import React from 'react';
import { motion, AnimatePresence } from 'motion/react';
import Sidebar from './Sidebar';
import Navbar from './Navbar';
import { useUIStore } from '../store/useUIStore';

interface MainLayoutProps {
  children: React.ReactNode;
  activeReceiver?: string;
  onSelectReceiver?: (id: string) => void;
  prefix?: string;
  contextName: string;
}

const MainLayout: React.FC<MainLayoutProps> = ({ 
  children, 
  activeReceiver, 
  onSelectReceiver,
  prefix,
  contextName
}) => {
  const { sidebarOpen, toggleSidebar } = useUIStore();

  return (
    <div className="chat-page-layout">
      <Sidebar activeReceiver={activeReceiver} onSelectReceiver={onSelectReceiver} />
      
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

      <main className="chat-main-content">
        <Navbar prefix={prefix} contextName={contextName} />
        {children}
      </main>
    </div>
  );
};

export default MainLayout;
