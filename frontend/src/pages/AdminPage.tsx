import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import api from '../api/axios';
import MainLayout from '../components/MainLayout';
import Avatar from '../components/Avatar';
import { Shield, Search, MessageSquare, Filter } from 'lucide-react';

interface User {
  id: string;
  username: string;
  displayName?: string;
  email?: string;
  enabled: boolean;
  roles: string[];
}

const AdminPage: React.FC = () => {
  const [users, setUsers] = useState<User[]>([]);
  const [query, setQuery] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [broadcastMsg, setBroadcastMsg] = useState('');
  const [status, setStatus] = useState<string | null>(null);

  const fetchUsers = async () => {
    try {
      const response = await api.get(`/users/search?query=${query}&page=${page}&size=10`);
      setUsers(response.data.content || []);
      setTotalPages(response.data.totalPages || 0);
    } catch (error) {
      console.error('Failed to fetch users', error);
    }
  };

  useEffect(() => {
    fetchUsers();
  }, [page]);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setPage(0);
    fetchUsers();
  };

  const handleBroadcast = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!broadcastMsg.trim()) return;
    try {
      await api.post('/messages/broadcast', { content: broadcastMsg });
      setStatus('Broadcast transmitted across all frequencies.');
      setBroadcastMsg('');
    } catch (error) {
      setStatus('Transmission failed. Frequency interference.');
    }
  };

  const toggleRole = async (userId: string, currentRoles: string[] = ['ROLE_USER']) => {
    const isCurrentlyAdmin = currentRoles.includes('ROLE_ADMIN');
    const newRoles = isCurrentlyAdmin ? ['ROLE_USER'] : ['ROLE_USER', 'ROLE_ADMIN'];
    try {
      await api.put(`/users/${userId}/roles`, newRoles);
      fetchUsers(); // Refresh
      setStatus(`Permissions updated for entity ${userId.substring(0,8)}.`);
    } catch (error) {
       setStatus('Permission override failed.');
    }
  };

  return (
    <MainLayout
      prefix="🛡️"
      contextName="Admin Command Center"
    >
      <section className="admin-content">
        <div className="admin-grid">
          
          <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="glass-panel admin-card">
            <h3><MessageSquare size={20} /> Global Broadcast</h3>
            <p className="helper-text">Send an overriding signal to the #general frequency for all entities.</p>
            <form onSubmit={handleBroadcast} className="broadcast-form">
              <textarea 
                value={broadcastMsg}
                onChange={(e) => setBroadcastMsg(e.target.value)}
                placeholder="Type global transmission..."
                className="lumina-input"
              />
              <button className="lumina-button">Initialize Broadcast</button>
            </form>
          </motion.div>

          <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="glass-panel admin-card">
            <h3><Search size={20} /> Entity Directory</h3>
            <form onSubmit={handleSearch} className="directory-search">
              <input 
                type="text" 
                placeholder="Scan by username..." 
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                className="lumina-input"
              />
              <button className="lumina-button secondary icon-only"><Filter size={18} /></button>
            </form>

            <div className="user-table">
              {users.map(u => (
                <div key={u.id} className="glass-card admin-user-row">
                  <Avatar seed={u.username} size="sm" />
                  <div className="user-main-info">
                    <div className="user-name">{u.displayName || u.username}</div>
                    <div className="user-id">ID: {u.id.substring(0,8)}...</div>
                  </div>
                  <button 
                    className="lumina-button secondary icon-only" 
                    title="Toggle Admin Role"
                    onClick={() => toggleRole(u.id)}
                  >
                    <Shield size={16} />
                  </button>
                </div>
              ))}
            </div>

            <div className="admin-pagination">
              <button className="lumina-button secondary" disabled={page === 0} onClick={() => setPage(p => p - 1)}>Prev</button>
              <span className="pagination-info">Cycle {page + 1} / {totalPages || 1}</span>
              <button className="lumina-button secondary" disabled={page >= totalPages - 1} onClick={() => setPage(p => p + 1)}>Next</button>
            </div>
          </motion.div>
        </div>

        <AnimatePresence>
          {status && (
            <motion.div 
              initial={{ y: 50, opacity: 0 }} 
              animate={{ y: 0, opacity: 1 }} 
              exit={{ y: 50, opacity: 0 }}
              className="glass-panel admin-status-toast"
            >
              {status}
              <button onClick={() => setStatus(null)} className="close-status-btn">×</button>
            </motion.div>
          )}
        </AnimatePresence>
      </section>
    </MainLayout>
  );
};

export default AdminPage;
