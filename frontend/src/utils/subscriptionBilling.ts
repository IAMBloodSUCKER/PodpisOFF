import { BillingPeriod } from '../types/api';
import { parseLocalDate, todayLocalIso } from './format';

export function effectiveNextBillingDate(
  nextBillingDate: string,
  billingPeriod: BillingPeriod = 'MONTHLY',
  today = todayLocalIso(),
): string {
  let cursor = parseLocalDate(nextBillingDate);
  const todayDate = parseLocalDate(today);
  let guard = 0;
  while (cursor.getTime() <= todayDate.getTime() && guard < 5000) {
    cursor =
      billingPeriod === 'YEARLY'
        ? new Date(cursor.getFullYear() + 1, cursor.getMonth(), cursor.getDate())
        : new Date(cursor.getFullYear(), cursor.getMonth() + 1, cursor.getDate());
    guard++;
  }
  const year = cursor.getFullYear();
  const month = String(cursor.getMonth() + 1).padStart(2, '0');
  const day = String(cursor.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}
