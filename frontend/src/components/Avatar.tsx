import React from 'react';

interface AvatarProps {
  seed: string;
  size?: 'sm' | 'md' | 'lg' | 'xl';
  className?: string;
}

const Avatar: React.FC<AvatarProps> = ({ seed, size = 'md', className = '' }) => {
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

  // Using DiceBear Identicon for a "techy/unique" look for bots and users
  const avatarUrl = `https://api.dicebear.com/7.x/identicon/svg?seed=${encodeURIComponent(avatarSeed)}&backgroundColor=transparent`;

  return (
    <div className={`avatar-wrapper avatar-${size} ${className}`}>
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
