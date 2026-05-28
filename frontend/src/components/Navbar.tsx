import React, { useEffect, useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuthStore } from '../store/useAuthStore';
import { useUIStore } from '../store/useUIStore';
import { useFrequencyStore } from '../store/useFrequencyStore';
import { Sun, Moon, Menu, Radio, MoreVertical, LogOut } from 'lucide-react';
import Avatar from './Avatar';

interface NavbarProps {
  prefix?: string;
  contextName: string;
}

const Navbar: React.FC<NavbarProps> = ({ prefix, contextName }) => {
  const { username, displayName, logout } = useAuthStore();
  const { currentTheme, setTheme, injectionPanelOpen, toggleSidebar, toggleInjectionPanel } = useUIStore();
  const { frequencies, leaveFrequency } = useFrequencyStore();
  const navigate = useNavigate();
  const location = useLocation();

  const [showContextActions, setShowContextActions] = useState(false);

  const isDark = currentTheme.mode !== 'light';

  // Extract current receiver ID from URL if possible
  const queryParams = new URLSearchParams(location.search);
  const currentReceiverId = queryParams.get('receiver');
  
  const currentFreq = frequencies.find(f => f.id === currentReceiverId);

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', currentTheme.mode);
  }, [currentTheme.mode]);

  const toggleTheme = () => {
    const newMode = isDark ? 'light' : 'dark';
    setTheme({ mode: newMode });
  };

  const handleLeaveFrequency = async () => {
    if (currentFreq && window.confirm(`Are you sure you want to leave ${currentFreq.name}?`)) {
      await leaveFrequency(currentFreq.id);
      navigate('/');
      setShowContextActions(false);
    }
  };

  return (
    <header className="chat-navbar glass-panel" role="banner">
      <div className="active-context" aria-live="polite">
        <button 
          className="hamburger-menu icon-only lumina-button secondary" 
          onClick={() => toggleSidebar()}
          aria-label="Toggle navigation menu"
        >
          <Menu size={20} />
        </button>
        {prefix && <span className="context-prefix" aria-hidden="true">{prefix}</span>}
        <span className="context-name">{contextName}</span>
        
        {currentFreq && (
          <div className="context-actions-wrapper">
            <button 
              className="lumina-button secondary icon-only mini-btn"
              onClick={() => setShowContextActions(!showContextActions)}
              aria-label="Context actions"
            >
              <MoreVertical size={16} />
            </button>
            
            {showContextActions && (
              <div className="context-dropdown glass-panel animate-in">
                <button className="dropdown-item danger" onClick={handleLeaveFrequency}>
                  <LogOut size={14} />
                  <span>Leave Frequency</span>
                </button>
              </div>
            )}
          </div>
        )}
      </div>
      <div className="user-controls">
        <button 
          className="lumina-button secondary icon-only" 
          onClick={() => toggleInjectionPanel()}
          aria-label="Toggle signal injections"
          title="Signal Injections"
          aria-pressed={injectionPanelOpen}
        >
          <Radio size={20} />
        </button>

        <button 
          className="lumina-button secondary icon-only theme-toggle" 
          onClick={toggleTheme}
          title={`Switch to ${isDark ? 'Light' : 'Dark'} Mode`}
          aria-label={`Switch to ${isDark ? 'light' : 'dark'} mode`}
          aria-pressed={isDark}
        >
          {isDark ? <Sun size={20} /> : <Moon size={20} />}
        </button>
        
        <button 
          className="user-badge glass-card clickable" 
          onClick={() => navigate('/profile')}
          aria-label="View your profile"
        >
          <Avatar seed={username || 'me'} size="sm" />
          <span className="username">{displayName || username}</span>
        </button>
        
        <button className="lumina-button secondary logout-btn" onClick={logout} aria-label="Logout of your account">Logout</button>
      </div>
    </header>
  );
};

export default Navbar;
