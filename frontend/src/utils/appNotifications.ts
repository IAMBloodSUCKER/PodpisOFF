export type AppNotificationOptions = {
  tag?: string;
  url?: string;
};

export function canShowAppNotifications(): boolean {
  return typeof Notification !== 'undefined' && Notification.permission === 'granted';
}

export async function getServiceWorkerRegistration(): Promise<ServiceWorkerRegistration | null> {
  if (!('serviceWorker' in navigator)) return null;

  try {
    let registration = await navigator.serviceWorker.getRegistration();
    if (!registration) {
      registration = await navigator.serviceWorker.register('/sw.js', { updateViaCache: 'none' });
    }

    await Promise.race([
      navigator.serviceWorker.ready,
      new Promise<never>((_, reject) => {
        window.setTimeout(() => reject(new Error('sw-timeout')), 4000);
      }),
    ]);

    return registration;
  } catch {
    return null;
  }
}

export async function showAppNotification(
  title: string,
  body: string,
  options: AppNotificationOptions = {},
): Promise<boolean> {
  if (!canShowAppNotifications()) {
    return false;
  }

  const tag = options.tag ?? `podpisoff-${Date.now()}`;
  const url = options.url ?? '/dashboard';
  const payload = {
    body,
    icon: '/icon-192.png',
    badge: '/icon-192.png',
    tag,
    silent: false,
    renotify: true,
    data: { url, title },
  } as NotificationOptions & { renotify?: boolean };

  const registration = await getServiceWorkerRegistration();
  if (registration) {
    try {
      await registration.showNotification(title, payload);
      return true;
    } catch {
      // fall through to Notification API
    }
  }

  try {
    new Notification(title, payload);
    return true;
  } catch {
    return false;
  }
}
