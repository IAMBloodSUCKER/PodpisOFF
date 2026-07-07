export const SUPPORTED_CURRENCIES = [
  { code: 'RUB', labelRu: 'Российский рубль (₽)', labelEn: 'Russian ruble (₽)' },
  { code: 'USD', labelRu: 'Доллар США ($)', labelEn: 'US dollar ($)' },
  { code: 'EUR', labelRu: 'Евро (€)', labelEn: 'Euro (€)' },
  { code: 'GBP', labelRu: 'Фунт стерлингов (£)', labelEn: 'British pound (£)' },
  { code: 'CNY', labelRu: 'Китайский юань (¥)', labelEn: 'Chinese yuan (¥)' },
  { code: 'TRY', labelRu: 'Турецкая лира (₺)', labelEn: 'Turkish lira (₺)' },
  { code: 'KZT', labelRu: 'Казахстанский тенге (₸)', labelEn: 'Kazakhstani tenge (₸)' },
  { code: 'BYN', labelRu: 'Белорусский рубль (Br)', labelEn: 'Belarusian ruble (Br)' },
  { code: 'UAH', labelRu: 'Украинская гривна (₴)', labelEn: 'Ukrainian hryvnia (₴)' },
  { code: 'JPY', labelRu: 'Японская иена (¥)', labelEn: 'Japanese yen (¥)' },
  { code: 'CHF', labelRu: 'Швейцарский франк (CHF)', labelEn: 'Swiss franc (CHF)' },
  { code: 'PLN', labelRu: 'Польский злотый (zł)', labelEn: 'Polish zloty (zł)' },
  { code: 'AED', labelRu: 'Дирхам ОАЭ (AED)', labelEn: 'UAE dirham (AED)' },
  { code: 'THB', labelRu: 'Тайский бат (฿)', labelEn: 'Thai baht (฿)' },
  { code: 'INR', labelRu: 'Индийская рупия (₹)', labelEn: 'Indian rupee (₹)' },
] as const;

export type SupportedCurrencyCode = (typeof SUPPORTED_CURRENCIES)[number]['code'];

export const SUPPORTED_CURRENCY_CODES = SUPPORTED_CURRENCIES.map((item) => item.code);

export function isSupportedCurrency(code: string): boolean {
  return SUPPORTED_CURRENCY_CODES.includes(code.trim().toUpperCase() as SupportedCurrencyCode);
}

export function currencyLabel(code: SupportedCurrencyCode, locale: 'ru' | 'en'): string {
  const item = SUPPORTED_CURRENCIES.find((entry) => entry.code === code);
  if (!item) return code;
  return locale === 'ru' ? item.labelRu : item.labelEn;
}
