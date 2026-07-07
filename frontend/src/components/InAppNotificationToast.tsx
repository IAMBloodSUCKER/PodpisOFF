import { useEffect, useState } from 'react';
import { useI18n } from '../context/I18nContext';
import {
  IN_APP_NOTIFICATION_EVENT,
  type InAppNotificationDetail,
} from '../utils/notificationEvents';

type ToastItem = InAppNotificationDetail & { id: number };

const AUTO_DISMISS_MS = 8000;

export function InAppNotificationToast() {
  const { t } = useI18n();
  const [toasts, setToasts] = useState<ToastItem[]>([]);

  useEffect(() => {
    let nextId = 0;

    function onNotify(event: Event) {
      const detail = (event as CustomEvent<InAppNotificationDetail>).detail;
      if (!detail?.title) return;

      const id = ++nextId;
      setToasts((prev) => [...prev, { ...detail, id }]);
      window.setTimeout(() => {
        setToasts((prev) => prev.filter((item) => item.id !== id));
      }, AUTO_DISMISS_MS);
    }

    window.addEventListener(IN_APP_NOTIFICATION_EVENT, onNotify);
    return () => window.removeEventListener(IN_APP_NOTIFICATION_EVENT, onNotify);
  }, []);

  if (toasts.length === 0) return null;

  return (
    <div className="app-notify-toast-stack" aria-live="polite">
      {toasts.map((toast) => (
        <div key={toast.id} className="app-notify-toast" role="status">
          <button
            type="button"
            className="ghost app-notify-toast-close"
            onClick={() => setToasts((prev) => prev.filter((item) => item.id !== toast.id))}
            aria-label={t('reminderDismiss')}
          >
            ×
          </button>
          <strong>{toast.title}</strong>
          {toast.body && <p>{toast.body}</p>}
        </div>
      ))}
    </div>
  );
}
