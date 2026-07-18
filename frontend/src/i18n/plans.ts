import { BillingPlan } from '../types/api';

export const FALLBACK_PLANS: BillingPlan[] = [
  {
    id: 'FREE',
    priceRub: 0,
    priceUsd: 0,
    priceYearRub: 0,
    priceYearUsd: 0,
    subscriptionLimit: 3,
    reminderLimit: 5,
    featureKeys: [
      'planFeatureProTrial',
      'planFeatureSubs3',
      'planFeatureDashboard',
      'planFeatureBillingReminders',
      'planFeatureCurrencyRub',
    ],
  },
  {
    id: 'PRO',
    priceRub: 149,
    priceUsd: 1.99,
    priceYearRub: 1290,
    priceYearUsd: 12.99,
    subscriptionLimit: -1,
    reminderLimit: -1,
    featureKeys: [
      'planFeatureSubsUnlimited',
      'planFeatureDashboard',
      'planFeatureBillingReminders',
      'planFeatureMultiCurrency',
      'planFeatureCsvExport',
      'planFeatureAnalytics',
      'planFeatureSpendingCharts',
      'planFeatureMonthOverrides',
      'planFeatureTopSubscriptions',
    ],
  },
];

export function yearlySavingsRub(plan: BillingPlan): number {
  return Math.max(0, plan.priceRub * 12 - plan.priceYearRub);
}

export function yearlyFreeMonths(plan: BillingPlan): number {
  if (plan.priceRub <= 0) return 0;
  return Math.max(1, Math.round(yearlySavingsRub(plan) / plan.priceRub));
}

export function yearlyMonthlyEquivalentRub(plan: BillingPlan): number {
  return Math.round(plan.priceYearRub / 12);
}

export function yearlySavingsUsd(plan: BillingPlan): number {
  return Math.max(0, Math.round((plan.priceUsd * 12 - plan.priceYearUsd) * 100) / 100);
}

export function yearlyMonthlyEquivalentUsd(plan: BillingPlan): number {
  return Math.round((plan.priceYearUsd / 12) * 100) / 100;
}

export function normalizeBillingPlans(plans: BillingPlan[]): BillingPlan[] {
  const fallbackPro = FALLBACK_PLANS.find((plan) => plan.id === 'PRO');
  return plans.map((plan) => {
    if (plan.id !== 'PRO' || !fallbackPro) return plan;
    return {
      ...plan,
      priceYearRub: plan.priceYearRub || fallbackPro.priceYearRub,
      priceYearUsd: plan.priceYearUsd || fallbackPro.priceYearUsd,
    };
  });
}
