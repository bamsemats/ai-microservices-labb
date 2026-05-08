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
  const { username, token, logout } = useAuthStore();
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
    const colors = ['var(--accent-primary)', 'var(--accent-secondary)', 'var(--accent-tertiary)', '#10b981', '#f59e0b'];
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

      <style>{`
        .discovery-content {
          flex: 1;
          overflow: hidden;
          padding: 1rem 2rem 2rem;
        }

        .discovery-scroll-area {
          height: 100%;
          overflow-y: auto;
          display: flex;
          flex-direction: column;
          gap: 3rem;
          padding-bottom: 2rem;
        }

        .discovery-section h3 {
          font-size: 1.5rem;
          font-weight: 800;
          margin-bottom: 1.5rem;
          display: flex;
          align-items: center;
          gap: 1rem;
        }

        .badge {
          font-size: 0.8rem;
          background: var(--accent-gradient);
          padding: 0.2rem 0.6rem;
          border-radius: 2rem;
          color: white;
        }

        .horizontal-scroll {
          display: flex;
          gap: 1.5rem;
          padding-bottom: 1rem;
          overflow-x: auto;
          scrollbar-width: none;
        }

        .horizontal-scroll::-webkit-scrollbar {
          display: none;
        }

        .topic-card {
          min-width: 240px;
          position: relative;
          overflow: hidden;
          display: flex;
          flex-direction: column;
          gap: 1rem;
        }

        .topic-glow {
          position: absolute;
          top: -20%;
          right: -20%;
          width: 60%;
          height: 60%;
          background: var(--topic-color);
          filter: blur(40px);
          opacity: 0.2;
          z-index: 0;
        }

        .topic-header {
          display: flex;
          align-items: center;
          gap: 0.5rem;
          z-index: 1;
        }

        .topic-hash {
          color: var(--topic-color);
          font-weight: 900;
          font-size: 1.25rem;
        }

        .topic-meta {
          display: flex;
          align-items: center;
          gap: 0.5rem;
          font-size: 0.875rem;
          color: var(--text-secondary);
          z-index: 1;
        }

        .activity-indicator {
          width: 6px;
          height: 6px;
          border-radius: 50%;
          background: #10b981;
          box-shadow: 0 0 8px #10b981;
        }

        .creator-card {
          min-width: 220px;
          text-align: center;
          display: flex;
          flex-direction: column;
          align-items: center;
          gap: 0.75rem;
          padding: 2rem !important;
        }

        .creator-avatar {
          width: 5rem;
          height: 5rem;
          background: var(--accent-gradient);
          border-radius: 50%;
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 2rem;
          font-weight: 800;
          color: white;
          box-shadow: var(--accent-glow);
          margin-bottom: 0.5rem;
        }

        .creator-role {
          font-size: 0.75rem;
          font-weight: 800;
          color: var(--accent-tertiary);
          text-transform: uppercase;
          letter-spacing: 0.1em;
        }

        .creator-mini-bio {
          font-size: 0.8125rem;
          color: var(--text-muted);
          line-height: 1.4;
          margin-bottom: 1rem;
        }

        .full-width {
          width: 100%;
        }

        .grid-layout {
          display: grid;
          grid-template-columns: 2fr 1fr;
          gap: 1.5rem;
        }

        @media (max-width: 1024px) {
          .grid-layout {
            grid-template-columns: 1fr;
          }
        }

        @media (max-width: 480px) {
          .discovery-content {
            padding: 1rem;
          }
          .discovery-section h3 {
            font-size: 1.25rem;
          }
        }

        .pulse-card {
          padding: 2rem !important;
        }

        .pulse-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
        }

        .live-indicator {
          font-size: 0.7rem;
          font-weight: 900;
          color: white;
          background: var(--error);
          padding: 0.2rem 0.5rem;
          border-radius: 4px;
          animation: blink 2s infinite;
        }

        @keyframes blink {
          0%, 100% { opacity: 1; }
          50% { opacity: 0.5; }
        }

        .pulse-viz {
          display: flex;
          align-items: flex-end;
          gap: 6px;
          height: 60px;
          margin: 2rem 0;
        }

        .pulse-bar {
          flex: 1;
          background: var(--accent-primary);
          border-radius: 4px;
          animation: pulse 1.5s ease-in-out infinite;
        }

        .health-stat {
          display: flex;
          justify-content: space-between;
          padding: 0.75rem 0;
          border-bottom: 1px solid var(--glass-border);
          font-size: 0.875rem;
        }

        .health-stat:last-child {
          border-bottom: none;
        }

        .stat-val {
          font-weight: 700;
        }

        .stat-val.success {
          color: var(--success);
        }
      `}</style>
    </div>
  );
};

export default DiscoveryPage;
