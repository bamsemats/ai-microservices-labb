import React, { useEffect, useId, useRef } from 'react';
import styles from './BrandLogo.module.css';

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
  const instanceId = useId().replace(/:/g, '');
  const svgRef = useRef<SVGSVGElement>(null);
  
  const ids = {
    gradient: `logo-gradient-${instanceId}`,
    glow: `glow-${instanceId}`,
    pulseR: `pulse-r-${instanceId}`,
    pulseOpacity: `pulse-opacity-${instanceId}`
  };

  const sizes = {
    sm: { icon: 24, font: '1rem' },
    md: { icon: 32, font: '1.25rem' },
    lg: { icon: 48, font: '1.75rem' },
    xl: { icon: 64, font: '2.5rem' }
  };

  const { icon, font } = sizes[size];

  useEffect(() => {
    const mediaQuery = window.matchMedia('(prefers-reduced-motion: reduce)');
    
    const handleMotionPreference = () => {
      if (!svgRef.current) return;
      
      const animateR = svgRef.current.querySelector(`#${ids.pulseR}`) as unknown as SVGAnimateElement;
      const animateOpacity = svgRef.current.querySelector(`#${ids.pulseOpacity}`) as unknown as SVGAnimateElement;
      
      if (mediaQuery.matches) {
        try {
          animateR?.endElement();
          animateOpacity?.endElement();
        } catch {
          // Fallback
        }
      } else {
        try {
          animateR?.beginElement();
          animateOpacity?.beginElement();
        } catch {
          // Fallback
        }
      }
    };

    handleMotionPreference();
    mediaQuery.addEventListener('change', handleMotionPreference);
    
    return () => {
      mediaQuery.removeEventListener('change', handleMotionPreference);
    };
  }, [ids.pulseR, ids.pulseOpacity]);

  return (
    <div className={`brand-logo-container ${styles.container} ${className}`}>
      <svg 
        ref={svgRef}
        width={icon} 
        height={icon} 
        viewBox="0 0 32 32" 
        fill="none" 
        xmlns="http://www.w3.org/2000/svg"
        className="brand-logo-svg"
      >
        <defs>
          <linearGradient id={ids.gradient} x1="0" y1="0" x2="32" y2="32" gradientUnits="userSpaceOnUse">
            <stop stopColor="var(--color-accent-primary)" />
            <stop offset="1" stopColor="var(--color-accent-secondary)" />
          </linearGradient>
          <filter id={ids.glow} x="-20%" y="-20%" width="140%" height="140%">
            <feGaussianBlur stdDeviation="2" result="blur" />
            <feComposite in="SourceGraphic" in2="blur" operator="over" />
          </filter>
        </defs>
        
        {/* Geometric Hexagon Base */}
        <path 
          d="M16 2L28 9V23L16 30L4 23V9L16 2Z" 
          fill="rgb(from white r g b / 0.05)" 
          stroke={`url(#${ids.gradient})`} 
          strokeWidth="1.5"
        />
        
        {/* Inner stylized 'A' / Network Motif */}
        <path 
          d="M10 22L16 8L22 22M13 18H19" 
          stroke="white" 
          strokeWidth="2.5" 
          strokeLinecap="round" 
          strokeLinejoin="round"
          style={{ filter: `url(#${ids.glow})` }}
        />
        
        {/* Pulse Dot */}
        <circle cx="16" cy="8" r="2" fill="var(--color-accent-tertiary)">
          <animate 
            id={ids.pulseR}
            attributeName="r" 
            values="1.5;2.5;1.5" 
            dur="2s" 
            repeatCount="indefinite" 
            begin="indefinite"
          />
          <animate 
            id={ids.pulseOpacity}
            attributeName="opacity" 
            values="1;0.5;1" 
            dur="2s" 
            repeatCount="indefinite" 
            begin="indefinite"
          />
        </circle>
      </svg>
      
      {showText && (
        <span className="brand-logo-text" style={{ fontSize: font }}>
          AdaptaChat
        </span>
      )}
    </div>
  );

};

export default BrandLogo;
