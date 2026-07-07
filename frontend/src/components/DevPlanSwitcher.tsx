import { useEffect, useState } from 'react';
import { api } from '../api/api';
import { resolveApiError } from '../api/errors';
import { useAuth } from '../context/AuthContext';
import { useI18n } from '../context/I18nContext';
import { PlanType } from '../types/api';

interface DevPlanSwitcherProps {
  currentPlan: PlanType;
  onPlanChanged: (plan: PlanType) => void;
}

export function DevPlanSwitcher({ currentPlan, onPlanChanged }: DevPlanSwitcherProps) {
  const { t } = useI18n();
  const { updateUser } = useAuth();
  const [allowed, setAllowed] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    api
      .devTools()
      .then((response) => setAllowed(response.devTools))
      .catch(() => setAllowed(false));
  }, []);

  if (!allowed) {
    return null;
  }

  const targetPlan: PlanType = currentPlan === 'PRO' ? 'FREE' : 'PRO';
  const label = targetPlan === 'PRO' ? t('devSwitchToPro') : t('devSwitchToFree');

  async function switchPlan(plan: PlanType, expired = false) {
    setLoading(true);
    setError('');
    try {
      const updated = await api.switchDevPlan(plan, expired);
      updateUser(updated);
      onPlanChanged(updated.plan);
    } catch (err) {
      setError(resolveApiError(err, t));
    } finally {
      setLoading(false);
    }
  }

  return (
    <article className="card stack dev-plan-switcher">
      <h3>{t('devPlanTitle')}</h3>
      <p className="muted">{t('devPlanHint')}</p>
      <p className="dev-plan-current">
        {t('settingsPlan')}: <strong>{currentPlan === 'PRO' ? t('settingsPlanPro') : t('settingsPlanFree')}</strong>
      </p>
      <div className="actions">
        <button type="button" className="primary" disabled={loading} onClick={() => void switchPlan(targetPlan)}>
          {loading ? t('loading') : label}
        </button>
        <button
          type="button"
          className="ghost"
          disabled={loading}
          onClick={() => void switchPlan('PRO', true)}
        >
          {t('devSimulateExpiredPro')}
        </button>
      </div>
      {error && <p className="error">{error}</p>}
    </article>
  );
}
