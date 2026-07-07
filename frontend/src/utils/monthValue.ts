export function currentMonthValue(): string {
  const now = new Date();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  return `${now.getFullYear()}-${month}`;
}

export function parseMonthValue(value: string): { year: number; month: number } | null {
  const match = /^(\d{4})-(\d{2})$/.exec(value);
  if (!match) return null;
  const year = Number(match[1]);
  const month = Number(match[2]);
  if (month < 1 || month > 12) return null;
  return { year, month };
}

export function toMonthValue(year: number, month: number): string {
  return `${year}-${String(month).padStart(2, '0')}`;
}

export function formatMonthLabel(value: string, locale: 'ru' | 'en'): string {
  const parsed = parseMonthValue(value);
  if (!parsed) return value;
  const date = new Date(parsed.year, parsed.month - 1, 1);
  return date.toLocaleString(locale === 'ru' ? 'ru-RU' : 'en-US', { month: 'long', year: 'numeric' });
}
