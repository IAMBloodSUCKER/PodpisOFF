export const NOTIFICATIONS_CHANGED_EVENT = 'podpisoff:notifications-changed';
export const IN_APP_NOTIFICATION_EVENT = 'podpisoff:in-app-notification';

export type InAppNotificationDetail = {
  title: string;
  body: string;
};

export function notifyNotificationsChanged(): void {
  window.dispatchEvent(new Event(NOTIFICATIONS_CHANGED_EVENT));
}

export function dispatchInAppNotification(detail: InAppNotificationDetail): void {
  window.dispatchEvent(new CustomEvent<InAppNotificationDetail>(IN_APP_NOTIFICATION_EVENT, { detail }));
}

export function scheduleNotificationsRefresh(delayMs: number): void {
  window.setTimeout(() => notifyNotificationsChanged(), delayMs);
}

/** Poll the bell several times after a delayed server notification. */
export function scheduleNotificationsPolling(delaySeconds: number): void {
  const offsetsMs = [200, 800, 1500, 3000, 5000];
  const base = delaySeconds * 1000;
  for (const offset of offsetsMs) {
    window.setTimeout(() => notifyNotificationsChanged(), base + offset);
  }
}
