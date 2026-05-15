import React from 'react';
import { motion } from 'motion/react';
import { type InjectedContent } from '../store/useChatStore';

interface ContentWidgetProps {
  content: InjectedContent;
}

const ContentWidget: React.FC<ContentWidgetProps> = ({ content }) => {
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
          {thumbnail ? (
            <img src={thumbnail} alt="Stream Preview" onError={(e) => (e.currentTarget.style.display = 'none')} />
          ) : (
            <div className="thumbnail-placeholder" />
          )}
          <div className="viewer-count">
            <span className="live-dot"></span>
            {viewers} viewers
          </div>
        </div>
        <button className="lumina-button small full-width" disabled>Watch Together (Soon)</button>

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
        <div className="youtube-preview">
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
        </div>
        <div className="video-stats">
          <span>{views} views</span>
          <span>•</span>
          <span>{publishedAt}</span>
        </div>
        <button className="lumina-button small full-width secondary" disabled>Open in Player (Soon)</button>

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

  return null;
};

export default ContentWidget;
