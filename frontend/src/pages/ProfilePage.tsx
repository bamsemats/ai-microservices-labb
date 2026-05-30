import React, { useState, useEffect } from 'react';
import { motion } from 'motion/react';
import { useAuthStore } from '../store/useAuthStore';
import api from '../api/axios';
import MainLayout from '../components/MainLayout';
import Avatar from '../components/Avatar';

interface SocialLinks {
  twitter?: string;
  github?: string;
  website?: string;
}

const ProfilePage: React.FC = () => {
  const { username, token, forcePasswordChange, setForcePasswordChange } = useAuthStore();
  const [displayName, setDisplayName] = useState('');
  const [bio, setBio] = useState('');
  const [socialLinks, setSocialLinks] = useState<SocialLinks>({});
  const [oldPassword, setOldPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [isSaving, setIsSaving] = useState(false);
  const [isChangingPassword, setIsChangingPassword] = useState(false);
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

  const handleChangePassword = async (e: React.FormEvent) => {
    e.preventDefault();
    if (newPassword !== confirmPassword) {
      setFeedback({ message: 'New passwords do not match.', type: 'error' });
      return;
    }
    
    setIsChangingPassword(true);
    setFeedback(null);
    try {
      await api.put('/users/password', { oldPassword, newPassword });
      setFeedback({ message: 'Password updated successfully!', type: 'success' });
      setOldPassword('');
      setNewPassword('');
      setConfirmPassword('');
      setForcePasswordChange(false);
    } catch (error) {
      console.error('Failed to change password', error);
      setFeedback({ message: 'Failed to change password. Check your current password.', type: 'error' });
    } finally {
      setIsChangingPassword(false);
    }
  };

  const handleSocialChange = (key: keyof SocialLinks, value: string) => {
    setSocialLinks(prev => ({ ...prev, [key]: value }));
  };

  return (
    <MainLayout
      activeReceiver="profile"
      prefix="👤"
      contextName="User Profile"
    >
      <section className="profile-content">
        <div className="profile-card-wrapper">
          {forcePasswordChange && (
            <motion.div 
              initial={{ opacity: 0, y: -20 }}
              animate={{ opacity: 1, y: 0 }}
              className="error-toast"
              style={{ position: 'relative', inset: 'auto', marginBottom: '1.5rem', width: '100%' }}
            >
              <span>Security Alert: You are using a temporary password. Please update it to continue using the platform.</span>
            </motion.div>
          )}

          <motion.div 
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className="glass-panel profile-editor"
          >
            <div className="editor-header">
              <Avatar seed={username || 'me'} size="xl" />
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
                <div className="settings-group">
                   <label htmlFor="website">Website</label>
                   <div className="input-with-icon">
                     <span className="input-icon">🔗</span>
                     <input
                                          id="website"
                     type="url"
                     value={socialLinks.website || ''}
                     onChange={(e) => handleSocialChange('website', e.target.value)}
                     placeholder="https://example.com"
                     className="lumina-input"
                     />
                   </div>
                </div>
                <div className="editor-footer" style={{border: 'none', padding: 0, marginTop: '1.5rem'}}>
                  <button
                      className="lumina-button"
                      onClick={handleSave}
                      disabled={isSaving}
                  >
                    {isSaving ? 'Syncing...' : 'Save Profile'}
                  </button>
                </div>
              </div>

              <div className="editor-section">
                <h3>Security & Access</h3>
                <form onSubmit={handleChangePassword} className="password-change-form">
                  <div className="settings-group">
                    <label htmlFor="oldPassword">Current Password</label>
                    <input
                        id="oldPassword"
                        type="password"
                        value={oldPassword}
                        onChange={(e) => setOldPassword(e.target.value)}
                        className="lumina-input"
                        required
                    />
                  </div>
                  <div className="settings-group">
                    <label htmlFor="newPassword">New Password</label>
                    <input 
                      id="newPassword"
                      type="password" 
                      value={newPassword} 
                      onChange={(e) => setNewPassword(e.target.value)}
                      className="lumina-input"
                      required
                      minLength={8}
                    />
                  </div>
                  <div className="settings-group">
                    <label htmlFor="confirmPassword">Confirm New Password</label>
                    <input 
                      id="confirmPassword"
                      type="password" 
                      value={confirmPassword} 
                      onChange={(e) => setConfirmPassword(e.target.value)}
                      className="lumina-input"
                      required
                    />
                  </div>
                  <button 
                    type="submit" 
                    className={`lumina-button ${forcePasswordChange ? 'primary' : 'secondary'} full-width`}
                    disabled={isChangingPassword}
                  >
                    {isChangingPassword ? 'Updating...' : 'Update Password'}
                  </button>
                </form>

                <h3 style={{ marginTop: '2rem' }}>Social Frequencies</h3>
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
              </div>
            </div>

            <div className="editor-footer">
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
