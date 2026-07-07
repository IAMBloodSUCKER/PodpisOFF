import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { SETTINGS_PLANS_PATH } from '../utils/settingsPaths';
import { api } from '../api/api';
import { resolveApiError } from '../api/errors';
import { useI18n } from '../context/I18nContext';
import { DashboardAnalytics } from '../types/api';
import { currency } from '../utils/format';
import { sumApproxRub } from '../utils/currencyConversion';

interface ProAnalyticsPanelProps {
  isPro: boolean;
}

function monthShortLabel(year: number, month: number, locale: 'ru' | 'en'): string {
  const date = new Date(year, month - 1, 1);
  return date.toLocaleString(locale === 'ru' ? 'ru-RU' : 'en-US', { month: 'short' });
}

export function ProAnalyticsPanel({ isPro }: ProAnalyticsPanelProps) {
  const { t, locale } = useI18n();
  const [analytics, setAnalytics] = useState<DashboardAnalytics | null>(null);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!isPro) return;
    api
      .dashboardAnalytics(6)
      .then(setAnalytics)
      .catch((err) => setError(resolveApiError(err, t)));
  }, [isPro, t]);

  const trendRub = useMemo(() => {
    if (!analytics) return [];
    return analytics.monthlyTrend.map((point) => ({
      key: `${point.year}-${point.month}`,
      label: monthShortLabel(point.year, point.month, locale),
      rub: sumApproxRub(point.spendByCurrency),
    }));
  }, [analytics, locale]);

  const maxRub = useMemo(() => Math.max(...trendRub.map((p) => p.rub), 0), [trendRub]);

  const yAxisTicks = useMemo(() => {
    if (maxRub <= 0) return [0];
    const mid = Math.round(maxRub / 2);
    return [maxRub, mid, 0];
  }, [maxRub]);

  const hasTrendData = trendRub.some((p) => p.rub > 0);

  const categoryRows = useMemo(() => {
    if (!analytics) return [];
    return Object.entries(analytics.byCategory)
      .map(([category, totals]) => ({
        category,
        rub: sumApproxRub(totals),
      }))
      .filter((row) => row.rub > 0)
      .sort((a, b) => b.rub - a.rub);
  }, [analytics]);

  const maxCategoryRub = useMemo(() => Math.max(...categoryRows.map((r) => r.rub), 1), [categoryRows]);

  if (!isPro) {
    return (
      <article className="card pro-analytics-locked stack">
        <div className="row-between">
          <h3>{t('proAnalyticsTitle')}</h3>
          <span className="plan-badge pro-badge">{t('settingsPlanPro')}</span>
        </div>
        <p className="muted">{t('proAnalyticsLocked')}</p>
        <div className="pro-analytics-preview" aria-hidden="true">
          <div className="trend-chart preview">
            {[40, 65, 50, 80, 55, 70].map((h, i) => (
              <div className="trend-bar-col" key={i}>
                <div className="trend-bar-track">
                  <div className="trend-bar" style={{ height: `${h}%` }} />
                </div>
              </div>
            ))}
          </div>
        </div>
        <Link to={SETTINGS_PLANS_PATH} className="primary">
          {t('planUpgradeCta')}
        </Link>
      </article>
    );
  }

  if (error) {
    return (
      <article className="card stack">
        <h3>{t('proAnalyticsTitle')}</h3>
        <p className="error">{error}</p>
      </article>
    );
  }

  if (!analytics) {
    return (
      <article className="card stack">
        <h3>{t('proAnalyticsTitle')}</h3>
        <p className="muted">{t('loading')}</p>
      </article>
    );
  }

  return (
    <section className="stack pro-analytics">
      <h3 className="pro-analytics-heading">{t('proAnalyticsTitle')}</h3>

      <article className="card stack">
        <h4>{t('proChartTrend')}</h4>
        <p className="field-hint">{t('proChartTrendHint')}</p>
        {!hasTrendData ? (
          <p className="muted">{t('proChartTrendEmpty')}</p>
        ) : (
          <div className="trend-chart-wrap">
            <div className="trend-y-axis" aria-hidden="true">
              {yAxisTicks.map((tick) => (
                <span key={tick}>{currency(tick, 'RUB', locale)}</span>
              ))}
            </div>
            <div className="trend-chart-area">
              <div className="trend-chart" role="img" aria-label={t('proChartTrend')}>
                {trendRub.map((point) => (
                  <div className="trend-bar-col" key={point.key}>
                    <div className="trend-bar-track">
                      {point.rub > 0 && (
                        <div
                          className="trend-bar"
                          style={{ height: `${(point.rub / maxRub) * 100}%` }}
                          title={currency(point.rub, 'RUB', locale)}
                        />
                      )}
                    </div>
                    <span className="trend-label">{point.label}</span>
                  </div>
                ))}
              </div>
              <p className="trend-axis-caption">{t('proChartAxisLabel')}</p>
            </div>
          </div>
        )}
      </article>

      <div className="stats-grid">
        <article className="card stack">
          <h4>{t('proChartCategories')}</h4>
          {categoryRows.length === 0 ? (
            <p className="muted">{t('empty')}</p>
          ) : (
            <ul className="category-bars">
              {categoryRows.map((row) => (
                <li key={row.category}>
                  <div className="row-between category-bar-head">
                    <span>{row.category}</span>
                    <strong>{currency(row.rub, 'RUB', locale)}</strong>
                  </div>
                  <div className="category-bar-track">
                    <span style={{ width: `${(row.rub / maxCategoryRub) * 100}%` }} />
                  </div>
                </li>
              ))}
            </ul>
          )}
        </article>

        <article className="card stack">
          <h4>{t('proChartTopSubs')}</h4>
          {analytics.topByMonthlyCost.length === 0 ? (
            <p className="muted">{t('empty')}</p>
          ) : (
            <ol className="top-subs-list">
              {analytics.topByMonthlyCost.map((item, index) => (
                <li key={item.id}>
                  <span className="top-subs-rank">{index + 1}</span>
                  <div>
                    <strong>{item.title}</strong>
                    <p className="muted">
                      {item.category} · {currency(item.monthlyBurn, item.currency, locale)}
                      {t('proPerMonthShort')}
                    </p>
                  </div>
                </li>
              ))}
            </ol>
          )}
        </article>
      </div>
    </section>
  );
}
