import { isSupportedCurrency } from '../i18n/currencies';
import { LocaleCode } from '../types/api';

const CURRENCY_LOCALES: Record<string, string> = {
  RUB: 'ru-RU',
  USD: 'en-US',
  EUR: 'de-DE',
  GBP: 'en-GB',
  CNY: 'zh-CN',
  TRY: 'tr-TR',
  KZT: 'kk-KZ',
  BYN: 'be-BY',
  UAH: 'uk-UA',
  JPY: 'ja-JP',
  CHF: 'de-CH',
  PLN: 'pl-PL',
  AED: 'ar-AE',
  THB: 'th-TH',
  INR: 'en-IN',
};

function resolveFormatLocale(code: string, locale?: LocaleCode): string | undefined {
  if (CURRENCY_LOCALES[code]) return CURRENCY_LOCALES[code];
  if (locale === 'ru') return 'ru-RU';
  if (locale === 'en') return 'en-US';
  return undefined;
}

export function currency(value: number, code: string, locale?: LocaleCode): string {
  const normalized = code.trim().toUpperCase();
  const formatLocale = resolveFormatLocale(normalized, locale);
  try {
    return new Intl.NumberFormat(formatLocale, { style: 'currency', currency: normalized }).format(value);
  } catch {
    return `${value.toFixed(2)} ${normalized}`;
  }
}

export function formatCurrencyTotals(totals: Record<string, number>, locale?: LocaleCode): string[] {
  return Object.entries(totals).map(([code, amount]) => currency(amount, code, locale));
}

/** Parse YYYY-MM-DD as local calendar date (avoids UTC shift). */
export function parseLocalDate(value: string): Date {
  const [year, month, day] = value.split('-').map(Number);
  return new Date(year, month - 1, day);
}

export function dateLabel(value: string): string {
  return parseLocalDate(value).toLocaleDateString();
}

export function todayLocalIso(): string {
  const now = new Date();
  const y = now.getFullYear();
  const m = String(now.getMonth() + 1).padStart(2, '0');
  const d = String(now.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

export type SubscriptionFormField = 'title' | 'category' | 'amount' | 'currency' | 'note' | 'resourceUrl';

export function validateSubscriptionFormFields(input: {
  title: string;
  category: string;
  amount: string;
  currency: string;
  note: string;
  resourceUrl: string;
}): Partial<Record<SubscriptionFormField, string>> {
  const errors: Partial<Record<SubscriptionFormField, string>> = {};

  const title = input.title.trim();
  if (!title || title.length > 120) errors.title = 'errorSubscriptionTitle';

  const category = input.category.trim();
  if (!category || category.length > 80) errors.category = 'errorSubscriptionCategory';

  const amount = Number(input.amount);
  if (!Number.isFinite(amount) || amount < 0.01) errors.amount = 'errorSubscriptionAmount';

  const currencyCode = input.currency.trim().toUpperCase();
  if (!currencyCode || !isSupportedCurrency(currencyCode)) errors.currency = 'errorSubscriptionCurrency';

  const note = input.note.trim();
  if (note.length > 500) errors.note = 'errorSubscriptionNote';

  const resourceUrl = input.resourceUrl.trim();
  if (resourceUrl.length > 500) {
    errors.resourceUrl = 'errorSubscriptionResourceUrl';
  } else if (resourceUrl && !normalizeResourceUrl(resourceUrl)) {
    errors.resourceUrl = 'errorInvalidResourceUrl';
  }

  return errors;
}

export function validateSubscriptionForm(input: {
  title: string;
  category: string;
  amount: string;
  currency: string;
}): string | null {
  const errors = validateSubscriptionFormFields({ ...input, note: '', resourceUrl: '' });
  const first = Object.values(errors)[0];
  return first ?? null;
}

export function normalizeResourceUrl(value: string): string | null {
  const trimmed = value.trim();
  if (!trimmed) return null;
  const withScheme = /^https?:\/\//i.test(trimmed) ? trimmed : `https://${trimmed}`;
  try {
    const url = new URL(withScheme);
    if (!url.hostname) return null;
    return url.href;
  } catch {
    return null;
  }
}

export function validateResourceUrlInput(value: string): string | null {
  if (!value.trim()) return null;
  return normalizeResourceUrl(value) ? null : 'errorInvalidResourceUrl';
}
