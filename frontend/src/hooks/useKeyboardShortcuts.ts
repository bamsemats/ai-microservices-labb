import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';

export const useKeyboardShortcuts = () => {
  const navigate = useNavigate();

  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.isComposing) return;

      // Use Alt key for navigation shortcuts (tighten guard)
      if (event.altKey && !event.ctrlKey && !event.metaKey && !event.getModifierState('AltGraph')) {
        switch (event.key.toLowerCase()) {
          case 'h':
            event.preventDefault();
            navigate('/');
            break;
          case 'e':
            event.preventDefault();
            navigate('/explore');
            break;
          case 'i':
            event.preventDefault();
            navigate('/insights');
            break;
          case 'p':
            event.preventDefault();
            navigate('/profile');
            break;
        }
      }

      // Focus search/message composer with '/' if not already typing in an input/editable context
      const active = document.activeElement;
      const isEditable = active && (
        active.tagName === 'INPUT' || 
        active.tagName === 'TEXTAREA' || 
        active.tagName === 'SELECT' || 
        (active as HTMLElement).isContentEditable
      );

      if (event.key === '/' && !isEditable) {
        event.preventDefault();
        const composer = document.querySelector('.composer-form input') as HTMLInputElement;
        if (composer) composer.focus();
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [navigate]);
};
