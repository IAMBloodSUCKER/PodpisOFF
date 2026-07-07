export type BrowserGuideId =
  | 'chrome'
  | 'edge'
  | 'firefox'
  | 'safari'
  | 'safari-ios'
  | 'yandex'
  | 'opera'
  | 'samsung'
  | 'chrome-android'
  | 'unknown';

export const ALL_BROWSER_GUIDE_IDS: BrowserGuideId[] = [
  'chrome',
  'yandex',
  'edge',
  'firefox',
  'safari',
  'opera',
  'samsung',
  'safari-ios',
  'chrome-android',
  'unknown',
];

export function detectBrowserGuideId(): BrowserGuideId {
  const ua = navigator.userAgent;
  const isIOS = /iPhone|iPad|iPod/i.test(ua);
  const isAndroid = /Android/i.test(ua);

  if (isIOS) return 'safari-ios';
  if (/YaBrowser/i.test(ua)) return 'yandex';
  if (/SamsungBrowser/i.test(ua)) return 'samsung';
  if (/OPR|Opera/i.test(ua)) return 'opera';
  if (/Edg\//i.test(ua)) return 'edge';
  if (/Firefox/i.test(ua)) return 'firefox';
  if (/Safari/i.test(ua) && !/Chrome|Chromium/i.test(ua)) return 'safari';
  if (/Chrome/i.test(ua)) return isAndroid ? 'chrome-android' : 'chrome';
  return 'unknown';
}
