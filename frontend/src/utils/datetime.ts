export function pad2(n: number): string {
  return String(n).padStart(2, '0');
}

export function toDateIso(year: number, month: number, day: number): string {
  return `${year}-${pad2(month)}-${pad2(day)}`;
}

export function toDateTimeLocalIso(year: number, month: number, day: number, hour: number, minute: number): string {
  return `${toDateIso(year, month, day)}T${pad2(hour)}:${pad2(minute)}`;
}

export function parseDateIso(value: string): { year: number; month: number; day: number } | null {
  const match = /^(\d{4})-(\d{2})-(\d{2})/.exec(value);
  if (!match) return null;
  return { year: Number(match[1]), month: Number(match[2]), day: Number(match[3]) };
}

export function parseDateTimeLocal(value: string): {
  year: number;
  month: number;
  day: number;
  hour: number;
  minute: number;
} | null {
  const match = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})/.exec(value);
  if (!match) return null;
  return {
    year: Number(match[1]),
    month: Number(match[2]),
    day: Number(match[3]),
    hour: Number(match[4]),
    minute: Number(match[5]),
  };
}

export function daysInMonth(year: number, month: number): number {
  return new Date(year, month, 0).getDate();
}

/** Monday = 0 … Sunday = 6 */
export function weekdayIndex(year: number, month: number, day: number, weekStartsOnMonday: boolean): number {
  const jsDay = new Date(year, month - 1, day).getDay();
  const mondayBased = jsDay === 0 ? 6 : jsDay - 1;
  return weekStartsOnMonday ? mondayBased : jsDay;
}

export function firstWeekdayOfMonth(year: number, month: number, weekStartsOnMonday: boolean): number {
  return weekdayIndex(year, month, 1, weekStartsOnMonday);
}

export function todayParts(): { year: number; month: number; day: number } {
  const now = new Date();
  return { year: now.getFullYear(), month: now.getMonth() + 1, day: now.getDate() };
}

export function defaultDateTimeValue(): string {
  const now = new Date();
  now.setMinutes(0, 0, 0);
  now.setHours(now.getHours() + 1);
  return toDateTimeLocalIso(
    now.getFullYear(),
    now.getMonth() + 1,
    now.getDate(),
    now.getHours(),
    now.getMinutes(),
  );
}
