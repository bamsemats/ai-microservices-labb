import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../store/useAuthStore';
import { useUIStore } from '../store/useUIStore';
import { usePresenceStore } from '../store/usePresenceStore';
import BrandLogo from './BrandLogo';

interface SidebarProps {
  activeReceiver?: string;
  onSelectReceiver?: (id: string) => void;
}

const CHANNELS = [
  { id: 'home', name: 'general', icon: '#' },
];

const Sidebar: React.FC<SidebarProps> = ({ activeReceiver, onSelectReceiver }) => {
  const navigate = useNavigate();
  const { userId } = useAuthStore();
  const { sidebarOpen, toggleSidebar } = useUIStore();
  const { presences } = usePresenceStore();

  const handleNav = (path: string) => {
    navigate(path);
    if (window.innerWidth <= 768) {
      toggleSidebar(false);
    }
  };

  const handleReceiverSelect = (id: string) => {
    if (onSelectReceiver) {
      onSelectReceiver(id);
    } else {
      navigate(`/?receiver=${encodeURIComponent(id)}`);
    }
    if (window.innerWidth <= 768) {
      toggleSidebar(false);
    }
  };

  const onlineUsers = Object.values(presences).filter(p => p.userId !== userId);

  return (
    <aside className={`sidebar glass-panel ${sidebarOpen ? 'mobile-open' : ''}`} role="navigation" aria-label="Main navigation">
      <button 
        className="sidebar-header" 
        onClick={() => handleNav('/')} 
        aria-label="Go to Home"
        style={{ cursor: 'pointer', background: 'none', border: 'none', padding: 0 }}
      >
        <BrandLogo size="md" />
      </button>

      <div className="sidebar-section" role="group" aria-labelledby="main-nav-label">
        <h3 id="main-nav-label">Channels</h3>
        <ul className="channel-list">
          {CHANNELS.map((channel) => (
            <li key={channel.id}>
              <button
                className={`channel-item ${activeReceiver === channel.id ? 'active' : ''}`}
                onClick={() => handleReceiverSelect(channel.id)}
                aria-current={activeReceiver === channel.id ? 'page' : undefined}
              >
                <span className="at" aria-hidden="true">{channel.icon}</span> {channel.name}
                {channel.id === 'home' && onlineUsers.length > 0 && <span className="status-indicator online" aria-hidden="true"></span>}
              </button>
            </li>
          ))}
        </ul>
      </div>

      <div className="sidebar-section">
        <h3>Intelligence</h3>
        <div className="action-grid">
          <button 
            className={`channel-item ${activeReceiver === 'explore' ? 'active' : ''}`} 
            onClick={() => handleNav('/explore')}
            aria-label="Discovery Hub"
          >
            <span className="icon" aria-hidden="true">🌐</span> Discovery
          </button>
          <button 
            className={`channel-item ${activeReceiver === 'insights' ? 'active' : ''}`} 
            onClick={() => handleNav('/insights')}
            aria-label="AI Insights"
          >
            <span className="icon" aria-hidden="true">📊</span> Insights
          </button>
        </div>
      </div>

      <div className="sidebar-section" style={{ marginTop: 'auto' }}>
        <h3>Presence</h3>
        <button 
          className={`channel-item ${activeReceiver === userId ? 'active' : ''}`}
          onClick={() => handleReceiverSelect(userId || 'me')}
          aria-label="Me (Notes) - online"
        >
          <span className="at" aria-hidden="true">@</span> Me (Notes)
          <span className="status-indicator online" aria-hidden="true"></span>
        </button>

        {onlineUsers.length > 0 ? (
          onlineUsers.map(u => (
            <button 
              key={u.userId} 
              className={`channel-item ${activeReceiver === u.userId ? 'active' : ''}`}
              onClick={() => handleReceiverSelect(u.userId)}
              aria-label={`${u.username} - ${u.status.toLowerCase()}`}
            >
              <span className="at" aria-hidden="true">@</span> {u.username}
              <span className={`status-indicator ${u.status.toLowerCase()}`} aria-hidden="true"></span>
            </button>
          ))
        ) : (
          <button className="channel-item" disabled style={{ opacity: 0.6, cursor: 'default' }}>
            <span className="at" aria-hidden="true">@</span> No users online
          </button>
        )}
      </div>
    </aside>
  );
};

export default Sidebar;
