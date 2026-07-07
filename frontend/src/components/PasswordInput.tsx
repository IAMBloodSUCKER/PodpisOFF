import { useState } from 'react';
import { useI18n } from '../context/I18nContext';

interface PasswordInputProps {
  value: string;
  onChange: (value: string) => void;
  autoComplete?: string;
}

export function PasswordInput({ value, onChange, autoComplete }: PasswordInputProps) {
  const { t } = useI18n();
  const [visible, setVisible] = useState(false);

  return (
    <div className="password-field">
      <input
        type={visible ? 'text' : 'password'}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        autoComplete={autoComplete}
      />
      <button
        type="button"
        className="password-toggle"
        onClick={() => setVisible((current) => !current)}
        aria-label={visible ? t('authPasswordHide') : t('authPasswordShow')}
        aria-pressed={visible}
      >
        {visible ? (
          <svg width="20" height="20" viewBox="0 0 24 24" aria-hidden="true">
            <path
              fill="currentColor"
              d="M12 5c-5 0-9.27 3.11-10 7.5.73 4.39 5 7.5 10 7.5s9.27-3.11 10-7.5C21.27 8.11 17 5 12 5zm0 12.5A5 5 0 1 1 12 7.5a5 5 0 0 1 0 10zm0-2.5a2.5 2.5 0 1 0 0-5 2.5 2.5 0 0 0 0 5z"
            />
          </svg>
        ) : (
          <svg width="20" height="20" viewBox="0 0 24 24" aria-hidden="true">
            <path
              fill="currentColor"
              d="M12 6.5c2.76 0 5.26 1.56 6.47 4h-2.1a4.01 4.01 0 0 0-8.74 0H5.53C6.74 8.06 9.24 6.5 12 6.5zM3.27 3 2 4.27l2.28 2.28A11.77 11.77 0 0 0 1 12c1.73 4.39 6 7.5 11 7.5 2.12 0 4.11-.55 5.85-1.52L21.73 22 23 20.73 3.27 3zm7.53 7.53L14.73 15A3.99 3.99 0 0 1 10.8 11.03zM12 17.5c-2.76 0-5.26-1.56-6.47-4h2.1a4.01 4.01 0 0 0 7.74 0h2.1c-.63 1.58-1.67 2.9-2.97 3.77l-1.4-1.4c.56-.45 1.03-1.01 1.4-1.67z"
            />
          </svg>
        )}
      </button>
    </div>
  );
}
