import { NavLink } from 'react-router-dom';
import { useI18n } from '../context/I18nContext';

export function AppShell({ children }: { children: React.ReactNode }) {
  const { t, locale } = useI18n();

  return (
    <div className="layout">
      <header className="topbar">
        <div className="brand">
          <span className="brand-main">{t('brand')}</span>
          <span className="brand-sub">{t('brandSub')}</span>
        </div>
        <span className="locale-pill">{locale.toUpperCase()}</span>
      </header>
      <nav className="tabs-nav">
        <NavLink to="/dashboard">{t('navDashboard')}</NavLink>
        <NavLink to="/subscriptions">{t('navSubscriptions')}</NavLink>
        <NavLink to="/settings">{t('navSettings')}</NavLink>
      </nav>
      <main className="content">{children}</main>
    </div>
  );
}
