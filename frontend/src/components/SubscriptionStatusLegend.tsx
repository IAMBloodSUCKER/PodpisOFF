import { useI18n } from '../context/I18nContext';

interface SubscriptionStatusLegendProps {
  showHowTo?: boolean;
}

export function SubscriptionStatusLegend({ showHowTo = false }: SubscriptionStatusLegendProps) {
  const { t } = useI18n();

  return (
    <div className="status-legend">
      <div className="status-legend-grid">
        <div className="status-legend-item">
          <span className="badge active">{t('statusActive')}</span>
          <p>{t('statusLegendActive')}</p>
        </div>
        <div className="status-legend-item">
          <span className="badge paused">{t('statusPaused')}</span>
          <p>{t('statusLegendPaused')}</p>
        </div>
        <div className="status-legend-item">
          <span className="badge off">{t('statusOff')}</span>
          <p>{t('statusLegendOff')}</p>
        </div>
      </div>
      {showHowTo && (
        <div className="status-legend-howto">
          <p>
            <strong>{t('statusLegendPauseTitle')}</strong>
            <span className="muted"> — {t('statusLegendPauseSteps')}</span>
          </p>
          <p>
            <strong>{t('statusLegendOffTitle')}</strong>
            <span className="muted"> — {t('statusLegendOffSteps')}</span>
          </p>
        </div>
      )}
    </div>
  );
}
