import { useEffect, useId, useMemo, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { SUPPORTED_CURRENCIES, currencyLabel, isSupportedCurrency } from '../i18n/currencies';
import { useI18n } from '../context/I18nContext';
import { SETTINGS_PLANS_PATH } from '../utils/settingsPaths';

interface CurrencyPickerProps {
  value: string;
  onChange: (code: string) => void;
  foreignCurrencyAllowed: boolean;
  isEdit?: boolean;
}

export function CurrencyPicker({
  value,
  onChange,
  foreignCurrencyAllowed,
  isEdit = false,
}: CurrencyPickerProps) {
  const { t, locale } = useI18n();
  const fallbackId = useId();
  const fieldId = fallbackId;
  const rootRef = useRef<HTMLDivElement>(null);
  const [open, setOpen] = useState(false);
  const [proModalOpen, setProModalOpen] = useState(false);

  const normalizedValue = value.trim().toUpperCase() || 'RUB';

  const options = useMemo(() => {
    if (normalizedValue && !isSupportedCurrency(normalizedValue)) {
      return [{ code: normalizedValue, labelRu: normalizedValue, labelEn: normalizedValue }, ...SUPPORTED_CURRENCIES];
    }
    return [...SUPPORTED_CURRENCIES];
  }, [normalizedValue]);

  const displayLabel = isSupportedCurrency(normalizedValue)
    ? currencyLabel(normalizedValue as (typeof SUPPORTED_CURRENCIES)[number]['code'], locale)
    : normalizedValue;

  function isSelectable(code: string): boolean {
    if (foreignCurrencyAllowed) return true;
    if (code === 'RUB') return true;
    if (isEdit && code === normalizedValue) return true;
    return false;
  }

  useEffect(() => {
    if (!open) return;
    function onPointerDown(event: MouseEvent) {
      if (!rootRef.current?.contains(event.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener('mousedown', onPointerDown);
    return () => document.removeEventListener('mousedown', onPointerDown);
  }, [open]);

  function pickCurrency(code: string) {
    if (!isSelectable(code)) {
      setProModalOpen(true);
      return;
    }
    onChange(code);
    setOpen(false);
  }

  return (
    <>
      <div className="currency-picker" ref={rootRef}>
        <button
          type="button"
          id={fieldId}
          className="currency-picker-trigger"
          onClick={() => setOpen((state) => !state)}
          aria-haspopup="listbox"
          aria-expanded={open}
        >
          <span className="currency-picker-value">{displayLabel}</span>
          <span className="currency-picker-chevron" aria-hidden="true" />
        </button>
        {open && (
          <ul className="currency-picker-list" role="listbox" aria-labelledby={fieldId}>
            {options.map((item) => {
              const selected = item.code === normalizedValue;
              const selectable = isSelectable(item.code);
              const label = locale === 'ru' ? item.labelRu : item.labelEn;
              return (
                <li key={item.code} role="presentation">
                  <button
                    type="button"
                    role="option"
                    aria-selected={selected}
                    className={`currency-picker-option${selected ? ' selected' : ''}${selectable ? '' : ' locked'}`}
                    onClick={() => pickCurrency(item.code)}
                  >
                    <span>{label}</span>
                    {!selectable && <span className="currency-picker-pro-badge">PRO</span>}
                  </button>
                </li>
              );
            })}
          </ul>
        )}
      </div>

      {proModalOpen && (
        <div
          className="modal-backdrop"
          onClick={() => setProModalOpen(false)}
          role="dialog"
          aria-modal="true"
          aria-labelledby="currency-pro-modal-title"
        >
          <div className="modal-card confirm-modal" onClick={(event) => event.stopPropagation()}>
            <h3 id="currency-pro-modal-title">{t('formCurrencyProTitle')}</h3>
            <p className="muted">{t('formCurrencyProBody')}</p>
            <div className="actions confirm-modal-actions">
              <button type="button" className="ghost" onClick={() => setProModalOpen(false)}>
                {t('cancel')}
              </button>
              <Link to={SETTINGS_PLANS_PATH} className="primary" onClick={() => setProModalOpen(false)}>
                {t('planLimitCta')}
              </Link>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
