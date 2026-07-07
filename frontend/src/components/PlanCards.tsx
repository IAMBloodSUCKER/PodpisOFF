import { useState } from 'react';
import { Link } from 'react-router-dom';
import { SETTINGS_PLANS_PATH } from '../utils/settingsPaths';
import { BillingPlan } from '../types/api';
import { useI18n } from '../context/I18nContext';
import { PlanType } from '../types/api';
import { currency } from '../utils/format';
import { yearlyFreeMonths, yearlyMonthlyEquivalentRub, yearlyMonthlyEquivalentUsd, yearlySavingsRub, yearlySavingsUsd } from '../i18n/plans';

type BillingPeriod = 'monthly' | 'yearly';

interface PlanCardsProps {
  plans: BillingPlan[];
  currentPlan: PlanType;
  planExpiresAt?: string | null;
}

export function PlanCards({ plans, currentPlan, planExpiresAt }: PlanCardsProps) {
  const { t, locale } = useI18n();
  const [billingPeriod, setBillingPeriod] = useState<BillingPeriod>('yearly');

  function planTitle(id: PlanType): string {
    return id === 'PRO' ? t('settingsPlanPro') : t('settingsPlanFree');
  }

  function formatProPrice(plan: BillingPlan): string {
    if (billingPeriod === 'yearly') {
      if (locale === 'ru') {
        return `${currency(plan.priceYearRub, 'RUB', locale)}${t('planPerYear')}`;
      }
      return `${currency(plan.priceYearUsd, 'USD', locale)}${t('planPerYear')}`;
    }
    if (locale === 'ru') {
      return `${currency(plan.priceRub, 'RUB', locale)}${t('planPerMonth')}`;
    }
    return `${currency(plan.priceUsd, 'USD', locale)}${t('planPerMonth')}`;
  }

  return (
    <div className="plan-grid">
      {plans.map((plan) => {
        const isCurrent = plan.id === currentPlan;
        const isPro = plan.id === 'PRO';
        const savingsRub = yearlySavingsRub(plan);
        const savingsUsd = yearlySavingsUsd(plan);
        const freeMonths = yearlyFreeMonths(plan);
        const yearlyPerMonthRub = yearlyMonthlyEquivalentRub(plan);
        const yearlyPerMonthUsd = yearlyMonthlyEquivalentUsd(plan);

        return (
          <article key={plan.id} className={`plan-card ${isCurrent ? 'current' : ''} ${isPro ? 'pro' : ''}`}>
            <div className="plan-card-head">
              <h3>{planTitle(plan.id)}</h3>
              {isCurrent && <span className="plan-badge">{t('planCurrent')}</span>}
              {isPro && billingPeriod === 'yearly' && !isCurrent && (
                <span className="plan-badge plan-badge-value">{t('planProBestValue')}</span>
              )}
            </div>

            {isPro && (
              <div className="plan-billing-toggle" role="tablist" aria-label={t('planBillingToggleLabel')}>
                <button
                  type="button"
                  role="tab"
                  aria-selected={billingPeriod === 'monthly'}
                  className={billingPeriod === 'monthly' ? 'active' : ''}
                  onClick={() => setBillingPeriod('monthly')}
                >
                  {t('planBillingMonthly')}
                </button>
                <button
                  type="button"
                  role="tab"
                  aria-selected={billingPeriod === 'yearly'}
                  className={billingPeriod === 'yearly' ? 'active' : ''}
                  onClick={() => setBillingPeriod('yearly')}
                >
                  {t('planBillingYearly')}
                </button>
              </div>
            )}

            <p className="plan-price">{isPro ? formatProPrice(plan) : t('planPriceFree')}</p>

            {isPro && billingPeriod === 'yearly' && (
              <p className="plan-price-sub">
                {locale === 'ru'
                  ? t('planYearlyPerMonth', { amount: currency(yearlyPerMonthRub, 'RUB', locale) })
                  : t('planYearlyPerMonth', { amount: currency(yearlyPerMonthUsd, 'USD', locale) })}
              </p>
            )}

            {isPro && billingPeriod === 'yearly' && (locale === 'ru' ? savingsRub > 0 : savingsUsd > 0) && (
              <p className="plan-price-save">
                {t('planYearlySave', {
                  amount:
                    locale === 'ru'
                      ? currency(savingsRub, 'RUB', locale)
                      : currency(savingsUsd, 'USD', locale),
                  months: String(freeMonths),
                })}
              </p>
            )}

            {isPro && billingPeriod === 'monthly' && (
              <p className="plan-price-sub muted">
                {t('planComparedYearly', {
                  amount:
                    locale === 'ru'
                      ? currency(plan.priceYearRub, 'RUB', locale)
                      : currency(plan.priceYearUsd, 'USD', locale),
                  full:
                    locale === 'ru'
                      ? currency(plan.priceRub * 12, 'RUB', locale)
                      : currency(Math.round(plan.priceUsd * 12 * 100) / 100, 'USD', locale),
                })}
              </p>
            )}

            {isPro && (
              <div className="plan-value-lines">
                <p className="plan-value-line">{t('planPriceCoffee')}</p>
                <p className="plan-value-line">{t('planPriceRoi')}</p>
              </div>
            )}

            {isCurrent && planExpiresAt && currentPlan === 'PRO' && plan.id === 'PRO' && (
              <p className="muted plan-expires">
                {t('settingsPlanUntil')} {new Date(planExpiresAt).toLocaleDateString(locale === 'ru' ? 'ru-RU' : 'en-US')}
              </p>
            )}

            <ul className="plan-features">
              {plan.featureKeys.map((key) => (
                <li key={key}>{t(key)}</li>
              ))}
            </ul>

            {isPro ? (
              <button type="button" className="primary plan-pay" disabled title={t('planPaySoon')}>
                {billingPeriod === 'yearly' ? t('planPayYearly') : t('planPay')}
              </button>
            ) : isCurrent ? (
              <span className="plan-note muted">{t('planIncluded')}</span>
            ) : null}
          </article>
        );
      })}
    </div>
  );
}

interface PlanUsageProps {
  count: number;
  limit: number;
  plan: PlanType;
}

export function PlanUsage({ count, limit, plan }: PlanUsageProps) {
  const { t } = useI18n();
  if (plan === 'PRO' || limit < 0) {
    return (
      <article className="card plan-usage">
        <h3>{t('dashboardPlanUsage')}</h3>
        <p className="big">{count}</p>
        <p className="muted">{t('planFeatureSubsUnlimited')}</p>
      </article>
    );
  }

  const overLimit = plan === 'FREE' && limit > 0 && count > limit;
  const ratio = limit > 0 ? Math.min(count / limit, 1) : 0;
  return (
    <article className={`card plan-usage ${overLimit ? 'plan-usage-over' : ''}`}>
      <div className="row-between">
        <h3>{t('dashboardPlanUsage')}</h3>
        <Link to={SETTINGS_PLANS_PATH} className="plan-upgrade-link">
          {t('planUpgradeCta')}
        </Link>
      </div>
      <p className="big">
        {count} / {limit}
      </p>
      <div className={`plan-meter ${overLimit ? 'plan-meter-over' : ''}`} aria-hidden="true">
        <span style={{ width: `${ratio * 100}%` }} />
      </div>
      <p className="muted">{overLimit ? t('planUsageOverLimit') : t('dashboardPlanUsageHint')}</p>
      {overLimit && <p className="plan-value-line plan-usage-nudge">{t('planPriceRoi')}</p>}
    </article>
  );
}
