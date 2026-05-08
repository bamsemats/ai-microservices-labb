import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../store/useAuthStore';
import { usePresenceStore, type PresenceStatus } from '../store/usePresenceStore';
import logoWithName from '../assets/logo-with-name.png';

interface SidebarProps {
  activeReceiver: string;
  onSelectReceiver: (id: string) => void;
}

const Sidebar: React.FC<SidebarProps> = ({ activeReceiver, onSelectReceiver }) => {
  const { userId, token } = useAuthStore();
  const { presences, fetchPresences } = usePresenceStore();
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
    <aside className="sidebar glass-panel">
      <div className="sidebar-header">
        <img src={logoWithName} alt="AdaptaChat Logo" className="sidebar-logo" />
      </div>

      <div className="sidebar-section">
        <h3>Main</h3>
        <button 
          className={`channel-item ${activeReceiver === 'home' ? 'active' : ''}`}
          onClick={() => handleNav('/')}
        >
          <span className="icon">🏠</span> Home
        </button>
        <button 
          className={`channel-item ${activeReceiver === 'explore' ? 'active' : ''}`}
          onClick={() => handleNav('/explore')}
        >
          <span className="icon">✨</span> Discovery
        </button>
        <button 
          className={`channel-item ${activeReceiver === 'insights' ? 'active' : ''}`}
          onClick={() => handleNav('/insights')}
        >
          <span className="icon">📊</span> Insights
        </button>
      </div>
      
      <div className="sidebar-section">
        <h3>Quick Actions</h3>
        <div className="action-grid">
          <button className="lumina-button secondary small">
            <span className="icon">🤖</span> AI Agent
          </button>
          <button className="lumina-button secondary small">
            <span className="icon">🧠</span> Memory
          </button>
        </div>
      </div>

      <div className="sidebar-section">
        <h3>Channels</h3>
        <button 
          className={`channel-item ${activeReceiver === 'all' ? 'active' : ''}`}
          onClick={() => onSelectReceiver('all')}
        >
          <span className="hash">#</span> general
        </button>
      </div>

      <div className="sidebar-section">
        <h3>Direct Messages</h3>
        <button 
          className={`channel-item ${activeReceiver === userId ? 'active' : ''}`}
          onClick={() => userId && onSelectReceiver(userId)}
          disabled={!userId}
        >
          <span className="at">@</span> Me (Notes)
          <span className="status-indicator online"></span>
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
                >
                  <span className="at">@</span> {presence.username}
                  <span className={`status-indicator ${getStatusClass(presence.status)}`}></span>
                </button>
              ))}
              
              {filteredPeers.length === 0 && (
                <button className="channel-item disabled" disabled>
                  <span className="at">@</span> No users online
                </button>
              )}
            </>
          );
        })()}
      </div>
      
      <style href="Sidebar" precedence="default">{`
        .sidebar {
          width: 280px;
          height: calc(100vh - 2rem);
          margin: 1rem;
          display: flex;
          flex-direction: column;
          padding: 1.5rem;
          border-radius: 1.5rem;
          background: rgba(11, 19, 38, 0.4);
          flex-shrink: 0;
          overflow-y: auto;
        }

        .sidebar-header {
          margin-bottom: 2rem;
          display: flex;
          justify-content: center;
        }

        .sidebar-logo {
          width: 100%;
          max-width: 180px;
          height: auto;
          filter: drop-shadow(var(--accent-glow));
        }

        .sidebar-header h2 {
          font-size: 1.25rem;
          font-weight: 800;
          letter-spacing: -0.02em;
          background: var(--accent-gradient);
          -webkit-background-clip: text;
          -webkit-text-fill-color: transparent;
        }

        .sidebar-section {
          margin-bottom: 2rem;
        }

        .sidebar-section h3 {
          font-size: 0.75rem;
          text-transform: uppercase;
          letter-spacing: 0.1em;
          color: var(--text-muted);
          margin-bottom: 1rem;
          font-weight: 700;
        }

        .action-grid {
          display: grid;
          grid-template-columns: 1fr 1fr;
          gap: 0.75rem;
        }

        .lumina-button.small {
          padding: 0.5rem;
          font-size: 0.75rem;
          border-radius: 0.5rem;
        }

        .channel-item {
          padding: 0.75rem 1rem;
          border-radius: 0.75rem;
          margin-bottom: 0.25rem;
          cursor: pointer;
          font-size: 0.9375rem;
          font-weight: 500;
          color: var(--text-secondary);
          transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
          display: flex;
          align-items: center;
          gap: 0.75rem;
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
