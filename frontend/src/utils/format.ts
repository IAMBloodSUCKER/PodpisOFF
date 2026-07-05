export function currency(value: number, code: string): string {
  try {
    return new Intl.NumberFormat(undefined, { style: 'currency', currency: code }).format(value);
  } catch {
    return `${value.toFixed(2)} ${code}`;
  }
}

export function dateLabel(value: string): string {
  return new Date(value).toLocaleDateString();
}

export function generateRecoveryKey(username: string): string {
  const seed = `${username}-${Date.now()}`.replace(/\s+/g, '').slice(0, 12);
  const random = Math.random().toString(36).slice(2, 8).toUpperCase();
  return `${seed}${random}9`;
}
