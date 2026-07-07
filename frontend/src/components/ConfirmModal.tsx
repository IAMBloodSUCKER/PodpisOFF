import { useI18n } from '../context/I18nContext';

type ConfirmModalProps = {
  open: boolean;
  title: string;
  message?: string;
  confirmLabel?: string;
  cancelLabel?: string;
  danger?: boolean;
  busy?: boolean;
  onConfirm: () => void;
  onCancel: () => void;
};

export function ConfirmModal({
  open,
  title,
  message,
  confirmLabel,
  cancelLabel,
  danger = false,
  busy = false,
  onConfirm,
  onCancel,
}: ConfirmModalProps) {
  const { t } = useI18n();

  if (!open) {
    return null;
  }

  return (
    <div className="modal-backdrop" onClick={onCancel} role="dialog" aria-modal="true" aria-labelledby="confirm-modal-title">
      <div className="modal-card confirm-modal" onClick={(event) => event.stopPropagation()}>
        <h3 id="confirm-modal-title">{title}</h3>
        {message && <p className="muted">{message}</p>}
        <div className="actions confirm-modal-actions">
          <button type="button" className="ghost" onClick={onCancel} disabled={busy}>
            {cancelLabel ?? t('cancel')}
          </button>
          <button type="button" className={danger ? 'danger' : 'primary'} onClick={onConfirm} disabled={busy}>
            {confirmLabel ?? t('delete')}
          </button>
        </div>
      </div>
    </div>
  );
}
