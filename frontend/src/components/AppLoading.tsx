import { useI18n } from '../context/I18nContext';

export function AppLoading() {
  const { t } = useI18n();

  return (
    <div className="app-loading" role="status" aria-live="polite">
      <div className="app-loading-card">
        <span className="app-loading-brand">{t('brand')}</span>
        <span className="app-loading-spinner" aria-hidden="true" />
        <span className="muted">{t('loading')}</span>
      </div>
    </div>
  );
}
