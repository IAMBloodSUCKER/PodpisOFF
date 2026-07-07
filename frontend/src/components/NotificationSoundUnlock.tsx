import { useEffect } from 'react';
import { unlockNotificationSound } from '../utils/notificationSound';

/** Browsers need a user gesture before Web Audio can play. */
export function NotificationSoundUnlock() {
  useEffect(() => {
    const unlock = () => {
      void unlockNotificationSound();
    };
    window.addEventListener('pointerdown', unlock, { once: true });
    window.addEventListener('keydown', unlock, { once: true });
    return () => {
      window.removeEventListener('pointerdown', unlock);
      window.removeEventListener('keydown', unlock);
    };
  }, []);

  return null;
}
