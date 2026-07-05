interface ApiErrorBody {
  message?: string;
  error?: string;
}

const MESSAGE_KEYS: Record<string, string> = {
  'размер должен находиться в диапазоне от 8 до 100': 'errorPasswordLength',
  'size must be between 8 and 100': 'errorPasswordLength',
  'размер должен находиться в диапазоне от 3 до 50': 'errorUsernameLength',
  'size must be between 3 and 50': 'errorUsernameLength',
  'Username already exists': 'errorUsernameTaken',
};

export function parseApiError(text: string, status: number): string {
  const trimmed = text.trim();
  if (!trimmed) {
    return `Request failed (${status})`;
  }

  try {
    const body = JSON.parse(trimmed) as ApiErrorBody;
    if (body.message?.trim()) {
      return body.message.trim();
    }
    if (body.error?.trim()) {
      return body.error.trim();
    }
  } catch {
    // plain text response
  }

  return trimmed;
}

export function apiErrorTranslationKey(message: string): string | null {
  return MESSAGE_KEYS[message] ?? null;
}
