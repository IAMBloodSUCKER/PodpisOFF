import { useEffect, useRef, useState } from 'react';
import { BrowserNotificationGuidePanel } from './BrowserNotificationGuidePanel';
import { useI18n } from '../context/I18nContext';

const STORAGE_KEY = 'podpisoff.notifyGuide.denied';

function readFirstVisit(): boolean {
  try {
    return localStorage.getItem(STORAGE_KEY) === null;
  } catch {
    return true;
  }
}

function markSeen(): void {
  try {
    localStorage.setItem(STORAGE_KEY, 'seen');
  } catch {
    // ignore
  }
}

export function NotificationDeniedGuide() {
  const { t } = useI18n();
  const firstVisitRef = useRef(readFirstVisit());
  const [open, setOpen] = useState(firstVisitRef.current);
  const wasOpenRef = useRef(open);

  useEffect(() => {
    wasOpenRef.current = open;
  }, [open]);

  useEffect(() => {
    return () => {
      if (wasOpenRef.current) {
        markSeen();
      }
    };
  }, []);

  function hide() {
    setOpen(false);
    markSeen();
    firstVisitRef.current = false;
  }

  function show() {
    setOpen(true);
  }

  if (!open) {
    return (
      <aside className="concept-guide concept-guide-collapsed notify-guide-collapsed" role="status">
        <button type="button" className="concept-guide-toggle" onClick={show} aria-expanded={false}>
          <span className="concept-guide-toggle-label">{t('conceptGuideShow')}</span>
          <span className="concept-guide-toggle-title">{t('reminderNotifyDeniedTitle')}</span>
        </button>
      </aside>
    );
  }

  return (
    <div className="notify-bar notify-bar-info" role="status">
      <div className="notify-guide-top">
        <p className="notify-bar-title">{t('reminderNotifyDeniedTitle')}</p>
        <button
          type="button"
          className="notify-bar-collapse-toggle notify-bar-collapse-hide"
          onClick={hide}
          aria-expanded={true}
        >
          {t('conceptGuideHide')}
        </button>
      </div>
      <p className="notify-bar-note">{t('reminderNotifyDeniedNote')}</p>
      <BrowserNotificationGuidePanel />
    </div>
  );
}
