import { useI18n } from '../context/I18nContext';

export function AuthHelpPanel() {
  const { t, locale } = useI18n();

  return (
    <aside className="auth-help" aria-label={t('authHelpTitle')}>
      <h2>{t('authHelpTitle')}</h2>
      <p>{locale === 'ru' ? t('authHelpBodyRu') : t('authHelpBodyEn')}</p>
      <ul className="auth-help-list">
        <li>{t('authHelpPointLogin')}</li>
        <li>{t('authHelpPointRecovery')}</li>
        <li>{t('authHelpPointOAuth')}</li>
      </ul>
      <p className="field-hint">{t('authHelpFooter')}</p>
    </aside>
  );
}
