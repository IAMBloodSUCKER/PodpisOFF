import { useMemo, useState } from 'react';
import { mobileInstallGuides, mobileNotificationGuides, MobilePlatform } from '../i18n/mobileGuides';
import { useI18n } from '../context/I18nContext';
import { detectMobilePlatform } from '../utils/mobilePlatform';

export function MobileGuideContent() {
  const { t, locale } = useI18n();
  const detected = useMemo(() => detectMobilePlatform(), []);
  const [platform, setPlatform] = useState<MobilePlatform>(detected ?? 'android');

  const install = mobileInstallGuides[platform];
  const notify = mobileNotificationGuides[platform];

  return (
    <div className="stack mobile-guide-content">
      <p className="muted">{t('mobileGuideIntro')}</p>
      <p className="muted">{t('mobileGuideIntroReminders')}</p>

      <div className="mobile-guide-tabs" role="tablist" aria-label={t('mobileGuidePickPlatform')}>
        <button
          type="button"
          role="tab"
          aria-selected={platform === 'android'}
          className={platform === 'android' ? 'active' : ''}
          onClick={() => setPlatform('android')}
        >
          Android
          {detected === 'android' && <span className="mobile-guide-here">{t('mobileGuideThisDevice')}</span>}
        </button>
        <button
          type="button"
          role="tab"
          aria-selected={platform === 'ios'}
          className={platform === 'ios' ? 'active' : ''}
          onClick={() => setPlatform('ios')}
        >
          iPhone / iPad
          {detected === 'ios' && <span className="mobile-guide-here">{t('mobileGuideThisDevice')}</span>}
        </button>
      </div>

      <section className="mobile-guide-section">
        <h4>{install.title[locale]}</h4>
        <ol className="mobile-guide-steps">
          {install.steps[locale].map((step, index) => (
            <li key={`install-${index}`}>{step}</li>
          ))}
        </ol>
      </section>

      <section className="mobile-guide-section">
        <h4>{notify.title[locale]}</h4>
        <ol className="mobile-guide-steps">
          {notify.steps[locale].map((step, index) => (
            <li key={`notify-${index}`}>{step}</li>
          ))}
        </ol>
      </section>

      <p className="mobile-guide-honesty">{t('mobileGuideHonesty')}</p>
    </div>
  );
}
