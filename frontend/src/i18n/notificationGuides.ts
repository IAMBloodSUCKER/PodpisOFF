import { BrowserGuideId } from '../utils/detectBrowser';
import { LocaleCode } from '../types/api';

export interface BrowserNotificationGuide {
  name: Record<LocaleCode, string>;
  addressBarHint: Record<LocaleCode, string | null>;
  steps: Record<LocaleCode, string[]>;
}

export const notificationGuides: Record<BrowserGuideId, BrowserNotificationGuide> = {
  chrome: {
    name: { ru: 'Google Chrome', en: 'Google Chrome' },
    addressBarHint: {
      ru: 'Вверху окна: [ иконка ]  адрес-сайта  …  ← нажмите на иконку слева от адреса',
      en: 'Top of the window: [ icon ]  site-address  …  ← click the icon left of the address',
    },
    steps: {
      ru: [
        'Посмотрите в самый верх окна — на строку, где написан адрес сайта. Слева от текста адреса есть маленькая иконка: замок, буква «i» в круге или надпись «Не защищено». Нажмите на неё.',
        'В выпадающем меню найдите строку «Уведомления».',
        'Справа от неё выберите «Разрешить» в списке.',
        'Обновите страницу — клавиша F5 или кнопка со стрелкой-кругом ↻ справа от адреса.',
      ],
      en: [
        'Look at the very top of the window — the bar with the site address. To the left of the address you will see a small icon: a lock, a circled “i”, or “Not secure”. Click it.',
        'In the menu, find the “Notifications” row.',
        'Set the dropdown on the right to “Allow”.',
        'Refresh the page — press F5 or the circular arrow ↻ next to the address bar.',
      ],
    },
  },
  yandex: {
    name: { ru: 'Яндекс Браузер', en: 'Yandex Browser' },
    addressBarHint: {
      ru: 'Вверху окна: [ 🌐 или 🔒 ]  адрес-сайта  …  ← нажмите на круглую иконку слева',
      en: 'Top of the window: [ 🌐 or 🔒 ]  site-address  …  ← click the round icon on the left',
    },
    steps: {
      ru: [
        'Вверху окна найдите строку адреса (имя этого сайта). Слева от адреса — круглая иконка: часто это земной шар, замок или «i». Нажмите на неё один раз.',
        'В открывшемся списке найдите пункт «Уведомления».',
        'Переключите на «Разрешить» или включите переключатель рядом.',
        'Обновите страницу (F5 или кнопка ↻).',
      ],
      en: [
        'At the top, find the address bar (this site’s name). On its left is a round icon — often a globe, lock, or “i”. Click it once.',
        'In the list, find “Notifications”.',
        'Switch to “Allow” or turn the toggle on.',
        'Refresh the page (F5 or ↻).',
      ],
    },
  },
  edge: {
    name: { ru: 'Microsoft Edge', en: 'Microsoft Edge' },
    addressBarHint: {
      ru: 'Вверху окна: [ иконка ]  адрес-сайта  …  ← нажмите на иконку слева от адреса',
      en: 'Top of the window: [ icon ]  site-address  …  ← click the icon left of the address',
    },
    steps: {
      ru: [
        'Вверху окна, слева от адреса сайта, нажмите на иконку — замок, «i» в круге или значок настроек сайта.',
        'Выберите «Разрешения для этого сайта» (или сразу «Уведомления»).',
        'Установите «Уведомления» → «Разрешить».',
        'Обновите страницу (F5 или ↻).',
      ],
      en: [
        'At the top, left of the site address, click the icon — lock, circled “i”, or site settings.',
        'Choose “Permissions for this site” (or “Notifications” directly).',
        'Set “Notifications” to “Allow”.',
        'Refresh the page (F5 or ↻).',
      ],
    },
  },
  firefox: {
    name: { ru: 'Mozilla Firefox', en: 'Mozilla Firefox' },
    addressBarHint: {
      ru: 'Вверху окна: [ 🔒 или (i) ]  адрес-сайта  …  ← нажмите слева от адреса',
      en: 'Top of the window: [ 🔒 or (i) ]  site-address  …  ← click left of the address',
    },
    steps: {
      ru: [
        'Слева от адреса нажмите на значок замка или «i» в круге.',
        'В панели нажмите «>» у строки «Отправлять уведомления» (или «Разрешения» → «Уведомления»).',
        'Снимите галочку «Блокировать» и выберите «Разрешить».',
        'Обновите страницу (F5).',
      ],
      en: [
        'Left of the address, click the lock or circled “i”.',
        'In the panel, click “>” next to “Send notifications” (or “Permissions” → “Notifications”).',
        'Uncheck “Block” and choose “Allow”.',
        'Refresh the page (F5).',
      ],
    },
  },
  safari: {
    name: { ru: 'Safari (Mac)', en: 'Safari (Mac)' },
    addressBarHint: {
      ru: 'В строке адреса слева может быть кнопка «aA» — или откройте меню Safari',
      en: 'In the address bar look for “aA” on the left — or use the Safari menu',
    },
    steps: {
      ru: [
        'Вариант 1: в строке адреса нажмите «aA» слева → «Настройки для этого веб-сайта» → «Уведомления» → «Разрешить».',
        'Вариант 2: меню Safari (вверху экрана) → «Настройки для…» → вкладка «Веб-сайты» → «Уведомления» → найдите этот сайт и выберите «Разрешить».',
        'Обновите страницу (Cmd+R).',
      ],
      en: [
        'Option 1: in the address bar click “aA” on the left → “Settings for This Website” → “Notifications” → “Allow”.',
        'Option 2: Safari menu (top of screen) → “Settings for…” → “Websites” tab → “Notifications” → find this site and choose “Allow”.',
        'Refresh the page (Cmd+R).',
      ],
    },
  },
  'safari-ios': {
    name: { ru: 'Safari (iPhone / iPad)', en: 'Safari (iPhone / iPad)' },
    addressBarHint: null,
    steps: {
      ru: [
        'Нажмите «Поделиться» внизу экрана (квадрат со стрелкой вверх).',
        'Выберите «На экран "Домой"» — сайт станет иконкой, как приложение.',
        'Откройте сайт с главного экрана. Когда спросит про уведомления — нажмите «Разрешить».',
        'Если уже отказали: «Настройки» iPhone → «Уведомления» → найдите ПодписOFF и включите.',
      ],
      en: [
        'Tap Share at the bottom (square with arrow up).',
        'Choose “Add to Home Screen” — the site becomes an app icon.',
        'Open it from the home screen. When asked about notifications, tap “Allow”.',
        'If you denied before: iPhone Settings → Notifications → find SubOFF and turn on.',
      ],
    },
  },
  opera: {
    name: { ru: 'Opera', en: 'Opera' },
    addressBarHint: {
      ru: 'Вверху окна: [ иконка ]  адрес-сайта  …  ← нажмите слева от адреса',
      en: 'Top of the window: [ icon ]  site-address  …  ← click left of the address',
    },
    steps: {
      ru: [
        'Слева от адреса нажмите на замок или «i».',
        'Найдите «Уведомления» в списке разрешений.',
        'Выберите «Разрешить».',
        'Обновите страницу (F5).',
      ],
      en: [
        'Left of the address, click the lock or “i”.',
        'Find “Notifications” in the permissions list.',
        'Choose “Allow”.',
        'Refresh the page (F5).',
      ],
    },
  },
  samsung: {
    name: { ru: 'Samsung Internet', en: 'Samsung Internet' },
    addressBarHint: {
      ru: 'Вверху: значок замка или меню ≡ слева от адреса',
      en: 'At the top: lock icon or ≡ menu left of the address',
    },
    steps: {
      ru: [
        'Нажмите на замок слева от адреса (или меню ≡ → «Настройки»).',
        'Откройте «Разрешения сайта» → «Уведомления».',
        'Включите «Разрешить».',
        'Обновите страницу.',
      ],
      en: [
        'Tap the lock left of the address (or ≡ menu → Settings).',
        'Open “Site permissions” → “Notifications”.',
        'Turn on “Allow”.',
        'Refresh the page.',
      ],
    },
  },
  'chrome-android': {
    name: { ru: 'Chrome (Android)', en: 'Chrome (Android)' },
    addressBarHint: null,
    steps: {
      ru: [
        'Справа от адреса нажмите на ⋮ (три точки).',
        'Выберите «Настройки сайта» или «Сведения о сайте».',
        'Нажмите «Уведомления» → «Разрешить».',
        'Вернитесь и обновите страницу.',
      ],
      en: [
        'Right of the address, tap ⋮ (three dots).',
        'Choose “Site settings” or “Site information”.',
        'Tap “Notifications” → “Allow”.',
        'Go back and refresh the page.',
      ],
    },
  },
  unknown: {
    name: { ru: 'Другой браузер', en: 'Other browser' },
    addressBarHint: {
      ru: 'Ищите иконку слева от адреса сайта в самом верху окна',
      en: 'Look for an icon to the left of the site address at the top of the window',
    },
    steps: {
      ru: [
        'Вверху окна найдите строку с адресом сайта. Слева от неё обычно есть иконка (замок, «i», шестерёнка или шар) — нажмите на неё.',
        'В меню найдите «Уведомления», «Разрешения» или «Permissions».',
        'Включите или выберите «Разрешить» / «Allow».',
        'Обновите страницу.',
      ],
      en: [
        'At the top, find the address bar. Left of it there is usually an icon (lock, “i”, gear, or globe) — click it.',
        'In the menu, find “Notifications” or “Permissions”.',
        'Turn on or select “Allow”.',
        'Refresh the page.',
      ],
    },
  },
};

export function getNotificationGuide(id: BrowserGuideId): BrowserNotificationGuide {
  return notificationGuides[id];
}
