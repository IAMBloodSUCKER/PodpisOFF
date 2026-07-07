interface ApiErrorBody {
  message?: string;
  error?: string;
}

const MESSAGE_KEYS: Record<string, string> = {
  'размер должен находиться в диапазоне от 8 до 100': 'errorPasswordLength',
  'size must be between 8 and 100': 'errorPasswordLength',
  'размер должен находиться в диапазоне от 3 до 50': 'errorUsernameLength',
  'size must be between 3 and 50': 'errorUsernameLength',
  'не должно быть пустым': 'errorRequiredField',
  'must not be blank': 'errorRequiredField',
  'Username already exists': 'errorUsernameTaken',
  'Captcha expired': 'errorCaptchaExpired',
  'Captcha mismatch': 'errorCaptchaMismatch',
  'Invalid credentials': 'errorInvalidCredentials',
  'Terms must be accepted': 'errorTermsRequired',
  'User not found': 'errorUserNotFound',
  'Password must not match username': 'errorPasswordSameAsUsername',
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

function isHtmlPayload(text: string): boolean {
  const lower = text.trim().toLowerCase();
  return lower.startsWith('<!doctype') || lower.startsWith('<html') || lower.startsWith('<');
}

export function parseApiError(text: string, status: number): string {
  const trimmed = text.trim();
  if (!trimmed) {
    return status >= 500 ? 'Server unavailable' : `Request failed (${status})`;
  }

  if (isHtmlPayload(trimmed)) {
    if (status === 502 || trimmed.includes('502')) {
      return 'Bad Gateway';
    }
    if (status >= 500) {
      return 'Server unavailable';
    }
    return 'Request failed';
  }

  if (trimmed.startsWith('{')) {
    try {
      const body = JSON.parse(trimmed) as ApiErrorBody;
      if (body.message?.trim()) {
        return body.message.trim();
      }
      if (body.error?.trim()) {
        return body.error.trim();
      }
    } catch {
      // fall through
    }
  }

  if (isHtmlPayload(trimmed)) {
    return status >= 500 ? 'Server unavailable' : 'Request failed';
  }

  return trimmed;
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

export function resolveApiError(err: unknown, t: (key: string) => string, status = 500): string {
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
