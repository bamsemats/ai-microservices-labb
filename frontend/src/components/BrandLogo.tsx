import React from 'react';

interface BrandLogoProps {
  className?: string;
  size?: 'sm' | 'md' | 'lg' | 'xl';
  showText?: boolean;
}

const BrandLogo: React.FC<BrandLogoProps> = ({ 
  className = '', 
  size = 'md', 
  showText = true 
}) => {
  const sizes = {
    sm: { icon: 24, font: '1rem' },
    md: { icon: 32, font: '1.25rem' },
    lg: { icon: 48, font: '1.75rem' },
    xl: { icon: 64, font: '2.5rem' }
  };

  const { icon, font } = sizes[size];

  return (
    <div className={`brand-logo-container ${className}`} style={{ 
      display: 'flex', 
      alignItems: 'center', 
      gap: '0.75rem',
      userSelect: 'none'
    }}>
      <svg 
        width={icon} 
        height={icon} 
        viewBox="0 0 32 32" 
        fill="none" 
        xmlns="http://www.w3.org/2000/svg"
        style={{ filter: 'drop-shadow(0 0 8px var(--accent-primary))' }}
      >
        <defs>
          <linearGradient id="logo-gradient" x1="0" y1="0" x2="32" y2="32" gradientUnits="userSpaceOnUse">
            <stop stopColor="var(--accent-primary)" />
            <stop offset="1" stopColor="var(--accent-secondary)" />
          </linearGradient>
          <filter id="glow" x="-20%" y="-20%" width="140%" height="140%">
            <feGaussianBlur stdDeviation="2" result="blur" />
            <feComposite in="SourceGraphic" in2="blur" operator="over" />
          </filter>
        </defs>
        
        {/* Geometric Hexagon Base */}
        <path 
          d="M16 2L28 9V23L16 30L4 23V9L16 2Z" 
          fill="rgba(255, 255, 255, 0.05)" 
          stroke="url(#logo-gradient)" 
          strokeWidth="1.5"
        />
        
        {/* Inner stylized 'A' / Network Motif */}
        <path 
          d="M10 22L16 8L22 22M13 18H19" 
          stroke="white" 
          strokeWidth="2.5" 
          strokeLinecap="round" 
          strokeLinejoin="round"
          style={{ filter: 'url(#glow)' }}
        />
        
        {/* Pulse Dot */}
        <circle cx="16" cy="8" r="2" fill="var(--accent-tertiary)">
          <animate 
            attributeName="r" 
            values="1.5;2.5;1.5" 
            dur="2s" 
            repeatCount="indefinite" 
          />
          <animate 
            attributeName="opacity" 
            values="1;0.5;1" 
            dur="2s" 
            repeatCount="indefinite" 
          />
        </circle>
      </svg>
      
      {showText && (
        <span style={{ 
          fontSize: font, 
          fontWeight: 800, 
          letterSpacing: '-0.03em',
          background: 'var(--accent-gradient)',
          WebkitBackgroundClip: 'text',
          WebkitTextFillColor: 'transparent',
          fontFamily: 'Plus Jakarta Sans, Inter, sans-serif'
        }}>
          AdaptaChat
        </span>
      )}

      <style>{`
        .brand-logo-container {
          transition: transform 0.3s ease;
        }
        .brand-logo-container:hover {
          transform: scale(1.02);
        }
      `}</style>
    </div>
  );
};

export default BrandLogo;
