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
  'Recovery key is invalid': 'errorRecoveryKeyInvalid',
  'Unauthorized': 'errorUnauthorized',
  'FREE plan allows up to 5 subscriptions': 'errorSubscriptionLimit',
  'CSV export is available only for PRO plan': 'errorExportProOnly',
  'YooKassa is not configured': 'errorBillingNotConfigured',
};

export function parseApiError(text: string, status: number): string {
  const trimmed = text.trim();
  if (!trimmed) {
    return `Request failed (${status})`;
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

  return trimmed;
}

export function apiErrorTranslationKey(message: string): string | null {
  if (MESSAGE_KEYS[message]) {
    return MESSAGE_KEYS[message];
  }

  for (const [needle, key] of Object.entries(MESSAGE_KEYS)) {
    if (message.includes(needle)) {
      return key;
    }
  }

  return null;
}

export function resolveApiError(err: unknown, t: (key: string) => string): string {
  const initial = err instanceof Error ? err.message : String(err);
  const message = initial.trim().startsWith('{') ? parseApiError(initial, 400) : initial;
  const key = apiErrorTranslationKey(message);
  return key ? t(key) : message || t('errorGeneric');
}
