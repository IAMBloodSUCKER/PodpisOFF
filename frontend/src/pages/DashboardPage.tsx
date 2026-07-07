import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api/api';
import { resolveApiError } from '../api/errors';
import { ApproxRubTotal } from '../components/ApproxRubTotal';
import { EmptyState } from '../components/EmptyState';
import { MonthPicker } from '../components/MonthPicker';
import { PlanLimitBanner } from '../components/PlanLimitBanner';
import { ProAnalyticsPanel } from '../components/ProAnalyticsPanel';
import { PageHeader } from '../components/PageHeader';
import { PlanUsage } from '../components/PlanCards';
import { useAuth } from '../context/AuthContext';
import { useI18n } from '../context/I18nContext';
import { DashboardSummary, Subscription } from '../types/api';
import { currency, dateLabel } from '../utils/format';
import { needsRubEquivalent } from '../utils/currencyConversion';
import { currentMonthValue, formatMonthLabel, parseMonthValue } from '../utils/monthValue';
import { effectivePlan, isPro, subscriptionLimit, subscriptionsIncludedInPlan } from '../utils/plan';
import { formatSubscriptionPrice } from '../utils/subscriptionFormat';

function hasMultipleCurrencies(totals: Record<string, number>): boolean {
  return Object.keys(totals).length > 1;
}

function needsRubSummary(summary: DashboardSummary): boolean {
  return (
    needsRubEquivalent(summary.monthSpendByCurrency) ||
    needsRubEquivalent(summary.monthlyByCurrency) ||
    needsRubEquivalent(summary.yearlyByCurrency)
  );
}

export function DashboardPage() {
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [subscriptions, setSubscriptions] = useState<Subscription[]>([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [monthValue, setMonthValue] = useState(currentMonthValue);
  const { t, locale } = useI18n();
  const { user } = useAuth();

  const selectedMonth = useMemo(() => parseMonthValue(monthValue), [monthValue]);

  const load = useCallback(async () => {
    if (!selectedMonth) return;
    setLoading(true);
    setError('');
    try {
      const [nextSummary, nextSubs] = await Promise.all([
        api.dashboardSummary(selectedMonth.year, selectedMonth.month),
        api.listSubscriptions(),
      ]);
      setSummary(nextSummary);
      setSubscriptions(nextSubs);
    } catch (err) {
      setError(resolveApiError(err, t));
    } finally {
      setLoading(false);
    }
  }, [selectedMonth, t]);

  useEffect(() => {
    void load();
  }, [load]);

  const mixedCurrencies = useMemo(() => {
    if (!summary) return false;
    return hasMultipleCurrencies(summary.monthlyByCurrency) || hasMultipleCurrencies(summary.yearlyByCurrency);
  }, [summary]);

  const currentPlan = effectivePlan(user);
  const subLimit = subscriptionLimit(currentPlan);
  const includedSubscriptions = useMemo(
    () => subscriptionsIncludedInPlan(currentPlan, subscriptions),
    [currentPlan, subscriptions],
  );
  const activeCount = includedSubscriptions.length;
  const hasSubscriptions = subscriptions.length > 0;

  const monthlyEntries = summary ? Object.entries(summary.monthlyByCurrency) : [];
  const yearlyEntries = summary ? Object.entries(summary.yearlyByCurrency) : [];
  const monthSpendEntries = summary ? Object.entries(summary.monthSpendByCurrency) : [];

  return (
    <section className="stack">
      <PageHeader
        title={t('dashboardWelcome', { name: user?.username ?? '' })}
        subtitle={t('dashboardWelcomeSub')}
        actionLabel={hasSubscriptions ? t('addSubscription') : undefined}
        actionTo={hasSubscriptions ? '/subscriptions/new' : undefined}
      />

      {error && (
        <article className="card error-banner stack">
          <p className="error">{error}</p>
          <button type="button" className="ghost" onClick={() => void load()}>
            {t('errorRetry')}
          </button>
        </article>
      )}

      {loading && !summary && <p className="muted">{t('loading')}</p>}

      {!loading && summary && !hasSubscriptions && (
        <EmptyState
          title={t('dashboardEmptyTitle')}
          description={t('dashboardEmptyDesc')}
          actionLabel={t('dashboardEmptyCta')}
          actionTo="/subscriptions/new"
        />
      )}

      {!loading && summary && hasSubscriptions && (
        <>
          <PlanLimitBanner user={user} subscriptionCount={subscriptions.length} />

          <div className="stats-grid">
            <PlanUsage count={includedSubscriptions.length} limit={subLimit} plan={currentPlan} />
            <article className="card stat-card">
              <h3>{t('dashboardActiveSubs')}</h3>
              <p className="big">{activeCount}</p>
              <p className="muted">{t('dashboardTotalSubs', { count: String(subscriptions.length) })}</p>
            </article>
          </div>

          {mixedCurrencies && <p className="field-hint">{t('dashboardMixedCurrency')}</p>}

          {summary && needsRubSummary(summary) && (
            <p className="field-hint dashboard-rub-hint">{t('dashboardApproxRubHint')}</p>
          )}

          <article className="card stack dashboard-month-picker">
            <label htmlFor="dashboard-month">{t('dashboardPickMonth')}</label>
            <MonthPicker id="dashboard-month" value={monthValue} onChange={setMonthValue} />
          </article>

          <div className="stats-grid">
            <article className="card stat-card stat-card-highlight">
              <h3>{t('dashboardMonthSpend')}</h3>
              <p className="muted month-spend-label">{formatMonthLabel(monthValue, locale)}</p>
              {monthSpendEntries.length === 0 ? (
                <p className="muted">{t('empty')}</p>
              ) : (
                <div className="totals-list">
                  {monthSpendEntries.map(([code, total]) => (
                    <p className="big" key={code}>
                      {currency(total, code, locale)}
                    </p>
                  ))}
                </div>
              )}
              <ApproxRubTotal totals={summary.monthSpendByCurrency} />
              <p className="field-hint">{t('dashboardMonthSpendHint')}</p>
            </article>
          </div>

          <div className="stats-grid">
            <article className="card stat-card">
              <h3>{t('totalsMonthly')}</h3>
              {monthlyEntries.length === 0 ? (
                <p className="muted">{t('empty')}</p>
              ) : (
                <div className="totals-list">
                  {monthlyEntries.map(([code, total]) => (
                    <p className="big" key={code}>
                      {currency(total, code, locale)}
                    </p>
                  ))}
                </div>
              )}
              <ApproxRubTotal totals={summary.monthlyByCurrency} />
              <p className="field-hint">{t('dashboardTotalsMonthlyHint')}</p>
            </article>
            <article className="card stat-card">
              <h3>{t('totalsYearly')}</h3>
              {yearlyEntries.length === 0 ? (
                <p className="muted">{t('empty')}</p>
              ) : (
                <div className="totals-list">
                  {yearlyEntries.map(([code, total]) => (
                    <p className="big" key={code}>
                      {currency(total, code, locale)}
                    </p>
                  ))}
                </div>
              )}
              <ApproxRubTotal totals={summary.yearlyByCurrency} />
              <p className="field-hint">{t('dashboardTotalsYearlyHint')}</p>
            </article>
          </div>

          <ProAnalyticsPanel isPro={isPro(currentPlan)} />

          <article className="card">
            <h3>{t('categoryBreakdown')}</h3>
            {Object.entries(summary.byCategory).length === 0 ? (
              <p className="muted">{t('empty')}</p>
            ) : (
              <ul className="list">
                {Object.entries(summary.byCategory).map(([category, totalsByCurrency]) => (
                  <li key={category}>
                    <span>{category}</span>
                    <div className="totals-inline">
                      {Object.entries(totalsByCurrency).map(([code, total]) => (
                        <strong key={code}>{currency(total, code, locale)}</strong>
                      ))}
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </article>

          <article className="card">
            <div className="row-between">
              <h3>{t('upcomingCharges')}</h3>
              <Link to="/subscriptions">{t('navSubscriptions')}</Link>
            </div>
            {summary.upcomingBilling.length === 0 ? (
              <p className="muted">{t('empty')}</p>
            ) : (
              <ul className="list">
                {summary.upcomingBilling.map((item) => (
                  <li key={item.id}>
                    <span>
                      {item.title} — {dateLabel(item.nextBillingDate)} (
                      {formatSubscriptionPrice(item.amount, item.currency, item.billingPeriod ?? 'MONTHLY', locale, currency)})
                    </span>
                  </li>
                ))}
              </ul>
            )}
          </article>
        </>
      )}
    </section>
  );
}
