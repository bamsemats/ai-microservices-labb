import React, { useState, useEffect } from 'react';
import { motion } from 'motion/react';
import Sidebar from '../components/Sidebar';
import Navbar from '../components/Navbar';
import { useAuthStore } from '../store/useAuthStore';
import api from '../api/axios';

interface SocialLinks {
  twitter?: string;
  github?: string;
  website?: string;
}

const ProfilePage: React.FC = () => {
  const { username, token } = useAuthStore();
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

  const handleSave = async () => {
    setIsSaving(true);
    setFeedback(null);
    try {
      await api.put('/users/profile', { displayName, bio, socialLinks });
      setFeedback({ message: 'Profile updated successfully!', type: 'success' });
      setTimeout(() => setFeedback(null), 3000);
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

  return (
    <div className="chat-page-layout">
      <Sidebar activeReceiver="profile" onSelectReceiver={() => {}} />

      <main className="chat-main-content">
        <Navbar prefix="👤" contextName="User Profile" />

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
                    <label>Display Name</label>
                    <input 
                      type="text" 
                      value={displayName} 
                      onChange={(e) => setDisplayName(e.target.value)}
                      placeholder="Your public name..."
                      className="lumina-input"
                    />
                  </div>
                  <div className="settings-group">
                    <label>Bio (AI Context)</label>
                    <textarea 
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
                    <label>Twitter / X</label>
                    <div className="input-with-icon">
                      <span className="input-icon">🐦</span>
                      <input 
                        type="text" 
                        value={socialLinks.twitter || ''} 
                        onChange={(e) => handleSocialChange('twitter', e.target.value)}
                        placeholder="@username"
                        className="lumina-input"
                      />
                    </div>
                  </div>
                  <div className="settings-group">
                    <label>GitHub</label>
                    <div className="input-with-icon">
                      <span className="input-icon">🐙</span>
                      <input 
                        type="text" 
                        value={socialLinks.github || ''} 
                        onChange={(e) => handleSocialChange('github', e.target.value)}
                        placeholder="github-profile"
                        className="lumina-input"
                      />
                    </div>
                  </div>
                  <div className="settings-group">
                    <label>Personal Frequency (Website)</label>
                    <div className="input-with-icon">
                      <span className="input-icon">🌐</span>
                      <input 
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
      </main>

      <style>{`
        .profile-content {
          flex: 1;
          overflow-y: auto;
          padding: 2rem;
          display: flex;
          justify-content: center;
        }

        .profile-card-wrapper {
          width: 100%;
          max-width: 900px;
        }

        .profile-editor {
          padding: 3rem !important;
        }

        .editor-header {
          display: flex;
          align-items: center;
          gap: 2rem;
          margin-bottom: 3rem;
          padding-bottom: 2rem;
          border-bottom: 1px solid var(--glass-border);
        }

        .profile-avatar-large {
          width: 6rem;
          height: 6rem;
          background: var(--accent-gradient);
          border-radius: 50%;
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 2.5rem;
          font-weight: 800;
          color: white;
          box-shadow: var(--accent-glow);
        }

        .header-text h2 {
          font-size: 2rem;
          margin-bottom: 0.25rem;
        }

        .header-text p {
          color: var(--text-secondary);
        }

        .editor-grid {
          display: grid;
          grid-template-columns: 1.5fr 1fr;
          gap: 3rem;
        }

        @media (max-width: 768px) {
          .editor-grid {
            grid-template-columns: 1fr;
          }
        }

        .editor-section h3 {
          font-size: 1.1rem;
          margin-bottom: 1.5rem;
          color: var(--accent-primary);
        }

        .settings-group {
          margin-bottom: 1.5rem;
        }

        .settings-group label {
          display: block;
          font-size: 0.8125rem;
          font-weight: 700;
          color: var(--text-secondary);
          text-transform: uppercase;
          letter-spacing: 0.05em;
          margin-bottom: 0.5rem;
        }

        .helper-text {
          font-size: 0.75rem;
          color: var(--text-muted);
          margin-top: 0.5rem;
        }

        .input-with-icon {
          position: relative;
          display: flex;
          align-items: center;
        }

        .input-icon {
          position: absolute;
          left: 1rem;
          font-size: 1.1rem;
        }

        .input-with-icon .lumina-input {
          padding-left: 3rem;
        }

        .editor-footer {
          margin-top: 3rem;
          padding-top: 2rem;
          border-top: 1px solid var(--glass-border);
          display: flex;
          align-items: center;
          gap: 1.5rem;
        }

        .feedback-toast {
          font-weight: 600;
          font-size: 0.9rem;
        }

        .feedback-toast.success {
          color: var(--success);
        }

        .feedback-toast.error {
          color: var(--error);
        }
      `}</style>
    </div>
  );
};

export default ProfilePage;
