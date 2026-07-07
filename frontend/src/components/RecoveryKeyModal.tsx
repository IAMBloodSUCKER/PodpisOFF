import { useI18n } from '../context/I18nContext';

interface RecoveryKeyModalProps {
  recoveryKey: string;
  onConfirm: () => void;
}

export function RecoveryKeyModal({ recoveryKey, onConfirm }: RecoveryKeyModalProps) {
  const { t } = useI18n();

  async function copyKey() {
    try {
      await navigator.clipboard.writeText(recoveryKey);
    } catch {
      // clipboard may be unavailable
    }
  }

  return (
    <div className="modal-backdrop" role="dialog" aria-modal="true">
      <div className="modal-card">
        <h2>{t('authRecoveryKeyTitle')}</h2>
        <p className="muted">{t('authRecoveryKeyWarning')}</p>
        <code className="recovery-key">{recoveryKey}</code>
        <div className="actions">
          <button type="button" className="ghost" onClick={() => void copyKey()}>
            {t('authRecoveryKeyCopy')}
          </button>
          <button type="button" className="primary" onClick={onConfirm}>
            {t('authRecoveryKeyConfirm')}
          </button>
        </div>
      </div>
    </div>
  );
}
