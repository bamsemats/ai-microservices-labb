import React, { useState } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { Search, Filter, Calendar, User, Hash, Smile } from 'lucide-react';
import MainLayout from '../components/MainLayout';
import MessageBubble from '../components/MessageBubble';
import api from '../api/axios';
import { useAuthStore } from '../store/useAuthStore';
import { useFrequencyStore } from '../store/useFrequencyStore';
import { useSocialStore } from '../store/useSocialStore';
import { type Message } from '../store/useChatStore';

const SearchPage: React.FC = () => {
  const { userId } = useAuthStore();
  const [q, setQ] = useState('');
  const [results, setResults] = useState<Message[]>([]);
  const [loading, setLoading] = useState(false);
  const [showFilters, setShowFilters] = useState(false);
  
  // Filters
  const [channelId, setChannelId] = useState('');
  const [senderId, setSenderId] = useState('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [sentimentTheme, setSentimentTheme] = useState('');
  const [minIntensity, setMinIntensity] = useState(0);

  const { frequencies } = useFrequencyStore();
  const { friends } = useSocialStore();

  const handleSearch = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!q.trim() || q.length < 2) return;

    setLoading(true);
    try {
      const params: Record<string, string | number | boolean | undefined> = { q };
      if (channelId) params.channelId = channelId;
      if (senderId) params.senderId = senderId;
      
      if (startDate) {
        const sd = new Date(startDate);
        if (!isNaN(sd.getTime())) params.startDate = sd.toISOString();
      }
      if (endDate) {
        const ed = new Date(endDate);
        if (!isNaN(ed.getTime())) params.endDate = ed.toISOString();
      }
      
      if (sentimentTheme) params.sentimentTheme = sentimentTheme;
      if (minIntensity > 0) params.minIntensity = minIntensity / 100;

      const response = await api.get<Message[]>('/messages/search', { params });
      setResults(response.data);
    } catch (error) {
      console.error('Search failed', error);
    } finally {
      setLoading(false);
    }
  };

  const themes = ['emergency', 'vibrant', 'zen', 'deep', 'neutral'];

  return (
    <MainLayout prefix="🔍" contextName="Global Search">
      <div className="site-page page--discovery">
        <div className="page-scroll">
          <section className="discovery-search-bar glass-panel">
            <form onSubmit={handleSearch} className="search-main-form">
              <div className="input-group">
                <Search className="input-icon" size={20} />
                <input
                    type="text"
                    value={q}
                    onChange={(e) => setQ(e.target.value)}
                    placeholder="Search across all frequencies..."
                    className="input"
                />
                <button
                    type="button"
                    className={`btn btn-icon ${showFilters ? 'is-active' : ''}`}
                    onClick={() => setShowFilters(!showFilters)}
                    title="Advanced Filters"
                >
                  <Filter size={20} />
                </button>
                <button type="submit" className="btn btn-primary" disabled={loading || q.length < 2}>
                  {loading ? 'Scanning...' : 'Search'}
                </button>
              </div>

              <AnimatePresence>
                {showFilters && (
                    <motion.div
                        initial={{ height: 0, opacity: 0 }}
                        animate={{ height: 'auto', opacity: 1 }}
                        exit={{ height: 0, opacity: 0 }}
                        className="search-filters-grid"
                    >
                      <div className="form-field">
                        <label className="form-label"><Hash size={14} /> Frequency</label>
                        <select value={channelId} onChange={(e) => setChannelId(e.target.value)} className="input">
                          <option value="">All Frequencies</option>
                          <option value="general">general</option>
                          {frequencies.filter(f => f.id !== 'general').map(f => <option key={f.id} value={f.id}>{f.name}</option>)}
                        </select>
                      </div>

                      <div className="form-field">
                        <label className="form-label"><User size={14} /> Sender</label>
                        <select value={senderId} onChange={(e) => setSenderId(e.target.value)} className="input">
                          <option value="">Any Entity</option>
                          {friends.map(f => <option key={f.id} value={f.id}>{f.username}</option>)}
                        </select>
                      </div>

                      <div className="form-field">
                        <label className="form-label"><Calendar size={14} /> From Date</label>
                        <input type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)} className="input" />
                      </div>

                      <div className="form-field">
                        <label className="form-label"><Calendar size={14} /> To Date</label>
                        <input type="date" value={endDate} onChange={(e) => setEndDate(e.target.value)} className="input" />
                      </div>

                      <div className="form-field">
                        <label className="form-label"><Smile size={14} /> Sentiment Theme</label>
                        <select value={sentimentTheme} onChange={(e) => setSentimentTheme(e.target.value)} className="input">
                          <option value="">Any Mood</option>
                          {themes.map(t => <option key={t} value={t}>{t}</option>)}
                        </select>
                      </div>

                      <div className="form-field">
                        <label className="form-label">Min Intensity: {minIntensity}%</label>
                        <input
                            type="range"
                            min="0" max="100"
                            value={minIntensity}
                            onChange={(e) => setMinIntensity(parseInt(e.target.value))}
                            className="lumina-range"
                        />
                      </div>
                    </motion.div>
                )}
              </AnimatePresence>
            </form>
          </section>
          <div className="page-content">
            <header className="page-header">
              <h1 className="page-title">Historical Recovery</h1>
              <p className="page-subtitle">Scan the network history for encrypted signals and sentiment patterns.</p>
            </header>

            <section className="discovery-results">
              {loading ? (
                <div className="message-empty">
                  <div className="pulse-loader"></div>
                  <p>Scanning encrypted history...</p>
                </div>
              ) : results.length > 0 ? (
                <div className="search-results-list">
                  <div className="badge badge-muted">{results.length} entities recovered from history</div>
                  {results.map((msg, idx) => (
                    <div key={msg.id || idx} className="search-result-item">
                      <div className="result-meta">
                        <span className="result-channel">#{msg.channelId}</span>
                        <span className="result-date">{new Date(msg.timestamp).toLocaleString()}</span>
                      </div>
                      <MessageBubble message={msg} isOwn={msg.senderId === userId} />
                    </div>
                  ))}
                </div>
              ) : q.trim() ? (
                <div className="message-empty">
                  <p>No signal matches your query.</p>
                </div>
              ) : (
                <div className="message-empty">
                  <Search size={48} className="message-empty-icon" />
                  <p>Enter a query to begin historical recovery.</p>
                </div>
              )}
            </section>
          </div>
        </div>
      </div>
    </MainLayout>
  );
};

export default SearchPage;
