import React from 'react';

interface AvatarProps {
  seed: string;
  size?: 'sm' | 'md' | 'lg' | 'xl';
  className?: string;
  isBot?: boolean;
}

const Avatar: React.FC<AvatarProps> = ({ seed, size = 'md', className = '', isBot = false }) => {
  const pixelSize = {
    sm: 24,
    md: 40,
    lg: 64,
    xl: 96
  }[size];

  // Using a stable hash of the seed to avoid leaking PII (like usernames) to external services
  const avatarSeed = React.useMemo(() => {
    let hash = 5381;
    for (let i = 0; i < seed.length; i++) {
      hash = (hash * 33) ^ seed.charCodeAt(i);
    }
    return (hash >>> 0).toString(16);
  }, [seed]);

  // Using DiceBear Identicon for users and Bottts for bots
  const style = isBot ? 'bottts' : 'identicon';
  const avatarUrl = `https://api.dicebear.com/7.x/${style}/svg?seed=${encodeURIComponent(avatarSeed)}&backgroundColor=transparent`;

  return (
    <div className={`avatar avatar-${size} ${className}`}>
      <img 
        src={avatarUrl} 
        alt="User avatar"
        className="avatar-image"
        width={pixelSize}
        height={pixelSize}
      />
    </div>
  );
};

export default Avatar;
