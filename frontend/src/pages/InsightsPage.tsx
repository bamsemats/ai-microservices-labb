import React, { useState, useRef, useEffect } from 'react';
import { motion } from 'motion/react';
import Sidebar from '../components/Sidebar';
import { useAuthStore } from '../store/useAuthStore';
import { useNavigate } from 'react-router-dom';
import api from '../api/axios';

const USER_STATS = [
  { label: 'Messages Sent', value: '1,284', trend: '+12%', icon: '✉️' },
  { label: 'AI Tokens Generated', value: '45.2k', trend: '+5%', icon: '✨' },
  { label: 'Connection Index', value: '89', trend: '+2', icon: '🔗' },
  { label: 'Luminous Status', value: 'Radiant', trend: 'Peak', icon: '🌟' },
];

const InsightsPage: React.FC = () => {
  const { username, logout } = useAuthStore();
  const [accentGlow, setAccentGlow] = useState(0.5);
  const [displayName, setDisplayName] = useState(username || '');
  const [bio, setBio] = useState('');
  const [isSaving, setIsSaving] = useState(false);
  const [feedback, setFeedback] = useState<string | null>(null);
  const [feedbackType, setFeedbackType] = useState<'success' | 'error' | null>(null);
  const feedbackTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    return () => {
      if (feedbackTimeoutRef.current) clearTimeout(feedbackTimeoutRef.current);
    };
  }, []);

  const handleUpdateGlow = (e: React.ChangeEvent<HTMLInputElement>) => {
    const val = parseFloat(e.target.value);
    setAccentGlow(val);
    document.documentElement.style.setProperty('--accent-glow-intensity', val.toString());
  };

  const handleSave = async () => {
    setIsSaving(true);
    setFeedback(null);
    setFeedbackType(null);
    
    if (feedbackTimeoutRef.current) {
      clearTimeout(feedbackTimeoutRef.current);
    }

    try {
      await api.put('/users/profile', { displayName, bio });
      setFeedback("Profile frequency updated successfully.");
      setFeedbackType('success');
      feedbackTimeoutRef.current = setTimeout(() => {
        setFeedback(null);
        setFeedbackType(null);
      }, 3000);
    } catch (error: any) {
      console.error("Failed to update profile:", error);
      setFeedback("Failed to update profile. Static interference detected.");
      setFeedbackType('error');
    } finally {
      setIsSaving(false);
    }
  };

  const navigate = useNavigate();

  const handleSelectReceiver = (id: string) => {
    navigate(`/?receiver=${id}`);
  };

  return (
    <div className="chat-page-layout">
      <Sidebar activeReceiver="insights" onSelectReceiver={handleSelectReceiver} />

      <main className="chat-main-content">
        <header className="chat-navbar glass-panel">
          <div className="active-context">
            <span className="context-prefix">📊</span>
            <span className="context-name">AI Insights & Profile</span>
          </div>
          <div className="user-controls">
            <div className="user-badge glass-card">
              <span className="username">{username}</span>
              <div className="user-avatar">{username?.charAt(0).toUpperCase()}</div>
            </div>
            <button className="lumina-button secondary logout-btn" onClick={logout}>Logout</button>
          </div>
        </header>

        <section className="insights-content">
          <div className="insights-grid">
            <div className="stats-row">
              {USER_STATS.map((stat, index) => (
                <motion.div 
                  key={index}
                  initial={{ opacity: 0, scale: 0.9 }}
                  animate={{ opacity: 1, scale: 1 }}
                  transition={{ delay: index * 0.1 }}
                  className="glass-card stat-card"
                >
                  <div className="stat-icon">{stat.icon}</div>
                  <div className="stat-info">
                    <span className="stat-label">{stat.label}</span>
                    <span className="stat-value">{stat.value}</span>
                  </div>
                  <div className={`stat-trend ${stat.trend.startsWith('+') ? 'up' : ''}`}>
                    {stat.trend}
                  </div>
                </motion.div>
              ))}
            </div>

            <div className="dashboard-main-grid">
              <motion.div 
                initial={{ opacity: 0, x: -20 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: 0.4 }}
                className="glass-panel profile-settings"
              >
                <h3>Profile Settings</h3>
                <div className="settings-group">
                  <label htmlFor="displayName">Display Name</label>
                  <input 
                    id="displayName"
                    type="text" 
                    value={displayName} 
                    onChange={(e) => setDisplayName(e.target.value)}
                    className="lumina-input" 
                  />
                </div>
                <div className="settings-group">
                  <label htmlFor="bio">Bio (AI Context)</label>
                  <textarea 
                    id="bio"
                    placeholder="Tell the AI about your interests..." 
                    className="lumina-input"
                    rows={4}
                    value={bio}
                    onChange={(e) => setBio(e.target.value)}
                  ></textarea>
                </div>
                <div className="button-row">
                  <button 
                    className="lumina-button" 
                    onClick={handleSave}
                    disabled={isSaving}
                  >
                    {isSaving ? 'Processing...' : 'Save Changes'}
                  </button>
                  {feedback && (
                    <motion.span 
                      initial={{ opacity: 0, x: 10 }}
                      animate={{ opacity: 1, x: 0 }}
                      className={`feedback-msg ${feedbackType}`}
                    >
                      {feedback}
                    </motion.span>
                  )}
                </div>
              </motion.div>

              <motion.div 
                initial={{ opacity: 0, x: 20 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: 0.5 }}
                className="glass-panel design-preferences"
              >
                <h3>Adapta Theme Preferences</h3>
                <div className="settings-group">
                  <div className="label-row">
                    <label>Accent Glow Intensity</label>
                    <span className="value-tag">{(accentGlow * 100).toFixed(0)}%</span>
                  </div>
                  <input 
                    type="range" 
                    min="0" 
                    max="1" 
                    step="0.05" 
                    value={accentGlow} 
                    onChange={handleUpdateGlow}
                    className="lumina-range"
                  />
                  <p className="helper-text">Modulates the intensity of all luminous surface effects.</p>
                </div>
                
                <div className="theme-preview">
                  <div className="preview-bubble own">Luminous Preview</div>
                  <div className="preview-bubble">Adaptive Context</div>
                </div>
              </motion.div>
            </div>
          </div>
        </section>
      </main>

      <style>{`
        .insights-content {
          flex: 1;
          overflow-y: auto;
          padding: 1rem 2rem 2rem;
        }

        .insights-grid {
          display: flex;
          flex-direction: column;
          gap: 2rem;
        }

        .stats-row {
          display: grid;
          grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
          gap: 1.5rem;
        }

        .stat-card {
          display: flex;
          align-items: center;
          gap: 1rem;
          padding: 1.25rem !important;
          position: relative;
        }

        .stat-icon {
          font-size: 1.5rem;
          width: 3rem;
          height: 3rem;
          background: rgba(255, 255, 255, 0.05);
          border-radius: 50%;
          display: flex;
          align-items: center;
          justify-content: center;
        }

        .stat-info {
          display: flex;
          flex-direction: column;
        }

        .stat-label {
          font-size: 0.75rem;
          color: var(--text-muted);
          text-transform: uppercase;
          letter-spacing: 0.05em;
        }

        .stat-value {
          font-size: 1.25rem;
          font-weight: 800;
        }

        .stat-trend {
          position: absolute;
          top: 1rem;
          right: 1rem;
          font-size: 0.7rem;
          font-weight: 700;
          color: var(--text-muted);
          padding: 0.2rem 0.5rem;
          background: rgba(255, 255, 255, 0.03);
          border-radius: 1rem;
        }

        .stat-trend.up {
          color: var(--success);
          background: rgba(16, 185, 129, 0.1);
        }

        .dashboard-main-grid {
          display: grid;
          grid-template-columns: 1fr 1fr;
          gap: 2rem;
        }

        @media (max-width: 1024px) {
          .dashboard-main-grid {
            grid-template-columns: 1fr;
          }
        }

        .glass-panel h3 {
          margin-bottom: 2rem;
          font-size: 1.25rem;
          font-weight: 800;
        }

        .settings-group {
          margin-bottom: 1.5rem;
        }

        .label-row {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 0.75rem;
        }

        .value-tag {
          font-size: 0.75rem;
          font-weight: 800;
          color: var(--accent-primary);
          background: rgba(139, 92, 246, 0.1);
          padding: 0.2rem 0.5rem;
          border-radius: 0.5rem;
        }

        .helper-text {
          font-size: 0.75rem;
          color: var(--text-muted);
          margin-top: 0.75rem;
        }

        .lumina-input {
          width: 100%;
          background: rgba(0, 0, 0, 0.2);
          border: 1px solid rgba(255, 255, 255, 0.1);
          border-radius: 0.75rem;
          padding: 0.875rem 1rem;
          color: white;
          font-size: 0.9375rem;
          transition: all 0.2s ease;
        }

        .lumina-input:focus {
          outline: none;
          border-color: var(--accent-primary);
          background: rgba(0, 0, 0, 0.3);
          box-shadow: 0 0 0 4px rgba(139, 92, 246, 0.1);
        }

        .lumina-range {
          width: 100%;
          accent-color: var(--accent-primary);
          cursor: pointer;
        }

        .theme-preview {
          margin-top: 2rem;
          padding: 2rem;
          background: rgba(0, 0, 0, 0.2);
          border-radius: 1rem;
          display: flex;
          flex-direction: column;
          gap: 1rem;
        }

        .preview-bubble {
          padding: 0.75rem 1rem;
          border-radius: 1rem;
          background: rgba(255, 255, 255, 0.05);
          font-size: 0.875rem;
          width: fit-content;
        }

        .preview-bubble.own {
          align-self: flex-end;
          background: var(--accent-gradient);
          color: white;
          box-shadow: var(--accent-glow);
        }
      `}</style>
    </div>
  );
};

export default InsightsPage;
