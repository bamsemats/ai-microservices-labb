import React from 'react';

interface AvatarProps {
  seed: string;
  size?: 'sm' | 'md' | 'lg' | 'xl';
  className?: string;
}

const Avatar: React.FC<AvatarProps> = ({ seed, size = 'md', className = '' }) => {
  const sizeMap = {
    sm: '1.5rem',
    md: '2.5rem',
    lg: '4rem',
    xl: '6rem'
  };

  const pixelSize = {
    sm: 24,
    md: 40,
    lg: 64,
    xl: 96
  }[size];

  // Using DiceBear Identicon for a "techy/unique" look for bots and users
  const avatarUrl = `https://api.dicebear.com/7.x/identicon/svg?seed=${encodeURIComponent(seed)}&backgroundColor=transparent`;

  return (
    <div 
      className={`avatar-wrapper ${className}`}
      style={{
        width: sizeMap[size],
        height: sizeMap[size],
        borderRadius: '50%',
        overflow: 'hidden',
        background: 'linear-gradient(135deg, var(--color-accent-primary) 0%, var(--color-accent-secondary) 100%)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        flexShrink: 0,
        boxShadow: size === 'xl' ? 'var(--color-accent-glow)' : 'none'
      }}
    >
      <img 
        src={avatarUrl} 
        alt={`${seed}'s avatar`}
        style={{
          width: '100%',
          height: '100%',
          objectFit: 'cover'
        }}
        width={pixelSize}
        height={pixelSize}
      />
    </div>
  );
};

export default Avatar;
