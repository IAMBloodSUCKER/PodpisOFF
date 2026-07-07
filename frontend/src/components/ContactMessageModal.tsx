import { FormEvent, useState } from 'react';
import { api } from '../api/api';
import { useI18n } from '../context/I18nContext';
import { resolveApiError } from '../api/errors';
import { FeedbackKind } from '../types/api';

type ContactMessageModalProps = {
  open: boolean;
  kind: FeedbackKind;
  onClose: () => void;
  onSuccess?: () => void;
};

export function ContactMessageModal({ open, kind, onClose, onSuccess }: ContactMessageModalProps) {
  const { t } = useI18n();
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [sending, setSending] = useState(false);
  const [sent, setSent] = useState(false);

  const isSupport = kind === 'SUPPORT';
  const titleKey = isSupport ? 'supportTitle' : 'feedbackTitle';
  const hintKey = isSupport ? 'supportHint' : 'feedbackHint';
  const placeholderKey = isSupport ? 'supportPlaceholder' : 'feedbackPlaceholder';
  const sendKey = isSupport ? 'supportSend' : 'feedbackSend';
  const successKey = isSupport ? 'supportSuccess' : 'feedbackSuccess';

  if (!open) {
    return null;
  }

  async function onSubmit(event: FormEvent) {
    event.preventDefault();
    setError('');
    setSending(true);
    try {
      await api.submitFeedback(message.trim(), kind);
      setSent(true);
      setMessage('');
      onSuccess?.();
    } catch (err) {
      setError(resolveApiError(err, t));
    } finally {
      setSending(false);
    }
  }

  function close() {
    setSent(false);
    setError('');
    onClose();
  }

  return (
    <div className="modal-backdrop" onClick={close} role="dialog" aria-modal="true">
      <div className="modal-card feedback-modal" onClick={(event) => event.stopPropagation()}>
        <h3>{t(titleKey)}</h3>
        <p className="muted">{t(hintKey)}</p>
        {sent ? (
          <p className="success">{t(successKey)}</p>
        ) : (
          <form className="stack" onSubmit={(event) => void onSubmit(event)}>
            <textarea
              value={message}
              onChange={(event) => setMessage(event.target.value)}
              rows={5}
              maxLength={2000}
              placeholder={t(placeholderKey)}
              required
            />
            {error && <p className="error">{error}</p>}
            <div className="actions">
              <button type="button" className="ghost" onClick={close}>
                {t('cancel')}
              </button>
              <button type="submit" className="primary" disabled={sending || message.trim().length < 5}>
                {t(sendKey)}
              </button>
            </div>
          </form>
        )}
        {sent && (
          <button type="button" className="primary" onClick={close}>
            {t('close')}
          </button>
        )}
      </div>
    </div>
  );
}
