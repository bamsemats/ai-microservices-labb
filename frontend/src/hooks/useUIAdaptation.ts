import { useEffect, useRef } from 'react';
import { useUIStore } from '../store/useUIStore';
import { animate, type AnimationPlaybackControls } from 'motion/react';

const isValidColor = (color: string) => {
  if (typeof window === 'undefined') return false;
  const s = new Option().style;
  s.color = color;
  return s.color !== '';
};

const safeNum = (val: string | number, fallback: number, min = 0, max = 100) => {
  const n = typeof val === 'string' ? parseFloat(val) : val;
  return Number.isFinite(n) ? Math.min(Math.max(n, min), max) : fallback;
};

export const useUIAdaptation = () => {
  const { currentTheme } = useUIStore();
  const controlsRef = useRef<AnimationPlaybackControls[]>([]);

  useEffect(() => {
    const root = document.documentElement;

    // Apply base aesthetic attribute
    if (currentTheme.theme) {
      root.setAttribute('data-theme-base', currentTheme.theme);
    } else {
      root.removeAttribute('data-theme-base');
    }
    
    // Stop previous manual animations
    controlsRef.current.forEach(c => c.stop());
    controlsRef.current = [];

    // Animate Glow Intensity (Manual JS animation for precision)
    const currentGlow = safeNum(getComputedStyle(root).getPropertyValue('--sentiment-glow-intensity'), 0.5, 0, 1);
    const targetGlow = safeNum(currentTheme.glowIntensity ?? currentTheme.intensity, 0.5, 0, 1);
    
    const intensityAnim = animate(
      currentGlow,
      targetGlow,
      {
        duration: 0.8,
        ease: [0.4, 0, 0.2, 1],
        onUpdate: (latest) => {
          if (Number.isFinite(latest)) {
            root.style.setProperty('--sentiment-glow-intensity', latest.toFixed(3));
          }
        }
      }
    );
    controlsRef.current.push(intensityAnim);

    // Animate Color if present (Manual JS animation for smooth interpolation)
    const targetColor = currentTheme.primaryColor ?? currentTheme.color;
    if (targetColor) {
      const sourceColor = getComputedStyle(root).getPropertyValue('--color-accent-primary').trim() || "#6366f1";

      if (isValidColor(sourceColor) && isValidColor(targetColor)) {
        const colorAnim = animate(
          sourceColor,
          targetColor,
          {
            duration: 1.5,
            ease: "easeInOut",
            onUpdate: (latest) => root.style.setProperty('--color-accent-primary', latest)
          }
        );
        controlsRef.current.push(colorAnim);
      }
    }
    
    // For Blur and Opacity, we now use CSS transitions defined in base.css
    // We just set the properties directly
    let targetBlurRaw = currentTheme.blurAmount;
    let targetOpacityRaw = currentTheme.glassOpacity;

    // Fallback logic for derived tokens
    if (targetBlurRaw === undefined || targetOpacityRaw === undefined) {
      switch (currentTheme.theme) {
        case 'emergency':
          targetBlurRaw = targetBlurRaw ?? 24;
          targetOpacityRaw = targetOpacityRaw ?? 0.15;
          break;
        case 'zen':
          targetBlurRaw = targetBlurRaw ?? 8;
          targetOpacityRaw = targetOpacityRaw ?? 0.02;
          break;
        case 'vibrant':
          targetBlurRaw = targetBlurRaw ?? 16;
          targetOpacityRaw = targetOpacityRaw ?? 0.08;
          break;
        case 'deep':
          targetBlurRaw = targetBlurRaw ?? 20;
          targetOpacityRaw = targetOpacityRaw ?? 0.12;
          break;
        default:
          targetBlurRaw = targetBlurRaw ?? 16;
          targetOpacityRaw = targetOpacityRaw ?? 0.04;
      }
    }

    root.style.setProperty('--sentiment-blur', `${safeNum(targetBlurRaw, 16, 0, 100).toFixed(1)}px`);
    root.style.setProperty('--sentiment-opacity', safeNum(targetOpacityRaw, 0.04, 0, 1).toFixed(3));

    return () => {
      controlsRef.current.forEach(c => c.stop());
    };
  }, [currentTheme]);
};
