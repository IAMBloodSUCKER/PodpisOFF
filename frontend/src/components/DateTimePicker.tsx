import { useEffect, useMemo, useState } from 'react';
import { useI18n } from '../context/I18nContext';
import {
  daysInMonth,
  firstWeekdayOfMonth,
  parseDateIso,
  parseDateTimeLocal,
  todayParts,
  toDateIso,
  toDateTimeLocalIso,
} from '../utils/datetime';

interface DateTimePickerProps {
  value: string;
  onChange: (value: string) => void;
  includeTime?: boolean;
}

interface SelectedParts {
  year: number;
  month: number;
  day: number;
  hour: number;
  minute: number;
}

function readParts(value: string, includeTime: boolean): SelectedParts | null {
  if (!value) return null;
  if (includeTime) {
    const parsed = parseDateTimeLocal(value);
    if (parsed) return parsed;
  }
  const parsed = parseDateIso(value);
  if (!parsed) return null;
  return { ...parsed, hour: 9, minute: 0 };
}

export function DateTimePicker({ value, onChange, includeTime = true }: DateTimePickerProps) {
  const { locale, t } = useI18n();
  const weekStartsOnMonday = locale === 'ru';
  const today = todayParts();

  const initial = readParts(value, includeTime);
  const [viewYear, setViewYear] = useState(initial?.year ?? today.year);
  const [viewMonth, setViewMonth] = useState(initial?.month ?? today.month);
  const [selected, setSelected] = useState<SelectedParts | null>(initial);
  const [open, setOpen] = useState(false);

  useEffect(() => {
    const parsed = readParts(value, includeTime);
    setSelected(parsed);
    if (parsed) {
      setViewYear(parsed.year);
      setViewMonth(parsed.month);
    }
  }, [value, includeTime]);

  const monthLabel = useMemo(() => {
    const formatter = new Intl.DateTimeFormat(locale === 'ru' ? 'ru-RU' : 'en-US', {
      month: 'long',
      year: 'numeric',
    });
    return formatter.format(new Date(viewYear, viewMonth - 1, 1));
  }, [locale, viewMonth, viewYear]);

  const weekdayLabels = useMemo(() => {
    const formatter = new Intl.DateTimeFormat(locale === 'ru' ? 'ru-RU' : 'en-US', { weekday: 'short' });
    const startDate = weekStartsOnMonday ? new Date(2024, 0, 1) : new Date(2023, 11, 31);
    return Array.from({ length: 7 }, (_, index) => {
      const date = new Date(startDate);
      date.setDate(startDate.getDate() + index);
      return formatter.format(date);
    });
  }, [locale, weekStartsOnMonday]);

  const cells = useMemo(() => {
    const totalDays = daysInMonth(viewYear, viewMonth);
    const leading = firstWeekdayOfMonth(viewYear, viewMonth, weekStartsOnMonday);
    const prevMonthDays = daysInMonth(viewYear, viewMonth === 1 ? 12 : viewMonth - 1);
    const items: Array<{ day: number; month: number; year: number; outside: boolean }> = [];

    for (let i = leading - 1; i >= 0; i--) {
      const day = prevMonthDays - i;
      const month = viewMonth === 1 ? 12 : viewMonth - 1;
      const year = viewMonth === 1 ? viewYear - 1 : viewYear;
      items.push({ day, month, year, outside: true });
    }

    for (let day = 1; day <= totalDays; day++) {
      items.push({ day, month: viewMonth, year: viewYear, outside: false });
    }

    while (items.length % 7 !== 0) {
      const month = viewMonth === 12 ? 1 : viewMonth + 1;
      const year = viewMonth === 12 ? viewYear + 1 : viewYear;
      const day = items.length - leading - totalDays + 1;
      items.push({ day, month, year, outside: true });
    }

    return items;
  }, [viewMonth, viewYear, weekStartsOnMonday]);

  function emit(next: SelectedParts) {
    setSelected(next);
    if (includeTime) {
      onChange(toDateTimeLocalIso(next.year, next.month, next.day, next.hour, next.minute));
    } else {
      onChange(toDateIso(next.year, next.month, next.day));
    }
  }

  function pickDay(day: number, month: number, year: number) {
    const base = selected ?? { year, month, day, hour: 9, minute: 0 };
    emit({ ...base, year, month, day });
    if (month !== viewMonth || year !== viewYear) {
      setViewYear(year);
      setViewMonth(month);
    }
  }

  function shiftMonth(delta: number) {
    let month = viewMonth + delta;
    let year = viewYear;
    while (month < 1) {
      month += 12;
      year -= 1;
    }
    while (month > 12) {
      month -= 12;
      year += 1;
    }
    setViewMonth(month);
    setViewYear(year);
  }

  function pickToday() {
    const base = selected ?? { ...today, hour: 9, minute: 0 };
    emit({ ...base, ...today });
    setViewYear(today.year);
    setViewMonth(today.month);
  }

  function clearValue() {
    setSelected(null);
    onChange('');
  }

  function updateTime(field: 'hour' | 'minute', raw: string) {
    const base = selected ?? { ...today, hour: 9, minute: 0 };
    const num = Math.max(0, Math.min(field === 'hour' ? 23 : 59, Number(raw) || 0));
    emit({ ...base, [field]: num });
  }

  const minuteOptions = useMemo(() => {
    const base = Array.from({ length: 12 }, (_, i) => i * 5);
    const current = selected?.minute;
    if (current != null && !base.includes(current)) {
      return [...base, current].sort((a, b) => a - b);
    }
    return base;
  }, [selected?.minute]);

  const hours = useMemo(() => Array.from({ length: 24 }, (_, i) => i), []);

  const preview = selected
    ? includeTime
      ? new Date(
          toDateTimeLocalIso(selected.year, selected.month, selected.day, selected.hour, selected.minute),
        ).toLocaleString(locale === 'ru' ? 'ru-RU' : 'en-US')
      : new Date(selected.year, selected.month - 1, selected.day).toLocaleDateString(
          locale === 'ru' ? 'ru-RU' : 'en-US',
        )
    : '';

  if (!open) {
    return (
      <div className="dt-picker dt-picker-collapsed">
        <button type="button" className="dt-trigger" onClick={() => setOpen(true)}>
          {preview || t('pickerPickDate')}
        </button>
        {includeTime && selected && (
          <div className="dt-time-row dt-time-inline">
            <select
              value={selected.hour}
              onChange={(e) => updateTime('hour', e.target.value)}
              aria-label={t('pickerHour')}
            >
              {hours.map((hour) => (
                <option key={hour} value={hour}>
                  {String(hour).padStart(2, '0')}
                </option>
              ))}
            </select>
            <span className="dt-time-sep">:</span>
            <select
              value={selected.minute}
              onChange={(e) => updateTime('minute', e.target.value)}
              aria-label={t('pickerMinute')}
            >
              {minuteOptions.map((minute) => (
                <option key={minute} value={minute}>
                  {String(minute).padStart(2, '0')}
                </option>
              ))}
            </select>
          </div>
        )}
      </div>
    );
  }

  return (
    <div className="dt-picker">
      <div className="dt-header">
        <button type="button" className="dt-nav" onClick={() => shiftMonth(-1)} aria-label={t('pickerPrevMonth')}>
          ‹
        </button>
        <span className="dt-title">{monthLabel}</span>
        <button type="button" className="dt-nav" onClick={() => shiftMonth(1)} aria-label={t('pickerNextMonth')}>
          ›
        </button>
        <button type="button" className="dt-close ghost" onClick={() => setOpen(false)} aria-label={t('pickerClose')}>
          ✕
        </button>
      </div>

      <div className="dt-weekdays">
        {weekdayLabels.map((label) => (
          <span key={label} className="dt-weekday">
            {label}
          </span>
        ))}
      </div>

      <div className="dt-grid">
        {cells.map((cell) => {
          const isSelected =
            selected?.year === cell.year && selected?.month === cell.month && selected?.day === cell.day;
          const isToday = today.year === cell.year && today.month === cell.month && today.day === cell.day;
          return (
            <button
              key={`${cell.year}-${cell.month}-${cell.day}-${cell.outside}`}
              type="button"
              className={[
                'dt-day',
                cell.outside ? 'outside' : '',
                isSelected ? 'selected' : '',
                isToday ? 'today' : '',
              ]
                .filter(Boolean)
                .join(' ')}
              onClick={() => {
                pickDay(cell.day, cell.month, cell.year);
                if (!includeTime) setOpen(false);
              }}
            >
              {cell.day}
            </button>
          );
        })}
      </div>

      <div className="dt-footer">
        <button type="button" className="link-like" onClick={clearValue}>
          {t('pickerClear')}
        </button>
        <button type="button" className="link-like" onClick={pickToday}>
          {t('pickerToday')}
        </button>
      </div>

      {includeTime && (
        <div className="dt-time">
          <label>{t('pickerTime')}</label>
          <div className="dt-time-row">
            <select
              value={selected?.hour ?? 9}
              onChange={(e) => updateTime('hour', e.target.value)}
              aria-label={t('pickerHour')}
            >
              {hours.map((hour) => (
                <option key={hour} value={hour}>
                  {String(hour).padStart(2, '0')}
                </option>
              ))}
            </select>
            <span className="dt-time-sep">:</span>
            <select
              value={selected?.minute ?? 0}
              onChange={(e) => updateTime('minute', e.target.value)}
              aria-label={t('pickerMinute')}
            >
              {minuteOptions.map((minute) => (
                <option key={minute} value={minute}>
                  {String(minute).padStart(2, '0')}
                </option>
              ))}
            </select>
          </div>
        </div>
      )}

      {selected && includeTime && (
        <button type="button" className="primary dt-done" onClick={() => setOpen(false)}>
          {t('pickerDone')}
        </button>
      )}
    </div>
  );
}
