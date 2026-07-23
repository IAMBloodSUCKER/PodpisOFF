import type { SubscriptionFormField } from '../utils/format';

interface ApiErrorBody {
  message?: string;
  error?: string;
  fieldErrors?: Record<string, string>;
}

export class ApiClientError extends Error {
  readonly status: number;
  readonly fieldErrors?: Record<string, string>;

  constructor(message: string, status: number, fieldErrors?: Record<string, string>) {
    super(message);
    this.name = 'ApiClientError';
    this.status = status;
    this.fieldErrors = fieldErrors;
  }
}

const MESSAGE_KEYS: Record<string, string> = {
  'размер должен находиться в диапазоне от 8 до 100': 'errorPasswordLength',
  'size must be between 8 and 100': 'errorPasswordLength',
  'размер должен находиться в диапазоне от 3 до 50': 'errorUsernameLength',
  'size must be between 3 and 50': 'errorUsernameLength',
  'размер должен находиться в диапазоне от 0 до 120': 'errorSubscriptionTitle',
  'size must be between 0 and 120': 'errorSubscriptionTitle',
  'размер должен находиться в диапазоне от 0 до 80': 'errorSubscriptionCategory',
  'size must be between 0 and 80': 'errorSubscriptionCategory',
  'не должно быть пустым': 'errorRequiredField',
  'must not be blank': 'errorRequiredField',
  'Username already exists': 'errorUsernameTaken',
  'Captcha expired': 'errorCaptchaExpired',
  'Captcha mismatch': 'errorCaptchaMismatch',
  'Invalid credentials': 'errorInvalidCredentials',
  'Terms must be accepted': 'errorTermsRequired',
  'User not found': 'errorUserNotFound',
  'Password must not match username': 'errorPasswordSameAsUsername',
  'Account is blocked': 'errorAccountBlocked',
  'Yandex OAuth is not configured': 'errorOAuthUnavailable',
  Unauthorized: 'errorUnauthorized',
  'FREE plan allows up to 3 subscriptions': 'errorSubscriptionLimit',
  'FREE plan allows up to 5 reminders': 'errorReminderLimit',
  'CSV export is available only for PRO plan': 'errorExportProOnly',
  'Foreign currencies are available only for PRO plan': 'errorCurrencyProOnly',
  'Recurring reminders on Free are available only during the first month': 'errorRecurringTrialEnded',
  'Subscription not found': 'errorSubscriptionNotFound',
  'Unsupported currency': 'errorSubscriptionCurrency',
  'Invalid month': 'errorInvalidMonth',
  'Dev tools are not available': 'errorDevToolsForbidden',
  'Bad Gateway': 'errorServerUnavailable',
  'Server unavailable': 'errorServerUnavailable',
  'Request failed': 'errorGeneric',
};

const API_FIELD_DEFAULT_KEYS: Record<string, string> = {
  title: 'errorSubscriptionTitle',
  category: 'errorSubscriptionCategory',
  amount: 'errorSubscriptionAmount',
  currency: 'errorSubscriptionCurrency',
  note: 'errorSubscriptionNote',
  resourceUrl: 'errorSubscriptionResourceUrl',
};

const API_FIELD_NAMES: Record<string, SubscriptionFormField> = {
  title: 'title',
  category: 'category',
  amount: 'amount',
  currency: 'currency',
  note: 'note',
  resourceUrl: 'resourceUrl',
};

function isHtmlPayload(text: string): boolean {
  const lower = text.trim().toLowerCase();
  return lower.startsWith('<!doctype') || lower.startsWith('<html') || lower.startsWith('<');
}

export function parseApiErrorBody(
  text: string,
  status: number,
): { message: string; fieldErrors?: Record<string, string> } {
  const trimmed = text.trim();
  if (!trimmed) {
    return {
      message: status >= 500 ? 'Server unavailable' : `Request failed (${status})`,
    };
  }

  if (isHtmlPayload(trimmed)) {
    if (status === 502 || trimmed.includes('502')) {
      return { message: 'Bad Gateway' };
    }
    if (status >= 500) {
      return { message: 'Server unavailable' };
    }
    return { message: 'Request failed' };
  }

  if (trimmed.startsWith('{')) {
    try {
      const body = JSON.parse(trimmed) as ApiErrorBody;
      const message = body.message?.trim() || body.error?.trim() || 'Request failed';
      const fieldErrors =
        body.fieldErrors && Object.keys(body.fieldErrors).length > 0 ? body.fieldErrors : undefined;
      return { message, fieldErrors };
    } catch {
      // fall through
    }
  }

  return { message: trimmed };
}

export function parseApiError(text: string, status: number): string {
  return parseApiErrorBody(text, status).message;
}

export function apiErrorTranslationKey(message: string): string | null {
  if (MESSAGE_KEYS[message]) {
    return MESSAGE_KEYS[message];
  }

  if (isHtmlPayload(message)) {
    return 'errorServerUnavailable';
  }

  for (const [needle, key] of Object.entries(MESSAGE_KEYS)) {
    if (message.includes(needle)) {
      return key;
    }
  }

  return null;
}

export function mapApiFieldErrorsToForm(
  fieldErrors: Record<string, string> | undefined,
): Partial<Record<SubscriptionFormField, string>> {
  if (!fieldErrors) return {};

  const result: Partial<Record<SubscriptionFormField, string>> = {};
  for (const [field, message] of Object.entries(fieldErrors)) {
    const formField = API_FIELD_NAMES[field];
    if (!formField) continue;
    const key = apiErrorTranslationKey(message) ?? API_FIELD_DEFAULT_KEYS[field];
    if (key) {
      result[formField] = key;
    }
  }
  return result;
}

export function resolveApiError(err: unknown, t: (key: string) => string, status = 500): string {
  if (err instanceof ApiClientError) {
    const key = apiErrorTranslationKey(err.message);
    return key ? t(key) : err.message || t('errorGeneric');
  }

  const initial = err instanceof Error ? err.message : String(err);
  const message = isHtmlPayload(initial) || initial.trim().startsWith('{')
    ? parseApiError(initial, status)
    : initial;
  const key = apiErrorTranslationKey(message);
  return key ? t(key) : isHtmlPayload(message) ? t('errorServerUnavailable') : message || t('errorGeneric');
}

export function isRetryableStatus(status: number): boolean {
  return status === 502 || status === 503 || status === 504;
}
