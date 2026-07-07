import { useEffect } from 'react';

/** Poll on mount, on interval, and when user returns to the tab. */
export function usePageVisiblePoll(poll: () => void, intervalMs: number, enabled = true): void {
  useEffect(() => {
    if (!enabled) return;

    const tick = () => poll();

    tick();
    const timer = window.setInterval(tick, intervalMs);

    const onVisibility = () => {
      if (document.visibilityState === 'visible') {
        tick();
      }
    };
    document.addEventListener('visibilitychange', onVisibility);

    return () => {
      window.clearInterval(timer);
      document.removeEventListener('visibilitychange', onVisibility);
    };
  }, [poll, intervalMs, enabled]);
}
