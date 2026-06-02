import React, { useEffect, useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuthStore } from '../store/useAuthStore';
import { useUIStore } from '../store/useUIStore';
import { useFrequencyStore } from '../store/useFrequencyStore';
import { useSocialStore } from '../store/useSocialStore';
import { Sun, Moon, Menu, Radio, MoreVertical, LogOut, UserPlus } from 'lucide-react';
import Avatar from './Avatar';

interface NavbarProps {
  prefix?: string;
  contextName: string;
}

const Navbar: React.FC<NavbarProps> = ({ prefix, contextName }) => {
  const { userId, username, displayName, logout } = useAuthStore();
  const { currentTheme, setTheme, injectionPanelOpen, toggleSidebar, toggleInjectionPanel } = useUIStore();
  const { frequencies, leaveFrequency, inviteMember } = useFrequencyStore();
  const { friends } = useSocialStore();
  const navigate = useNavigate();
  const location = useLocation();

  const [showContextActions, setShowContextActions] = useState(false);
  const [showInviteSubmenu, setShowInviteSubmenu] = useState(false);

  const isDark = currentTheme.mode !== 'light';

  // Extract current receiver ID from URL if possible
  const queryParams = new URLSearchParams(location.search);
  const currentReceiverId = queryParams.get('receiver');
  
  const currentFreq = frequencies.find(f => f.id === currentReceiverId);
  const isOwner = currentFreq?.ownerId === userId;

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', currentTheme.mode);
  }, [currentTheme.mode]);

  useEffect(() => {
    // Close dropdowns on route change
    setShowContextActions(false);
    setShowInviteSubmenu(false);
  }, [location.pathname, location.search]);

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

  const invitableFriends = friends.filter(f => currentFreq && !currentFreq.members.includes(f.id));

  return (
    <header className="app-navbar glass-panel" role="banner">
      <nav className="navbar-start" aria-label="Quick actions" aria-live="polite">
        <button 
          className="btn btn-icon btn-icon--round"
          onClick={() => toggleSidebar()}
          aria-label={sidebarOpen ? "Close navigation menu" : "Open navigation menu"}
          aria-expanded={sidebarOpen}
        >
          <Menu size={20} />
        </button>
        {prefix && <span className="context-prefix" aria-hidden="true">{prefix}</span>}
        <span className="context-name">{contextName}</span>
        
        {currentFreq && (
          <div className="context-actions-wrapper">
            <button 
              className="btn btn-icon"
              onClick={() => setShowContextActions(!showContextActions)}
              aria-label="Context actions"
              aria-haspopup="true"
              aria-expanded={showContextActions}
            >
              <MoreVertical size={16} />
            </button>
            
            {showContextActions && (
              <div className="dropdown glass-panel" role="menu">
                {isOwner && (
                  <div className="dropdown-submenu-wrapper" role="none">
                    <button 
                      className="dropdown-item" 
                      onClick={() => setShowInviteSubmenu(!showInviteSubmenu)}
                      aria-haspopup="true"
                      aria-expanded={showInviteSubmenu}
                      role="menuitem"
                    >
                      <UserPlus size={14} aria-hidden="true" />
                      <span>Invite Entity</span>
                    </button>
                    
                    {showInviteSubmenu && (
                      <div className="invite-submenu glass-panel" role="menu" aria-label="Friends to invite">
                        {invitableFriends.length > 0 ? (
                          invitableFriends.map(friend => (
                            <button 
                              key={friend.id}
                              className="dropdown-item mini"
                              onClick={async () => {
                                await inviteMember(currentFreq.id, friend.id);
                                setShowInviteSubmenu(false);
                                setShowContextActions(false);
                              }}
                              role="menuitem"
                            >
                              <Avatar seed={friend.username} size="sm" isBot={friend.isBot} />
                              <span>{friend.username}</span>
                            </button>
                          ))
                        ) : (
                          <div className="dropdown-item disabled" role="menuitem" aria-disabled="true">No entities available</div>
                        )}
                      </div>
                    )}
                  </div>
                )}
                <button className="dropdown-item is-danger" onClick={handleLeaveFrequency} role="menuitem">
                  <LogOut size={14} aria-hidden="true" />
                  <span>Leave Frequency</span>
                </button>
              </div>
            )}
          </div>
        )}
      </nav>
      <nav className="navbar-end" aria-label="System controls">
        <button 
          className="btn btn-icon"
          onClick={() => toggleInjectionPanel()}
          aria-label="Toggle signal injections"
          title="Signal Injections"
          aria-pressed={injectionPanelOpen}
        >
          <Radio size={20} />
        </button>

        <button 
          className="btn btn-icon"
          onClick={toggleTheme}
          title={`Switch to ${isDark ? 'Light' : 'Dark'} Mode`}
          aria-label={`Switch to ${isDark ? 'light' : 'dark'} mode`}
          aria-pressed={isDark}
        >
          {isDark ? <Sun size={20} /> : <Moon size={20} />}
        </button>
        
        <button 
          className="btn btn-ghost user-baddge"
          onClick={() => navigate('/profile')}
          aria-label="View your profile"
        >
          <Avatar seed={username || 'me'} size="sm" />
          <span className="username" aria-hidden="true">{displayName || username}</span>
        </button>
        
        <button className="btn btn-ghost" onClick={logout} aria-label="Logout of your account">Logout</button>
      </nav>
    </header>
  );
};

export default Navbar;
