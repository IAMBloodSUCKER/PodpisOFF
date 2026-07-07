import { LocaleCode } from '../types/api';

export type MobilePlatform = 'android' | 'ios';

export interface MobileGuideSection {
  title: Record<LocaleCode, string>;
  steps: Record<LocaleCode, string[]>;
}

export const mobileInstallGuides: Record<MobilePlatform, MobileGuideSection> = {
  android: {
    title: { ru: 'Установка на Android', en: 'Install on Android' },
    steps: {
      ru: [
        'Откройте сайт в Chrome (или Яндекс Браузере).',
        'Нажмите ⋮ (три точки) справа вверху.',
        'Выберите «Установить приложение» или «Добавить на главный экран».',
        'Подтвердите — на главном экране появится иконка ПодписOFF, как у обычного приложения.',
        'Запускайте именно с этой иконки — так удобнее и надёжнее для напоминаний.',
      ],
      en: [
        'Open the site in Chrome (or Yandex Browser).',
        'Tap ⋮ (three dots) at the top right.',
        'Choose “Install app” or “Add to Home screen”.',
        'Confirm — a SubOFF icon will appear on your home screen.',
        'Launch from that icon — it works better for reminders.',
      ],
    },
  },
  ios: {
    title: { ru: 'Установка на iPhone / iPad', en: 'Install on iPhone / iPad' },
    steps: {
      ru: [
        'Откройте сайт в Safari (не в Chrome — на iPhone установка работает через Safari).',
        'Нажмите «Поделиться» внизу — квадрат со стрелкой вверх.',
        'Пролистайте вниз и выберите «На экран "Домой"».',
        'Нажмите «Добавить» — иконка появится на главном экране.',
        'Открывайте ПодписOFF только с этой иконки — так на iPhone работают напоминания.',
      ],
      en: [
        'Open the site in Safari (on iPhone, install works through Safari).',
        'Tap Share at the bottom — square with arrow up.',
        'Scroll down and choose “Add to Home Screen”.',
        'Tap “Add” — an icon appears on your home screen.',
        'Open SubOFF only from that icon — that is how reminders work on iPhone.',
      ],
    },
  },
};

export const mobileNotificationGuides: Record<MobilePlatform, MobileGuideSection> = {
  android: {
    title: { ru: 'Напоминания на Android', en: 'Reminders on Android' },
    steps: {
      ru: [
        'Откройте приложение с иконки на главном экране (не просто вкладку в браузере).',
        'На странице «Напоминания» нажмите «Разрешить уведомления» и выберите «Разрешить».',
        'В нужное время придёт сообщение на экран — как от банка или календаря.',
        'О списаниях подписок напомним так же, если включено в Настройках.',
        'Если закрыли приложение полностью — иногда напоминание может не прийти. Заглядывайте в приложение время от времени.',
      ],
      en: [
        'Open the app from the home screen icon (not just a browser tab).',
        'On Reminders, tap “Enable notifications” and choose “Allow”.',
        'At the scheduled time a message appears on screen — like from a bank or calendar.',
        'Subscription charge reminders work the same way if enabled in Settings.',
        'If you fully close the app, a reminder might not arrive. Check the app from time to time.',
      ],
    },
  },
  ios: {
    title: { ru: 'Напоминания на iPhone / iPad', en: 'Reminders on iPhone / iPad' },
    steps: {
      ru: [
        'Сначала добавьте на главный экран (инструкция выше) — без этого на iPhone напоминания не работают.',
        'Откройте ПодписOFF с иконки на главном экране.',
        'Разрешите уведомления, когда спросит.',
        'Сообщение появится на экране в заблокированном телефоне — как от других приложений.',
        'Если отказали раньше: Настройки iPhone → Уведомления → ПодписOFF → включить.',
        'Полностью закрытое приложение может не успеть напомнить — иногда открывайте сами.',
      ],
      en: [
        'First add to Home Screen (see above) — on iPhone reminders do not work without that.',
        'Open SubOFF from the home screen icon.',
        'Allow notifications when asked.',
        'A message will appear on the lock screen — like other apps.',
        'If you denied before: iPhone Settings → Notifications → SubOFF → turn on.',
        'A fully closed app may miss a reminder — open it sometimes yourself.',
      ],
    },
  },
};
