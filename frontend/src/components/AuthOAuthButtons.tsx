import { useEffect, useState } from 'react';
import { useI18n } from '../context/I18nContext';

const API_BASE = import.meta.env.VITE_API_URL ?? '';
const REMEMBER_KEY = 'podpisoff.oauth.remember';

interface AuthOAuthButtonsProps {
  termsAccepted: boolean;
  rememberMe: boolean;
  onRequireTerms: () => void;
}

export function AuthOAuthButtons({ termsAccepted, rememberMe, onRequireTerms }: AuthOAuthButtonsProps) {
  const { t, locale } = useI18n();
  const [yandexEnabled, setYandexEnabled] = useState(false);

  useEffect(() => {
    let cancelled = false;
    fetch(`${API_BASE}/api/auth/oauth/providers`, { cache: 'no-store' })
      .then(async (res) => {
        if (!res.ok) return null;
        return res.json() as Promise<{ yandex?: boolean }>;
      })
      .then((data) => {
        if (!cancelled && data?.yandex) {
          setYandexEnabled(true);
        }
      })
      .catch(() => {
        /* keep disabled */
      });
    return () => {
      cancelled = true;
    };
  }, []);

  function startYandex() {
    if (!termsAccepted) {
      onRequireTerms();
      return;
    }
    const timezone = Intl.DateTimeFormat().resolvedOptions().timeZone || 'Europe/Moscow';
    const params = new URLSearchParams({
      locale: locale === 'en' ? 'EN' : 'RU',
      timezone,
      termsAccepted: 'true',
    });
    sessionStorage.setItem(REMEMBER_KEY, rememberMe ? '1' : '0');
    window.location.href = `${API_BASE}/api/auth/oauth/yandex/start?${params.toString()}`;
  }

  return (
    <div className="auth-oauth">
      <p className="auth-oauth-divider">
        <span>{t('authOAuthDivider')}</span>
      </p>
      <div className="auth-oauth-grid">
        <button
          type="button"
          className="oauth-btn"
          disabled
          title={t('authOAuthSoon')}
          aria-disabled="true"
        >
          <span className="oauth-icon oauth-icon-google" aria-hidden="true" />
          <span>{t('authOAuthGoogle')}</span>
          <span className="oauth-soon">{t('authOAuthSoon')}</span>
        </button>

        <button
          type="button"
          className="oauth-btn"
          disabled={!yandexEnabled}
          title={yandexEnabled ? t('authOAuthYandex') : t('authOAuthSoon')}
          onClick={yandexEnabled ? startYandex : undefined}
          aria-disabled={!yandexEnabled}
        >
          <span className="oauth-icon oauth-icon-yandex" aria-hidden="true" />
          <span>{t('authOAuthYandex')}</span>
          {!yandexEnabled && <span className="oauth-soon">{t('authOAuthSoon')}</span>}
        </button>

        <button
          type="button"
          className="oauth-btn"
          disabled
          title={t('authOAuthSoon')}
          aria-disabled="true"
        >
          <span className="oauth-icon oauth-icon-github" aria-hidden="true" />
          <span>{t('authOAuthGithub')}</span>
          <span className="oauth-soon">{t('authOAuthSoon')}</span>
        </button>
      </div>
      {yandexEnabled && (
        <p className="field-hint auth-oauth-hint">{t('authOAuthYandexHint')}</p>
      )}
    </div>
  );
}

export { REMEMBER_KEY as OAUTH_REMEMBER_KEY };
