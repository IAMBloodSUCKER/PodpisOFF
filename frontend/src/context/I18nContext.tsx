import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { translations } from '../i18n/translations';
import { LocaleCode } from '../types/api';

const LOCALE_KEY = 'podpisoff.locale';

interface I18nState {
  locale: LocaleCode;
  t: (key: string, vars?: Record<string, string>) => string;
  setLocale: (locale: LocaleCode) => void;
}

const I18nContext = createContext<I18nState | null>(null);

function readLocale(): LocaleCode {
  const raw = localStorage.getItem(LOCALE_KEY);
  return raw === 'en' ? 'en' : 'ru';
}

export function I18nProvider({ children }: { children: React.ReactNode }) {
  const [locale, setLocaleState] = useState<LocaleCode>(readLocale);

  useEffect(() => {
    document.documentElement.lang = locale;
  }, [locale]);

  const setLocale = useCallback((nextLocale: LocaleCode) => {
    setLocaleState(nextLocale);
    localStorage.setItem(LOCALE_KEY, nextLocale);
  }, []);

  const value = useMemo<I18nState>(
    () => ({
      locale,
      t: (key: string, vars?: Record<string, string>) => {
        let text = translations[locale][key] ?? key;
        if (vars) {
          for (const [name, value] of Object.entries(vars)) {
            text = text.replaceAll(`{${name}}`, value);
          }
        }
        return text;
      },
      setLocale,
    }),
    [locale, setLocale],
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
