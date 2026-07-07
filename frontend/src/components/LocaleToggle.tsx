import { useI18n } from '../context/I18nContext';
import { LocaleCode } from '../types/api';

interface LocaleToggleProps {
  compact?: boolean;
}

export function LocaleToggle({ compact = false }: LocaleToggleProps) {
  const { locale, setLocale } = useI18n();

  function pick(next: LocaleCode) {
    setLocale(next);
  }

  return (
    <div className={compact ? 'locale-toggle compact' : 'locale-toggle'} role="group" aria-label="Language">
      <button type="button" className={locale === 'ru' ? 'active' : ''} onClick={() => pick('ru')}>
        RU
      </button>
      <button type="button" className={locale === 'en' ? 'active' : ''} onClick={() => pick('en')}>
        EN
      </button>
    </div>
  );
}
