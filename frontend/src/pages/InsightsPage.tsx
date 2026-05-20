import React, { useState, useRef, useEffect } from 'react';
import { motion } from 'motion/react';
import { useAuthStore } from '../store/useAuthStore';
import { useUIStore } from '../store/useUIStore';
import { useNavigate } from 'react-router-dom';
import api from '../api/axios';

import MainLayout from '../components/MainLayout';

const InsightsPage: React.FC = () => {
  const { displayName: storedDisplayName, token, setDisplayName: setAuthDisplayName } = useAuthStore();
  const { currentTheme, setTheme } = useUIStore();
  const [displayName, setDisplayName] = useState(storedDisplayName || '');
  const [bio, setBio] = useState('');
  const [isSaving, setIsSaving] = useState(false);
  const [feedback, setFeedback] = useState<string | null>(null);
  const [feedbackType, setFeedbackType] = useState<'success' | 'error' | null>(null);
  const feedbackTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const [userStats, setUserStats] = useState([
    { label: 'Messages Sent', value: '0', trend: 'Initial', icon: '✉️' },
    { label: 'AI Tokens Generated', value: '0', trend: 'Initial', icon: '✨' },
    { label: 'Connection Index', value: '0', trend: 'Initial', icon: '🔗' },
    { label: 'Luminous Status', value: 'Dormant', trend: 'Floor', icon: '🌟' },
  ]);

  useEffect(() => {
    const fetchProfile = async () => {
      try {
        const response = await api.get('/users/me');
        const user = response.data;
        if (user.displayName) setDisplayName(user.displayName);
        if (user.bio) setBio(user.bio);
      } catch (error) {
        console.error('Failed to fetch profile', error);
      }
    };

    const fetchStats = async () => {
      try {
        const response = await api.get('/analytics/user-stats');
        const data = response.data;
        setUserStats([
          { label: 'Messages Sent', value: data.messagesSent.toLocaleString(), trend: 'Live', icon: '✉️' },
          { label: 'AI Tokens Generated', value: (data.aiTokens / 1000).toFixed(1) + 'k', trend: 'Live', icon: '✨' },
          { label: 'Connection Index', value: data.connectionIndex.toString(), trend: 'Live', icon: '🔗' },
          { 
            label: 'Luminous Status', 
            value: data.connectionIndex > 80 ? 'Radiant' : data.connectionIndex > 40 ? 'Active' : 'Dormant', 
            trend: 'Peak', 
            icon: '🌟' 
          },
        ]);
      } catch (error) {
        console.error('Failed to fetch user stats', error);
      }
    };

    if (token) {
      fetchProfile();
      fetchStats();
      const interval = setInterval(fetchStats, 15000);
      return () => clearInterval(interval);
    }
  }, [token]);

  useEffect(() => {
    return () => {
      if (feedbackTimeoutRef.current) clearTimeout(feedbackTimeoutRef.current);
    }
    }, []);

    const handleSave = async () => {
    setIsSaving(true);
    setFeedback(null);
    setFeedbackType(null);
    
    if (feedbackTimeoutRef.current) {
      clearTimeout(feedbackTimeoutRef.current);
    }

    try {
      await api.put('/users/profile', { displayName, bio });
      setAuthDisplayName(displayName);
      setFeedback("Profile frequency updated successfully.");
      setFeedbackType('success');
      feedbackTimeoutRef.current = setTimeout(() => {
        setFeedback(null);
        setFeedbackType(null);
      }, 3000);
    } catch (error) {
      const err = error as Error;
      console.error("Failed to update profile:", err);
      setFeedback("Failed to update profile. Static interference detected.");
      setFeedbackType('error');
    } finally {
      setIsSaving(false);
    }
  };

  const navigate = useNavigate();

  const handleSelectReceiver = (id: string) => {
    navigate(`/?receiver=${encodeURIComponent(id)}`);
  };

  return (
    <MainLayout
      activeReceiver="insights"
      onSelectReceiver={handleSelectReceiver}
      prefix="📊"
      contextName="AI Insights & Profile"
    >
      <section className="insights-content">
        <div className="insights-grid">
          <div className="stats-row">
            {userStats.map((stat, index) => (
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
              
              <div className="settings-group toggle-group">
                <div className="label-row">
                  <label htmlFor="adaptation-toggle">Enable AI UI Adaptation</label>
                  <button 
                    id="adaptation-toggle"
                    className={`lumina-toggle ${currentTheme.adaptationEnabled ? 'active' : ''}`}
                    onClick={() => setTheme({ adaptationEnabled: !currentTheme.adaptationEnabled })}
                    aria-pressed={currentTheme.adaptationEnabled}
                  >
                    <div className="toggle-handle"></div>
                  </button>
                </div>
                <p className="helper-text">Allow the AI to dynamically modulate your UI based on sentiment and context.</p>
              </div>

              <div className="settings-group">
                <div className="label-row">
                  <label htmlFor="accent-glow-range">Adaptation Magnitude</label>
                  <span className="value-tag">{(currentTheme.intensity * 100).toFixed(0)}%</span>
                </div>
                <input 
                  id="accent-glow-range"
                  type="range" 
                  min="0" 
                  max="1" 
                  step="0.05" 
                  value={currentTheme.intensity} 
                  onChange={(e) => setTheme({ intensity: parseFloat(e.target.value) })}
                  className="lumina-range"
                />
                <p className="helper-text">Controls how strongly the AI can influence visual properties like glow and blur.</p>
              </div>

              <div className="settings-group">
                <label htmlFor="theme-selector">Base Aesthetic</label>
                <select 
                  id="theme-selector"
                  value={currentTheme.theme}
                  onChange={(e) => setTheme({ theme: e.target.value })}
                  className="lumina-input"
                >
                  <option value="default">Prism Aura (Default)</option>
                  <option value="cyber">Cybernetic Pulse</option>
                  <option value="nature">Emerald Grove</option>
                  <option value="minimal">Void Clarity</option>
                  <option value="warm">Sunset Horizon</option>
                </select>
              </div>

              <div className="settings-group">
                <label htmlFor="primary-color-picker">Primary Frequency (Color)</label>
                <div className="color-picker-row">
                  <input 
                    id="primary-color-picker"
                    type="color" 
                    value={currentTheme.primaryColor || '#6366f1'} 
                    onChange={(e) => setTheme({ primaryColor: e.target.value })}
                    className="lumina-color-input"
                  />
                  <button 
                    className="lumina-button secondary mini"
                    onClick={() => setTheme({ primaryColor: undefined })}
                  >
                    Reset to Aura
                  </button>
                </div>
              </div>
              
              <div className="theme-preview">
                <div className="preview-bubble own" style={{ 
                  backgroundColor: currentTheme.primaryColor || 'var(--primary-glow)',
                  boxShadow: `0 0 ${currentTheme.intensity * 20}px ${currentTheme.primaryColor || 'var(--primary-glow)'}`
                }}>
                  Luminous Preview
                </div>
                <div className="preview-bubble">Adaptive Context</div>
              </div>
            </motion.div>
          </div>
        </div>
      </section>
    </MainLayout>
  );
};

export default InsightsPage;
