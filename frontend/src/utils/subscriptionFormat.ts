import { BillingPeriod } from '../types/api';

export function billingPeriodLabel(period: BillingPeriod, locale: 'ru' | 'en'): string {
  if (period === 'YEARLY') {
    return locale === 'ru' ? 'в год' : 'per year';
  }
  return locale === 'ru' ? 'в месяц' : 'per month';
}

export function formatSubscriptionPrice(
  amount: number,
  currency: string,
  period: BillingPeriod,
  locale: 'ru' | 'en',
  formatMoney: (value: number, code: string, loc: 'ru' | 'en') => string,
): string {
  return `${formatMoney(amount, currency, locale)} / ${billingPeriodLabel(period, locale)}`;
}
