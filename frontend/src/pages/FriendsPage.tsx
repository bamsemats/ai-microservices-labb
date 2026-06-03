import React, { useEffect, useState } from 'react';
import MainLayout from '../components/MainLayout';
import { useSocialStore, type Friend } from '../store/useSocialStore';
import { usePresenceStore } from '../store/usePresenceStore';
import { useFrequencyStore } from '../store/useFrequencyStore';
import { useChatStore } from '../store/useChatStore';
import { useNavigate } from 'react-router-dom';
import Avatar from '../components/Avatar';
import { UserMinus, MessageSquare, Search, Users, Check } from 'lucide-react';

const FriendsPage: React.FC = () => {
  const navigate = useNavigate();
  const { friends, pendingFriends, fetchFriends, fetchPendingFriends, removeFriend, acceptRequest } = useSocialStore();
  const { presences } = usePresenceStore();
  const { createFrequency } = useFrequencyStore();
  const { setActiveChannelId } = useChatStore();
  
  const [searchQuery, setSearchQuery] = useState('');

  useEffect(() => {
    fetchFriends();
    fetchPendingFriends();
  }, [fetchFriends, fetchPendingFriends]);

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

  const handleAcceptRequest = async (friendId: string) => {
    try {
      await acceptRequest(friendId);
    } catch (error) {
      console.error('Failed to accept request', error);
    }
  };

  return (
    <MainLayout contextName="Social Hub" activeReceiver="friends">
      <div className="main-layout-content">
        <div className="site-page page--friends">
          <div className="page-scroll">
            <div className="page-content">
              <header className="page-header">
                <h1 className="page-title">Social Hub</h1>
                <p className="page-subtitle">Manage your entities and direct synchronizations.</p>
              </header>

              <section className="glass-panel">
                <form className="input-group" onSubmit={handleSearch}>
                  <Search size={18} />
                  <input
                    type="text"
                    className="input"
                    placeholder="Search for entities to sync..."
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    aria-label="Search entities to sync"
                  />
                  <button className="btn btn-primary" type="submit">
                    Scan
                  </button>
                </form>
                {searchQuery && (
                  <div className="search-hint">
                    Press Enter to deep-scan the network for "{searchQuery}"
                  </div>
                )}
              </section>

              {pendingFriends.length > 0 && (
                <section className="friends-list-section">
                  <div className="page-header">
                    <h2 className="page-title">
                      Pending Requests ({pendingFriends.length})
                    </h2>
                  </div>
                  <div className="friends-grid">
                    {pendingFriends.map((friend) => (
                      <div key={friend.id} className="card card--interactive friend-card">
                        <div className="friend-card-top">
                          <div className="avatar">
                            <Avatar seed={friend.username} size="lg" isBot={friend.isBot} />
                          </div>
                          <div className="friend-info">
                            <span className="friend-name">{friend.username}</span>
                            <span className="friend-handle">{friend.displayName || 'Entity'}</span>
                            <span className="friend-status-text offline">wants to connect</span>
                          </div>
                        </div>
                        <div className="friend-card-actions">
                          <button
                            className="btn btn-primary full-width"
                            title="Accept Connection"
                            onClick={() => handleAcceptRequest(friend.id)}
                          >
                            <Check size={18} />
                            <span>Accept</span>
                          </button>
                        </div>
                      </div>
                    ))}
                  </div>
                </section>
              )}

              <section className="friends-list-section">
                <div className="page-header">
                  <h2 className="page-title">Synchronized Entities ({friends.length})</h2>
                </div>

                {friends.length === 0 ? (
                  <div className="card message-empty">
                    <Users size={48} />
                    <h3>No entities found</h3>
                    <p>Scan the network in Discovery to find and synchronize with others.</p>
                    <button className="btn btn-primary" onClick={() => navigate('/explore')}>
                      Go to Discovery
                    </button>
                  </div>
                ) : (
                  <div className="friends-grid">
                    {friends.map((friend) => {
                      const presence = presences[friend.id];
                      const status = presence?.status || 'OFFLINE';

                      return (
                        <div key={friend.id} className="card card--interactive friend-card">
                          <div className="friend-card-top">
                            <div className="avatar">
                              <Avatar seed={friend.username} size="lg" isBot={friend.isBot} />
                              <span className={`status-badge ${status.toLowerCase()}`}></span>
                            </div>
                            <div className="friend-info">
                              <span className="friend-name">{friend.username}</span>
                              <span className="friend-handle">{friend.displayName || 'Entity'}</span>
                              <span className={`friend-status-text ${status.toLowerCase()}`}>{status}</span>
                            </div>
                          </div>

                          <div className="friend-card-actions">
                            <button
                              className="btn btn-ghost"
                              title="Start Private Frequency"
                              onClick={() => handleStartFrequency(friend)}
                            >
                              <MessageSquare size={18} />
                              <span>Sync</span>
                            </button>
                            <button
                              className="btn btn-danger"
                              title="Remove Connection"
                              onClick={() => handleRemoveFriend(friend.id)}
                              aria-label="Remove friend"
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
          </div>
        </div>
      </div>
    </MainLayout>
  );
};

export default FriendsPage;
