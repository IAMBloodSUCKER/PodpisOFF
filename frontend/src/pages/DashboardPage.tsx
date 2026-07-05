import { useEffect, useState } from 'react';
import { api } from '../api/api';
import { useI18n } from '../context/I18nContext';
import { DashboardSummary } from '../types/api';
import { currency, dateLabel } from '../utils/format';

export function DashboardPage() {
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [error, setError] = useState('');
  const { t } = useI18n();

  useEffect(() => {
    api
      .dashboardSummary()
      .then(setSummary)
      .catch((err) => setError(err instanceof Error ? err.message : 'Failed to load dashboard'));
  }, []);

  if (error) return <p className="error">{error}</p>;
  if (!summary) return <p className="muted">{t('loading')}</p>;

  const firstCurrency = summary.upcomingBilling[0]?.currency ?? 'USD';

  return (
    <section className="stack">
      <div className="stats-grid">
        <article className="card">
          <h3>{t('totalsMonthly')}</h3>
          <p className="big">{currency(summary.monthlyTotal, firstCurrency)}</p>
        </article>
        <article className="card">
          <h3>{t('totalsYearly')}</h3>
          <p className="big">{currency(summary.yearlyTotal, firstCurrency)}</p>
        </article>
      </div>

      <article className="card">
        <h3>{t('categoryBreakdown')}</h3>
        {Object.entries(summary.byCategory).length === 0 ? (
          <p className="muted">{t('empty')}</p>
        ) : (
          <ul className="list">
            {Object.entries(summary.byCategory).map(([category, total]) => (
              <li key={category}>
                <span>{category}</span>
                <strong>{currency(total, firstCurrency)}</strong>
              </li>
            ))}
          </ul>
        )}
      </article>

      <article className="card">
        <h3>{t('upcomingCharges')}</h3>
        {summary.upcomingBilling.length === 0 ? (
          <p className="muted">{t('empty')}</p>
        ) : (
          <ul className="list">
            {summary.upcomingBilling.map((item) => (
              <li key={item.id}>
                <span>
                  {item.title} - {dateLabel(item.nextBillingDate)}
                </span>
                <strong>{currency(item.amount, item.currency)}</strong>
              </li>
            ))}
          </ul>
        )}
      </article>
    </section>
  );
}
