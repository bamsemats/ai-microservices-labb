import React, { useState, useEffect } from 'react';
import { motion } from 'motion/react';
import { useSearchParams } from 'react-router-dom';
import { useAuthStore } from '../store/useAuthStore';
import { useSocialStore } from '../store/useSocialStore';
import api from '../api/axios';
import MainLayout from '../components/MainLayout';
import Avatar from '../components/Avatar';
import { Search, UserPlus, TrendingUp } from 'lucide-react';

interface UserResult {
  id: string;
  username: string;
  displayName?: string;
  bio?: string;
}

const DiscoveryPage: React.FC = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const initialQuery = searchParams.get('query') || '';
  
  const [query, setQuery] = useState(initialQuery);
  const [results, setResults] = useState<UserResult[]>([]);
  const [loading, setLoading] = useState(false);
  const [connectingUserId, setConnectingUserId] = useState<string | null>(null);
  const { friends, pendingFriends, sendRequest, fetchPendingFriends } = useSocialStore();
  const { userId } = useAuthStore();

  const handleSearch = async (e?: React.FormEvent, searchQuery: string = query) => {
    if (e) e.preventDefault();
    if (!searchQuery.trim()) {
      setResults([]);
      return;
    }

    setLoading(true);
    try {
      const response = await api.get(`/users/search?query=${encodeURIComponent(searchQuery.trim())}`);
      // Backend returns a Page object
      setResults(response.data.content || []);
      // Update URL without triggering reload
      setSearchParams({ query: searchQuery.trim() }, { replace: true });
    } catch (error) {
      console.error('Search failed', error);
    } finally {
      setLoading(false);
    }
  };

  const handleConnect = async (targetId: string) => {
    setConnectingUserId(targetId);
    try {
      await sendRequest(targetId);
    } catch (error) {
      console.error('Connection failed', error);
    } finally {
      setConnectingUserId(null);
    }
  };

  useEffect(() => {
    fetchPendingFriends();
    if (initialQuery) {
      handleSearch(undefined, initialQuery);
    }
  }, []);

  const isFriend = (id: string) => friends.some(f => f.id === id);
  const isPending = (id: string) => pendingFriends.some(f => f.id === id);

  return (
    <MainLayout
      prefix="🌐"
      contextName="Discovery Hub"
    >
      <section className="discovery-content">
        <div className="discovery-scroll-area">
          <motion.div 
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className="discovery-section"
          >
            <h3><Search size={24} /> Find Entities</h3>
            <form className="search-box glass-panel" onSubmit={handleSearch}>
              <input 
                type="text" 
                placeholder="Search by username..." 
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                className="lumina-input"
                aria-label="Search by username"
              />
              <button className="lumina-button" disabled={loading}>
                {loading ? 'Searching...' : 'Scan'}
              </button>
            </form>

            <div className="search-results">
              {results.length > 0 ? (
                results.map(user => (
                  <motion.div 
                    key={user.id}
                    layout
                    initial={{ opacity: 0, scale: 0.9 }}
                    animate={{ opacity: 1, scale: 1 }}
                    className="glass-panel creator-card"
                  >
                    <Avatar seed={user.username} size="lg" />
                    <div className="card-info">
                      <h4>{user.displayName || user.username}</h4>
                      <p className="creator-mini-bio">{user.bio || 'No biometrics provided.'}</p>
                    </div>
                    {user.id !== userId && (
                      <button 
                        className={`lumina-button ${isFriend(user.id) || isPending(user.id) ? 'secondary' : ''}`}
                        onClick={() => handleConnect(user.id)}
                        disabled={isFriend(user.id) || isPending(user.id) || connectingUserId === user.id}
                      >
                        {isFriend(user.id) 
                          ? 'Connected' 
                          : connectingUserId === user.id
                            ? 'Connecting...'
                            : isPending(user.id) 
                              ? 'Pending' 
                              : <><UserPlus size={16} className="btn-icon" /> Connect</>
                        }
                      </button>
                    )}
                  </motion.div>
                ))
              ) : query && !loading && (
                <p className="no-results-msg">No entities found matching that frequency.</p>
              )}
            </div>
          </motion.div>

          <div className="discovery-section">
            <h3><TrendingUp size={24} /> Trending Frequencies</h3>
            <div className="horizontal-scroll">
              {/* TODO: Replace hard-coded topics with dynamic data from content-aggregator-service or message-service metrics */}
              {[
                { name: 'general', activity: 'High', color: '#6366f1' },
                { name: 'tech-stack', activity: 'Med', color: '#ec4899' },
                { name: 'ai-lounge', activity: 'High', color: '#10b981' }
              ].map(topic => (
                <div key={topic.name} className="glass-panel topic-card">
                   <div className="topic-glow" style={{ '--topic-color': topic.color } as never}></div>
                   <span className="topic-hash">#</span>
                   <h4>{topic.name}</h4>
                   <div className="card-footer">
                     <span className="activity-indicator"></span>
                     <span className="activity-text">{topic.activity} Activity</span>
                   </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </section>
    </MainLayout>
  );
};

export default DiscoveryPage;
