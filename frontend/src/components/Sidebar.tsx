import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../store/useAuthStore';
import { useUIStore } from '../store/useUIStore';
import { usePresenceStore, type PresenceStatus } from '../store/usePresenceStore';
import logoWithName from '../assets/logo-with-name.png';

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
      <div className="sidebar-header">
        <img src={logoWithName} alt="AdaptaChat Logo" className="sidebar-logo" />
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
          onClick={() => onSelectReceiver('all')}
          aria-current={activeReceiver === 'all' ? 'true' : undefined}
        >
          <span className="hash" aria-hidden="true">#</span> general
        </button>
      </div>

      <div className="sidebar-section" role="group" aria-labelledby="dm-label">
        <h3 id="dm-label">Direct Messages</h3>
        <button 
          className={`channel-item ${activeReceiver === userId ? 'active' : ''}`}
          onClick={() => userId && onSelectReceiver(userId)}
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
                  onClick={() => onSelectReceiver(presence.userId)}
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
      
      <style href="Sidebar" precedence="default">{`
        .sidebar {
          width: var(--sidebar-width);
          height: calc(100vh - (2 * var(--app-padding)));
          margin: var(--app-padding);
          display: flex;
          flex-direction: column;
          padding: 1.25rem;
          border-radius: 1.25rem;
          background: rgba(11, 19, 38, 0.4);
          flex-shrink: 0;
          overflow-y: auto;
          transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
          z-index: 100;
        }

        @media (max-width: 768px) {
          .sidebar {
            position: fixed;
            left: -300px;
            margin: 0;
            height: 100vh;
            border-radius: 0 1.25rem 1.25rem 0;
            background: rgba(11, 19, 38, 0.95);
            backdrop-filter: blur(20px);
            width: 280px;
          }

          .sidebar.mobile-open {
            left: 0;
            box-shadow: 0 0 50px rgba(0, 0, 0, 0.5);
          }
        }

        .sidebar-header {
          margin-bottom: 1.5rem;
          display: flex;
          justify-content: center;
        }

        .sidebar-logo {
          width: 100%;
          max-width: 150px;
          height: auto;
          filter: drop-shadow(var(--accent-glow));
        }

        .sidebar-header h2 {
          font-size: 1.1rem;
          font-weight: 800;
          letter-spacing: -0.02em;
          background: var(--accent-gradient);
          -webkit-background-clip: text;
          -webkit-text-fill-color: transparent;
        }

        .sidebar-section {
          margin-bottom: 1.5rem;
        }

        .sidebar-section h3 {
          font-size: 0.7rem;
          text-transform: uppercase;
          letter-spacing: 0.1em;
          color: var(--text-muted);
          margin-bottom: 0.75rem;
          font-weight: 700;
        }

        .action-grid {
          display: grid;
          grid-template-columns: 1fr 1fr;
          gap: 0.5rem;
        }

        .lumina-button.small {
          padding: 0.4rem;
          font-size: 0.7rem;
          border-radius: 0.4rem;
        }

        .channel-item {
          padding: 0.6rem 0.75rem;
          border-radius: 0.6rem;
          margin-bottom: 0.2rem;
          cursor: pointer;
          font-size: 0.875rem;
          font-weight: 500;
          color: var(--text-secondary);
          transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
          display: flex;
          align-items: center;
          gap: 0.6rem;
          position: relative;
          background: transparent;
          border: none;
          width: 100%;
          text-align: left;
        }

        .channel-item:hover {
          background: rgba(255, 255, 255, 0.05);
          color: var(--text-primary);
          transform: translateX(4px);
        }

        .channel-item.active {
          background: var(--accent-gradient);
          color: white;
          box-shadow: var(--accent-glow);
        }

        .channel-item.disabled {
          opacity: 0.5;
          cursor: not-allowed;
        }

        .hash, .at, .icon {
          font-weight: 700;
          opacity: 0.5;
          width: 1.25rem;
          display: flex;
          justify-content: center;
        }

        .channel-item.active .icon {
          opacity: 1;
        }

        .status-indicator {
          width: 8px;
          height: 8px;
          border-radius: 50%;
          position: absolute;
          right: 1rem;
          transition: all 0.3s ease;
        }

        .status-indicator.online {
          background: var(--success);
          box-shadow: 0 0 8px var(--success);
        }

        .status-indicator.away {
          background: #ffb74d;
          box-shadow: 0 0 8px #ffb74d;
        }

        .status-indicator.dnd {
          background: #ef5350;
          box-shadow: 0 0 8px #ef5350;
        }

        .status-indicator.offline {
          background: #78909c;
          box-shadow: none;
        }
      `}</style>
    </aside>
  );
};

export default Sidebar;
