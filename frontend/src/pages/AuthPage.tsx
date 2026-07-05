import { FormEvent, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api/api';
import { useAuth } from '../context/AuthContext';
import { useI18n } from '../context/I18nContext';
import { CaptchaResponse } from '../types/api';
import { generateRecoveryKey } from '../utils/format';

type Mode = 'login' | 'register' | 'recover';

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

  const { t, locale } = useI18n();
  const { login, isAuthenticated } = useAuth();
  const navigate = useNavigate();

  const canSubmit = useMemo(() => {
    if (!username.trim() || !password.trim() || !captchaAnswer.trim() || !captcha) return false;
    if (mode === 'register' && !termsAccepted) return false;
    if (mode === 'recover' && !recoveryKey.trim()) return false;
    return true;
  }, [captcha, captchaAnswer, mode, password, recoveryKey, termsAccepted, username]);

  useEffect(() => {
    if (isAuthenticated) {
      navigate('/dashboard', { replace: true });
    }
  }, [isAuthenticated, navigate]);

  async function refreshCaptcha() {
    try {
      const nextCaptcha = await api.getCaptcha();
      setCaptcha(nextCaptcha);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Captcha error');
    }
  }

  useEffect(() => {
    void refreshCaptcha();
  }, []);

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
    if (!captcha) return;

    setBusy(true);
    try {
      if (mode === 'login') {
        const user = await api.login({ username: username.trim(), password });
        login({ token: user.token, user }, rememberMe);
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
          captchaId: captcha.captchaId,
          captchaAnswer,
        });
        login({ token: result.auth.token, user: result.auth }, rememberMe);
        setSuccess(`${t('authRecoveryKeySaved')}: ${result.recoveryKey}`);
        navigate('/dashboard');
        return;
      }

      await api.recoverPassword({
        username: username.trim(),
        recoveryKey,
        newPassword: password,
        captchaId: captcha.captchaId,
        captchaAnswer,
      });
      setSuccess('Password updated. Please login.');
      setMode('login');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Request failed');
    } finally {
      setBusy(false);
      setCaptchaAnswer('');
      void refreshCaptcha();
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-card">
        <h1>{t('brand')}</h1>
        <p className="muted">{t('brandSub')}</p>
        <div className="auth-tabs">
          <button type="button" className={mode === 'login' ? 'active' : ''} onClick={() => setMode('login')}>
            {t('authLoginTab')}
          </button>
          <button type="button" className={mode === 'register' ? 'active' : ''} onClick={() => setMode('register')}>
            {t('authRegisterTab')}
          </button>
          <button type="button" className={mode === 'recover' ? 'active' : ''} onClick={() => setMode('recover')}>
            {t('authRecoverTab')}
          </button>
        </div>

        <form onSubmit={onSubmit}>
          <label>{t('authUsername')}</label>
          <input value={username} onChange={(event) => setUsername(event.target.value)} />

          {mode === 'register' && (
            <>
              <label>{t('authEmail')}</label>
              <input value={email} type="email" onChange={(event) => setEmail(event.target.value)} />
            </>
          )}

          <label>{t('authPassword')}</label>
          <input type="password" value={password} onChange={(event) => setPassword(event.target.value)} />

          {mode === 'recover' && (
            <>
              <label>{t('authRecoveryKey')}</label>
              <div className="field-row">
                <input value={recoveryKey} onChange={(event) => setRecoveryKey(event.target.value)} />
                <button type="button" className="ghost" onClick={() => setRecoveryKey(generateRecoveryKey(username))}>
                  {t('authGenerateKey')}
                </button>
              </div>
            </>
          )}

          {mode === 'register' && (
            <>
              <label className="remember">
                <input type="checkbox" checked={termsAccepted} onChange={(event) => setTermsAccepted(event.target.checked)} />
                <span>{t('authTerms')}</span>
              </label>
              {usernameAvailable === false && <p className="error">Username is already used</p>}
            </>
          )}

          <label>{t('authCaptcha')}</label>
          <div className="field-row">
            <div className="captcha-question">{captcha?.question ?? t('loading')}</div>
            <button type="button" className="ghost" onClick={() => void refreshCaptcha()}>
              ↻
            </button>
          </div>
          <input
            value={captchaAnswer}
            placeholder={t('authCaptchaPlaceholder')}
            onChange={(event) => setCaptchaAnswer(event.target.value)}
          />

          {(mode === 'login' || mode === 'register') && (
            <label className="remember">
              <input type="checkbox" checked={rememberMe} onChange={(event) => setRememberMe(event.target.checked)} />
              <span>{t('authRemember')}</span>
            </label>
          )}

          {mode === 'login' && (
            <button type="button" className="link-like" onClick={() => setMode('recover')}>
              {t('authForgot')}
            </button>
          )}
          {mode === 'recover' && (
            <button type="button" className="link-like" onClick={() => setMode('login')}>
              {t('authBackToLogin')}
            </button>
          )}

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
        </form>
      </div>
    </div>
  );
}
