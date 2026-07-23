import { FormEvent, useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { api } from '../api/api';
import { resolveApiError } from '../api/errors';
import { AuthHelpPanel } from '../components/AuthHelpPanel';
import { AuthOAuthButtons } from '../components/AuthOAuthButtons';
import { RecoveryKeyModal } from '../components/RecoveryKeyModal';
import { LocaleToggle } from '../components/LocaleToggle';
import { PasswordInput } from '../components/PasswordInput';
import { TermsPanel } from '../components/TermsPanel';
import { useAuth } from '../context/AuthContext';
import { useI18n } from '../context/I18nContext';
import { CaptchaResponse, AuthResponse } from '../types/api';

type Mode = 'login' | 'register' | 'recover';

const CAPTCHA_REFRESH_MS = 8 * 60 * 1000;

const OAUTH_ERROR_KEYS: Record<string, string> = {
  oauth_denied: 'errorOAuthDenied',
  oauth_missing_code: 'errorOAuthFailed',
  oauth_blocked: 'errorAccountBlocked',
  oauth_unavailable: 'errorOAuthUnavailable',
  oauth_failed: 'errorOAuthFailed',
  oauth_terms: 'errorOAuthTermsRequired',
};

export function AuthPage() {
  const [mode, setMode] = useState<Mode>('login');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [email, setEmail] = useState('');
  const [recoveryKey, setRecoveryKey] = useState('');
  const [captcha, setCaptcha] = useState<CaptchaResponse | null>(null);
  const [captchaAnswer, setCaptchaAnswer] = useState('');
  const [rememberMe, setRememberMe] = useState(true);
  const [termsAccepted, setTermsAccepted] = useState(false);
  const [usernameAvailable, setUsernameAvailable] = useState<boolean | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [pendingRecoveryKey, setPendingRecoveryKey] = useState<string | null>(null);
  const [pendingSession, setPendingSession] = useState<{ token: string; user: AuthResponse } | null>(null);

  const { t, locale, setLocale } = useI18n();
  const { login, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  const captchaRequired = mode === 'register' || mode === 'recover';

  const refreshCaptcha = useCallback(async () => {
    try {
      const nextCaptcha = await api.getCaptcha();
      setCaptcha(nextCaptcha);
      setCaptchaAnswer('');
    } catch (err) {
      setError(resolveApiError(err, t));
    }
  }, [t]);

  const switchMode = (nextMode: Mode) => {
    setMode(nextMode);
    setError('');
    setSuccess('');
    setCaptchaAnswer('');
    if (nextMode !== 'recover') {
      setRecoveryKey('');
    }
    if (nextMode === 'recover') {
      setPassword('');
    }
  };

  const canSubmit = useMemo(() => {
    const name = username.trim();
    if (!name || !password.trim()) return false;
    if (name.length < 3 || name.length > 50) return false;
    if (password.length < 8 || password.length > 100) return false;
    if (
      (mode === 'register' || mode === 'recover')
      && name.length > 0
      && password.toLowerCase() === name.toLowerCase()
    ) {
      return false;
    }
    if (captchaRequired && (!captchaAnswer.trim() || !captcha)) return false;
    if (mode === 'register') {
      if (!termsAccepted || usernameAvailable === false) return false;
    }
    if (mode === 'recover' && !recoveryKey.trim()) return false;
    return true;
  }, [captcha, captchaAnswer, captchaRequired, mode, password, recoveryKey, termsAccepted, username, usernameAvailable]);

  const validationHint = useMemo(() => {
    const name = username.trim();
    if (mode === 'register' && name.length > 0 && (name.length < 3 || name.length > 50)) {
      return t('errorUsernameLength');
    }
    if ((mode === 'register' || mode === 'login') && password.length > 0 && password.length < 8) {
      return t('errorPasswordLength');
    }
    if ((mode === 'register' || mode === 'login') && password.length > 100) {
      return t('errorPasswordLength');
    }
    if (mode === 'recover' && password.length > 0 && password.length < 8) {
      return t('errorNewPasswordLength');
    }
    if (mode === 'recover' && password.length > 100) {
      return t('errorNewPasswordLength');
    }
    if (mode === 'register' && usernameAvailable === false) {
      return t('errorUsernameTaken');
    }
    if (
      (mode === 'register' || mode === 'recover')
      && name.length > 0
      && password.length > 0
      && password.toLowerCase() === name.toLowerCase()
    ) {
      return t('errorPasswordSameAsUsername');
    }
    return '';
  }, [mode, password, t, username, usernameAvailable]);

  useEffect(() => {
    if (isAuthenticated) {
      navigate('/dashboard', { replace: true });
    }
  }, [isAuthenticated, navigate]);

  useEffect(() => {
    const oauthError = searchParams.get('oauthError');
    if (!oauthError) return;
    const key = OAUTH_ERROR_KEYS[oauthError] ?? 'errorOAuthFailed';
    setError(t(key));
    const next = new URLSearchParams(searchParams);
    next.delete('oauthError');
    setSearchParams(next, { replace: true });
  }, [searchParams, setSearchParams, t]);

  useEffect(() => {
    if (!captchaRequired) {
      setCaptcha(null);
      setCaptchaAnswer('');
      return;
    }
    void refreshCaptcha();
  }, [captchaRequired, refreshCaptcha]);

  useEffect(() => {
    if (!captchaRequired) return undefined;
    const timer = window.setInterval(() => void refreshCaptcha(), CAPTCHA_REFRESH_MS);
    return () => window.clearInterval(timer);
  }, [captchaRequired, refreshCaptcha]);

  useEffect(() => {
    if (mode !== 'register' || username.trim().length < 3) {
      setUsernameAvailable(null);
      return;
    }
    const timer = window.setTimeout(async () => {
      try {
        const status = await api.checkUsername(username.trim());
        setUsernameAvailable(status.available);
      } catch {
        setUsernameAvailable(null);
      }
    }, 300);
    return () => window.clearTimeout(timer);
  }, [mode, username]);

  async function onSubmit(event: FormEvent) {
    event.preventDefault();
    setError('');
    setSuccess('');
    if (captchaRequired && !captcha) return;

    setBusy(true);
    try {
      if (mode === 'login') {
        const user = await api.login({ username: username.trim(), password });
        login({ token: user.token, user }, rememberMe);
        setLocale(user.locale);
        navigate('/dashboard');
        return;
      }

      if (mode === 'register') {
        const result = await api.register({
          username: username.trim(),
          password,
          email: email.trim() || undefined,
          timezone: Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC',
          locale,
          termsAccepted,
          captchaId: captcha!.captchaId,
          captchaAnswer,
        });
        setPendingSession({ token: result.auth.token, user: result.auth });
        setPendingRecoveryKey(result.recoveryKey);
        return;
      }

      await api.recoverPassword({
        username: username.trim(),
        recoveryKey,
        newPassword: password,
        captchaId: captcha!.captchaId,
        captchaAnswer,
      });
      setSuccess(t('authRecoverSuccess'));
      switchMode('login');
    } catch (err) {
      const message = resolveApiError(err, t);
      setError(message);
      if (captchaRequired) {
        await refreshCaptcha();
        if (message === t('errorCaptchaExpired') || message === t('errorCaptchaMismatch')) {
          setError(`${message}. ${t('authCaptchaRefreshed')}`);
        }
      }
    } finally {
      setBusy(false);
    }
  }

  function confirmRecoveryKey() {
    if (!pendingSession) return;
    login(pendingSession, rememberMe);
    setLocale(pendingSession.user.locale);
    setPendingRecoveryKey(null);
    setPendingSession(null);
    navigate('/dashboard');
  }

  return (
    <div className="auth-page">
      {pendingRecoveryKey && <RecoveryKeyModal recoveryKey={pendingRecoveryKey} onConfirm={confirmRecoveryKey} />}
      <div className="auth-topbar">
        <LocaleToggle compact />
      </div>
      <div className="auth-layout">
        <div className="auth-card">
        <h1>{t('brand')}</h1>
        <p className="muted">{t('brandSub')}</p>
        <div className="auth-tabs">
          <button type="button" className={mode === 'login' ? 'active' : ''} onClick={() => switchMode('login')}>
            {t('authLoginTab')}
          </button>
          <button type="button" className={mode === 'register' ? 'active' : ''} onClick={() => switchMode('register')}>
            {t('authRegisterTab')}
          </button>
          <button type="button" className={mode === 'recover' ? 'active' : ''} onClick={() => switchMode('recover')}>
            {t('authRecoverTab')}
          </button>
        </div>

        <form onSubmit={onSubmit}>
          {mode === 'recover' && <p className="auth-recover-intro">{t('authRecoverIntro')}</p>}

          <label>{t('authUsername')}</label>
          <input value={username} onChange={(event) => setUsername(event.target.value)} autoComplete="username" />
          {mode === 'register' && <p className="field-hint">{t('authUsernameHint')}</p>}
          {mode === 'recover' && <p className="field-hint">{t('authRecoverUsernameHint')}</p>}

          {mode === 'register' && (
            <>
              <label>{t('authEmail')}</label>
              <input value={email} type="email" onChange={(event) => setEmail(event.target.value)} autoComplete="email" />
              <p className="field-hint">{t('authEmailHint')}</p>
            </>
          )}

          {mode === 'recover' && (
            <>
              <label>{t('authRecoveryKey')}</label>
              <input
                value={recoveryKey}
                onChange={(event) => setRecoveryKey(event.target.value)}
                autoComplete="off"
                spellCheck={false}
              />
              <p className="field-hint">{t('authRecoverKeyHint')}</p>
            </>
          )}

          <label>{mode === 'recover' ? t('authNewPassword') : t('authPassword')}</label>
          <PasswordInput
            value={password}
            onChange={setPassword}
            autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
          />
          {mode === 'register' && <p className="field-hint">{t('authPasswordHint')}</p>}
          {mode === 'recover' && <p className="field-hint">{t('authNewPasswordHint')}</p>}

          {(mode === 'login' || mode === 'register') && (
            <>
              <label className="remember">
                <input type="checkbox" checked={termsAccepted} onChange={(event) => setTermsAccepted(event.target.checked)} />
                <span>{t('authTerms')}</span>
              </label>
              {mode === 'register' && <TermsPanel />}
            </>
          )}

          {captchaRequired && (
            <>
              <label>{t('authCaptcha')}</label>
              <div className="field-row">
                <div className="captcha-question">{captcha?.question ?? t('loading')}</div>
                <button type="button" className="ghost" onClick={() => void refreshCaptcha()} title={t('authCaptchaRefresh')}>
                  ↻
                </button>
              </div>
              <input
                value={captchaAnswer}
                placeholder={t('authCaptchaPlaceholder')}
                onChange={(event) => setCaptchaAnswer(event.target.value)}
                inputMode="numeric"
              />
              <p className="field-hint">{t('authCaptchaHint')}</p>
            </>
          )}

          {(mode === 'login' || mode === 'register') && (
            <label className="remember">
              <input type="checkbox" checked={rememberMe} onChange={(event) => setRememberMe(event.target.checked)} />
              <span>{t('authRemember')}</span>
            </label>
          )}

          {mode === 'login' && (
            <button type="button" className="link-like" onClick={() => switchMode('recover')}>
              {t('authForgot')}
            </button>
          )}
          {mode === 'recover' && (
            <button type="button" className="link-like" onClick={() => switchMode('login')}>
              {t('authBackToLogin')}
            </button>
          )}

          {validationHint && <p className="error">{validationHint}</p>}
          {error && <p className="error">{error}</p>}
          {success && <p className="success">{success}</p>}

          <button type="submit" className="primary" disabled={busy || !canSubmit}>
            {busy
              ? t('loading')
              : mode === 'login'
                ? t('authLoginAction')
                : mode === 'register'
                  ? t('authRegisterAction')
                  : t('authRecoverAction')}
          </button>

          {(mode === 'login' || mode === 'register') && (
            <AuthOAuthButtons
              termsAccepted={termsAccepted}
              rememberMe={rememberMe}
              onRequireTerms={() => setError(t('errorOAuthTermsRequired'))}
            />
          )}
        </form>
        </div>
        <AuthHelpPanel />
      </div>
    </div>
  );
}
