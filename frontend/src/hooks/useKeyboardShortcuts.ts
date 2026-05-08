import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';

export const useKeyboardShortcuts = () => {
  const navigate = useNavigate();

  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      // Use Alt key for navigation shortcuts
      if (event.altKey) {
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
          case 't':
            event.preventDefault();
            // This is a bit tricky since it's in useUIStore, 
            // but we can trigger the theme toggle if we wanted to.
            // For now, let's stick to navigation.
            break;
        }
      }

      // Focus search/message composer with '/' if not already typing in an input
      if (event.key === '/' && document.activeElement?.tagName !== 'INPUT' && document.activeElement?.tagName !== 'TEXTAREA') {
        event.preventDefault();
        const composer = document.querySelector('.composer-form input') as HTMLInputElement;
        if (composer) composer.focus();
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [navigate]);
};
