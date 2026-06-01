import React, { useState } from 'react';
import { motion } from 'motion/react';
import { type InjectedContent } from '../store/useChatStore';

interface ContentWidgetProps {
  content: InjectedContent;
}

const ContentWidget: React.FC<ContentWidgetProps> = ({ content }) => {
  const [isPlaying, setIsPlaying] = useState(false);

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
              className="border-none"
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
        <div className="button-group">
          <button className="btn btn-primary" onClick={() => setIsPlaying(!isPlaying)}>
            {isPlaying ? "Close Player" : "Watch Here"}
          </button>
          <a href={twitchUrl} target="_blank" rel="noopener noreferrer" className="btn btn-primary">
            Open Twitch
          </a>
        </div>
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
    
    // Check if we have a direct videoId provided
    const videoId = content.data.videoId;
    const fallbackUrl = content.data.url;
    if (!videoId) return (
      <motion.div
        initial={{ opacity: 0, scale: 0.9, y: 20 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
        className="glass-card youtube-widget"
      >
        <div className="widget-badge youtube">YOUTUBE CONTENT</div>
        <div className="youtube-header">
          <div className="video-info">
            <h4>{title}</h4>
            <p>Channel: {channel}</p>
          </div>
        </div>
        <div className="thumbnail-placeholder" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '1rem' }}>
          <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Video content unavailable</p>
        </div>
        {fallbackUrl && (
           <a href={fallbackUrl} target="_blank" rel="noopener noreferrer" className="lumina-button small full-width secondary center-link no-underline">
              Open YouTube
           </a>
        )}
      </motion.div>
    );

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
        <div
          className="youtube-preview"
          onClick={() => !isPlaying && setIsPlaying(true)}
          onKeyDown={(e) => { if ((e.key === 'Enter' || e.key === ' ') && !isPlaying) setIsPlaying(true); }}
          role="button"
          tabIndex={isPlaying ? -1 : 0}
          aria-label="Play video"
        >
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
        <div className="flex-gap-sm">
          <button className="btn btn-primary" onClick={() => setIsPlaying(!isPlaying)}>
            {isPlaying ? "Close Player" : "Watch Here"}
          </button>
          <a href={ytUrl} target="_blank" rel="noopener noreferrer" className="lumina-button small full-width secondary center-link no-underline">
            Open YouTube
          </a>
        </div>
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
        <a href={url} target="_blank" rel="noopener noreferrer" className="lumina-button small full-width secondary center-link no-underline">
          Read Full Article
        </a>
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
        <a href={url} target="_blank" rel="noopener noreferrer" className="lumina-button small full-width secondary center-link no-underline">
          View on {platform}
        </a>
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
        <a href={url} target="_blank" rel="noopener noreferrer" className="lumina-button small full-width secondary center-link no-underline">
          Join Discussion
        </a>
      </motion.div>
    );
  }

  return null;
};

export default ContentWidget;
