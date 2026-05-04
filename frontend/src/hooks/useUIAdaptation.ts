import { useEffect, useRef } from 'react';
import { useUIStore } from '../store/useUIStore';
import { animate, type AnimationPlaybackControls } from 'motion/react';

const isValidColor = (color: string) => {
  if (typeof window === 'undefined') return false;
  const s = new Option().style;
  s.color = color;
  return s.color !== '';
};

export const useUIAdaptation = () => {
  const { currentTheme } = useUIStore();
  const controlsRef = useRef<AnimationPlaybackControls[]>([]);

  useEffect(() => {
    const root = document.documentElement;
    
    // Stop previous animations
    controlsRef.current.forEach(c => c.stop());
    controlsRef.current = [];

    // Animate Intensity
    const intensityAnim = animate(
      parseFloat(getComputedStyle(root).getPropertyValue('--accent-glow-intensity') || "0.5"),
      currentTheme.intensity,
      {
        duration: 0.8,
        ease: [0.4, 0, 0.2, 1], // Custom cubic-bezier
        onUpdate: (latest) => root.style.setProperty('--accent-glow-intensity', latest.toFixed(3))
      }
    );
    controlsRef.current.push(intensityAnim);

    // Animate Color if present
    if (currentTheme.color) {
      const sourceColor = getComputedStyle(root).getPropertyValue('--accent-primary').trim() || "#8b5cf6";
      const targetColor = currentTheme.color;

      if (isValidColor(sourceColor) && isValidColor(targetColor)) {
        const colorAnim = animate(
          sourceColor,
          targetColor,
          {
            duration: 1.2,
            ease: "easeInOut",
            onUpdate: (latest) => root.style.setProperty('--accent-primary', latest)
          }
        );
        controlsRef.current.push(colorAnim);
      }
    }
    
    // Theme-specific logic for blur and opacity
    let targetBlur = '12px';
    let targetOpacity = '0.05';

    switch (currentTheme.theme) {
      case 'emergency':
        targetBlur = '24px';
        targetOpacity = '0.15';
        break;
      case 'zen':
        targetBlur = '8px';
        targetOpacity = '0.02';
        break;
      case 'vibrant':
        targetBlur = '16px';
        targetOpacity = '0.08';
        break;
      case 'deep':
        targetBlur = '20px';
        targetOpacity = '0.12';
        break;
    }

    const blurVal = parseFloat(targetBlur);
    const currentBlur = parseFloat(getComputedStyle(root).getPropertyValue('--glass-blur-amount') || "12px");
    
    const blurAnim = animate(currentBlur, blurVal, {
      duration: 1.0,
      ease: "backOut",
      onUpdate: (latest) => root.style.setProperty('--glass-blur-amount', `${latest.toFixed(1)}px`)
    });
    controlsRef.current.push(blurAnim);

    const opacityVal = parseFloat(targetOpacity);
    const currentOpacity = parseFloat(getComputedStyle(root).getPropertyValue('--glass-opacity') || "0.05");

    const opacityAnim = animate(currentOpacity, opacityVal, {
      duration: 1.0,
      onUpdate: (latest) => root.style.setProperty('--glass-opacity', latest.toFixed(3))
    });
    controlsRef.current.push(opacityAnim);

    return () => {
      controlsRef.current.forEach(c => c.stop());
    };
  }, [currentTheme]);
};
