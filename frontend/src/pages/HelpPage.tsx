import { useEffect } from 'react';
import { Link } from 'react-router-dom';
import { BrowserNotificationGuidePanel } from '../components/BrowserNotificationGuidePanel';
import { MobileGuideContent } from '../components/MobileGuideContent';
import { PageHeader } from '../components/PageHeader';
import { SubscriptionStatusLegend } from '../components/SubscriptionStatusLegend';
import { HELP_ARTICLE_IDS, HELP_ARTICLE_TITLE_KEYS, HelpArticleId } from '../i18n/helpArticles';
import { useI18n } from '../context/I18nContext';

function scrollToArticle(id: string) {
  const target = document.getElementById(id);
  if (!target) return;
  target.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

export function HelpPage() {
  const { t } = useI18n();

  useEffect(() => {
    const hash = window.location.hash.replace(/^#/, '');
    if (!hash || !HELP_ARTICLE_IDS.includes(hash as HelpArticleId)) return;
    const frame = window.requestAnimationFrame(() => scrollToArticle(hash));
    return () => window.cancelAnimationFrame(frame);
  }, []);

  return (
    <section className="stack help-page">
      <PageHeader title={t('helpTitle')} subtitle={t('helpSubtitle')} />

      <nav className="card stack help-toc" aria-label={t('helpTocTitle')}>
        <h3>{t('helpTocTitle')}</h3>
        <ul className="help-toc-list">
          {HELP_ARTICLE_IDS.map((id) => (
            <li key={id}>
              <a href={`#${id}`} onClick={() => scrollToArticle(id)}>
                {t(HELP_ARTICLE_TITLE_KEYS[id])}
              </a>
            </li>
          ))}
        </ul>
      </nav>

      <article className="card stack help-article" id="reminders">
        <h3>{t('helpArticleRemindersTitle')}</h3>
        <p className="muted">{t('reminderHelpBody')}</p>
        <ul className="help-bullets">
          <li>{t('reminderHelpPointBrowser')}</li>
          <li>{t('reminderHelpPointOpen')}</li>
          <li>{t('reminderHelpPointList')}</li>
          <li>{t('reminderHelpPointFuture')}</li>
        </ul>
        <p className="field-hint">
          <Link to="/settings">{t('helpOpenSettings')}</Link>
        </p>
      </article>

      <article className="card stack help-article" id="notifications">
        <h3>{t('helpArticleNotificationsTitle')}</h3>
        <p className="muted">{t('helpArticleNotificationsIntro')}</p>
        <BrowserNotificationGuidePanel />
      </article>

      <article className="card stack help-article" id="mobile">
        <h3>{t('mobileGuideTitle')}</h3>
        <MobileGuideContent />
      </article>

      <article className="card stack help-article" id="subscription-status">
        <h3>{t('helpArticleStatusTitle')}</h3>
        <p className="muted">{t('helpArticleStatusIntro')}</p>
        <SubscriptionStatusLegend showHowTo />
        <p className="field-hint">
          <Link to="/subscriptions">{t('helpOpenSubscriptions')}</Link>
        </p>
      </article>

      <article className="card stack help-article" id="subscriptions">
        <h3>{t('conceptSubsTitle')}</h3>
        <p className="muted">{t('conceptSubsBody')}</p>
        <p className="muted">{t('conceptSubsVsReminders')}</p>
      </article>

      <article className="card stack help-article" id="plans">
        <h3>{t('helpArticlePlansTitle')}</h3>
        <p className="muted">{t('helpArticlePlansIntro')}</p>
        <ul className="help-bullets">
          <li>{t('helpArticlePlansFree')}</li>
          <li>{t('helpArticlePlansPro')}</li>
        </ul>
        <p className="field-hint">
          <Link to="/settings#plans">{t('helpOpenPlans')}</Link>
        </p>
      </article>
    </section>
  );
}
