import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../store/useAuthStore';
import { useUIStore } from '../store/useUIStore';
import { usePresenceStore, type PresenceStatus } from '../store/usePresenceStore';
import BrandLogo from './BrandLogo';

interface SidebarProps {
  activeReceiver: string;
  onSelectReceiver: (id: string) => void;
}

const Sidebar: React.FC<SidebarProps> = ({ activeReceiver, onSelectReceiver }) => {
  const { userId, token } = useAuthStore();
  const { presences, fetchPresences } = usePresenceStore();
  const { sidebarOpen, toggleSidebar } = useUIStore();
  const navigate = useNavigate();

  React.useEffect(() => {
    if (token) {
      fetchPresences(token);
    }
  }, [token, fetchPresences]);

  const handleNav = (path: string, receiverId?: string) => {
    navigate(path);
    if (receiverId) {
      onSelectReceiver(receiverId);
    }
    toggleSidebar(false);
  };

  const getStatusClass = (status: PresenceStatus) => {
    switch (status) {
      case 'ONLINE': return 'online';
      case 'AWAY': return 'away';
      case 'DND': return 'dnd';
      default: return 'offline';
    }
  };

  return (
    <aside className={`sidebar glass-panel ${sidebarOpen ? 'mobile-open' : ''}`} role="navigation" aria-label="Main navigation">
      <div className="sidebar-header" onClick={() => handleNav('/')} style={{ cursor: 'pointer' }}>
        <BrandLogo size="md" />
      </div>

      <div className="sidebar-section" role="group" aria-labelledby="main-nav-label">
        <h3 id="main-nav-label">Main</h3>
        <button 
          className={`channel-item ${activeReceiver === 'home' ? 'active' : ''}`}
          onClick={() => handleNav('/')}
          aria-current={activeReceiver === 'home' ? 'page' : undefined}
        >
          <span className="icon" aria-hidden="true">🏠</span> Home
        </button>
        <button 
          className={`channel-item ${activeReceiver === 'explore' ? 'active' : ''}`}
          onClick={() => handleNav('/explore')}
          aria-current={activeReceiver === 'explore' ? 'page' : undefined}
        >
          <span className="icon" aria-hidden="true">✨</span> Discovery
        </button>
        <button 
          className={`channel-item ${activeReceiver === 'insights' ? 'active' : ''}`}
          onClick={() => handleNav('/insights')}
          aria-current={activeReceiver === 'insights' ? 'page' : undefined}
        >
          <span className="icon" aria-hidden="true">📊</span> Insights
        </button>
      </div>
      
      <div className="sidebar-section" role="group" aria-labelledby="quick-actions-label">
        <h3 id="quick-actions-label">Quick Actions</h3>
        <div className="action-grid">
          <button 
            className="lumina-button secondary small"
            onClick={() => handleNav('/', 'AdaptaAI')}
            aria-label="Start chat with AI Agent"
          >
            <span className="icon" aria-hidden="true">🤖</span> AI Agent
          </button>
          <button 
            className="lumina-button secondary small"
            onClick={() => handleNav('/insights')}
            aria-label="View AI Insights and Memory"
          >
            <span className="icon" aria-hidden="true">🧠</span> Memory
          </button>
        </div>
      </div>

      <div className="sidebar-section" role="group" aria-labelledby="channels-label">
        <h3 id="channels-label">Channels</h3>
        <button 
          className={`channel-item ${activeReceiver === 'all' ? 'active' : ''}`}
          onClick={() => { onSelectReceiver('all'); toggleSidebar(false); }}
          aria-current={activeReceiver === 'all' ? 'true' : undefined}
        >
          <span className="hash" aria-hidden="true">#</span> general
        </button>
      </div>

      <div className="sidebar-section" role="group" aria-labelledby="dm-label">
        <h3 id="dm-label">Direct Messages</h3>
        <button 
          className={`channel-item ${activeReceiver === userId ? 'active' : ''}`}
          onClick={() => { if (userId) { onSelectReceiver(userId); toggleSidebar(false); } }}
          disabled={!userId}
          aria-current={activeReceiver === userId ? 'true' : undefined}
          aria-label="Me (Notes)"
        >
          <span className="at" aria-hidden="true">@</span> Me (Notes)
          <span className="status-indicator online" aria-label="Status: Online"></span>
        </button>
        
        {(() => {
          const filteredPeers = Object.values(presences).filter(p => p.userId !== userId);
          return (
            <>
              {filteredPeers.map((presence) => (
                <button 
                  key={presence.userId}
                  className={`channel-item ${activeReceiver === presence.userId ? 'active' : ''}`}
                  onClick={() => { onSelectReceiver(presence.userId); toggleSidebar(false); }}
                  aria-current={activeReceiver === presence.userId ? 'true' : undefined}
                  aria-label={`Chat with ${presence.username}, status: ${presence.status.toLowerCase()}`}
                >
                  <span className="at" aria-hidden="true">@</span> {presence.username}
                  <span className={`status-indicator ${getStatusClass(presence.status)}`} aria-hidden="true"></span>
                </button>
              ))}
              
              {filteredPeers.length === 0 && (
                <button className="channel-item disabled" disabled aria-disabled="true">
                  <span className="at" aria-hidden="true">@</span> No users online
                </button>
              )}
            </>
          );
        })()}
      </div>
    </aside>
  );
};

export default Sidebar;
