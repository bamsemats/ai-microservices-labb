import React, { useEffect, useState } from 'react';
import MainLayout from '../components/MainLayout';
import { useSocialStore, type Friend } from '../store/useSocialStore';
import { usePresenceStore } from '../store/usePresenceStore';
import { useFrequencyStore } from '../store/useFrequencyStore';
import { useChatStore } from '../store/useChatStore';
import { useNavigate } from 'react-router-dom';
import Avatar from '../components/Avatar';
import { UserMinus, MessageSquare, Search, Users } from 'lucide-react';

const FriendsPage: React.FC = () => {
  const navigate = useNavigate();
  const { friends, fetchFriends, removeFriend } = useSocialStore();
  const { presences } = usePresenceStore();
  const { createFrequency } = useFrequencyStore();
  const { setActiveChannelId } = useChatStore();
  
  const [searchQuery, setSearchQuery] = useState('');

  useEffect(() => {
    fetchFriends();
  }, [fetchFriends]);

  const handleSearch = (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    if (!searchQuery.trim()) return;
    navigate(`/explore?query=${encodeURIComponent(searchQuery.trim())}`);
  };

  const handleStartFrequency = async (friend: Friend) => {
    try {
      // Start a direct frequency with the friend
      const freqName = `Private: ${friend.username}`;
      const freq = await createFrequency(freqName, `Direct connection with ${friend.username}`);
      setActiveChannelId(freq.id);
      navigate({ pathname: '/', search: `?receiver=${freq.id}` });
    } catch (error) {
      console.error('Failed to start frequency', error);
    }
  };

  const handleRemoveFriend = async (friendId: string) => {
    if (window.confirm('Are you sure you want to remove this friend?')) {
      await removeFriend(friendId);
    }
  };

  return (
    <MainLayout contextName="Social Hub" activeReceiver="friends">
      <div className="friends-page content-container">
        <header className="page-header">
          <h1>Social Hub</h1>
          <p className="subtitle">Manage your entities and direct synchronizations.</p>
        </header>

        <section className="search-section glass-panel">
          <form className="input-with-icon" onSubmit={handleSearch}>
            <Search size={18} />
            <input 
              type="text" 
              className="lumina-input" 
              placeholder="Search for entities to sync..." 
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
            <button className="lumina-button primary mini-btn" type="submit">
              Scan
            </button>
          </form>
          {searchQuery && (
            <div className="search-hint">
              Press Enter to deep-scan the network for "{searchQuery}"
            </div>
          )}
        </section>

        <section className="friends-list-section">
          <div className="section-header">
            <h2>Synchronized Entities ({friends.length})</h2>
          </div>

          {friends.length === 0 ? (
            <div className="empty-state glass-panel">
              <Users size={48} />
              <h3>No entities found</h3>
              <p>Scan the network in Discovery to find and synchronize with others.</p>
              <button className="lumina-button primary" onClick={() => navigate('/explore')}>
                Go to Discovery
              </button>
            </div>
          ) : (
            <div className="friends-grid">
              {friends.map((friend) => {
                const presence = presences[friend.id];
                const status = presence?.status || 'OFFLINE';
                
                return (
                  <div key={friend.id} className="friend-card glass-panel animate-in">
                    <div className="friend-card-info">
                      <div className="avatar-wrapper">
                        <Avatar seed={friend.username} size="lg" />
                        <span className={`status-indicator-large ${status.toLowerCase()}`}></span>
                      </div>
                      <div className="friend-details">
                        <span className="friend-username">{friend.username}</span>
                        <span className="friend-display-name">{friend.displayName || 'Entity'}</span>
                        <span className={`status-text ${status.toLowerCase()}`}>{status}</span>
                      </div>
                    </div>
                    
                    <div className="friend-card-actions">
                      <button 
                        className="lumina-button secondary" 
                        title="Start Private Frequency"
                        onClick={() => handleStartFrequency(friend)}
                      >
                        <MessageSquare size={18} />
                        <span>Sync</span>
                      </button>
                      <button 
                        className="lumina-button danger ghost" 
                        title="Remove Connection"
                        onClick={() => handleRemoveFriend(friend.id)}
                      >
                        <UserMinus size={18} />
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </section>
      </div>
    </MainLayout>
  );
};

export default FriendsPage;
