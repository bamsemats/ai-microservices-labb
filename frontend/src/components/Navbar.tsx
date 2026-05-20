import React, { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../store/useAuthStore';
import { useUIStore } from '../store/useUIStore';
import { Sun, Moon } from 'lucide-react';

interface NavbarProps {
  prefix?: string;
  contextName: string;
}

const Navbar: React.FC<NavbarProps> = ({ prefix, contextName }) => {
  const { username, displayName, logout } = useAuthStore();
  const { currentTheme, setTheme, toggleSidebar } = useUIStore();
  const navigate = useNavigate();

  const isDark = currentTheme.mode !== 'light';

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', currentTheme.mode);
  }, [currentTheme.mode]);

  const toggleTheme = () => {
    const newMode = isDark ? 'light' : 'dark';
    setTheme({ mode: newMode });
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
          aria-pressed={isDark}
        >
          {isDark ? <Sun/> : <Moon/>}
        </button>
        
        <button 
          className="user-badge glass-card clickable" 
          onClick={() => navigate('/profile')}
          aria-label="View your profile"
          style={{ background: 'none', border: 'none', color: 'inherit', font: 'inherit' }}
        >
          <span className="username">{displayName || username}</span>
        </button>
        
        <button className="lumina-button secondary logout-btn" onClick={logout} aria-label="Logout of your account">Logout</button>
      </div>
    </header>
  );
};

export default Navbar;
