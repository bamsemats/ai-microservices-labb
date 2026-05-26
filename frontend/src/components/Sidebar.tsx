import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../store/useAuthStore';
import { useUIStore } from '../store/useUIStore';
import { usePresenceStore } from '../store/usePresenceStore';
import { useFrequencyStore } from '../store/useFrequencyStore';
import { useSocialStore } from '../store/useSocialStore';
import BrandLogo from './BrandLogo';
import Avatar from './Avatar';
import { Plus, Hash, Globe, BarChart3, ShieldAlert, Users } from 'lucide-react';

interface SidebarProps {
  activeReceiver?: string;
  onSelectReceiver?: (id: string) => void;
  className?: string;
}

const Sidebar: React.FC<SidebarProps> = ({ activeReceiver, onSelectReceiver, className }) => {
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
  }, []);

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
    <aside className={`sidebar glass-panel ${sidebarOpen ? 'mobile-open' : ''} ${className || ''}`} role="navigation" aria-label="Main navigation">
      <button 
        className="sidebar-header" 
        onClick={() => handleNav('/')} 
        aria-label="Go to Home"
      >
        <BrandLogo size="md" />
      </button>

      <div className="sidebar-section" role="group" aria-labelledby="main-nav-label">
        <div className="sidebar-section-header">
          <h3 id="main-nav-label">Frequencies</h3>
          <button 
            className="lumina-button secondary icon-only mini-btn" 
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
              className="lumina-input"
              placeholder="Frequency name..."
              value={newFreqName}
              onChange={(e) => setNewFreqName(e.target.value)}
            />
          </form>
        )}

        <ul className="channel-list">
          <li>
            <button
              className={`channel-item ${activeReceiver === 'home' ? 'active' : ''}`}
              onClick={() => handleReceiverSelect('home')}
            >
              <span className="at"><Hash size={16} /></span> general
              {onlineUsers.length > 0 && <span className="status-indicator online"></span>}
            </button>
          </li>
          {frequencies.map((freq) => (
            <li key={freq.id}>
              <button
                className={`channel-item ${activeReceiver === freq.id ? 'active' : ''}`}
                onClick={() => handleReceiverSelect(freq.id)}
              >
                <span className="at"><Hash size={16} /></span> {freq.name}
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
          >
            <Globe size={16} /> Discovery
          </button>
          <button 
            className={`channel-item ${activeReceiver === 'insights' ? 'active' : ''}`} 
            onClick={() => handleNav('/insights')}
          >
            <BarChart3 size={16} /> Insights
          </button>
          {isAdmin && (
            <button 
              className={`channel-item ${activeReceiver === 'admin' ? 'active' : ''}`} 
              onClick={() => handleNav('/admin')}
            >
              <ShieldAlert size={16} /> Admin Panel
            </button>
          )}
        </div>
      </div>

      <div className="sidebar-section sidebar-footer">
        <div className="sidebar-section-title-row">
          <Users size={14} className="sidebar-entities-icon" />
          <h3>Entities</h3>
        </div>
        
        <button 
          className={`channel-item ${activeReceiver === userId ? 'active' : ''}`}
          onClick={() => handleReceiverSelect(userId || 'me')}
        >
          <Avatar seed={username || 'me'} size="sm" />
          Me (Notes)
          <span className="status-indicator online"></span>
        </button>

        {friendList.length > 0 && (
          <div className="sidebar-friend-list">
            {friendList.map(u => (
              <button 
                key={u.id} 
                className={`channel-item ${activeReceiver === u.id ? 'active' : ''}`}
                onClick={() => handleReceiverSelect(u.id)}
              >
                <Avatar seed={u.username} size="sm" />
                {u.username}
                <span className={`status-indicator ${u.status.toLowerCase()}`}></span>
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
