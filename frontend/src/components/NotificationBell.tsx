import { useCallback, useEffect, useRef, useState } from 'react';
import { api } from '../api/api';
import { usePageVisiblePoll } from '../hooks/usePageVisiblePoll';
import { useI18n } from '../context/I18nContext';
import type { UserNotification } from '../types/api';
import {
  NOTIFICATIONS_CHANGED_EVENT,
  dispatchInAppNotification,
} from '../utils/notificationEvents';
import { canShowAppNotifications, showAppNotification } from '../utils/appNotifications';
import { hasPushSubscription } from '../utils/pushSubscription';
import { playNotificationSound } from '../utils/notificationSound';

const POLL_MS = 15_000;

export function NotificationBell() {
  const { t, locale } = useI18n();
  const [open, setOpen] = useState(false);
  const [items, setItems] = useState<UserNotification[]>([]);
  const [unread, setUnread] = useState(0);
  const rootRef = useRef<HTMLDivElement>(null);
  const bootstrappedRef = useRef(false);
  const announcedIdsRef = useRef<Set<number>>(new Set());

  const announce = useCallback(async (item: UserNotification) => {
    if (announcedIdsRef.current.has(item.id)) return;

    const body = item.body ?? '';
    const visible = document.visibilityState === 'visible';

    if (visible) {
      announcedIdsRef.current.add(item.id);
      dispatchInAppNotification({ title: item.title, body });
      void playNotificationSound();
      return;
    }

    const pushEnabled = await hasPushSubscription();
    if (pushEnabled) {
      announcedIdsRef.current.add(item.id);
      return;
    }

    if (canShowAppNotifications()) {
      const shown = await showAppNotification(item.title, body, {
        tag: `notification-${item.id}`,
        url: '/dashboard',
      });
      if (shown) {
        announcedIdsRef.current.add(item.id);
      }
    }
  }, []);

  const refresh = useCallback(async () => {
    try {
      const [list, count] = await Promise.all([api.notifications(), api.notificationUnreadCount()]);

      if (!bootstrappedRef.current) {
        for (const item of list) {
          if (item.unread) announcedIdsRef.current.add(item.id);
        }
        bootstrappedRef.current = true;
      } else {
        const freshUnread = list.filter((item) => item.unread && !announcedIdsRef.current.has(item.id));
        for (const item of freshUnread) {
          await announce(item);
        }
      }

      setItems(list);
      setUnread(count.count);
    } catch {
      /* ignore */
    }
  }, [announce]);

  usePageVisiblePoll(() => void refresh(), POLL_MS);

  useEffect(() => {
    function onVisible() {
      if (document.visibilityState !== 'visible') return;
      void refresh();
    }
    document.addEventListener('visibilitychange', onVisible);
    return () => document.removeEventListener('visibilitychange', onVisible);
  }, [refresh]);

  useEffect(() => {
    const onChanged = () => void refresh();
    window.addEventListener(NOTIFICATIONS_CHANGED_EVENT, onChanged);
    return () => window.removeEventListener(NOTIFICATIONS_CHANGED_EVENT, onChanged);
  }, [refresh]);

  useEffect(() => {
    function onDocClick(event: MouseEvent) {
      if (!rootRef.current?.contains(event.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener('click', onDocClick);
    return () => document.removeEventListener('click', onDocClick);
  }, []);

  async function onOpen() {
    setOpen((value) => !value);
    if (!open) {
      await refresh();
    }
  }

  async function onRead(item: UserNotification) {
    if (item.unread) {
      await api.markNotificationRead(item.id);
      await refresh();
    }
  }

  async function onReadAll() {
    await api.markAllNotificationsRead();
    await refresh();
  }

  function formatDate(value: string) {
    return new Date(value).toLocaleString(locale === 'ru' ? 'ru-RU' : 'en-US', {
      day: '2-digit',
      month: 'short',
      hour: '2-digit',
      minute: '2-digit',
    });
  }

  return (
    <div className="notify-bell" ref={rootRef}>
      <button type="button" className="notify-bell-btn" onClick={() => void onOpen()} aria-label={t('notificationsTitle')}>
        <svg className="notify-bell-icon" viewBox="0 0 24 24" aria-hidden focusable="false">
          <path
            fill="currentColor"
            d="M12 22a2.5 2.5 0 0 0 2.45-2h-4.9A2.5 2.5 0 0 0 12 22Zm7-6V11a7 7 0 0 0-5-6.71V3.5a1.5 1.5 0 1 0-3 0v.79A7 7 0 0 0 6 11v5l-1.7 3.4A1 1 0 0 0 5.2 21h13.6a1 1 0 0 0 .9-1.45L19 16Z"
          />
        </svg>
        {unread > 0 && <span className="notify-bell-badge">{unread > 9 ? '9+' : unread}</span>}
      </button>
      {open && (
        <div className="notify-bell-panel card">
          <div className="row-between">
            <strong>{t('notificationsTitle')}</strong>
            {unread > 0 && (
              <button type="button" className="ghost compact" onClick={() => void onReadAll()}>
                {t('notificationsReadAll')}
              </button>
            )}
          </div>
          <div className="notify-bell-list">
            {items.length === 0 ? (
              <p className="muted">{t('notificationsEmpty')}</p>
            ) : (
              items.map((item) => (
                <button
                  key={item.id}
                  type="button"
                  className={`notify-bell-item ${item.unread ? 'unread' : ''}`}
                  onClick={() => void onRead(item)}
                >
                  <strong>{item.title}</strong>
                  {item.body && <p>{item.body}</p>}
                  <span className="muted">{formatDate(item.createdAt)}</span>
                </button>
              ))
            )}
          </div>
        </div>
      )}
    </div>
  );
}
