import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { api } from '../api/api';
import { resolveApiError } from '../api/errors';
import { OAUTH_REMEMBER_KEY } from '../components/AuthOAuthButtons';
import { useAuth } from '../context/AuthContext';
import { useI18n } from '../context/I18nContext';

export function OAuthCallbackPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { login } = useAuth();
  const { t, setLocale } = useI18n();
  const [error, setError] = useState('');

  useEffect(() => {
    const ticket = searchParams.get('ticket');
    if (!ticket) {
      setError(t('errorOAuthFailed'));
      return;
    }

    let cancelled = false;
    api
      .exchangeOAuthTicket(ticket)
      .then((user) => {
        if (cancelled) return;
        const rememberMe = sessionStorage.getItem(OAUTH_REMEMBER_KEY) !== '0';
        sessionStorage.removeItem(OAUTH_REMEMBER_KEY);
        setLocale(user.locale);
        login({ token: user.token, user }, rememberMe);
        navigate('/dashboard', { replace: true });
      })
      .catch((err) => {
        if (cancelled) return;
        sessionStorage.removeItem(OAUTH_REMEMBER_KEY);
        setError(resolveApiError(err, t));
      });

    return () => {
      cancelled = true;
    };
  }, [login, navigate, searchParams, setLocale, t]);

  if (error) {
    return (
      <section className="auth-page">
        <div className="card stack auth-card">
          <p className="error">{error}</p>
          <button type="button" className="primary" onClick={() => navigate('/auth', { replace: true })}>
            {t('authBackToLogin')}
          </button>
        </div>
      </section>
    );
  }

  return (
    <section className="auth-page">
      <div className="card stack auth-card">
        <p className="muted">{t('authOAuthCompleting')}</p>
      </div>
    </section>
  );
}
