import { useI18n } from '../context/I18nContext';

interface OffToggleProps {
  active: boolean;
  onChange: (next: boolean) => void;
}

export function OffToggle({ active, onChange }: OffToggleProps) {
  const { t } = useI18n();

  return (
    <button
      type="button"
      className={`off-toggle ${active ? 'is-on' : 'is-off'}`}
      onClick={() => onChange(!active)}
      aria-pressed={active}
    >
      <span className="off-toggle-pill" />
      <span className="off-toggle-label">{active ? t('toggleOn') : t('toggleOff')}</span>
    </button>
  );
}
