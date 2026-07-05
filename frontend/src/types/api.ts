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
  createdAt: string;
}

export interface Subscription {
  id: number;
  title: string;
  category: string;
  amount: number;
  currency: string;
  nextBillingDate: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface SubscriptionPayload {
  title: string;
  category: string;
  amount: number;
  currency: string;
  nextBillingDate: string;
  active: boolean;
}

export interface DashboardSummary {
  monthlyTotal: number;
  yearlyTotal: number;
  byCategory: Record<string, number>;
  upcomingBilling: Subscription[];
}

export interface BillingStatus {
  plan: PlanType;
  planExpiresAt: string | null;
}
