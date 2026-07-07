import { useI18n } from '../context/I18nContext';
import { currency } from '../utils/format';
import { needsRubEquivalent, sumApproxRub } from '../utils/currencyConversion';

interface ApproxRubTotalProps {
  totals: Record<string, number>;
}

export function ApproxRubTotal({ totals }: ApproxRubTotalProps) {
  const { t, locale } = useI18n();
  if (!needsRubEquivalent(totals)) return null;

  const rubTotal = sumApproxRub(totals);
  if (rubTotal <= 0) return null;

  return (
    <p className="approx-rub-total">
      {t('dashboardApproxRubTotal', { amount: currency(rubTotal, 'RUB', locale) })}
    </p>
  );
}
