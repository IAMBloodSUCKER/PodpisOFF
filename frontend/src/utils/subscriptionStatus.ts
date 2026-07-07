import { Subscription } from '../types/api';
import { parseLocalDate, todayLocalIso } from './format';

export type SubscriptionDisplayStatus = 'active' | 'paused' | 'off';

export function resolveSubscriptionStatus(
  active: boolean,
  nextBillingDate: string,
): SubscriptionDisplayStatus {
  if (active) return 'active';
  const targetDate = parseLocalDate(nextBillingDate);
  const today = parseLocalDate(todayLocalIso());
  return targetDate.getTime() >= today.getTime() ? 'paused' : 'off';
}

export function resolveSubscriptionStatusFromItem(subscription: Subscription): SubscriptionDisplayStatus {
  return resolveSubscriptionStatus(subscription.active, subscription.nextBillingDate);
}
