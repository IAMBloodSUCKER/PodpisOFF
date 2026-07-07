import { Link } from 'react-router-dom';
import { useI18n } from '../context/I18nContext';
import { AuthResponse } from '../types/api';
import { SETTINGS_PLANS_PATH } from '../utils/settingsPaths';
import {
  FREE_SUBSCRIPTION_LIMIT,
  atSubscriptionCreateLimit,
  effectivePlan,
  isExpiredPro,
  isOverSubscriptionLimit,
} from '../utils/plan';
interface PlanLimitBannerProps {
  user: AuthResponse | null;
  subscriptionCount: number;
  focus?: 'subscriptions' | 'general';
}

export function PlanLimitBanner({ user, subscriptionCount, focus = 'general' }: PlanLimitBannerProps) {
  const { t, locale } = useI18n();
  const plan = effectivePlan(user);
  const expired = isExpiredPro(user);
  const overSubs = isOverSubscriptionLimit(plan, subscriptionCount);
  const blockedSubs = atSubscriptionCreateLimit(plan, subscriptionCount);

  const showExpired = expired && (overSubs || blockedSubs);
  const showOverSubs = overSubs && (focus === 'subscriptions' || focus === 'general');
  const showBlockedOnly = !showExpired && !showOverSubs && blockedSubs;

  if (!showExpired && !showOverSubs && !showBlockedOnly) {
    return null;
  }

  let titleKey = 'planLimitTitle';
  let bodyKey = 'planLimitBody';

  if (showExpired) {
    titleKey = 'planExpiredTitle';
    bodyKey = 'planExpiredBody';
  } else if (showOverSubs) {
    titleKey = 'planOverSubsTitle';
    bodyKey = 'planOverSubsBody';
  } else if (blockedSubs) {
    titleKey = 'planAtSubsLimitTitle';
    bodyKey = 'planAtSubsLimitBody';
  }

  const expiresLabel = user?.planExpiresAt
    ? new Date(user.planExpiresAt).toLocaleDateString(locale === 'ru' ? 'ru-RU' : 'en-US')
    : '';

  return (
    <article className="card plan-limit-banner stack" role="status">
      <h3>{t(titleKey)}</h3>
      <p className="muted">
        {t(bodyKey, {
          subs: String(subscriptionCount),
          subsLimit: String(FREE_SUBSCRIPTION_LIMIT),
          date: expiresLabel,
        })}
      </p>
      <Link to={SETTINGS_PLANS_PATH} className="primary plan-limit-cta">
        {t('planLimitCta')}
      </Link>
    </article>
  );
}
