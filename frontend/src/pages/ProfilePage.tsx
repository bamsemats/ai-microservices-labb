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
        <div className="site-page page--profile">
          <div className="page-scroll">
            <div className="page-content">
              <header className="page-header">
                <h1 className="page-title">Digital Identity</h1>
                <p className="page-subtitle">Customize your digital footprint and synchronization parameters.</p>
              </header>

              <div className="profile-card-wrapper">
              {forcePasswordChange && (
                <motion.div
                  initial={{ opacity: 0, y: -20 }}
                  animate={{ opacity: 1, y: 0 }}
                  className="toast toast--error"
                  style={{ inset: 'auto', marginBottom: '1.5rem', width: '100%' }}
                >
                  <span>Security Alert: You are using a temporary password. Please update it to continue using the platform.</span>
                </motion.div>
              )}

              <motion.div
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                className="glass-panel card"
              >
                <div className="profile-hero">
                  <Avatar seed={username || 'me'} size="xl" />
                  <div className="header-text">
                    <h2>{username}</h2>
                    <p>Manage your core identity across the Adapta Network.</p>
                  </div>
                </div>

                <div className="dashboard-main-grid">
                  <div className="settings-section">
                    <h3>Basic Identity</h3>
                    <div className="form-field">
                      <label
                          htmlFor="displayName"
                          className="form-label"
                      >
                        Display Name
                      </label>
                      <input
                          id="displayName"
                          type="text"
                          value={displayName}
                          onChange={(e) => setDisplayName(e.target.value)}
                          placeholder="Your public name..."
                          className="input"
                      />
                    </div>
                    <div className="form-group" style={{ marginTop: '1.5rem' }}>
                      <label
                          htmlFor="bio"
                          className="form-label"
                      >
                        Bio (AI Context)
                      </label>
                      <textarea
                          id="bio"
                          value={bio}
                          onChange={(e) => setBio(e.target.value)}
                          placeholder="Tell the community and AI about yourself..."
                          className="input"
                          rows={5}
                      />
                      <p className="helper-text">This bio helps the AI understand your preferences and personality.</p>
                    </div>
                    <div className="form-group">
                      <h3 style={{ marginTop: '2.5rem' }}>Social Frequencies</h3>
                      <div className="form-group">
                        <label
                            htmlFor="twitter"
                            className="form-label"
                        >
                          Twitter / X
                        </label>
                        <div className="input-group">
                          <span>🐦</span>
                          <input
                              id="twitter"
                              type="text"
                              value={socialLinks.twitter || ''}
                              onChange={(e) => handleSocialChange('twitter', e.target.value)}
                              placeholder="@username"
                              className="input"
                          />
                        </div>
                      </div>
                      <div className="form-group">
                        <label
                            htmlFor="github"
                            className="form-label"
                        >
                          GitHub
                        </label>
                        <div className="input-group">
                          <span>🐙</span>
                          <input
                              id="github"
                              type="text"
                              value={socialLinks.github || ''}
                              onChange={(e) => handleSocialChange('github', e.target.value)}
                              placeholder="github-profile"
                              className="input"
                          />
                        </div>
                      </div>
                      <div className="form-group">
                      <label
                           htmlFor="website"
                           className="form-label"
                       >
                         Website
                       </label>
                       <div className="input-group">
                         <span>🔗</span>
                         <input
                          id="website"
                          type="url"
                          value={socialLinks.website || ''}
                          onChange={(e) => handleSocialChange('website', e.target.value)}
                          placeholder="https://example.com"
                          className="input"
                         />
                       </div>
                      </div>
                    </div>
                  </div>

                  <div className="settings-section">
                    <h3>Security & Access</h3>
                    <form onSubmit={handleChangePassword} className="password-change-form">
                      <div className="form-group">
                        <label
                            htmlFor="oldPassword"
                            className="form-label"
                        >
                          Current Password
                        </label>
                        <input
                            id="oldPassword"
                            type="password"
                            value={oldPassword}
                            onChange={(e) => setOldPassword(e.target.value)}
                            className="input"
                            required
                        />
                      </div>
                      <div className="form-group" style={{ marginTop: '1rem' }}>
                        <label
                            htmlFor="newPassword"
                            className="form-label"
                        >
                          New Password
                        </label>
                        <input
                          id="newPassword"
                          type="password"
                          value={newPassword}
                          onChange={(e) => setNewPassword(e.target.value)}
                          className="input"
                          required
                          minLength={8}
                        />
                      </div>
                      <div className="form-group" style={{ marginTop: '1rem' }}>
                        <label
                            htmlFor="confirmPassword"
                            className="form-label"
                        >
                          Confirm New Password
                        </label>
                        <input
                          id="confirmPassword"
                          type="password"
                          value={confirmPassword}
                          onChange={(e) => setConfirmPassword(e.target.value)}
                          className="input"
                          required
                        />
                      </div>
                      <button
                        type="submit"
                        className={`btn ${forcePasswordChange ? 'btn-primary' : 'btn-ghost'}`}
                        style={{ marginTop: '1.5rem', width: '100%' }}
                        disabled={isChangingPassword}
                      >
                        {isChangingPassword ? 'Updating...' : 'Update Password'}
                      </button>
                    </form>
                    <div className="modal-footer" style={{border: 'none', padding: 0, marginTop: '2rem'}}>
                      <button
                          className="btn btn-primary"
                          onClick={handleSave}
                          disabled={isSaving}
                      >
                        {isSaving ? 'Syncing...' : 'Save Profile'}
                      </button>
                    </div>
                  </div>
                </div>

                <div className="modal-footer">
                  {feedback && (
                    <motion.span
                      initial={{ opacity: 0, x: 10 }}
                      animate={{ opacity: 1, x: 0 }}
                      className={`toast toast--success ${feedback.type}`}
                    >
                      {feedback.message}
                    </motion.span>
                  )}
                </div>
              </motion.div>
            </div>
          </div>
        </div>
      </div>
    </MainLayout>
  );
};

export default ProfilePage;
