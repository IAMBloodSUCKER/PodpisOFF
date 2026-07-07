import { useMemo, useState } from 'react';
import { notificationGuides } from '../i18n/notificationGuides';
import { useI18n } from '../context/I18nContext';
import {
  ALL_BROWSER_GUIDE_IDS,
  BrowserGuideId,
  detectBrowserGuideId,
} from '../utils/detectBrowser';

const PICKER_BROWSER_IDS: BrowserGuideId[] = ALL_BROWSER_GUIDE_IDS.filter((id) => id !== 'unknown');

export function BrowserNotificationGuidePanel() {
  const { t, locale } = useI18n();
  const detectedId = useMemo(() => detectBrowserGuideId(), []);
  const [pickedId, setPickedId] = useState<BrowserGuideId>(detectedId);

  const guide = notificationGuides[pickedId];
  const steps = guide.steps[locale];
  const addressHint = guide.addressBarHint[locale];

  return (
    <div className="stack notify-guide-panel">
      <div className="notify-guide-header">
        <span className="notify-guide-label">{t('notifyGuideForBrowser')}</span>
        <strong className="notify-guide-name">{guide.name[locale]}</strong>
        {pickedId === detectedId && <span className="notify-guide-detected">{t('notifyGuideDetectedHere')}</span>}
      </div>

      {addressHint && <p className="notify-guide-address-hint">{addressHint}</p>}

      <ol className="notify-bar-steps">
        {steps.map((step, index) => (
          <li key={`${pickedId}-${index}`}>{step}</li>
        ))}
      </ol>

      <details className="notify-guide-others">
        <summary>{t('notifyGuideOtherBrowsers')}</summary>
        <p className="notify-guide-picker-label">{t('notifyGuidePickBrowser')}</p>
        <div className="notify-guide-browser-list" role="listbox" aria-label={t('notifyGuidePickBrowser')}>
          {[...PICKER_BROWSER_IDS, 'unknown' as BrowserGuideId].map((id) => (
            <button
              key={id}
              type="button"
              role="option"
              aria-selected={pickedId === id}
              className={pickedId === id ? 'active' : ''}
              onClick={() => setPickedId(id)}
            >
              {notificationGuides[id].name[locale]}
              {id === detectedId ? ` (${t('notifyGuideThisDevice')})` : ''}
            </button>
          ))}
        </div>
      </details>
    </div>
  );
}
