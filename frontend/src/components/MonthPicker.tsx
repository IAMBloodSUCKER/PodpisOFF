import { useEffect, useId, useMemo, useRef, useState } from 'react';
import { useI18n } from '../context/I18nContext';
import { currentMonthValue, formatMonthLabel, parseMonthValue, toMonthValue } from '../utils/monthValue';
import { todayParts } from '../utils/datetime';

interface MonthPickerProps {
  id?: string;
  value: string;
  onChange: (value: string) => void;
  disabled?: boolean;
  className?: string;
}

export function MonthPicker({ id, value, onChange, disabled = false, className }: MonthPickerProps) {
  const { locale, t } = useI18n();
  const fallbackId = useId();
  const fieldId = id ?? fallbackId;
  const rootRef = useRef<HTMLDivElement>(null);

  const parsed = parseMonthValue(value);
  const today = todayParts();
  const [open, setOpen] = useState(false);
  const [viewYear, setViewYear] = useState(parsed?.year ?? today.year);

  useEffect(() => {
    const next = parseMonthValue(value);
    if (next) {
      setViewYear(next.year);
    }
  }, [value]);

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

  const monthLabels = useMemo(() => {
    const formatter = new Intl.DateTimeFormat(locale === 'ru' ? 'ru-RU' : 'en-US', { month: 'short' });
    return Array.from({ length: 12 }, (_, index) => formatter.format(new Date(2024, index, 1)));
  }, [locale]);

  const displayLabel = value ? formatMonthLabel(value, locale) : t('pickerPickMonth');

  function pickMonth(month: number) {
    onChange(toMonthValue(viewYear, month));
    setOpen(false);
  }

  function pickCurrentMonth() {
    onChange(currentMonthValue());
    setViewYear(today.year);
    setOpen(false);
  }

  function clearValue() {
    onChange('');
    setOpen(false);
  }

  function shiftYear(delta: number) {
    setViewYear((year) => year + delta);
  }

  return (
    <div className={`month-picker ${className ?? ''}`.trim()} ref={rootRef}>
      <button
        type="button"
        id={fieldId}
        className="month-picker-trigger"
        onClick={() => !disabled && setOpen((state) => !state)}
        disabled={disabled}
        aria-haspopup="dialog"
        aria-expanded={open}
      >
        <span className="month-picker-value">{displayLabel}</span>
        <span className="month-picker-icon" aria-hidden="true" />
      </button>
      {open && (
        <div className="month-picker-popover" role="dialog" aria-label={t('pickerPickMonth')}>
          <div className="dt-header">
            <button type="button" className="dt-nav" onClick={() => shiftYear(-1)} aria-label={t('pickerPrevYear')}>
              ‹
            </button>
            <span className="dt-title">{viewYear}</span>
            <button type="button" className="dt-nav" onClick={() => shiftYear(1)} aria-label={t('pickerNextYear')}>
              ›
            </button>
          </div>

          <div className="month-picker-grid">
            {monthLabels.map((label, index) => {
              const month = index + 1;
              const isSelected = parsed?.year === viewYear && parsed?.month === month;
              const isCurrent = today.year === viewYear && today.month === month;
              return (
                <button
                  key={label}
                  type="button"
                  className={['month-picker-month', isSelected ? 'selected' : '', isCurrent ? 'today' : ''].filter(Boolean).join(' ')}
                  onClick={() => pickMonth(month)}
                >
                  {label.replace(/\.$/, '')}
                </button>
              );
            })}
          </div>

          <div className="dt-footer">
            <button type="button" className="link-like" onClick={clearValue}>
              {t('pickerClear')}
            </button>
            <button type="button" className="link-like" onClick={pickCurrentMonth}>
              {t('pickerThisMonth')}
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
