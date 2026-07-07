import { Link } from 'react-router-dom';
import { useReminderAlerts } from '../context/ReminderAlertsContext';
import { useI18n } from '../context/I18nContext';
import { billingReminderBody } from '../utils/billingNotifications';

export function ReminderDueBanner() {
  const { dueBilling, dismissBillingDue } = useReminderAlerts();
  const { t, locale } = useI18n();

  const billing = dueBilling[0];
  if (!billing) return null;

  const more = dueBilling.length - 1;

  return (
    <div className="due-toast due-toast-billing" role="status" aria-live="polite">
      <div className="due-toast-body">
        <strong>{t('billingDueNow')}</strong>
        <span>
          {billing.title} — {billingReminderBody(billing, locale)}
          {more > 0 && ` ${t('reminderDueMore', { count: String(more) })}`}
        </span>
      </div>
      <Link className="ghost due-toast-link" to="/subscriptions">
        {t('navSubscriptions')}
      </Link>
      <button
        type="button"
        className="ghost due-toast-close"
        onClick={() => dismissBillingDue(billing.id)}
        aria-label={t('reminderDismiss')}
      >
        ×
      </button>
    </div>
  );
}
