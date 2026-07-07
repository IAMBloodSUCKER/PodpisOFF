import { useCallback, useEffect, useMemo, useState } from 'react';
import { api } from '../api/api';
import { resolveApiError } from '../api/errors';
import { useAuth } from '../context/AuthContext';
import { useI18n } from '../context/I18nContext';
import { TelegramLinkResponse, TelegramLinkStatus } from '../types/api';

function qrImageUrl(deepLink: string): string {
  return `https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=${encodeURIComponent(deepLink)}`;
}

export function TelegramConnectPanel() {
  const { t } = useI18n();
  const { user, updateUser } = useAuth();
  const [termsAccepted, setTermsAccepted] = useState(false);
  const [status, setStatus] = useState<TelegramLinkStatus | null>(null);
  const [link, setLink] = useState<TelegramLinkResponse | null>(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [waiting, setWaiting] = useState(false);

  const loadStatus = useCallback(async () => {
    try {
      const next = await api.telegramLinkStatus();
      setStatus(next);
      if (next.linked) {
        setWaiting(false);
        setLink(null);
        const me = await api.me();
        updateUser(me);
      }
      return next;
    } catch (err) {
      setError(resolveApiError(err, t));
      return null;
    }
  }, [updateUser, t]);

  useEffect(() => {
    void loadStatus();
  }, [loadStatus]);

  useEffect(() => {
    if (!waiting) return;
    const timer = window.setInterval(() => {
      void loadStatus();
    }, 2500);
    return () => window.clearInterval(timer);
  }, [waiting, loadStatus]);

  const linked = status?.linked || Boolean(user?.telegramChatId && user.telegramNotificationsEnabled);
  const deepLink = link?.deepLink ?? status?.deepLink ?? null;
  const botUsername = link?.botUsername ?? status?.botUsername ?? null;

  const botLabel = useMemo(() => (botUsername ? `@${botUsername}` : t('settingsTelegramBotFallback')), [botUsername, t]);

  async function onConnect() {
    if (!termsAccepted) return;
    setBusy(true);
    setError('');
    try {
      const nextLink = await api.createTelegramLink();
      setLink(nextLink);
      setWaiting(true);
      await loadStatus();
    } catch (err) {
      setError(resolveApiError(err, t));
    } finally {
      setBusy(false);
    }
  }

  async function onDisconnect() {
    setBusy(true);
    setError('');
    try {
      const me = await api.disconnectTelegram();
      setLink(null);
      setWaiting(false);
      updateUser(me);
      await loadStatus();
    } catch (err) {
      setError(resolveApiError(err, t));
    } finally {
      setBusy(false);
    }
  }

  if (!status && !error) {
    return <p className="muted">{t('loading')}</p>;
  }

  return (
    <div className="telegram-connect stack">
      {linked ? (
        <>
          <p className="success">{t('settingsTelegramConnected', { bot: botLabel })}</p>
          <button type="button" className="ghost" disabled={busy} onClick={() => void onDisconnect()}>
            {t('settingsTelegramDisconnect')}
          </button>
        </>
      ) : (
        <>
          <p className="muted">{t('settingsTelegramIntro')}</p>
          <label className="settings-channel-toggle telegram-terms">
            <input
              type="checkbox"
              checked={termsAccepted}
              onChange={(event) => setTermsAccepted(event.target.checked)}
            />
            <span>{t('settingsTelegramTerms', { bot: botLabel })}</span>
          </label>

          {!deepLink ? (
            <button
              type="button"
              className="primary"
              disabled={busy || !termsAccepted || !status?.botUsername}
              onClick={() => void onConnect()}
            >
              {t('settingsTelegramConnect')}
            </button>
          ) : (
            <div className="telegram-connect-panel card stack">
              <h4>{t('settingsTelegramConnectTitle')}</h4>
              <ol className="telegram-connect-steps">
                <li>{t('settingsTelegramStep1')}</li>
                <li>{t('settingsTelegramStep2')}</li>
                <li>{t('settingsTelegramStep3')}</li>
              </ol>
              <div className="telegram-connect-qr-row">
                <img
                  className="telegram-connect-qr"
                  src={qrImageUrl(deepLink)}
                  width={200}
                  height={200}
                  alt={t('settingsTelegramQrAlt')}
                />
                <div className="stack">
                  <a className="primary telegram-connect-link" href={deepLink} target="_blank" rel="noopener noreferrer">
                    {t('settingsTelegramOpenBot')}
                  </a>
                  <p className="muted telegram-connect-waiting">
                    {waiting ? t('settingsTelegramWaiting') : t('settingsTelegramAfterStart')}
                  </p>
                  <button type="button" className="ghost" disabled={busy} onClick={() => void onConnect()}>
                    {t('settingsTelegramNewLink')}
                  </button>
                </div>
              </div>
            </div>
          )}
        </>
      )}

      {status && !status.botUsername && (
        <p className="field-hint">{t('settingsChannelsTelegramUnavailable')}</p>
      )}
      {error && <p className="error">{error}</p>}
    </div>
  );
}
