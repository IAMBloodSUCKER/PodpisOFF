import { useEffect, useMemo, useState } from 'react';
import { useI18n } from '../context/I18nContext';
import { CUSTOM_CATEGORY_VALUE, getCategoryOptions, isPresetCategory } from '../i18n/categories';

interface CategoryPickerProps {
  value: string;
  onChange: (value: string) => void;
}

export function CategoryPicker({ value, onChange }: CategoryPickerProps) {
  const { t, locale } = useI18n();
  const options = useMemo(() => getCategoryOptions(locale), [locale]);
  const [customMode, setCustomMode] = useState(() => Boolean(value.trim()) && !isPresetCategory(value, locale));

  useEffect(() => {
    if (value.trim() && !isPresetCategory(value, locale)) {
      setCustomMode(true);
    }
  }, [value, locale]);

  useEffect(() => {
    if (!customMode && !value.trim() && options[0]) {
      onChange(options[0].label);
    }
  }, [customMode, onChange, options, value]);

  if (customMode) {
    return (
      <div className="category-picker">
        <input
          value={value}
          maxLength={80}
          onChange={(event) => onChange(event.target.value)}
          placeholder={t('formCategoryCustomPlaceholder')}
          required
        />
        <button
          type="button"
          className="ghost category-picker-switch"
          onClick={() => {
            setCustomMode(false);
            onChange(options[0]?.label ?? '');
          }}
        >
          {t('formCategoryPickFromList')}
        </button>
      </div>
    );
  }

  const selectValue = isPresetCategory(value, locale) ? value : options[0]?.label ?? '';

  return (
    <div className="category-picker">
      <select
        value={selectValue}
        onChange={(event) => {
          if (event.target.value === CUSTOM_CATEGORY_VALUE) {
            setCustomMode(true);
            onChange('');
            return;
          }
          onChange(event.target.value);
        }}
        required
      >
        {options.map((item) => (
          <option key={item.key} value={item.label}>
            {item.label}
          </option>
        ))}
        <option value={CUSTOM_CATEGORY_VALUE}>{t('formCategoryCustom')}</option>
      </select>
      <p className="field-hint">{t('formCategoryHint')}</p>
    </div>
  );
}
