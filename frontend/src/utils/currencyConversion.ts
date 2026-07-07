/** Rough RUB per 1 unit — dashboard orientation only, not for billing. */
const APPROX_RUB_PER_UNIT: Record<string, number> = {
  RUB: 1,
  USD: 92,
  EUR: 99,
  GBP: 116,
  CNY: 12.7,
  TRY: 2.8,
  KZT: 0.19,
  BYN: 28,
  UAH: 2.3,
  JPY: 0.6,
  CHF: 105,
  PLN: 23,
  AED: 25,
  THB: 2.6,
  INR: 1.1,
};

export function needsRubEquivalent(totals: Record<string, number>): boolean {
  const entries = Object.entries(totals).filter(([, amount]) => amount > 0);
  if (entries.length === 0) return false;
  if (entries.length === 1) return entries[0][0] !== 'RUB';
  return true;
}

export function sumApproxRub(totals: Record<string, number>): number {
  return Object.entries(totals).reduce((sum, [code, amount]) => {
    if (amount <= 0) return sum;
    const normalized = code.trim().toUpperCase();
    const rate = APPROX_RUB_PER_UNIT[normalized];
    if (rate === undefined) return sum;
    return sum + amount * rate;
  }, 0);
}
