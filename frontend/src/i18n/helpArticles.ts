export const HELP_ARTICLE_IDS = [
  'reminders',
  'notifications',
  'mobile',
  'subscription-status',
  'subscriptions',
  'plans',
] as const;

export type HelpArticleId = (typeof HELP_ARTICLE_IDS)[number];

export const HELP_ARTICLE_TITLE_KEYS: Record<HelpArticleId, string> = {
  reminders: 'helpArticleRemindersTitle',
  notifications: 'helpArticleNotificationsTitle',
  mobile: 'mobileGuideTitle',
  'subscription-status': 'helpArticleStatusTitle',
  subscriptions: 'conceptSubsTitle',
  plans: 'helpArticlePlansTitle',
};
