import {
  AuthResponse,
  BillingStatus,
  CaptchaResponse,
  DashboardSummary,
  LocaleCode,
  LoginRequest,
  RecoverPasswordRequest,
  RegisterRequest,
  RegisterResponse,
  Subscription,
  SubscriptionPayload,
  UsernameCheckResponse,
} from '../types/api';

const API_BASE = import.meta.env.VITE_API_URL ?? '';

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

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const token = getStoredToken();
  const headers = new Headers(init?.headers);
  headers.set('Content-Type', 'application/json');
  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  const res = await fetch(`${API_BASE}${path}`, { ...init, headers });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || `Request failed (${res.status})`);
  }
  if (res.status === 204) {
    return undefined as T;
  }
  return (await res.json()) as T;
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
  dashboardSummary(year?: number, month?: number) {
    const params = new URLSearchParams();
    if (year) params.set('year', String(year));
    if (month) params.set('month', String(month));
    const suffix = params.toString();
    return request<DashboardSummary>(`/api/dashboard/summary${suffix ? `?${suffix}` : ''}`);
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
  billingStatus() {
    return request<BillingStatus>('/api/billing/status');
  },
  createPayment(plan: 'PRO', locale: LocaleCode) {
    return request<{ paymentId: string; paymentUrl: string; status: string }>('/api/billing/create-payment', {
      method: 'POST',
      body: JSON.stringify({ plan, locale }),
    });
  },
};
