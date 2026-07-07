import { BillingPeriod } from '../types/api';

const DRAFT_KEY = 'podpisoff.subscriptionDraft.new';

export interface SubscriptionDraft {
  title: string;
  category: string;
  amount: string;
  currency: string;
  billingPeriod: BillingPeriod;
  nextBillingDate: string;
  note: string;
  resourceUrl: string;
  active: boolean;
}

export function readSubscriptionDraft(): SubscriptionDraft | null {
  try {
    const raw = sessionStorage.getItem(DRAFT_KEY);
    if (!raw) return null;
    return JSON.parse(raw) as SubscriptionDraft;
  } catch {
    return null;
  }
}

export function writeSubscriptionDraft(draft: SubscriptionDraft): void {
  try {
    sessionStorage.setItem(DRAFT_KEY, JSON.stringify(draft));
  } catch {
    // ignore quota errors
  }
}

export function clearSubscriptionDraft(): void {
  try {
    sessionStorage.removeItem(DRAFT_KEY);
  } catch {
    // ignore
  }
}

export function hasSubscriptionDraft(): boolean {
  const draft = readSubscriptionDraft();
  if (!draft) return false;
  return Boolean(
    draft.title.trim() ||
      draft.category.trim() ||
      draft.amount.trim() ||
      draft.note.trim() ||
      draft.resourceUrl.trim(),
  );
}
