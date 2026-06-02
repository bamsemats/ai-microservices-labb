import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../store/useAuthStore';
import { useUIStore } from '../store/useUIStore';
import { usePresenceStore } from '../store/usePresenceStore';
import { useFrequencyStore } from '../store/useFrequencyStore';
import { useSocialStore } from '../store/useSocialStore';
import BrandLogo from './BrandLogo';
import Avatar from './Avatar';
import { Plus, Hash, Globe, BarChart3, ShieldAlert, Users, Search } from 'lucide-react';

interface SidebarProps {
  activeReceiver?: string;
  className?: string;
}

const Sidebar: React.FC<SidebarProps> = ({ activeReceiver, className }) => {
  const navigate = useNavigate();
  const { userId, username, isAdmin } = useAuthStore();
  const { sidebarOpen, toggleSidebar } = useUIStore();
  const { presences } = usePresenceStore();
  const { frequencies, fetchFrequencies, createFrequency } = useFrequencyStore();
  const { friends, fetchFriends } = useSocialStore();

  const [showCreateFreq, setShowCreateFreq] = useState(false);
  const [newFreqName, setNewFreqName] = useState('');

  useEffect(() => {
    fetchFrequencies();
    fetchFriends();
  }, [fetchFrequencies, fetchFriends]);

  const handleNav = (path: string) => {
    navigate(path);
    if (window.innerWidth <= 768) {
      toggleSidebar(false);
    }
  };

  const handleReceiverSelect = (id: string) => {
    navigate({ pathname: '/', search: `?receiver=${encodeURIComponent(id)}` });
    if (window.innerWidth <= 768) {
      toggleSidebar(false);
    }
  };

  const handleCreateFreq = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newFreqName.trim()) return;
    try {
      const freq = await createFrequency(newFreqName.trim());
      setNewFreqName('');
      setShowCreateFreq(false);
      handleReceiverSelect(freq.id);
    } catch (error) {
      console.error('Failed to create frequency', error);
    }
  };

  const onlineUsers = Object.values(presences).filter(p => p.userId !== userId);
  const friendList = friends.map(f => {
    const presence = presences[f.id];
    return { ...f, status: presence?.status || 'OFFLINE' };
  });

  return (
    <aside className={`sidebar ${sidebarOpen ? 'is-open' : ''} ${className || ''}`} role="navigation" aria-label="Main navigation">
      <button 
        className="sidebar-brand"
        onClick={() => handleNav('/')} 
        aria-label="Go to Home"
      >
        <BrandLogo size="md" />
      </button>

      <div className="sidebar-section" role="group" aria-labelledby="main-nav-label">
          <div className="frequencies-header">
            <h3 id="main-nav-label" className="sidebar-label">Frequencies</h3>
            <button
              className="btn btn-icon btn-sm btn-plus"
              onClick={() => setShowCreateFreq(!showCreateFreq)}
              title="Create new frequency"
            >
              <Plus size={14} />
            </button>
          </div>

        {showCreateFreq && (
          <form onSubmit={handleCreateFreq} className="sidebar-create-form">
            <input 
              autoFocus
              className="input"
              placeholder="Frequency name..."
              value={newFreqName}
              onChange={(e) => setNewFreqName(e.target.value)}
            />
          </form>
        )}

        <ul className="nav-list" role="list">
          <li>
            <button
              className={`nav-item ${activeReceiver === 'home' ? 'is-active' : ''}`}
              onClick={() => handleReceiverSelect('home')}
              aria-current={activeReceiver === 'home' ? 'page' : undefined}
            >
              <span className="at" aria-hidden="true"><Hash size={16} /></span> general
              {onlineUsers.length > 0 && <span className="status-dot online" aria-label="Users online"></span>}
            </button>
          </li>
          {frequencies.map((freq) => (
            <li key={freq.id}>
              <button
                className={`nav-item ${activeReceiver === freq.id ? 'is-active' : ''}`}
                onClick={() => handleReceiverSelect(freq.id)}
                aria-current={activeReceiver === freq.id ? 'page' : undefined}
              >
                <span className="at" aria-hidden="true"><Hash size={16} /></span> {freq.name}
              </button>
            </li>
          ))}
        </ul>
      </div>

      <nav className="sidebar-section" aria-label="Intelligence">
        <h3>Intelligence</h3>
        <div className="action-grid" role="list">
          <button 
            className={`nav-item ${activeReceiver === 'explore' ? 'is-active' : ''}`}
            onClick={() => handleNav('/explore')}
            role="listitem"
            aria-current={activeReceiver === 'explore' ? 'page' : undefined}
          >
            <Globe size={16} aria-hidden="true" /> Discovery
          </button>
          <button 
            className={`nav-item ${activeReceiver === 'insights' ? 'is-active' : ''}`}
            onClick={() => handleNav('/insights')}
            role="listitem"
            aria-current={activeReceiver === 'insights' ? 'page' : undefined}
          >
            <BarChart3 size={16} aria-hidden="true" /> Insights
          </button>
          <button 
            className={`nav-item ${activeReceiver === 'friends' ? 'is-active' : ''}`}
            onClick={() => handleNav('/friends')}
            role="listitem"
            aria-current={activeReceiver === 'friends' ? 'page' : undefined}
          >
            <Users size={16} aria-hidden="true" /> Social Hub
          </button>
          <button 
            className={`nav-item ${activeReceiver === 'search' ? 'is-active' : ''}`}
            onClick={() => handleNav('/search')}
            role="listitem"
            aria-current={activeReceiver === 'search' ? 'page' : undefined}
          >
            <Search size={16} aria-hidden="true" /> Search History
          </button>
          {isAdmin && (
            <button 
              className={`nav-item ${activeReceiver === 'admin' ? 'is-active' : ''}`}
              onClick={() => handleNav('/admin')}
              role="listitem"
              aria-current={activeReceiver === 'admin' ? 'page' : undefined}
            >
              <ShieldAlert size={16} aria-hidden="true" /> Admin Panel
            </button>
          )}
        </div>
      </nav>

      <div className="sidebar-section sidebar-footer" role="group" aria-labelledby="entities-label">
        <div>
          <Users size={14} className="sidebar-label svg" aria-hidden="true" />
          <h3 id="entities-label" className="sidebar-label">Entities</h3>
        </div>
        
        <button 
          className={`nav-item ${activeReceiver === userId ? 'is-active' : ''}`}
          onClick={() => handleReceiverSelect(userId || 'me')}
          aria-current={activeReceiver === userId ? 'page' : undefined}
        >
          <Avatar seed={username || 'me'} size="sm" />
          Me (Notes)
          <span className="status-dot online" aria-label="Online"></span>
        </button>

        {friendList.length > 0 && (
          <div className="sidebar-friend-list" role="list">
            {friendList.map(u => (
              <button 
                key={u.id} 
                className={`nav-item ${activeReceiver === u.id ? 'is-active' : ''}`}
                onClick={() => handleReceiverSelect(u.id)}
                aria-label={`${u.username}, ${u.status.toLowerCase()}${u.isBot ? ', AI entity' : ''}`}
                aria-current={activeReceiver === u.id ? 'page' : undefined}
                role="listitem"
              >
                <Avatar seed={u.username} size="sm" isBot={u.isBot} />
                {u.username}
                <span className={`status-dot ${u.status.toLowerCase()}`} aria-hidden="true"></span>
              </button>
            ))}
          </div>
        )}

        {friendList.length === 0 && onlineUsers.length > 0 && (
           <div className="sidebar-empty-hint">
              Scan for entities in Discovery to add friends.
           </div>
        )}
      </div>
    </aside>
  );
};

export default Sidebar;
