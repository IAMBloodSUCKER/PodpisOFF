import { useI18n } from '../context/I18nContext';

const PROVIDERS = [
  { id: 'google', labelKey: 'authOAuthGoogle' },
  { id: 'yandex', labelKey: 'authOAuthYandex' },
  { id: 'github', labelKey: 'authOAuthGithub' },
] as const;

export function AuthOAuthButtons() {
  const { t } = useI18n();

  return (
    <div className="auth-oauth">
      <p className="auth-oauth-divider">
        <span>{t('authOAuthDivider')}</span>
      </p>
      <div className="auth-oauth-grid">
        {PROVIDERS.map((provider) => (
          <button
            key={provider.id}
            type="button"
            className="oauth-btn"
            disabled
            title={t('authOAuthSoon')}
            aria-disabled="true"
          >
            <span className={`oauth-icon oauth-icon-${provider.id}`} aria-hidden="true" />
            <span>{t(provider.labelKey)}</span>
            <span className="oauth-soon">{t('authOAuthSoon')}</span>
          </button>
        ))}
      </div>
    </div>
  );
}
