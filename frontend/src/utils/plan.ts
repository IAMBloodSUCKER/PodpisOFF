import { AuthResponse, PlanType } from '../types/api';

export const FREE_SUBSCRIPTION_LIMIT = 3;

export function effectivePlan(user: Pick<AuthResponse, 'plan' | 'planExpiresAt'> | null | undefined): PlanType {
  if (!user) return 'FREE';
  if (user.plan === 'PRO') {
    if (user.planExpiresAt && new Date(user.planExpiresAt).getTime() < Date.now()) {
      return 'FREE';
    }
    return 'PRO';
  }
  return user.plan ?? 'FREE';
}

export function subscriptionsIncludedInPlan<T extends { id: number; createdAt: string; active: boolean }>(
  plan: PlanType,
  subscriptions: T[],
): T[] {
  if (plan === 'PRO') return subscriptions;
  return [...subscriptions]
    .filter((item) => item.active)
    .sort((left, right) => {
      const byDate = new Date(left.createdAt).getTime() - new Date(right.createdAt).getTime();
      return byDate !== 0 ? byDate : left.id - right.id;
    })
    .slice(0, FREE_SUBSCRIPTION_LIMIT);
}

export function isPro(plan: PlanType): boolean {
  return plan === 'PRO';
}

export function isExpiredPro(user: Pick<AuthResponse, 'plan' | 'planExpiresAt'> | null | undefined): boolean {
  if (!user?.planExpiresAt) return false;
  return new Date(user.planExpiresAt).getTime() < Date.now();
}

export function isOverSubscriptionLimit(plan: PlanType, count: number): boolean {
  return plan === 'FREE' && count > FREE_SUBSCRIPTION_LIMIT;
}

export function subscriptionLimit(plan: PlanType): number {
  return plan === 'PRO' ? -1 : FREE_SUBSCRIPTION_LIMIT;
}

export function atSubscriptionCreateLimit(plan: PlanType, count: number): boolean {
  return plan === 'FREE' && count >= FREE_SUBSCRIPTION_LIMIT;
}

export function canUseForeignCurrency(plan: PlanType): boolean {
  return plan === 'PRO';
}
