import {
  AdminFeedbackItem,
  AdminMetrics,
  AdminUserRow,
} from '../types/api';
import { parseApiError } from './errors';

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

function adminHeaders(): HeadersInit {
  const token = getStoredToken();
  const key = sessionStorage.getItem('podpisoff.adminKey');
  const headers: Record<string, string> = { 'Content-Type': 'application/json' };
  if (token) headers.Authorization = `Bearer ${token}`;
  if (key) headers['X-Admin-Key'] = key;
  return headers;
}

async function adminRequest<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${API_BASE}${path}`, {
    ...init,
    headers: { ...adminHeaders(), ...(init?.headers ?? {}) },
    cache: 'no-store',
  });
  const text = await res.text();
  if (!res.ok) {
    throw new Error(parseApiError(text, res.status));
  }
  return text ? (JSON.parse(text) as T) : (undefined as T);
}

export const adminApi = {
  verifyKey(key: string) {
    return adminRequest<{ valid: boolean }>('/api/admin/verify-key', {
      method: 'POST',
      body: JSON.stringify({ key }),
      headers: { 'Content-Type': 'application/json', ...(getStoredToken() ? { Authorization: `Bearer ${getStoredToken()}` } : {}) },
    });
  },
  metrics() {
    return adminRequest<AdminMetrics>('/api/admin/metrics');
  },
  users(plan?: 'all' | 'pro' | 'free') {
    const suffix = plan && plan !== 'all' ? `?plan=${plan}` : '';
    return adminRequest<AdminUserRow[]>(`/api/admin/users${suffix}`);
  },
  feedback() {
    return adminRequest<AdminFeedbackItem[]>('/api/admin/feedback');
  },
  replyToFeedback(id: number, reply: string) {
    return adminRequest<AdminFeedbackItem>(`/api/admin/feedback/${id}/reply`, {
      method: 'POST',
      body: JSON.stringify({ reply }),
    });
  },
  sendTestNotification(delaySeconds: 0 | 5 | 10) {
    return adminRequest<{ delaySeconds: number; deliverAt: string }>('/api/admin/test-notification', {
      method: 'POST',
      body: JSON.stringify({ delaySeconds }),
    });
  },
  notifyUser(userId: number, title: string, body: string) {
    return adminRequest<AdminUserRow>(`/api/admin/users/${userId}/notify`, {
      method: 'POST',
      body: JSON.stringify({ title, body }),
    });
  },
  blockUser(userId: number, permanent: boolean, hours?: number) {
    return adminRequest<AdminUserRow>(`/api/admin/users/${userId}/block`, {
      method: 'POST',
      body: JSON.stringify({ permanent, hours: hours ?? null }),
    });
  },
  unblockUser(userId: number) {
    return adminRequest<AdminUserRow>(`/api/admin/users/${userId}/unblock`, {
      method: 'POST',
    });
  },
  deleteUser(userId: number) {
    return adminRequest<void>(`/api/admin/users/${userId}`, {
      method: 'DELETE',
    });
  },
  setUserPlan(userId: number, plan: 'FREE' | 'PRO', planExpiresAt?: string | null) {
    return adminRequest<AdminUserRow>(`/api/admin/users/${userId}/plan`, {
      method: 'POST',
      body: JSON.stringify({ plan, planExpiresAt: planExpiresAt ?? null }),
    });
  },
};
