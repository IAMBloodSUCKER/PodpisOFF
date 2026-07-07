import { LocaleCode } from '../types/api';

export interface TermsSection {
  title: string;
  text: string;
}

export const termsByLocale: Record<LocaleCode, { documentTitle: string; sections: TermsSection[] }> = {
  ru: {
    documentTitle: 'Условия использования ПодписOFF',
    sections: [
      {
        title: '1. О сервисе',
        text: 'ПодписOFF — учётный инструмент для отслеживания подписок. Сервис не подключается к банку, не списывает деньги и не даёт финансовых или инвестиционных рекомендаций. Все суммы и даты вы вносите самостоятельно.',
      },
      {
        title: '2. Регистрация и аккаунт',
        text: 'Для создания аккаунта нужны логин и пароль. Email необязателен. При регистрации вы получаете ключ восстановления — сохраните его в надёжном месте. Без ключа восстановить пароль нельзя. Вы отвечаете за сохранность логина, пароля и ключа.',
      },
      {
        title: '3. Тарифы',
        text: 'Бесплатный тариф — до 3 подписок. Pro — расширенные возможности (без лимита подписок, экспорт CSV, расширенные напоминания) за плату, когда оплата будет доступна. Условия и цены Pro могут обновляться; актуальная информация отображается в приложении.',
      },
      {
        title: '4. Данные и конфиденциальность',
        text: 'Мы храним данные вашего аккаунта и введённые вами подписки для работы сервиса. Пароль хранится в зашифрованном виде. Мы не продаём ваши данные третьим лицам. Вы можете удалить подписки и прекратить пользоваться сервисом в любой момент.',
      },
      {
        title: '5. Напоминания',
        text: 'Напоминания о списаниях строятся на данных, которые вы указали. Мы не гарантируем, что сообщение на экране или письмо дойдут вовремя (сбой сети, настройки телефона). Рекомендуем иногда заглядывать на главную страницу сами.',
      },
      {
        title: '6. Ограничение ответственности',
        text: 'Сервис предоставляется «как есть». Мы не несём ответственности за пропущенные списания, неточности в ваших данных или косвенные убытки из-за использования или недоступности сервиса.',
      },
      {
        title: '7. Принятие условий',
        text: 'Отмечая «Я принимаю условия использования» при регистрации, вы подтверждаете, что прочитали и согласны с этими условиями. Если вы не согласны — не создавайте аккаунт.',
      },
    ],
  },
  en: {
    documentTitle: 'SubOFF Terms of Use',
    sections: [
      {
        title: '1. About the service',
        text: 'SubOFF is a subscription tracking tool. It does not connect to your bank, does not charge money, and does not provide financial or investment advice. All amounts and dates are entered by you.',
      },
      {
        title: '2. Registration and account',
        text: 'You need a username and password to create an account. Email is optional. On registration you receive a recovery key — store it securely. Without it, password reset is impossible. You are responsible for keeping your credentials safe.',
      },
      {
        title: '3. Plans',
        text: 'Free plan — up to 3 subscriptions. Pro — extended features (unlimited subscriptions, CSV export, advanced reminders) for a fee when billing is enabled. Pro pricing and features may change; current details are shown in the app.',
      },
      {
        title: '4. Data and privacy',
        text: 'We store your account data and subscriptions you enter to operate the service. Passwords are stored hashed. We do not sell your data to third parties. You may delete subscriptions and stop using the service at any time.',
      },
      {
        title: '5. Reminders',
        text: 'Billing reminders are based on data you provide. We cannot guarantee that on-screen messages or emails arrive on time (network or phone settings may interfere). Check the main page from time to time.',
      },
      {
        title: '6. Limitation of liability',
        text: 'The service is provided "as is". We are not liable for missed charges, inaccuracies in your data, or indirect losses from use or unavailability of the service.',
      },
      {
        title: '7. Acceptance',
        text: 'By checking "I accept the terms of use" when registering, you confirm that you have read and agree to these terms. If you do not agree, do not create an account.',
      },
    ],
  },
};
