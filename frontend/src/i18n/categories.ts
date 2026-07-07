export const SUBSCRIPTION_CATEGORIES = [
  { key: 'streaming', labelRu: 'Стриминг', labelEn: 'Streaming' },
  { key: 'music', labelRu: 'Музыка', labelEn: 'Music' },
  { key: 'software', labelRu: 'Софт и VPN', labelEn: 'Software & VPN' },
  { key: 'gaming', labelRu: 'Игры', labelEn: 'Gaming' },
  { key: 'cloud', labelRu: 'Облако', labelEn: 'Cloud storage' },
  { key: 'education', labelRu: 'Образование', labelEn: 'Education' },
  { key: 'mobile', labelRu: 'Связь и интернет', labelEn: 'Mobile & internet' },
  { key: 'fitness', labelRu: 'Спорт и здоровье', labelEn: 'Fitness & health' },
  { key: 'news', labelRu: 'Новости и медиа', labelEn: 'News & media' },
] as const;

export type SubscriptionCategoryKey = (typeof SUBSCRIPTION_CATEGORIES)[number]['key'];

export const CUSTOM_CATEGORY_VALUE = '__custom__';

export function getCategoryLabel(key: SubscriptionCategoryKey, locale: 'ru' | 'en'): string {
  const item = SUBSCRIPTION_CATEGORIES.find((entry) => entry.key === key);
  if (!item) return key;
  return locale === 'ru' ? item.labelRu : item.labelEn;
}

export function getCategoryOptions(locale: 'ru' | 'en') {
  return SUBSCRIPTION_CATEGORIES.map((item) => ({
    key: item.key,
    label: locale === 'ru' ? item.labelRu : item.labelEn,
  }));
}

export function isPresetCategory(value: string, locale: 'ru' | 'en'): boolean {
  const trimmed = value.trim();
  if (!trimmed) return false;
  return getCategoryOptions(locale).some((item) => item.label === trimmed);
}
