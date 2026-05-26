import React, {useState, useEffect, useCallback} from 'react';
import { motion } from 'motion/react';
import { useAuthStore } from '../store/useAuthStore';
import api from '../api/axios';
import MainLayout from '../components/MainLayout';

interface Feedback {
  id: string;
  userId: string | null;
  rating: number;
  comment: string;
  timestamp: string;
}

interface User {
  id: string;
  username: string;
  displayName: string | null;
  roles: string[];
}

const AdminPage: React.FC = () => {
  const { isAdmin } = useAuthStore();
  const [feedbackList, setFeedbackList] = useState<Feedback[]>([]);
  const [isLoadingFeedback, setIsLoadingFeedback] = useState(true);
  
  // Broadcast State
  const [broadcastContent, setBroadcastContent] = useState('');
  const [isBroadcasting, setIsBroadcasting] = useState(false);
  const [broadcastStatus, setBroadcastStatus] = useState<{ type: 'success' | 'error', msg: string } | null>(null);

  // User Management State
  const [searchQuery, setSearchQuery] = useState('');
  const [foundUser, setFoundUser] = useState<User | null>(null);
  const [isSearching, setIsSearching] = useState(false);
  const [roleUpdateStatus, setRoleUpdateStatus] = useState<{ type: 'success' | 'error', msg: string } | null>(null);

  const fetchFeedback = useCallback(async () => {
    try {
      setIsLoadingFeedback(true);

      const response = await api.get('/feedback');

      const sorted = (response.data as Feedback[]).sort(
          (a, b) =>
              new Date(b.timestamp).getTime() -
              new Date(a.timestamp).getTime()
      );

      setFeedbackList(sorted);
    } catch (error) {
      console.error('Failed to fetch feedback', error);
    } finally {
      setIsLoadingFeedback(false);
    }
  }, []);

  useEffect(() => {
    if (!isAdmin) return;

    // eslint-disable-next-line react-hooks/set-state-in-effect
    void fetchFeedback();
  }, [isAdmin, fetchFeedback]);

  const handleBroadcast = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!broadcastContent.trim()) return;

    setIsBroadcasting(true);
    setBroadcastStatus(null);
    try {
      await api.post('/messages/broadcast', { content: broadcastContent });
      setBroadcastStatus({ type: 'success', msg: 'Broadcast frequency synchronized successfully.' });
      setBroadcastContent('');
      setTimeout(() => setBroadcastStatus(null), 5000);
    } catch (error) {
      console.error('Broadcast failed', error);
      setBroadcastStatus({ type: 'error', msg: 'Broadcast failed. Static interference detected.' });
    } finally {
      setIsBroadcasting(false);
    }
  };

  const handleUserSearch = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!searchQuery.trim()) return;

    setIsSearching(true);
    setFoundUser(null);
    setRoleUpdateStatus(null);
    try {
      const response = await api.get(`/users/search?query=${encodeURIComponent(searchQuery)}`);
      // Assuming search returns a flux/list, we take the first match
      const users = response.data as User[];
      if (users && users.length > 0) {
        setFoundUser(users[0]);
      } else {
        setRoleUpdateStatus({ type: 'error', msg: 'User not found in local frequency.' });
      }
    } catch (error) {
      console.error('Search failed', error);
      setRoleUpdateStatus({ type: 'error', msg: 'Search failed. Interference detected.' });
    } finally {
      setIsSearching(false);
    }
  };

  const toggleAdminRole = async () => {
    if (!foundUser) return;

    const isCurrentlyAdmin = foundUser.roles.includes('ROLE_ADMIN');
    const newRoles = isCurrentlyAdmin 
      ? foundUser.roles.filter(r => r !== 'ROLE_ADMIN')
      : [...foundUser.roles, 'ROLE_ADMIN'];
    
    // Ensure ROLE_USER is always present
    if (!newRoles.includes('ROLE_USER')) newRoles.push('ROLE_USER');

    try {
      const response = await api.put(`/users/${foundUser.id}/roles`, newRoles);
      setFoundUser(response.data as User);
      setRoleUpdateStatus({ 
        type: 'success', 
        msg: `Permissions ${isCurrentlyAdmin ? 'revoked' : 'granted'} successfully.` 
      });
      setTimeout(() => setRoleUpdateStatus(null), 3000);
    } catch (error) {
      console.error('Role update failed', error);
      setRoleUpdateStatus({ type: 'error', msg: 'Failed to modulate permissions.' });
    }
  };

  if (!isAdmin) {
    return (
      <div className="auth-container">
        <div className="glass-panel" style={{ textAlign: 'center' }}>
          <h2>Access Denied</h2>
          <p>This frequency is restricted to administrative entities.</p>
          <button className="lumina-button" onClick={() => window.location.href = '/'}>Return Home</button>
        </div>
      </div>
    );
  }

  return (
    <MainLayout
      activeReceiver="admin"
      prefix="🛡️"
      contextName="Admin Command Center"
    >
      <section className="admin-content">
        <div className="admin-grid">
          {/* Left Column: Management Tools */}
          <div className="admin-tools">
            <motion.div 
              initial={{ opacity: 0, x: -20 }}
              animate={{ opacity: 1, x: 0 }}
              className="glass-panel broadcast-section"
            >
              <h3>Global Broadcast</h3>
              <p className="helper-text">Send a high-priority message to the #general channel.</p>
              <form onSubmit={handleBroadcast}>
                <textarea
                  className="lumina-input"
                  placeholder="Enter broadcast content..."
                  value={broadcastContent}
                  onChange={(e) => setBroadcastContent(e.target.value)}
                  rows={4}
                  required
                />
                <div className="button-row">
                  <button 
                    type="submit" 
                    className="lumina-button primary" 
                    disabled={isBroadcasting || !broadcastContent.trim()}
                  >
                    {isBroadcasting ? 'Transmitting...' : 'Transmit Broadcast'}
                  </button>
                  {broadcastStatus && (
                    <span className={`status-msg ${broadcastStatus.type}`}>
                      {broadcastStatus.msg}
                    </span>
                  )}
                </div>
              </form>
            </motion.div>

            <motion.div 
              initial={{ opacity: 0, x: -20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: 0.2 }}
              className="glass-panel user-management-section"
              style={{ marginTop: '1.5rem' }}
            >
              <h3>User Management</h3>
              <p className="helper-text">Promote or revoke administrative permissions.</p>
              <form onSubmit={handleUserSearch} className="search-form">
                <div className="search-input-group">
                  <input
                    type="text"
                    className="lumina-input"
                    placeholder="Search by username..."
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                  />
                  <button type="submit" className="lumina-button secondary" disabled={isSearching}>
                    {isSearching ? '...' : 'Search'}
                  </button>
                </div>
              </form>

              {foundUser && (
                <motion.div 
                  initial={{ opacity: 0, scale: 0.95 }}
                  animate={{ opacity: 1, scale: 1 }}
                  className="user-result-card glass-card"
                >
                  <div className="user-info">
                    <span className="username">@{foundUser.username}</span>
                    <span className="display-name">{foundUser.displayName || 'No display name'}</span>
                    <div className="role-tags">
                      {foundUser.roles.map(r => (
                        <span key={r} className={`role-tag ${r.toLowerCase().replace('_', '-')}`}>
                          {r.replace('ROLE_', '')}
                        </span>
                      ))}
                    </div>
                  </div>
                  <button 
                    className={`lumina-button ${foundUser.roles.includes('ROLE_ADMIN') ? 'danger' : 'primary'} mini`}
                    onClick={toggleAdminRole}
                  >
                    {foundUser.roles.includes('ROLE_ADMIN') ? 'Revoke Admin' : 'Grant Admin'}
                  </button>
                </motion.div>
              )}
              
              {roleUpdateStatus && (
                <div className={`status-msg ${roleUpdateStatus.type}`} style={{ marginTop: '1rem' }}>
                  {roleUpdateStatus.msg}
                </div>
              )}
            </motion.div>
          </div>

          {/* Right Column: Feedback List */}
          <motion.div 
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            className="glass-panel feedback-section"
          >
            <div className="section-header">
              <h3>User Feedback</h3>
              <button className="lumina-button secondary mini" onClick={fetchFeedback} disabled={isLoadingFeedback}>
                {isLoadingFeedback ? 'Refreshing...' : 'Refresh'}
              </button>
            </div>
            
            <div className="feedback-list">
              {isLoadingFeedback ? (
                <div className="loading-placeholder">Scanning feedback frequencies...</div>
              ) : feedbackList.length === 0 ? (
                <div className="empty-placeholder">No feedback data recorded.</div>
              ) : (
                feedbackList.map((feedback) => (
                  <div key={feedback.id} className="feedback-item glass-card">
                    <div className="feedback-header">
                      <div className="rating">
                        {Array.from({ length: 5 }).map((_, i) => (
                          <span key={i} className={`star ${i < feedback.rating ? 'active' : ''}`}>★</span>
                        ))}
                      </div>
                      <span className="timestamp">{new Date(feedback.timestamp).toLocaleString()}</span>
                    </div>
                    <p className="comment">{feedback.comment}</p>
                    <div className="feedback-footer">
                      <span className="user-id">User: {feedback.userId || 'Anonymous'}</span>
                    </div>
                  </div>
                ))
              )}
            </div>
          </motion.div>
        </div>
      </section>
    </MainLayout>
  );
};

export default AdminPage;
