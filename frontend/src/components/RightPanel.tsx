import React, { useState } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { useSearchParams } from 'react-router-dom';
import { useUIStore } from '../store/useUIStore';
import { useChatStore } from '../store/useChatStore';
import { useFrequencyStore } from '../store/useFrequencyStore';
import { useSocialStore } from '../store/useSocialStore';
import { useAuthStore } from '../store/useAuthStore';
import { usePresenceStore } from '../store/usePresenceStore';
import ContentWidget from './ContentWidget';
import Avatar from './Avatar';
import { Users, UserPlus, UserMinus, Edit2, Check, X } from 'lucide-react';

const RightPanel: React.FC = () => {
  const { injectionPanelOpen } = useUIStore();
  const { injectedContent } = useChatStore();
  const { frequencies, renameFrequency, kickMember, inviteMember } = useFrequencyStore();
  const { friends } = useSocialStore();
  const { presences } = usePresenceStore();
  const { userId } = useAuthStore();
  
  const [searchParams] = useSearchParams();
  const receiverId = searchParams.get('receiver');
  const currentFreq = frequencies.find(f => f.id === receiverId);

  const [isDesktop, setIsDesktop] = React.useState(typeof window !== 'undefined' ? window.innerWidth > 768 : true);
  
  const [isEditingName, setIsEditingName] = useState(false);
  const [internalEditName, setInternalEditName] = useState<string | null>(null);
  const [showInviteMenu, setShowInviteMenu] = useState(false);

  React.useEffect(() => {
    const handleResize = () => setIsDesktop(window.innerWidth > 768);
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  // Sync state when active frequency changes
  const activeFreqId = currentFreq?.id;
  React.useEffect(() => {
    Promise.resolve().then(() => {
      setIsEditingName(false);
      setShowInviteMenu(false);
      setInternalEditName(null);
    });
  }, [activeFreqId]);

  const editNameValue = (internalEditName ?? currentFreq?.name) || '';

  // Desktop only view
  if (!isDesktop) return null;

  const handleRename = async () => {
    if (currentFreq && editNameValue.trim() && editNameValue.trim() !== currentFreq.name) {
      await renameFrequency(currentFreq.id, editNameValue.trim());
    }
    setIsEditingName(false);
    setInternalEditName(null);
  };

  const isOwner = currentFreq?.ownerId === userId;
  
  // Find friends that are not already in the frequency
  const invitableFriends = friends.filter(f => !currentFreq?.members.includes(f.id));

  return (
    <AnimatePresence>
      {injectionPanelOpen && (
        <motion.aside
          initial={{ width: 0, opacity: 0 }}
          animate={{ width: '350px', opacity: 1 }}
          exit={{ width: 0, opacity: 0 }}
          transition={{ type: 'spring', stiffness: 300, damping: 30 }}
          className="injection-panel glass-panel"
        >
          {currentFreq ? (
            <div className="panel-content frequency-management">
              <div className="frequency-header">
                {isEditingName ? (
                  <div className="edit-name-form">
                    <input 
                      autoFocus
                      className="btn btn-ghost"
                      value={editNameValue}
                      onChange={(e) => setInternalEditName(e.target.value)}
                      onKeyDown={(e) => e.key === 'Enter' && handleRename()}
                    />
                    <button className="btn btn-icon" onClick={handleRename}><Check size={16} /></button>
                    <button className="btn btn-icon" onClick={() => setIsEditingName(false)}><X size={16} /></button>
                  </div>
                ) : (
                  <>
                    <h3>{currentFreq.name}</h3>
                    {isOwner && (
                      <button className="icon-btn" onClick={() => setIsEditingName(true)}>
                        <Edit2 size={16} />
                      </button>
                    )}
                  </>
                )}
              </div>

              <div className="frequency-members-section">
                <div className="section-title-row">
                  <h4><Users size={16} /> Members ({currentFreq.members.length})</h4>
                  {isOwner && (
                    <button 
                      className="btn btn-icon"
                      onClick={() => setShowInviteMenu(!showInviteMenu)}
                      title="Invite Friend"
                    >
                      <UserPlus size={16} />
                    </button>
                  )}
                </div>

                {showInviteMenu && isOwner && (
                  <div className="invite-menu glass-panel">
                    <h5>Invite Entities</h5>
                    {invitableFriends.length > 0 ? (
                      <ul className="invite-list">
                        {invitableFriends.map(friend => (
                          <li key={friend.id}>
                            <span>{friend.username}</span>
                            <button 
                              className="lumina-button primary mini-btn"
                              onClick={() => {
                                inviteMember(currentFreq.id, friend.id);
                                setShowInviteMenu(false);
                              }}
                            >
                              Add
                            </button>
                          </li>
                        ))}
                      </ul>
                    ) : (
                      <p className="helper-text">All friends are already in this frequency.</p>
                    )}
                  </div>
                )}

                <ul className="member-list">
                  {currentFreq.members.map(memberId => {
                    const presence = presences[memberId];
                    const displayName = presence?.username || memberId;
                    const friend = friends.find(f => f.id === memberId);
                    
                    return (
                      <li key={memberId} className="member-item">
                        <div className="member-info">
                          <Avatar seed={memberId} size="sm" isBot={friend?.isBot} />
                          <span className="member-name">
                            {displayName} {memberId === currentFreq.ownerId ? '(Owner)' : ''}
                          </span>
                        </div>
                        {isOwner && memberId !== userId && (
                          <button 
                            className="btn btn-icon"
                            onClick={() => kickMember(currentFreq.id, memberId)}
                            title="Kick Member"
                          >
                            <UserMinus size={16} />
                          </button>
                        )}
                      </li>
                    );
                  })}
                </ul>
              </div>
            </div>
          ) : (
            <>
              <div className="panel-header">
                <h3>Signal Injections</h3>
              </div>
              <div className="panel-content">
                {injectedContent.length > 0 ? (
                  injectedContent.map((content, idx) => (
                    <ContentWidget key={`${content.type}-${idx}`} content={content} />
                  ))
                ) : (
                  <div className="empty-panel">
                    <span className="empty-panel-icon">📡</span>
                    <p className="empty-panel-text">No active signal detected in current conversation.</p>
                  </div>
                )}
              </div>
            </>
          )}
        </motion.aside>
      )}
    </AnimatePresence>
  );
};

export default RightPanel;
