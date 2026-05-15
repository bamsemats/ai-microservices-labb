import React, { useEffect, useState } from 'react';
import { motion } from 'motion/react';
import Sidebar from '../components/Sidebar';
import { useAuthStore } from '../store/useAuthStore';
import { useNavigate } from 'react-router-dom';
import Navbar from '../components/Navbar';

interface TrendingChannel {
  channelId: string;
  score: number;
}

const FEATURED_CREATORS = [
  { id: 1, name: 'NexusPrime', role: 'Architect', avatar: 'N', bio: 'Building the core foundations of AdaptaChat.' },
  { id: 2, name: 'AdaptaAI', role: 'Assistant', avatar: 'A', bio: 'The heartbeat of our intelligent frequencies.' },
  { id: 3, name: 'EchoFlow', role: 'Curator', avatar: 'E', bio: 'Filtering the noise, amplifying the signal.' },
  { id: 4, name: 'VibeCheck', role: 'Moderator', avatar: 'V', bio: 'Ensuring harmony across all channels.' },
  { id: 5, name: 'Lumina', role: 'Designer', avatar: 'L', bio: 'Crafting the visual soul of the network.' },
];

const DiscoveryPage: React.FC = () => {
  const { token } = useAuthStore();
  const navigate = useNavigate();
  const [trendingChannels, setTrendingChannels] = useState<TrendingChannel[]>([]);

  useEffect(() => {
    const fetchTrending = async () => {
      try {
        const response = await fetch('/api/analytics/trending-channels?limit=5', {
          headers: {
            'Authorization': `Bearer ${token}`
          }
        });
        if (response.ok) {
          const data = await response.json();
          setTrendingChannels(data);
        }
      } catch (error) {
        console.error('Failed to fetch trending channels', error);
      }
    };
    if (token) {
      fetchTrending();
      const interval = setInterval(fetchTrending, 10000);
      return () => clearInterval(interval);
    }
  }, [token]);

  const handleSelectReceiver = (id: string) => {
    navigate(`/?receiver=${encodeURIComponent(id)}`);
  };

  const getColor = (index: number) => {
    const colors = ['var(--color-accent-primary)', 'var(--color-accent-secondary)', 'var(--color-accent-tertiary)', 'var(--color-success)', 'var(--color-warning)'];
    return colors[index % colors.length];
  };

  return (
    <div className="chat-page-layout">
      <Sidebar activeReceiver="explore" onSelectReceiver={handleSelectReceiver} />

      <main className="chat-main-content">
        <Navbar prefix="✨" contextName="Discovery Hub" />

        <section className="discovery-content">
          <div className="discovery-scroll-area">
            <motion.div 
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              className="discovery-section"
            >
              <h3>Trending Now <span className="badge">#AdaptaLabs</span></h3>
              <div className="horizontal-scroll">
                {trendingChannels.length === 0 ? (
                  <p className="text-muted">No trending channels yet. Start chatting!</p>
                ) : (
                  trendingChannels.map((topic, index) => (
                    <motion.div 
                      key={topic.channelId}
                      whileHover={{ scale: 1.05 }}
                      className="glass-card topic-card"
                      style={{ '--topic-color': getColor(index) } as React.CSSProperties}
                    >
                      <div className="topic-glow"></div>
                      <div className="topic-header">
                        <span className="topic-hash">#</span>
                        <h4>{topic.channelId}</h4>
                      </div>
                      <div className="topic-meta">
                        <span className="activity-indicator"></span>
                        {topic.score} msgs
                      </div>
                      <button 
                        className="lumina-button small"
                        onClick={() => handleSelectReceiver(topic.channelId)}
                      >
                        Join Frequency
                      </button>
                    </motion.div>
                  ))
                )}
              </div>
            </motion.div>

            <motion.div 
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.1 }}
              className="discovery-section"
            >
              <h3>Featured Creators</h3>
              <div className="horizontal-scroll creators-list">
                {FEATURED_CREATORS.map((creator) => (
                  <motion.div 
                    key={creator.id}
                    whileHover={{ y: -10, scale: 1.02 }}
                    className="glass-card creator-card"
                  >
                    <div className="creator-avatar">{creator.avatar}</div>
                    <h4>{creator.name}</h4>
                    <p className="creator-role">{creator.role}</p>
                    <p className="creator-mini-bio">{creator.bio}</p>
                    <button 
                      className="lumina-button secondary small full-width"
                      onClick={() => handleSelectReceiver(creator.name)}
                    >
                      Connect
                    </button>
                  </motion.div>
                ))}
              </div>
            </motion.div>

            <motion.div 
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.2 }}
              className="discovery-section"
            >
              <h3>Active Frequencies</h3>
              <div className="grid-layout">
                <div className="glass-card activity-summary pulse-card">
                  <div className="pulse-header">
                    <h4>Global Pulse</h4>
                    <span className="live-indicator">LIVE</span>
                  </div>
                  <div className="pulse-viz">
                    {[...Array(12)].map((_, i) => (
                      <div key={i} className="pulse-bar" style={{ animationDelay: `${i * 0.1}s` }}></div>
                    ))}
                  </div>
                  <div className="pulse-meta">
                    <p><strong>1,240</strong> users currently synchronized.</p>
                    <p className="text-muted small">Peak activity detected in #general</p>
                  </div>
                </div>

                <div className="glass-card info-card">
                  <h4>Network Health</h4>
                  <div className="health-stat">
                    <span>Latency</span>
                    <span className="stat-val success">12ms</span>
                  </div>
                  <div className="health-stat">
                    <span>Uptime</span>
                    <span className="stat-val success">99.99%</span>
                  </div>
                  <div className="health-stat">
                    <span>Nodes</span>
                    <span className="stat-val">Active</span>
                  </div>
                </div>
              </div>
            </motion.div>
          </div>
        </section>
      </main>
    </div>
  );
};

export default DiscoveryPage;
