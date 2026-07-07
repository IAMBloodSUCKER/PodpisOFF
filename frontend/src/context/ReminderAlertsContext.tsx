import { createContext, useCallback, useContext, useMemo, useState } from 'react';
import { api } from '../api/api';
import { usePageVisiblePoll } from '../hooks/usePageVisiblePoll';
import { useAuth } from './AuthContext';
import { Subscription } from '../types/api';
import {
  findDueBillingReminders,
  markBillingReminderFired,
} from '../utils/billingNotifications';
import { effectivePlan, subscriptionsIncludedInPlan } from '../utils/plan';
import { playNotificationSound } from '../utils/notificationSound';

type ReminderAlertsContextValue = {
  dueBilling: Subscription[];
  dismissBillingDue: (id: number) => void;
};

const ReminderAlertsContext = createContext<ReminderAlertsContextValue | null>(null);

const POLL_MS = 15_000;

export function ReminderAlertsProvider({ children }: { children: React.ReactNode }) {
  const { isAuthenticated, user } = useAuth();
  const [dueBilling, setDueBilling] = useState<Subscription[]>([]);

  const daysBefore = user?.billingReminderDaysBefore ?? 3;

  const processDue = useCallback(
    async (subscriptions: Subscription[]) => {
      const billingDue = findDueBillingReminders(subscriptions, daysBefore);
      for (const item of billingDue) {
        markBillingReminderFired(item);
      }
      if (billingDue.length) {
        if (document.visibilityState === 'visible') {
          void playNotificationSound();
        }
        setDueBilling((prev) => {
          const ids = new Set(prev.map((item) => item.id));
          const next = [...prev];
          for (const item of billingDue) {
            if (!ids.has(item.id)) next.push(item);
          }
          return next;
        });
      }
    },
    [daysBefore],
  );

  const poll = useCallback(async () => {
    if (!isAuthenticated) return;
    try {
      const subscriptions = await api.listSubscriptions();
      const plan = effectivePlan(user);
      const included = subscriptionsIncludedInPlan(plan, subscriptions);
      await processDue(included);
    } catch {
      // ignore polling errors
    }
  }, [isAuthenticated, processDue, user]);

  usePageVisiblePoll(() => void poll(), POLL_MS, isAuthenticated);

  const dismissBillingDue = useCallback((id: number) => {
    setDueBilling((prev) => prev.filter((item) => item.id !== id));
  }, []);

  const value = useMemo(
    () => ({ dueBilling, dismissBillingDue }),
    [dueBilling, dismissBillingDue],
  );

  return <ReminderAlertsContext.Provider value={value}>{children}</ReminderAlertsContext.Provider>;
}

export function useReminderAlerts() {
  const ctx = useContext(ReminderAlertsContext);
  if (!ctx) {
    throw new Error('useReminderAlerts must be used within ReminderAlertsProvider');
  }
  return ctx;
}
