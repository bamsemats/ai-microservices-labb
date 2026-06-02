import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import api from '../api/axios';
import MainLayout from '../components/MainLayout';
import Avatar from '../components/Avatar';
import { Shield, Search, MessageSquare, Filter, Edit2, X, Save, Lock } from 'lucide-react';

interface User {
  id: string;
  username: string;
  displayName?: string;
  email?: string;
  enabled: boolean;
  roles: string[];
  bio?: string;
}

interface EditProfileModalProps {
  user: User;
  onClose: () => void;
  onSave: (updatedData: Partial<User> & { newPassword?: string }) => Promise<void>;
}

const EditProfileModal: React.FC<EditProfileModalProps> = ({ user, onClose, onSave }) => {
  const [displayName, setDisplayName] = useState(user.displayName || '');
  const [bio, setBio] = useState(user.bio || '');
  const [enabled, setEnabled] = useState(user.enabled);
  const [newPassword, setNewPassword] = useState('');
  const [saving, setSaving] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    try {
      await onSave({
        displayName,
        bio,
        enabled,
        newPassword: newPassword.trim() || undefined
      });
      onClose();
    } catch (err) {
      console.error('Save failed', err);
    } finally {
      setSaving(false);
    }
  };

  return (
    <motion.div 
      initial={{ opacity: 0 }} 
      animate={{ opacity: 1 }} 
      exit={{ opacity: 0 }}
      className="modal-overlay"
    >
      <motion.div 
        initial={{ scale: 0.9, y: 20 }}
        animate={{ scale: 1, y: 0 }}
        className="glass-panel modal admin-edit-modal"
      >
        <div className="modal-header">
          <h3>Edit Entity: {user.username}</h3>
          <button type="button" onClick={onClose} className="btn btn-icon"><X size={18} /></button>
        </div>
        
        <form onSubmit={handleSubmit} className="modal-form">
          <div className="form-field">
            <label className="form-label">Display Name</label>
            <input 
              type="text" 
              value={displayName} 
              onChange={(e) => setDisplayName(e.target.value)} 
              className="input"
            />
          </div>
          
          <div className="form-field">
            <label className="form-label">Bio</label>
            <textarea 
              value={bio} 
              onChange={(e) => setBio(e.target.value)} 
              className="input mini"
            />
          </div>

          <div className="form-field checkbox-group">
            <label className="checkbox-label label form-label">
              <input 
                type="checkbox" 
                checked={enabled} 
                onChange={(e) => setEnabled(e.target.checked)} 
              />
              <span>Account Enabled</span>
            </label>
          </div>

          <div className="divider"></div>

          <div className="form-field">
            <label className="form-label"><Lock size={14} className="inline-icon" /> Reset Password (Optional)</label>
            <input 
              type="password" 
              value={newPassword} 
              onChange={(e) => setNewPassword(e.target.value)} 
              placeholder="Leave blank to keep current"
              className="input"
            />
          </div>

          <div className="modal-actions">
            <button type="button" onClick={onClose} className="btn btn-ghost">Cancel</button>
            <button type="submit" className="btn btn-primary" disabled={saving}>
              <Save size={16} className="btn-icon" /> {saving ? 'Saving...' : 'Apply Overrides'}
            </button>
          </div>
        </form>
      </motion.div>
    </motion.div>
  );
};

const AdminPage: React.FC = () => {
  const [users, setUsers] = useState<User[]>([]);
  const [query, setQuery] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [broadcastMsg, setBroadcastMsg] = useState('');
  const [status, setStatus] = useState<string | null>(null);
  const [editingUser, setEditingUser] = useState<User | null>(null);

  const fetchUsers = React.useCallback(async () => {
    try {
      const response = await api.get('/users/search', {
        params: { query, page, size: 10 }
      });
      setUsers(response.data.content || []);
      setTotalPages(response.data.totalPages || 0);
    } catch {
      console.error('Failed to fetch users');
    }
  }, [query, page]);

  // Initial fetch and updates on page/query change
  useEffect(() => {
    const triggerFetch = async () => {
      await fetchUsers();
    };
    triggerFetch();
  }, [page, query, fetchUsers]);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setPage(0);
    // fetchUsers will trigger via the useEffect dependency on page/query
  };

  const handleBroadcast = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!broadcastMsg.trim()) return;
    try {
      await api.post('/messages/broadcast', { content: broadcastMsg });
      setStatus('Broadcast transmitted across all frequencies.');
      setBroadcastMsg('');
    } catch {
      setStatus('Transmission failed. Frequency interference.');
    }
  };

  const toggleRole = async (userId: string, currentRoles: string[]) => {
    const isCurrentlyAdmin = currentRoles.includes('ROLE_ADMIN');
    const newRoles = isCurrentlyAdmin ? ['ROLE_USER'] : ['ROLE_USER', 'ROLE_ADMIN'];
    try {
      await api.put(`/users/${userId}/roles`, newRoles);
      fetchUsers(); // Refresh
      setStatus(`Permissions updated for entity ${userId.substring(0,8)}.`);
    } catch {
       setStatus('Permission override failed.');
    }
  };

  const handleAdminOverride = async (updatedData: Partial<User> & { newPassword?: string }) => {
    if (!editingUser) return;
    try {
      await api.patch(`/users/${editingUser.id}/admin-override`, updatedData);
      fetchUsers();
      setStatus(`Entity ${editingUser.username} synchronized with new parameters.`);
    } catch {
      setStatus('Override protocol failed.');
      throw new Error('Override failed');
    }
  };

  return (
    <MainLayout
      prefix="🛡️"
      contextName="Admin Control"
      activeReceiver="admin"
    >
        <div className="site-page page--admin">
          <div className="page-scroll">
            <div className="page-content">
              <header className="page-header">
                <h1 className="page-title">Admin Control</h1>
                <p className="page-subtitle">System-wide governance and entity management.</p>
              </header>

              <div className="admin-grid">

              <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="glass-panel card">
                <h3><MessageSquare size={20} /> Global Broadcast</h3>
                <p className="page-subtitle">Send an overriding signal to the #general frequency for all entities.</p>
                <form onSubmit={handleBroadcast} className="form-field">
                  <textarea
                    value={broadcastMsg}
                    onChange={(e) => setBroadcastMsg(e.target.value)}
                    placeholder="Type global transmission..."
                    className="input"
                    rows={5}
                  />
                  <button className="btn btn-primary">Initialize Broadcast</button>
                </form>
              </motion.div>

              <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="glass-panel card">
                <h3><Search size={20} /> Entity Directory</h3>
                <p className="page-subtitle">List of all known entities.</p>
                <form onSubmit={handleSearch} className="discovery-search">
                  <input
                    type="text"
                    placeholder="Scan by username..."
                    value={query}
                    onChange={(e) => setQuery(e.target.value)}
                    className="input"
                  />
                  <button type="button" aria-label="Filter" className="btn btn-icon"><Filter size={18} /></button>
                </form>

                <div className="data-table">
                  {users.map(u => (
                    <div key={u.id} className="card">
                      <Avatar seed={u.username} size="sm" />
                      <div className="user-main-info">
                        <div className="user-name">{u.displayName || u.username}</div>
                        <div className="user-id">ID: {u.id.substring(0,8)}...</div>
                      </div>
                      <div className="row-actions">
                        <button
                          type="button"
                          aria-label="Edit Profile"
                          className="btn btn-icon"
                          title="Edit Profile"
                          onClick={() => setEditingUser(u)}
                        >
                          <Edit2 size={16} />
                        </button>
                        <button
                          type="button"
                          aria-label="Toggle Admin Role"
                          className="btn btn-icon"
                          title="Toggle Admin Role"
                          onClick={() => toggleRole(u.id, u.roles)}
                        >
                          <Shield size={16} />
                        </button>
                      </div>
                    </div>
                  ))}
                </div>

                <div className="admin-pagination">
                  <button type="button" className="btn btn-ghost" disabled={page === 0} onClick={() => setPage(p => p - 1)}>Prev</button>
                  <span className="pagination-info">Cycle {page + 1} / {totalPages || 1}</span>
                  <button type="button" className="btn btn-ghost" disabled={page >= totalPages - 1} onClick={() => setPage(p => p + 1)}>Next</button>
                </div>
              </motion.div>
            </div>

            <AnimatePresence>
              {editingUser && (
                <EditProfileModal
                  user={editingUser}
                  onClose={() => setEditingUser(null)}
                  onSave={handleAdminOverride}
                />
              )}
            </AnimatePresence>

            <AnimatePresence>
              {status && (
                <motion.div
                  initial={{ y: 50, opacity: 0 }}
                  animate={{ y: 0, opacity: 1 }}
                  exit={{ y: 50, opacity: 0 }}
                  className="toast"
                >
                  {status}
                  <button type="button" aria-label="Close status" onClick={() => setStatus(null)} className="close-status-btn">×</button>
                </motion.div>
              )}
            </AnimatePresence>
           </div>
          </div>
        </div>
    </MainLayout>
  );
};

export default AdminPage;
