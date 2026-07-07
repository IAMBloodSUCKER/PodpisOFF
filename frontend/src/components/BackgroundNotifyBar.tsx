import { useEffect, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { useI18n } from '../context/I18nContext';
import { NotificationDeniedGuide } from './NotificationDeniedGuide';
import { ensurePushSubscription } from '../utils/pushSubscription';
import { requestNotificationPermission } from '../utils/reminderNotifications';
export function BackgroundNotifyBar() {
  const { t } = useI18n();
  const { isAuthenticated } = useAuth();
  const [permission, setPermission] = useState<NotificationPermission | 'unsupported'>(() => {
    if (typeof Notification === 'undefined') return 'unsupported';
    return Notification.permission;
  });

  useEffect(() => {
    if (typeof Notification === 'undefined') return;
    const sync = () => setPermission(Notification.permission);
    document.addEventListener('visibilitychange', sync);
    return () => document.removeEventListener('visibilitychange', sync);
  }, []);

  useEffect(() => {
    if (!isAuthenticated || permission !== 'granted') return;
    void ensurePushSubscription();
  }, [isAuthenticated, permission]);

  async function onEnable() {
    const next = await requestNotificationPermission();
    setPermission(next);
    if (next === 'granted') {
      await ensurePushSubscription();
    }
  }

  if (permission === 'granted' || permission === 'unsupported') {
    return null;
  }

  if (permission === 'denied') {
    return <NotificationDeniedGuide />;
  }
  return (
    <div className="notify-bar notify-bar-action">
      <p>{t('backgroundNotifyPrompt')}</p>
      <button type="button" className="ghost notify-enable-btn" onClick={() => void onEnable()}>
        {t('backgroundNotifyEnable')}
      </button>
    </div>
  );
}
