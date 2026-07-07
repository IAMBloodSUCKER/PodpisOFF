import { ContactMessageModal } from './ContactMessageModal';
import { useI18n } from '../context/I18nContext';

type FeedbackModalProps = {
  open: boolean;
  onClose: () => void;
  onSuccess?: () => void;
};

export function FeedbackModal({ open, onClose, onSuccess }: FeedbackModalProps) {
  return <ContactMessageModal open={open} kind="FEEDBACK" onClose={onClose} onSuccess={onSuccess} />;
}

export function FeedbackTopButton({ onClick }: { onClick: () => void }) {
  const { t } = useI18n();
  return (
    <button type="button" className="feedback-top-btn" onClick={onClick} title={t('feedbackTitle')}>
      <span className="feedback-top-btn-long">{t('feedbackTop')}</span>
      <span className="feedback-top-btn-short">{t('feedbackFab')}</span>
    </button>
  );
}

export function FeedbackFab({ onClick }: { onClick: () => void }) {
  const { t } = useI18n();
  return (
    <button type="button" className="feedback-fab" onClick={onClick} title={t('feedbackTitle')}>
      {t('feedbackFab')}
    </button>
  );
}
