import React from 'react';
import { motion } from 'framer-motion';
import Sidebar from '../components/Sidebar';
import { useAuthStore } from '../store/useAuthStore';
import { useNavigate } from 'react-router-dom';

const TRENDING_TOPICS = [
  { id: 1, name: 'AdaptaLabs', activity: 'High', color: 'var(--accent-primary)' },
  { id: 2, name: 'AdaptaDesign', activity: 'Medium', color: 'var(--accent-secondary)' },
  { id: 3, name: 'QuantumChat', activity: 'Trending', color: 'var(--accent-tertiary)' },
  { id: 4, name: 'FluidSystems', activity: 'New', color: '#10b981' },
];

const FEATURED_CREATORS = [
  { id: 1, name: 'NexusPrime', role: 'Architect', avatar: 'N' },
  { id: 2, name: 'AdaptaAI', role: 'Assistant', avatar: 'A' },
  { id: 3, name: 'EchoFlow', role: 'Curator', avatar: 'E' },
  { id: 4, name: 'VibeCheck', role: 'Moderator', avatar: 'V' },
];

const DiscoveryPage: React.FC = () => {
  const { username, logout } = useAuthStore();
  const navigate = useNavigate();

  const handleSelectReceiver = (id: string) => {
    navigate(`/?receiver=${encodeURIComponent(id)}`);
  };

  return (
    <div className="chat-page-layout">
      <Sidebar activeReceiver="explore" onSelectReceiver={handleSelectReceiver} />

      <main className="chat-main-content">
        <header className="chat-navbar glass-panel">
          <div className="active-context">
            <span className="context-prefix">✨</span>
            <span className="context-name">Discovery Hub</span>
          </div>
          <div className="user-controls">
            <div className="user-badge glass-card">
              <span className="username">{username}</span>
              <div className="user-avatar">{username?.charAt(0).toUpperCase()}</div>
            </div>
            <button className="lumina-button secondary logout-btn" onClick={logout}>Logout</button>
          </div>
        </header>

        <section className="discovery-content">
          <div className="discovery-scroll-area">
            <motion.div 
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              className="discovery-section"
            >
              <h3>Trending Now <span className="badge">#AdaptaLabs</span></h3>
              <div className="horizontal-scroll">
                {TRENDING_TOPICS.map((topic) => (
                  <motion.div 
                    key={topic.id}
                    whileHover={{ scale: 1.05 }}
                    className="glass-card topic-card"
                    style={{ '--topic-color': topic.color } as React.CSSProperties}
                  >
                    <div className="topic-glow"></div>
                    <div className="topic-header">
                      <span className="topic-hash">#</span>
                      <h4>{topic.name}</h4>
                    </div>
                    <div className="topic-meta">
                      <span className="activity-indicator"></span>
                      {topic.activity} Activity
                    </div>
                    <button className="lumina-button small">Join Frequency</button>
                  </motion.div>
                ))}
              </div>
            </motion.div>

            <motion.div 
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.1 }}
              className="discovery-section"
            >
              <h3>Featured Creators</h3>
              <div className="horizontal-scroll">
                {FEATURED_CREATORS.map((creator) => (
                  <motion.div 
                    key={creator.id}
                    whileHover={{ scale: 1.05 }}
                    className="glass-card creator-card"
                  >
                    <div className="creator-avatar">{creator.avatar}</div>
                    <h4>{creator.name}</h4>
                    <p>{creator.role}</p>
                    <button className="lumina-button secondary small">Connect</button>
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
                <div className="glass-card activity-summary">
                  <h4>Global Pulse</h4>
                  <div className="pulse-viz">
                    <div className="pulse-bar"></div>
                    <div className="pulse-bar"></div>
                    <div className="pulse-bar"></div>
                    <div className="pulse-bar"></div>
                  </div>
                  <p>1,240 users currently synchronized.</p>
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
          min-width: 180px;
          text-align: center;
          display: flex;
          flex-direction: column;
          align-items: center;
          gap: 0.75rem;
        }

        .creator-avatar {
          width: 4rem;
          height: 4rem;
          background: var(--accent-gradient);
          border-radius: 50%;
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 1.5rem;
          font-weight: 800;
          color: white;
          box-shadow: var(--accent-glow);
        }

        .creator-card h4 {
          margin-bottom: 0;
        }

        .creator-card p {
          font-size: 0.8125rem;
          color: var(--text-muted);
          margin-bottom: 0.5rem;
        }

        .activity-summary {
          width: 100%;
          max-width: 400px;
        }

        .pulse-viz {
          display: flex;
          align-items: flex-end;
          gap: 4px;
          height: 40px;
          margin: 1rem 0;
        }

        .pulse-bar {
          flex: 1;
          background: var(--accent-primary);
          border-radius: 2px;
          animation: pulse 1.5s ease-in-out infinite;
        }

        @keyframes pulse {
          0%, 100% { height: 20%; opacity: 0.5; }
          50% { height: 80%; opacity: 1; }
        }

        .pulse-bar:nth-child(2) { animation-delay: 0.2s; }
        .pulse-bar:nth-child(3) { animation-delay: 0.4s; }
        .pulse-bar:nth-child(4) { animation-delay: 0.6s; }

        .chat-navbar {
          flex-shrink: 0;
        }
      `}</style>
    </div>
  );
};

export default DiscoveryPage;
