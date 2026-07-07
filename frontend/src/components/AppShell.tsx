import { useState } from 'react';
import { NavLink, useLocation } from 'react-router-dom';
import { LocaleToggle } from './LocaleToggle';
import { ReminderDueBanner } from './ReminderDueBanner';
import { BackgroundNotifyBar } from './BackgroundNotifyBar';
import { InAppNotificationToast } from './InAppNotificationToast';
import { NotificationBell } from './NotificationBell';
import { AdminPanelLink } from './AdminPanelLink';
import { FeedbackTopButton, FeedbackModal } from './FeedbackModal';
import { useI18n } from '../context/I18nContext';
import { hasSubscriptionDraft } from '../utils/subscriptionDraft';

export function AppShell({ children }: { children: React.ReactNode }) {
  const { t } = useI18n();
  const [feedbackOpen, setFeedbackOpen] = useState(false);
  const location = useLocation();
  const subscriptionsPath = hasSubscriptionDraft() ? '/subscriptions/new' : '/subscriptions';

  return (
    <div className="layout">
      <header className="topbar">
        <div className="brand">
          <span className="brand-main">{t('brand')}</span>
          <span className="brand-sub">{t('brandSub')}</span>
        </div>
        <div className="topbar-actions">
          <AdminPanelLink />
          <FeedbackTopButton onClick={() => setFeedbackOpen(true)} />
          <NotificationBell />
          <LocaleToggle compact />
        </div>
      </header>
      <nav className="tabs-nav">
        <NavLink to="/dashboard">{t('navDashboard')}</NavLink>
        <NavLink
          to={subscriptionsPath}
          className={({ isActive }) =>
            isActive || location.pathname.startsWith('/subscriptions') ? 'active' : undefined
          }
        >
          {t('navSubscriptions')}
        </NavLink>
        <NavLink to="/help">{t('navHelp')}</NavLink>
        <NavLink to="/settings">{t('navSettings')}</NavLink>
      </nav>
      <div className="shell-notices">
        <BackgroundNotifyBar />
        <ReminderDueBanner />
      </div>
      <main className="content">{children}</main>
      <InAppNotificationToast />
      <FeedbackModal open={feedbackOpen} onClose={() => setFeedbackOpen(false)} />
    </div>
  );
}
