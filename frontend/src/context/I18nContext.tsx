import { createContext, useContext, useMemo, useState } from 'react';
import { translations } from '../i18n/translations';
import { LocaleCode } from '../types/api';

const LOCALE_KEY = 'podpisoff.locale';

interface I18nState {
  locale: LocaleCode;
  t: (key: string) => string;
  setLocale: (locale: LocaleCode) => void;
}

const I18nContext = createContext<I18nState | null>(null);

function readLocale(): LocaleCode {
  const raw = localStorage.getItem(LOCALE_KEY);
  return raw === 'en' ? 'en' : 'ru';
}

export function I18nProvider({ children }: { children: React.ReactNode }) {
  const [locale, setLocaleState] = useState<LocaleCode>(readLocale);

  const value = useMemo<I18nState>(
    () => ({
      locale,
      t: (key: string) => translations[locale][key] ?? key,
      setLocale: (nextLocale: LocaleCode) => {
        setLocaleState(nextLocale);
        localStorage.setItem(LOCALE_KEY, nextLocale);
      },
    }),
    [locale],
  );

  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>;
}

export function useI18n() {
  const ctx = useContext(I18nContext);
  if (!ctx) {
    throw new Error('useI18n must be used inside I18nProvider');
  }
  return ctx;
}
