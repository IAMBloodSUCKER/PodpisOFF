export type LocaleCode = 'ru' | 'en';
export type PlanType = 'FREE' | 'PRO';

export interface CaptchaResponse {
  captchaId: string;
  question: string;
}

export interface UsernameCheckResponse {
  available: boolean;
}

export interface RegisterRequest {
  username: string;
  password: string;
  email?: string;
  timezone: string;
  locale: LocaleCode;
  termsAccepted: boolean;
  captchaId: string;
  captchaAnswer: string;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RecoverPasswordRequest {
  username: string;
  recoveryKey: string;
  newPassword: string;
  captchaId: string;
  captchaAnswer: string;
}

export interface RegisterResponse {
  auth: AuthResponse;
  recoveryKey: string;
}

export interface AuthResponse {
  token: string;
  id: number;
  username: string;
  email: string | null;
  plan: PlanType;
  planExpiresAt: string | null;
  timezone: string;
  locale: LocaleCode;
  termsAccepted: boolean;
  billingReminderDaysBefore: number;
  emailNotificationsEnabled: boolean;
  telegramNotificationsEnabled: boolean;
  telegramChatId: string | null;
  createdAt: string;
}

export interface SettingsUpdateRequest {
  billingReminderDaysBefore?: number;
  emailNotificationsEnabled?: boolean;
  telegramNotificationsEnabled?: boolean;
  telegramChatId?: string | null;
  email?: string | null;
}

export interface NotificationChannelsInfo {
  emailConfigured: boolean;
  telegramConfigured: boolean;
  telegramBotUsername: string | null;
}

export interface TelegramLinkResponse {
  botUsername: string;
  deepLink: string;
  expiresAt: string;
}

export interface TelegramLinkStatus {
  linked: boolean;
  pending: boolean;
  botUsername: string | null;
  deepLink: string | null;
}

export interface DevToolsResponse {
  devTools: boolean;
}

export type BillingPeriod = 'MONTHLY' | 'YEARLY';

export interface Subscription {
  id: number;
  title: string;
  category: string;
  amount: number;
  currency: string;
  nextBillingDate: string;
  billingPeriod: BillingPeriod;
  active: boolean;
  note: string | null;
  resourceUrl: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface SubscriptionPayload {
  title: string;
  category: string;
  amount: number;
  currency: string;
  nextBillingDate: string;
  billingPeriod: BillingPeriod;
  active: boolean;
  note?: string | null;
  resourceUrl?: string | null;
}

export interface DashboardAnalytics {
  monthlyTrend: Array<{ year: number; month: number; spendByCurrency: Record<string, number> }>;
  topByMonthlyCost: Array<{
    id: number;
    title: string;
    category: string;
    monthlyBurn: number;
    currency: string;
    billingPeriod: BillingPeriod;
  }>;
  byCategory: Record<string, Record<string, number>>;
}

export interface DashboardSummary {
  selectedYear: number;
  selectedMonth: number;
  monthlyByCurrency: Record<string, number>;
  yearlyByCurrency: Record<string, number>;
  monthSpendByCurrency: Record<string, number>;
  byCategory: Record<string, Record<string, number>>;
  upcomingBilling: Subscription[];
}

export interface SubscriptionMonthCharge {
  id: number;
  subscriptionId: number;
  year: number;
  month: number;
  amount: number;
  note: string | null;
}

export interface SubscriptionMonthChargePayload {
  year: number;
  month: number;
  amount: number;
  note?: string | null;
}

export interface BillingStatus {
  plan: PlanType;
  planExpiresAt: string | null;
}

export interface BillingPlan {
  id: PlanType;
  priceRub: number;
  priceUsd: number;
  priceYearRub: number;
  priceYearUsd: number;
  subscriptionLimit: number;
  reminderLimit: number;
  featureKeys: string[];
}

export type ReminderRepeat = 'ONCE' | 'MONTHLY' | 'YEARLY';

export interface Reminder {
  id: number;
  title: string;
  note: string | null;
  remindAt: string;
  repeat: ReminderRepeat;
  nextRemindAt: string;
  done: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ReminderPayload {
  title: string;
  note?: string;
  remindAt: string;
  repeat: ReminderRepeat;
  done: boolean;
}

export interface UserNotification {
  id: number;
  type: string;
  title: string;
  body: string | null;
  referenceId: number | null;
  createdAt: string;
  readAt: string | null;
  unread: boolean;
}

export type FeedbackKind = 'FEEDBACK' | 'SUPPORT';

export interface FeedbackItem {
  id: number;
  message: string;
  createdAt: string;
  adminReply: string | null;
  adminRepliedAt: string | null;
}

export interface AdminFeedbackItem extends FeedbackItem {
  userId: number;
  username: string;
  kind: FeedbackKind;
}

export interface DailyCount {
  date: string;
  count: number;
}

export interface LabelCount {
  label: string;
  count: number;
}

export interface AdminMetrics {
  totalUsers: number;
  newUsersToday: number;
  newUsersWeek: number;
  newUsersMonth: number;
  freeUsers: number;
  proUsers: number;
  expiredProUsers: number;
  effectiveProUsers: number;
  effectiveFreeUsers: number;
  blockedUsers: number;
  totalSubscriptions: number;
  activeSubscriptions: number;
  pausedSubscriptions: number;
  offSubscriptions: number;
  totalReminders: number;
  recurringReminders: number;
  loginsToday: number;
  loginsWeek: number;
  loginsMonth: number;
  uniqueLoginsToday: number;
  uniqueLoginsWeek: number;
  uniqueLoginsMonth: number;
  totalFeedback: number;
  pendingFeedback: number;
  repliedFeedback: number;
  pushSubscribers: number;
  usersWithEmailNotify: number;
  usersWithTelegramNotify: number;
  avgSubscriptionsPerUser: number;
  registrationsLast7Days: DailyCount[];
  loginsLast7Days: DailyCount[];
  uniqueLoginsLast7Days: DailyCount[];
  subscriptionsCreatedLast7Days: DailyCount[];
  feedbackLast7Days: DailyCount[];
  effectivePlanSlice: LabelCount[];
  subscriptionStatusSlice: LabelCount[];
  billingPeriodSlice: LabelCount[];
  topCategories: LabelCount[];
  currencySlice: LabelCount[];
  localeSlice: LabelCount[];
  usersBySubscriptionCountSlice: LabelCount[];
  blockedUsersSlice: LabelCount[];
  notificationChannelsSlice: LabelCount[];
  reminderRepeatSlice: LabelCount[];
}

export interface AdminUserRow {
  id: number;
  username: string;
  email: string | null;
  emailNotificationsEnabled: boolean;
  telegramLinked: boolean;
  telegramNotificationsEnabled: boolean;
  plan: string;
  effectivePlan: string;
  planExpiresAt: string | null;
  createdAt: string;
  subscriptionCount: number;
  reminderCount: number;
  blockedPermanently: boolean;
  blockedUntil: string | null;
  currentlyBlocked: boolean;
}

export type AdminUserPlanFilter = 'all' | 'pro' | 'free';
export type AdminUserEmailFilter = 'all' | 'set' | 'unset' | 'notify_on' | 'notify_off';
export type AdminUserTelegramFilter = 'all' | 'connected' | 'not_connected' | 'notify_on' | 'notify_off';

export interface AdminUserListFilters {
  plan?: AdminUserPlanFilter;
  search?: string;
  emailStatus?: AdminUserEmailFilter;
  telegramStatus?: AdminUserTelegramFilter;
}
