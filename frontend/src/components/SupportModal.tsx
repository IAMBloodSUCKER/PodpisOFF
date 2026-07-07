import { ContactMessageModal } from './ContactMessageModal';
import { useI18n } from '../context/I18nContext';

type SupportModalProps = {
  open: boolean;
  onClose: () => void;
};

export function SupportModal({ open, onClose }: SupportModalProps) {
  return <ContactMessageModal open={open} kind="SUPPORT" onClose={onClose} />;
}

export function SupportButton({ onClick }: { onClick: () => void }) {
  const { t } = useI18n();
  return (
    <button type="button" className="ghost settings-support-btn" onClick={onClick}>
      {t('supportButton')}
    </button>
  );
}
