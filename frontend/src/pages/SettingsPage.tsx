import { useEffect, useRef, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { api } from '../api/api';
import { resolveApiError } from '../api/errors';
import { LocaleToggle } from '../components/LocaleToggle';
import { DevPlanSwitcher } from '../components/DevPlanSwitcher';
import { PlanCards } from '../components/PlanCards';
import { SettingsCollapsible } from '../components/SettingsCollapsible';
import { SupportButton, SupportModal } from '../components/SupportModal';
import { TelegramConnectPanel } from '../components/TelegramConnectPanel';
import { useAuth } from '../context/AuthContext';
import { useI18n } from '../context/I18nContext';
import { FALLBACK_PLANS, normalizeBillingPlans } from '../i18n/plans';
import { BillingPlan, BillingStatus, NotificationChannelsInfo, PlanType } from '../types/api';
import { BILLING_REMINDER_OPTIONS } from '../utils/billingNotifications';

export function SettingsPage() {
  const [billing, setBilling] = useState<BillingStatus | null>(null);
  const [plans, setPlans] = useState<BillingPlan[]>(FALLBACK_PLANS);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const { t } = useI18n();
  const { logout, user, updateUser } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const plansRef = useRef<HTMLElement>(null);
  const [plansOpen, setPlansOpen] = useState(location.hash === '#plans');
  const [billingDays, setBillingDays] = useState(user?.billingReminderDaysBefore ?? 3);
  const [savingBillingDays, setSavingBillingDays] = useState(false);
  const [channelInfo, setChannelInfo] = useState<NotificationChannelsInfo | null>(null);
  const [email, setEmail] = useState(user?.email ?? '');
  const [emailNotifications, setEmailNotifications] = useState(user?.emailNotificationsEnabled ?? false);
  const [savingChannels, setSavingChannels] = useState(false);
  const [supportOpen, setSupportOpen] = useState(false);

  useEffect(() => {
    setBillingDays(user?.billingReminderDaysBefore ?? 3);
    setEmail(user?.email ?? '');
    setEmailNotifications(user?.emailNotificationsEnabled ?? false);
  }, [user?.billingReminderDaysBefore, user?.email, user?.emailNotificationsEnabled]);

  useEffect(() => {
    api.notificationChannels().then(setChannelInfo).catch(() => setChannelInfo(null));
  }, []);

  useEffect(() => {
    api
      .billingPlans()
      .then((items) => setPlans(normalizeBillingPlans(items)))
      .catch(() => setPlans(FALLBACK_PLANS));

    api
      .billingStatus()
      .then(setBilling)
      .catch(() => {
        if (user) {
          setBilling({ plan: user.plan, planExpiresAt: user.planExpiresAt });
        }
      });
  }, [user]);

  useEffect(() => {
    if (location.hash === '#plans') {
      setPlansOpen(true);
    }
  }, [location.hash]);

  useEffect(() => {
    if (location.hash !== '#plans' || !plansRef.current || !plansOpen) return;
    const frame = window.requestAnimationFrame(() => {
      plansRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    });
    return () => window.cancelAnimationFrame(frame);
  }, [location.pathname, location.hash, plans.length, plansOpen]);

  function onLogout() {
    logout();
    navigate('/auth');
  }

  async function onExport() {
    setError('');
    setSuccess('');
    try {
      const blob = await api.exportSubscriptionsExcel();
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = 'podpisoff-subscriptions.xlsx';
      link.click();
      URL.revokeObjectURL(url);
      setSuccess(t('settingsExportSuccess'));
    } catch (err) {
      setError(resolveApiError(err, t));
    }
  }

  async function onBillingDaysChange(value: number) {
    setBillingDays(value);
    setError('');
    setSuccess('');
    setSavingBillingDays(true);
    try {
      const updated = await api.updateSettings({ billingReminderDaysBefore: value });
      updateUser(updated);
      setSuccess(t('settingsBillingReminderSaved'));
    } catch (err) {
      setError(resolveApiError(err, t));
      setBillingDays(user?.billingReminderDaysBefore ?? 3);
    } finally {
      setSavingBillingDays(false);
    }
  }

  async function onChannelsSave() {
    setError('');
    setSuccess('');
    setSavingChannels(true);
    try {
      const updated = await api.updateSettings({
        email: email.trim() || null,
        emailNotificationsEnabled: emailNotifications,
      });
      updateUser(updated);
      setSuccess(t('settingsChannelsSaved'));
    } catch (err) {
      setError(resolveApiError(err, t));
    } finally {
      setSavingChannels(false);
    }
  }

  function billingDaysLabel(days: number): string {
    if (days === 0) return t('settingsBillingReminderOff');
    return t('settingsBillingReminderDays', { count: String(days) });
  }

  const currentPlan: PlanType = billing?.plan ?? user?.plan ?? 'FREE';
  const planExpiresAt = billing?.planExpiresAt ?? user?.planExpiresAt;
  const canExport = currentPlan === 'PRO';

  return (
    <section className="stack">
      <h2>{t('settingsTitle')}</h2>

      <SettingsCollapsible title={t('settingsAccount')}>
        <p className="muted">{user?.username}</p>
      </SettingsCollapsible>

      <DevPlanSwitcher
        currentPlan={currentPlan}
        onPlanChanged={(plan) => setBilling({ plan, planExpiresAt: null })}
      />

      <SettingsCollapsible title={t('settingsBillingReminderTitle')}>
        <p className="muted">{t('settingsBillingReminderHint')}</p>
        <label htmlFor="billing-reminder-days">{t('settingsBillingReminderLabel')}</label>
        <select
          id="billing-reminder-days"
          value={billingDays}
          disabled={savingBillingDays}
          onChange={(event) => void onBillingDaysChange(Number(event.target.value))}
        >
          {BILLING_REMINDER_OPTIONS.map((days) => (
            <option key={days} value={days}>
              {billingDaysLabel(days)}
            </option>
          ))}
        </select>
        <p className="field-hint">
          <Link to="/help#reminders">{t('settingsHelpLink')}</Link>
          {' · '}
          <Link to="/help#notifications">{t('settingsHelpNotificationsLink')}</Link>
        </p>
      </SettingsCollapsible>

      <SettingsCollapsible title={t('settingsChannelsTitle')}>
        <p className="muted">{t('settingsChannelsHint')}</p>

        <label htmlFor="notify-email">{t('settingsChannelsEmail')}</label>
        <input
          id="notify-email"
          type="email"
          value={email}
          maxLength={255}
          placeholder={t('settingsChannelsEmailPlaceholder')}
          onChange={(event) => setEmail(event.target.value)}
        />

        <label className="settings-channel-toggle">
          <input
            type="checkbox"
            checked={emailNotifications}
            onChange={(event) => setEmailNotifications(event.target.checked)}
          />
          <span>{t('settingsChannelsEmailEnabled')}</span>
        </label>
        {!channelInfo?.emailConfigured && <p className="field-hint">{t('settingsChannelsEmailUnavailable')}</p>}

        <button type="button" className="primary" disabled={savingChannels} onClick={() => void onChannelsSave()}>
          {t('settingsChannelsSaveEmail')}
        </button>

        <hr className="settings-channels-divider" />

        <h4 className="settings-channels-subtitle">{t('settingsTelegramSectionTitle')}</h4>
        <TelegramConnectPanel />
      </SettingsCollapsible>

      <SettingsCollapsible title={t('settingsLocale')}>
        <LocaleToggle />
      </SettingsCollapsible>

      <article
        className={`card settings-collapsible settings-plans-section ${plansOpen ? 'open' : ''}`}
        id="plans"
        ref={plansRef}
      >
        <button
          type="button"
          className="settings-collapsible-trigger"
          onClick={() => setPlansOpen((value) => !value)}
          aria-expanded={plansOpen}
        >
          <h3>{t('settingsPlansTitle')}</h3>
          <span className="settings-collapsible-chevron" aria-hidden="true" />
        </button>
        {plansOpen && (
          <div className="settings-collapsible-body stack">
            <p className="plan-pro-pitch">{t('planProPitch')}</p>
            <PlanCards plans={plans} currentPlan={currentPlan} planExpiresAt={planExpiresAt} />
            <p className="field-hint">{t('planPaySoon')}</p>
          </div>
        )}
      </article>

      {canExport && (
        <SettingsCollapsible title={t('settingsData')}>
          <p className="muted">{t('settingsExportHint')}</p>
          <button type="button" className="primary" onClick={() => void onExport()}>
            {t('settingsExport')}
          </button>
        </SettingsCollapsible>
      )}

      {error && <p className="error">{error}</p>}
      {success && <p className="success">{success}</p>}

      <article className="card settings-support-card">
        <p className="muted settings-support-hint">{t('supportHintShort')}</p>
        <SupportButton onClick={() => setSupportOpen(true)} />
      </article>

      <article className="card">
        <button type="button" className="danger" onClick={onLogout}>
          {t('settingsLogout')}
        </button>
      </article>

      <SupportModal open={supportOpen} onClose={() => setSupportOpen(false)} />
    </section>
  );
}
