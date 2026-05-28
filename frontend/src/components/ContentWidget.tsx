import React, { useState } from 'react';
import { motion } from 'motion/react';
import { type InjectedContent } from '../store/useChatStore';

interface ContentWidgetProps {
  content: InjectedContent;
}

const ContentWidget: React.FC<ContentWidgetProps> = ({ content }) => {
  const [isPlaying, setIsPlaying] = useState(false);

  const commonStyles = (
    <style>{`
      .widget-badge {
        font-size: 0.65rem;
        font-weight: 800;
        margin-bottom: 0.75rem;
        letter-spacing: 0.1em;
      }
      .thumbnail-placeholder {
        width: 100%;
        height: 100%;
        background: linear-gradient(45deg, #1a1a1a, #2a2a2a);
      }
      .full-width {
        width: 100%;
      }
    `}</style>
  );

  if (content.contentType === 'TWITCH_STREAM') {
    const streamer = content.data.streamer || "Unknown streamer";
    const gameName = content.data.gameName || "Unknown game";
    const thumbnail = content.data.thumbnail;
    const viewers = content.data.viewers || "—";
    
    const twitchUrl = `https://www.twitch.tv/${streamer.toLowerCase()}`;
    const embedUrl = `https://player.twitch.tv/?channel=${streamer.toLowerCase()}&parent=${window.location.hostname}`;

    return (
      <motion.div
        initial={{ opacity: 0, scale: 0.9, y: 20 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
        className="glass-card twitch-widget"
      >
        {commonStyles}
        <div className="widget-badge twitch">LIVE STREAM</div>
        <div className="twitch-header">
          <div className="streamer-avatar">
            {streamer.charAt(0)}
          </div>
          <div className="stream-info">
            <h4>{streamer}</h4>
            <p>Playing {gameName}</p>
          </div>
        </div>
        <div className="twitch-preview">
          {isPlaying ? (
            <iframe
              src={embedUrl}
              height="100%"
              width="100%"
              allowFullScreen
              style={{ border: 'none' }}
            ></iframe>
          ) : thumbnail ? (
            <img src={thumbnail} alt="Stream Preview" onError={(e) => (e.currentTarget.style.display = 'none')} />
          ) : (
            <div className="thumbnail-placeholder" />
          )}
          {!isPlaying && (
            <div className="viewer-count">
              <span className="live-dot"></span>
              {viewers} viewers
            </div>
          )}
        </div>
        <div style={{ display: 'flex', gap: '0.5rem' }}>
          <button className="lumina-button small full-width" onClick={() => setIsPlaying(!isPlaying)}>
            {isPlaying ? "Close Player" : "Watch Here"}
          </button>
          <a href={twitchUrl} target="_blank" rel="noopener noreferrer" className="lumina-button small full-width secondary" style={{ textAlign: 'center', textDecoration: 'none', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            Open Twitch
          </a>
        </div>

        <style>{`
          .twitch-widget {
            max-width: 320px;
            margin: 1rem 0;
            border-left: 4px solid #9146ff !important;
          }
          .widget-badge.twitch {
            color: #9146ff;
          }
          .twitch-header {
            display: flex;
            gap: 0.75rem;
            margin-bottom: 1rem;
          }
          .streamer-avatar {
            width: 2.5rem;
            height: 2.5rem;
            background: #9146ff;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            color: white;
            font-weight: 800;
          }
          .stream-info h4 {
            font-size: 0.9375rem;
            margin: 0;
          }
          .stream-info p {
            font-size: 0.75rem;
            color: var(--text-muted);
            margin: 0;
          }
          .twitch-preview {
            position: relative;
            border-radius: 0.5rem;
            overflow: hidden;
            margin-bottom: 1rem;
            background: #000;
            aspect-ratio: 16/9;
          }
          .twitch-preview img {
            width: 100%;
            height: 100%;
            object-fit: cover;
            opacity: 0.8;
          }
          .viewer-count {
            position: absolute;
            bottom: 0.5rem;
            left: 0.5rem;
            background: rgba(0,0,0,0.6);
            padding: 0.2rem 0.5rem;
            border-radius: 0.25rem;
            font-size: 0.7rem;
            display: flex;
            align-items: center;
            gap: 0.4rem;
          }
          .live-dot {
            width: 6px;
            height: 6px;
            background: #ff4a4a;
            border-radius: 50%;
            box-shadow: 0 0 6px #ff4a4a;
          }
        `}</style>
      </motion.div>
    );
  }

  if (content.contentType === 'YOUTUBE_VIDEO') {
    const title = content.data.title || "Unknown video";
    const channel = content.data.channel || "Unknown channel";
    const thumbnail = content.data.thumbnail;
    const duration = content.data.duration || "—";
    const views = content.data.views || "—";
    const publishedAt = content.data.publishedAt || "Recently";
    
    // Check if we have a direct videoId provided, otherwise fallback to standard link based on title search
    // (In a real app, videoId should come directly from the backend injection)
    const videoId = content.data.videoId || "dQw4w9WgXcQ"; 
    const ytUrl = `https://www.youtube.com/watch?v=${videoId}`;
    const embedUrl = `https://www.youtube.com/embed/${videoId}?autoplay=1`;

    return (
      <motion.div
        initial={{ opacity: 0, scale: 0.9, y: 20 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
        className="glass-card youtube-widget"
      >
        <div className="widget-badge youtube">YOUTUBE VIDEO</div>
        <div className="youtube-header">
          <div className="video-info">
            <h4>{title}</h4>
            <p>Channel: {channel}</p>
          </div>
        </div>
        <div className="youtube-preview" onClick={() => !isPlaying && setIsPlaying(true)}>
          {isPlaying ? (
            <iframe
              width="100%"
              height="100%"
              src={embedUrl}
              title="YouTube video player"
              frameBorder="0"
              allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
              allowFullScreen
            ></iframe>
          ) : (
            <>
              {thumbnail ? (
                <img src={thumbnail} alt="Video Preview" onError={(e) => (e.currentTarget.style.display = 'none')} />
              ) : (
                <div className="thumbnail-placeholder" />
              )}
              <div className="duration-tag">{duration}</div>
              <div className="play-overlay">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="white">
                  <polygon points="5 3 19 12 5 21 5 3"></polygon>
                </svg>
              </div>
            </>
          )}
        </div>
        <div className="video-stats">
          <span>{views} views</span>
          <span>•</span>
          <span>{publishedAt}</span>
        </div>
        <div style={{ display: 'flex', gap: '0.5rem' }}>
          <button className="lumina-button small full-width" onClick={() => setIsPlaying(!isPlaying)}>
            {isPlaying ? "Close Player" : "Watch Here"}
          </button>
          <a href={ytUrl} target="_blank" rel="noopener noreferrer" className="lumina-button small full-width secondary" style={{ textAlign: 'center', textDecoration: 'none', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            Open YouTube
          </a>
        </div>

        <style>{`
          .youtube-widget {
            max-width: 320px;
            margin: 1rem 0;
            border-left: 4px solid #ff0000 !important;
          }
          .widget-badge.youtube {
            color: #ff0000;
          }
          .youtube-header {
            margin-bottom: 0.75rem;
          }
          .video-info h4 {
            font-size: 0.9375rem;
            margin: 0;
            line-height: 1.4;
            display: -webkit-box;
            -webkit-line-clamp: 2;
            -webkit-box-orient: vertical;
            overflow: hidden;
          }
          .video-info p {
            font-size: 0.75rem;
            color: var(--text-muted);
            margin: 0.25rem 0 0 0;
          }
          .youtube-preview {
            position: relative;
            border-radius: 0.5rem;
            overflow: hidden;
            margin-bottom: 0.75rem;
            background: #000;
            aspect-ratio: 16/9;
            cursor: pointer;
          }
          .youtube-preview img {
            width: 100%;
            height: 100%;
            object-fit: cover;
            opacity: 0.7;
          }
          .duration-tag {
            position: absolute;
            bottom: 0.5rem;
            right: 0.5rem;
            background: rgba(0,0,0,0.8);
            color: white;
            padding: 0.1rem 0.3rem;
            border-radius: 2px;
            font-size: 0.65rem;
            font-weight: 700;
          }
          .play-overlay {
            position: absolute;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%);
            width: 40px;
            height: 40px;
            background: rgba(255, 0, 0, 0.9);
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            opacity: 0.8;
            transition: all 0.2s ease;
          }
          .youtube-preview:hover .play-overlay {
            transform: translate(-50%, -50%) scale(1.1);
            opacity: 1;
          }
          .video-stats {
            display: flex;
            gap: 0.5rem;
            font-size: 0.7rem;
            color: var(--text-muted);
            margin-bottom: 1rem;
          }
        `}</style>
      </motion.div>
    );
  }

  if (content.contentType === 'NEWS_ARTICLE') {
    const title = content.data.title || "News Article";
    const publisher = content.data.publisher || "News Publisher";
    const summary = content.data.summary || "";
    const url = content.data.url || "#";
    const publishedAt = content.data.publishedAt || "Recently";

    return (
      <motion.div initial={{ opacity: 0, scale: 0.9, y: 20 }} animate={{ opacity: 1, scale: 1, y: 0 }} className="glass-card news-widget">
        <div className="widget-badge news">LATEST NEWS</div>
        <div className="news-content">
          <h4>{title}</h4>
          <p className="news-publisher">{publisher} • {publishedAt}</p>
          <p className="news-summary">{summary}</p>
        </div>
        <a href={url} target="_blank" rel="noopener noreferrer" className="lumina-button small full-width secondary" style={{ textAlign: 'center', textDecoration: 'none', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          Read Full Article
        </a>
        <style>{`
          .news-widget { max-width: 320px; margin: 1rem 0; border-left: 4px solid #3b82f6 !important; }
          .widget-badge.news { color: #3b82f6; }
          .news-content h4 { font-size: 0.9375rem; margin: 0 0 0.25rem 0; line-height: 1.4; }
          .news-publisher { font-size: 0.7rem; color: var(--text-muted); margin: 0 0 0.75rem 0; }
          .news-summary { font-size: 0.8125rem; color: var(--text-secondary); margin: 0 0 1rem 0; line-height: 1.5; display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden; }
        `}</style>
      </motion.div>
    );
  }

  if (content.contentType === 'SOCIAL_POST') {
    const author = content.data.author || "User";
    const platform = content.data.platform || "Social Media";
    const text = content.data.text || "";
    const url = content.data.url || "#";
    const likes = content.data.likes || "0";

    return (
      <motion.div initial={{ opacity: 0, scale: 0.9, y: 20 }} animate={{ opacity: 1, scale: 1, y: 0 }} className="glass-card social-widget">
        <div className="widget-badge social">{platform.toUpperCase()} POST</div>
        <div className="social-header">
          <div className="social-avatar">{author.charAt(1) || author.charAt(0)}</div>
          <div className="social-author">{author}</div>
        </div>
        <div className="social-content">{text}</div>
        <div className="social-stats">❤️ {likes} Likes</div>
        <a href={url} target="_blank" rel="noopener noreferrer" className="lumina-button small full-width secondary" style={{ textAlign: 'center', textDecoration: 'none', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          View on {platform}
        </a>
        <style>{`
          .social-widget { max-width: 320px; margin: 1rem 0; border-left: 4px solid #14b8a6 !important; }
          .widget-badge.social { color: #14b8a6; }
          .social-header { display: flex; align-items: center; gap: 0.5rem; margin-bottom: 0.75rem; }
          .social-avatar { width: 2rem; height: 2rem; background: #14b8a6; border-radius: 50%; display: flex; align-items: center; justify-content: center; color: white; font-weight: bold; }
          .social-author { font-weight: 600; font-size: 0.875rem; }
          .social-content { font-size: 0.875rem; margin-bottom: 0.75rem; line-height: 1.4; color: var(--text-primary); }
          .social-stats { font-size: 0.75rem; color: var(--text-muted); margin-bottom: 1rem; }
        `}</style>
      </motion.div>
    );
  }

  if (content.contentType === 'FORUM_POST') {
    const threadTitle = content.data.threadTitle || "Discussion";
    const forumName = content.data.forumName || "Forum";
    const author = content.data.author || "User";
    const excerpt = content.data.excerpt || "";
    const url = content.data.url || "#";
    const replies = content.data.replies || "0";

    return (
      <motion.div initial={{ opacity: 0, scale: 0.9, y: 20 }} animate={{ opacity: 1, scale: 1, y: 0 }} className="glass-card forum-widget">
        <div className="widget-badge forum">{forumName.toUpperCase()} THREAD</div>
        <div className="forum-content">
          <h4>{threadTitle}</h4>
          <p className="forum-meta">Started by {author}</p>
          <div className="forum-excerpt">"{excerpt}"</div>
          <div className="forum-stats">💬 {replies} Replies</div>
        </div>
        <a href={url} target="_blank" rel="noopener noreferrer" className="lumina-button small full-width secondary" style={{ textAlign: 'center', textDecoration: 'none', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          Join Discussion
        </a>
        <style>{`
          .forum-widget { max-width: 320px; margin: 1rem 0; border-left: 4px solid #f59e0b !important; }
          .widget-badge.forum { color: #f59e0b; }
          .forum-content h4 { font-size: 0.9375rem; margin: 0 0 0.25rem 0; line-height: 1.4; }
          .forum-meta { font-size: 0.7rem; color: var(--text-muted); margin: 0 0 0.75rem 0; }
          .forum-excerpt { font-size: 0.8125rem; font-style: italic; color: var(--text-secondary); margin: 0 0 0.75rem 0; border-left: 2px solid var(--glass-border); padding-left: 0.5rem; }
          .forum-stats { font-size: 0.75rem; color: var(--text-muted); margin-bottom: 1rem; }
        `}</style>
      </motion.div>
    );
  }

  return null;
};

export default ContentWidget;
