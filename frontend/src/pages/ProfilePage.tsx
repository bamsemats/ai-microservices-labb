import React, { useState, useEffect } from 'react';
import { motion } from 'motion/react';
import { useAuthStore } from '../store/useAuthStore';
import api from '../api/axios';
import { useNavigate } from 'react-router-dom';

interface SocialLinks {
  twitter?: string;
  github?: string;
  website?: string;
}

import MainLayout from '../components/MainLayout';

const ProfilePage: React.FC = () => {
  const { username, token } = useAuthStore();
  const navigate = useNavigate();
  const [displayName, setDisplayName] = useState('');
  const [bio, setBio] = useState('');
  const [socialLinks, setSocialLinks] = useState<SocialLinks>({});
  const [isSaving, setIsSaving] = useState(false);
  const [feedback, setFeedback] = useState<{ message: string; type: 'success' | 'error' } | null>(null);

  useEffect(() => {
    const fetchProfile = async () => {
      try {
        const response = await api.get('/users/me');
        const user = response.data;
        setDisplayName(user.displayName || '');
        setBio(user.bio || '');
        setSocialLinks(user.socialLinks || {});
      } catch (error) {
        console.error('Failed to fetch profile', error);
      }
    };
    if (token) fetchProfile();
  }, [token]);

  useEffect(() => {
    if (feedback?.type === 'success') {
      const timer = setTimeout(() => setFeedback(null), 3000);
      return () => clearTimeout(timer);
    }
  }, [feedback]);

  const handleSave = async () => {
    setIsSaving(true);
    setFeedback(null);
    try {
      await api.put('/users/profile', { displayName, bio, socialLinks });
      useAuthStore.getState().setDisplayName(displayName || null);
      setFeedback({ message: 'Profile updated successfully!', type: 'success' });
    } catch (error) {
      console.error('Failed to update profile', error);
      setFeedback({ message: 'Failed to update profile. Please try again.', type: 'error' });
    } finally {
      setIsSaving(false);
    }
  };

  const handleSocialChange = (key: keyof SocialLinks, value: string) => {
    setSocialLinks(prev => ({ ...prev, [key]: value }));
  };

  const handleSelectReceiver = (id: string) => {
    navigate(`/?receiver=${encodeURIComponent(id)}`);
  };

  return (
    <MainLayout
      activeReceiver="profile"
      onSelectReceiver={handleSelectReceiver}
      prefix="👤"
      contextName="User Profile"
    >
      <section className="profile-content">
        <div className="profile-card-wrapper">
          <motion.div 
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className="glass-panel profile-editor"
          >
            <div className="editor-header">
              <div className="profile-avatar-large">{username?.charAt(0).toUpperCase()}</div>
              <div className="header-text">
                <h2>{username}</h2>
                <p>Customize your digital identity across the Adapta Network.</p>
              </div>
            </div>

            <div className="editor-grid">
              <div className="editor-section">
                <h3>Basic Identity</h3>
                <div className="settings-group">
                  <label htmlFor="displayName">Display Name</label>
                  <input 
                    id="displayName"
                    type="text" 
                    value={displayName} 
                    onChange={(e) => setDisplayName(e.target.value)}
                    placeholder="Your public name..."
                    className="lumina-input"
                  />
                </div>
                <div className="settings-group">
                  <label htmlFor="bio">Bio (AI Context)</label>
                  <textarea 
                    id="bio"
                    value={bio} 
                    onChange={(e) => setBio(e.target.value)}
                    placeholder="Tell the community and AI about yourself..."
                    className="lumina-input"
                    rows={5}
                  />
                  <p className="helper-text">This bio helps the AI understand your preferences and personality.</p>
                </div>
              </div>

              <div className="editor-section">
                <h3>Social Frequencies</h3>
                <div className="settings-group">
                  <label htmlFor="twitter">Twitter / X</label>
                  <div className="input-with-icon">
                    <span className="input-icon">🐦</span>
                    <input 
                      id="twitter"
                      type="text" 
                      value={socialLinks.twitter || ''} 
                      onChange={(e) => handleSocialChange('twitter', e.target.value)}
                      placeholder="@username"
                      className="lumina-input"
                    />
                  </div>
                </div>
                <div className="settings-group">
                  <label htmlFor="github">GitHub</label>
                  <div className="input-with-icon">
                    <span className="input-icon">🐙</span>
                    <input 
                      id="github"
                      type="text" 
                      value={socialLinks.github || ''} 
                      onChange={(e) => handleSocialChange('github', e.target.value)}
                      placeholder="github-profile"
                      className="lumina-input"
                    />
                  </div>
                </div>
                <div className="settings-group">
                  <label htmlFor="website">Personal Frequency (Website)</label>
                  <div className="input-with-icon">
                    <span className="input-icon">🌐</span>
                    <input 
                      id="website"
                      type="text" 
                      value={socialLinks.website || ''} 
                      onChange={(e) => handleSocialChange('website', e.target.value)}
                      placeholder="https://..."
                      className="lumina-input"
                    />
                  </div>
                </div>
              </div>
            </div>

            <div className="editor-footer">
              <button 
                className="lumina-button" 
                onClick={handleSave}
                disabled={isSaving}
              >
                {isSaving ? 'Syncing...' : 'Save Profile'}
              </button>
              {feedback && (
                <motion.span 
                  initial={{ opacity: 0, x: 10 }}
                  animate={{ opacity: 1, x: 0 }}
                  className={`feedback-toast ${feedback.type}`}
                >
                  {feedback.message}
                </motion.span>
              )}
            </div>
          </motion.div>
        </div>
      </section>
    </MainLayout>
  );
};

export default ProfilePage;
