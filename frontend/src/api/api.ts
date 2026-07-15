import {
  AuthResponse,
  BillingStatus,
  BillingPlan,
  CaptchaResponse,
  DashboardSummary,
  DashboardAnalytics,
  LoginRequest,
  RecoverPasswordRequest,
  RegisterRequest,
  RegisterResponse,
  Reminder,
  ReminderPayload,
  DevToolsResponse,
  NotificationChannelsInfo,
  SettingsUpdateRequest,
  TelegramLinkResponse,
  TelegramLinkStatus,
  Subscription,
  SubscriptionPayload,
  SubscriptionMonthCharge,
  SubscriptionMonthChargePayload,
  UsernameCheckResponse,
} from '../types/api';
import { ApiClientError, isRetryableStatus, parseApiError, parseApiErrorBody } from './errors';

const API_BASE = import.meta.env.VITE_API_URL ?? '';
const MAX_RETRIES = 3;
const RETRY_DELAY_MS = 1200;

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

let unauthorizedHandler: (() => void) | null = null;

export function setUnauthorizedHandler(handler: () => void) {
  unauthorizedHandler = handler;
}

function getStoredToken(): string | null {
  const localRaw = localStorage.getItem('podpisoff.auth');
  if (localRaw) {
    try {
      return (JSON.parse(localRaw) as { token: string }).token;
    } catch {
      return null;
    }
  }
  const sessionRaw = sessionStorage.getItem('podpisoff.auth.session');
  if (sessionRaw) {
    try {
      return (JSON.parse(sessionRaw) as { token: string }).token;
    } catch {
      return null;
    }
  }
  return null;
}

async function request<T>(path: string, init?: RequestInit, attempt = 0): Promise<T> {
  const token = getStoredToken();
  const headers = new Headers(init?.headers);
  if (!headers.has('Content-Type') && init?.body) {
    headers.set('Content-Type', 'application/json');
  }
  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  const res = await fetch(`${API_BASE}${path}`, { ...init, headers, cache: 'no-store' });
  const text = await res.text();

  if (isRetryableStatus(res.status) && attempt < MAX_RETRIES) {
    await sleep(RETRY_DELAY_MS * (attempt + 1));
    return request<T>(path, init, attempt + 1);
  }

  if (res.status === 401) {
    unauthorizedHandler?.();
    const { message } = parseApiErrorBody(text, res.status);
    throw new ApiClientError(message, res.status);
  }

  if (!res.ok) {
    const { message, fieldErrors } = parseApiErrorBody(text, res.status);
    throw new ApiClientError(message, res.status, fieldErrors);
  }

  if (!text.trim()) {
    return undefined as T;
  }

  return JSON.parse(text) as T;
}

export const api = {
  getCaptcha() {
    return request<CaptchaResponse>('/api/auth/captcha', { method: 'POST' });
  },
  checkUsername(username: string) {
    const params = new URLSearchParams({ username });
    return request<UsernameCheckResponse>(`/api/auth/username-check?${params.toString()}`);
  },
  register(payload: RegisterRequest) {
    return request<RegisterResponse>('/api/auth/register', {
      method: 'POST',
      body: JSON.stringify(payload),
    });
  },
  login(payload: LoginRequest) {
    return request<AuthResponse>('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify(payload),
    });
  },
  recoverPassword(payload: RecoverPasswordRequest) {
    return request<void>('/api/auth/recover-password', {
      method: 'POST',
      body: JSON.stringify(payload),
    });
  },
  me() {
    return request<AuthResponse>('/api/auth/me');
  },
  updateSettings(payload: SettingsUpdateRequest) {
    return request<AuthResponse>('/api/settings', {
      method: 'PATCH',
      body: JSON.stringify(payload),
    });
  },
  notificationChannels() {
    return request<NotificationChannelsInfo>('/api/settings/notification-channels');
  },
  createTelegramLink() {
    return request<TelegramLinkResponse>('/api/settings/telegram/link', { method: 'POST' });
  },
  telegramLinkStatus() {
    return request<TelegramLinkStatus>('/api/settings/telegram/link-status');
  },
  disconnectTelegram() {
    return request<AuthResponse>('/api/settings/telegram/link', { method: 'DELETE' });
  },
  devTools() {
    return request<DevToolsResponse>('/api/dev/tools');
  },
  switchDevPlan(plan: 'FREE' | 'PRO', expired = false) {
    return request<AuthResponse>('/api/dev/plan', {
      method: 'POST',
      body: JSON.stringify({ plan, expired }),
    });
  },
  dashboardSummary(year?: number, month?: number) {
    const params = new URLSearchParams();
    if (year) params.set('year', String(year));
    if (month) params.set('month', String(month));
    const suffix = params.toString();
    return request<DashboardSummary>(`/api/dashboard/summary${suffix ? `?${suffix}` : ''}`);
  },
  dashboardAnalytics(months = 6) {
    return request<DashboardAnalytics>(`/api/dashboard/analytics?months=${months}`);
  },
  listSubscriptions() {
    return request<Subscription[]>('/api/subscriptions');
  },
  createSubscription(payload: SubscriptionPayload) {
    return request<Subscription>('/api/subscriptions', {
      method: 'POST',
      body: JSON.stringify(payload),
    });
  },
  updateSubscription(id: number, payload: SubscriptionPayload) {
    return request<Subscription>(`/api/subscriptions/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    });
  },
  deleteSubscription(id: number) {
    return request<void>(`/api/subscriptions/${id}`, { method: 'DELETE' });
  },
  listSubscriptionMonthCharges(subscriptionId: number) {
    return request<SubscriptionMonthCharge[]>(`/api/subscriptions/${subscriptionId}/month-charges`);
  },
  upsertSubscriptionMonthCharge(subscriptionId: number, payload: SubscriptionMonthChargePayload) {
    return request<SubscriptionMonthCharge>(`/api/subscriptions/${subscriptionId}/month-charges`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    });
  },
  deleteSubscriptionMonthCharge(subscriptionId: number, year: number, month: number) {
    const params = new URLSearchParams({ year: String(year), month: String(month) });
    return request<void>(`/api/subscriptions/${subscriptionId}/month-charges?${params.toString()}`, {
      method: 'DELETE',
    });
  },
  listReminders() {
    return request<Reminder[]>('/api/reminders');
  },
  createReminder(payload: ReminderPayload) {
    return request<Reminder>('/api/reminders', {
      method: 'POST',
      body: JSON.stringify(payload),
    });
  },
  updateReminder(id: number, payload: ReminderPayload) {
    return request<Reminder>(`/api/reminders/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    });
  },
  deleteReminder(id: number) {
    return request<void>(`/api/reminders/${id}`, { method: 'DELETE' });
  },
  billingStatus() {
    return request<BillingStatus>('/api/billing/status');
  },
  billingPlans() {
    return request<BillingPlan[]>('/api/billing/plans');
  },
  createPayment(targetPlan: 'PRO' = 'PRO') {
    return request<{ paymentId: string; paymentUrl: string; status: string }>('/api/billing/create-payment', {
      method: 'POST',
      body: JSON.stringify({ targetPlan }),
    });
  },
  async exportSubscriptionsExcel(): Promise<Blob> {
    const token = getStoredToken();
    const res = await fetch(`${API_BASE}/api/export/subscriptions.xlsx`, {
      headers: token ? { Authorization: `Bearer ${token}` } : {},
      cache: 'no-store',
    });
    if (res.status === 401) {
      unauthorizedHandler?.();
      const text = await res.text();
      throw new Error(parseApiError(text, res.status));
    }
    if (!res.ok) {
      const text = await res.text();
      throw new Error(parseApiError(text, res.status));
    }
    return res.blob();
  },
  submitFeedback(message: string, kind: import('../types/api').FeedbackKind = 'FEEDBACK') {
    return request<import('../types/api').FeedbackItem>('/api/feedback', {
      method: 'POST',
      body: JSON.stringify({ message, kind }),
    });
  },
  myFeedback() {
    return request<import('../types/api').FeedbackItem[]>('/api/feedback/mine');
  },
  notifications() {
    return request<import('../types/api').UserNotification[]>('/api/notifications');
  },
  notificationUnreadCount() {
    return request<{ count: number }>('/api/notifications/unread-count');
  },
  markNotificationRead(id: number) {
    return request<void>(`/api/notifications/${id}/read`, { method: 'PATCH' });
  },
  markAllNotificationsRead() {
    return request<void>('/api/notifications/read-all', { method: 'PATCH' });
  },
  pushVapidPublicKey() {
    return request<{ publicKey: string }>('/api/push/vapid-public-key');
  },
  pushSubscribe(body: { endpoint: string; p256dh: string; auth: string }) {
    return request<void>('/api/push/subscribe', {
      method: 'POST',
      body: JSON.stringify(body),
    });
  },
};
