import { Subscription } from '../types/api';
import { currency, parseLocalDate, todayLocalIso } from './format';
import { effectiveNextBillingDate } from './subscriptionBilling';

const STORAGE_KEY = 'podpisoff-fired-billing';

function loadFired(): Set<string> {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return new Set();
    const parsed = JSON.parse(raw) as string[];
    return new Set(Array.isArray(parsed) ? parsed : []);
  } catch {
    return new Set();
  }
}

function saveFired(keys: Set<string>) {
  const trimmed = [...keys].slice(-300);
  localStorage.setItem(STORAGE_KEY, JSON.stringify(trimmed));
}

export function billingFireKey(subscription: Subscription): string {
  const nextDate = effectiveNextBillingDate(
    subscription.nextBillingDate,
    subscription.billingPeriod ?? 'MONTHLY',
  );
  return `${subscription.id}:${nextDate}`;
}

export function daysUntilBilling(subscription: Subscription, today = todayLocalIso()): number {
  const nextDate = effectiveNextBillingDate(
    subscription.nextBillingDate,
    subscription.billingPeriod ?? 'MONTHLY',
    today,
  );
  const target = parseLocalDate(nextDate);
  const now = parseLocalDate(today);
  return Math.round((target.getTime() - now.getTime()) / 86_400_000);
}

export function isBillingReminderDue(subscription: Subscription, daysBefore: number): boolean {
  if (daysBefore <= 0 || !subscription.active) return false;
  const daysUntil = daysUntilBilling(subscription);
  return daysUntil >= 0 && daysUntil <= daysBefore;
}

export function findDueBillingReminders(
  subscriptions: Subscription[],
  daysBefore: number,
): Subscription[] {
  const fired = loadFired();
  return subscriptions.filter(
    (item) => isBillingReminderDue(item, daysBefore) && !fired.has(billingFireKey(item)),
  );
}

export function markBillingReminderFired(subscription: Subscription) {
  const fired = loadFired();
  fired.add(billingFireKey(subscription));
  saveFired(fired);
}

export function billingReminderTitle(subscription: Subscription, locale: 'ru' | 'en'): string {
  const daysUntil = daysUntilBilling(subscription);
  if (locale === 'ru') {
    if (daysUntil === 0) return `Списание сегодня: ${subscription.title}`;
    if (daysUntil === 1) return `Списание завтра: ${subscription.title}`;
    return `Скоро списание: ${subscription.title}`;
  }
  if (daysUntil === 0) return `Charge today: ${subscription.title}`;
  if (daysUntil === 1) return `Charge tomorrow: ${subscription.title}`;
  return `Upcoming charge: ${subscription.title}`;
}

export function billingReminderBody(subscription: Subscription, locale: 'ru' | 'en'): string {
  const daysUntil = daysUntilBilling(subscription);
  const amount = currency(subscription.amount, subscription.currency, locale);
  if (locale === 'ru') {
    if (daysUntil === 0) return `${amount} — сегодня`;
    if (daysUntil === 1) return `${amount} — завтра`;
    return `${amount} — через ${daysUntil} дн.`;
  }
  if (daysUntil === 0) return `${amount} — today`;
  if (daysUntil === 1) return `${amount} — tomorrow`;
  return `${amount} — in ${daysUntil} days`;
}

export const BILLING_REMINDER_OPTIONS = [0, 1, 3, 7, 14] as const;

export type BillingReminderDays = (typeof BILLING_REMINDER_OPTIONS)[number];
