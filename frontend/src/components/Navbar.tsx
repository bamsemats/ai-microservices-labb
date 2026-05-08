import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../store/useAuthStore';
import { useUIStore } from '../store/useUIStore';

interface NavbarProps {
  prefix?: string;
  contextName: string;
}

const Navbar: React.FC<NavbarProps> = ({ prefix, contextName }) => {
  const { username, logout } = useAuthStore();
  const { currentTheme, setTheme, toggleSidebar } = useUIStore();
  const navigate = useNavigate();

  const isDark = currentTheme.mode !== 'light';

  const toggleTheme = () => {
    const newMode = isDark ? 'light' : 'dark';
    setTheme({ mode: newMode });
    document.documentElement.setAttribute('data-theme', newMode);
  };

  return (
    <header className="chat-navbar glass-panel" role="banner">
      <div className="active-context" aria-live="polite">
        <button 
          className="hamburger-menu icon-only lumina-button secondary" 
          onClick={() => toggleSidebar()}
          aria-label="Toggle navigation menu"
        >
          ☰
        </button>
        {prefix && <span className="context-prefix" aria-hidden="true">{prefix}</span>}
        <span className="context-name">{contextName}</span>
      </div>
      <div className="user-controls">
        <button 
          className="lumina-button secondary icon-only theme-toggle" 
          onClick={toggleTheme}
          title={`Switch to ${isDark ? 'Light' : 'Dark'} Mode`}
          aria-label={`Switch to ${isDark ? 'light' : 'dark'} mode`}
        >
          {isDark ? '☀️' : '🌙'}
        </button>
        
        <div 
          className="user-badge glass-card clickable" 
          onClick={() => navigate('/profile')}
          role="link"
          aria-label="View your profile"
          tabIndex={0}
          onKeyDown={(e) => e.key === 'Enter' && navigate('/profile')}
        >
          <span className="username">{username}</span>
          <div className="user-avatar" aria-hidden="true">{username?.charAt(0).toUpperCase()}</div>
        </div>
        
        <button className="lumina-button secondary logout-btn" onClick={logout} aria-label="Logout of your account">Logout</button>
      </div>

      <style>{`
        .clickable {
          cursor: pointer;
          transition: all 0.2s ease;
        }
        .clickable:hover {
          background: rgba(255, 255, 255, 0.1) !important;
          border-color: var(--accent-primary) !important;
        }
        .icon-only {
          width: 2.5rem;
          height: 2.5rem;
          padding: 0 !important;
          border-radius: 50% !important;
          font-size: 1.25rem;
        }
        .theme-toggle {
          margin-right: 0.5rem;
        }
        .hamburger-menu {
          display: none;
          margin-right: 0.75rem;
        }
        @media (max-width: 768px) {
          .hamburger-menu {
            display: flex;
          }
        }
      `}</style>
    </header>
  );
};

export default Navbar;
