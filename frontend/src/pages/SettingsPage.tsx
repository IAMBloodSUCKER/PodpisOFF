import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api/api';
import { resolveApiError } from '../api/errors';
import { useAuth } from '../context/AuthContext';
import { useI18n } from '../context/I18nContext';
import { BillingStatus } from '../types/api';

export function SettingsPage() {
  const [billing, setBilling] = useState<BillingStatus | null>(null);
  const [error, setError] = useState('');
  const { locale, setLocale, t } = useI18n();
  const { logout } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    api
      .billingStatus()
      .then(setBilling)
      .catch((err) => setError(resolveApiError(err, t)));
  }, []);

  function onLogout() {
    logout();
    navigate('/auth');
  }

  return (
    <section className="stack">
      <h2>{t('settingsTitle')}</h2>
      <article className="card stack">
        <label>{t('settingsLocale')}</label>
        <div className="filters">
          <button type="button" className={locale === 'ru' ? 'active' : ''} onClick={() => setLocale('ru')}>
            RU
          </button>
          <button type="button" className={locale === 'en' ? 'active' : ''} onClick={() => setLocale('en')}>
            EN
          </button>
        </div>

        <h3>{t('settingsPlan')}</h3>
        {billing ? (
          <p>
            {billing.plan}
            {billing.planExpiresAt ? ` until ${new Date(billing.planExpiresAt).toLocaleDateString()}` : ''}
          </p>
        ) : (
          <p className="muted">{t('loading')}</p>
        )}

        {error && <p className="error">{error}</p>}

        <button type="button" className="danger" onClick={onLogout}>
          {t('settingsLogout')}
        </button>
      </article>
    </section>
  );
}
