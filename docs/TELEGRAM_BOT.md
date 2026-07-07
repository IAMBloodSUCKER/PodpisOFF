# Telegram-бот ПодписOFF

Официальный бот для сервисных уведомлений: напоминания о списаниях, ответы поддержки, смена тарифа.

Аватар для BotFather: [`docs/assets/telegram-bot-avatar.png`](../docs/assets/telegram-bot-avatar.png) (512×512, `/setuserpic` в BotFather).

## 1. Создание в BotFather

Откройте [@BotFather](https://t.me/BotFather) в Telegram.

### Имя и username

| Поле | RU (ПодписOFF) | EN (SubOFF) |
|------|----------------|-------------|
| **Имя бота (Name)** | `PodpisOFFBot` | `SubOFFBot` |
| **Username** | `PodpisOFF_bot` | `SubOFF_bot` |

> Username должен быть уникальным и заканчиваться на `bot`.

### Команды

```
start - Подключить аккаунт
help - Что присылает бот
stop - Отключить уведомления
```

В BotFather: `/setcommands` → выберите бота → вставьте список выше.

### Описания

**About (до 120 символов)** — `/setabouttext`:

```
Официальный бот ПодписOFF. Напоминания о списаниях и важные уведомления из аккаунта.
```

**Description (до 512 символов)** — `/setdescription`:

```
ПодписOFF помогает контролировать подписки.

Бот присылает:
• напоминания о предстоящих списаниях;
• ответы поддержки и сообщения администратора;
• уведомления о тарифе.

Подключение: Настройки → Уведомления → Подключить Telegram.
Отключить: /stop или кнопка в настройках на сайте.

Бот не читает переписку и не рассылает рекламу.
```

**Privacy Policy** — опционально, `/setprivacypolicy`:

```
https://podpisoff.app/privacy
```

(или пропустите, если страницы пока нет)

### Картинки (размеры обязательны)

| Файл | Размер | Команда BotFather |
|------|--------|-------------------|
| [`telegram-bot-avatar-512.png`](assets/telegram-bot-avatar-512.png) | **512×512** | `/setuserpic` |
| [`telegram-bot-description-640x360.png`](assets/telegram-bot-description-640x360.png) | **640×360** | `/setdescriptionpic` |

> Ошибка *«Must be 640x360»* — загружали квадратный аватар вместо description picture. Это **разные** поля.

### Аватар (botpic)

1. `/setuserpic` → `@PodpisOFF_bot`
2. Загрузите [`telegram-bot-avatar-512.png`](assets/telegram-bot-avatar-512.png)

### Картинка описания (description picture)

1. `/setdescriptionpic` → `@PodpisOFF_bot`
2. Загрузите [`telegram-bot-description-640x360.png`](assets/telegram-bot-description-640x360.png)

### Токен

1. `/newbot` или `/token` для существующего бота
2. Сохраните токен в `.env`:

```env
TELEGRAM_BOT_TOKEN=123456789:AAH...
TELEGRAM_BOT_USERNAME=PodpisOFF_bot
TELEGRAM_WEBHOOK_SECRET=случайная-длинная-строка
TELEGRAM_WEBHOOK_PUBLIC_URL=https://api.ваш-домен.ru
```

`TELEGRAM_BOT_USERNAME` — **без** `@`.

## 2. Webhook (продакшен)

Бот получает сообщения через webhook на бэкенд:

```
POST https://api.ваш-домен.ru/api/telegram/webhook
```

При старте приложения webhook регистрируется автоматически, если заданы `TELEGRAM_BOT_TOKEN` и `TELEGRAM_WEBHOOK_PUBLIC_URL`.

Ручная регистрация:

```bash
curl "https://api.telegram.org/bot<TOKEN>/setWebhook" \
  -d "url=https://api.ваш-домен.ru/api/telegram/webhook" \
  -d "secret_token=<TELEGRAM_WEBHOOK_SECRET>"
```

### Локальная разработка

Webhook требует публичный HTTPS. Варианты:

- [ngrok](https://ngrok.com/) → `TELEGRAM_WEBHOOK_PUBLIC_URL=https://xxxx.ngrok.io`
- Тестировать только отправку (без приёма `/start`) — не подходит для полного сценария
- Деплой на staging с реальным доменом

## 3. Сценарий для пользователя

### В настройках на сайте

1. **Настройки** → **Уведомления на почту и в Telegram**
2. Пользователь читает, что бот присылает
3. Ставит галочку **«Согласен получать уведомления от @PodpisOFFBot…»** — это принятие условий
4. Нажимает **«Подключить Telegram»**
5. Видит **QR-код** и **ссылку** вида `https://t.me/PodpisOFF_bot?start=<токен>`
6. На экране: «Ожидаем подключения…»

### В Telegram

1. Пользователь сканирует QR или открывает ссылку
2. Telegram открывает чат с ботом
3. Пользователь нажимает **«Запустить» / Start**
   - Telegram передаёт боту команду `/start <токен>`
4. Бэкенд по webhook:
   - проверяет одноразовый токен (15 минут)
   - сохраняет `chat_id` в аккаунт пользователя
   - включает `telegram_notifications_enabled`
5. Бот отвечает: *«Готово! Telegram подключён к аккаунту …»*

### На сайте

- Статус меняется на **«Telegram подключён»**
- Уведомления включены автоматически

### Отключение

- **На сайте:** «Отключить Telegram»
- **В боте:** кнопка «Отключить уведомления», команда `/stop` или «Остановить и блокировать» в профиле бота

## 4. Что присылает бот

Те же события, что и в колокольчике приложения (если включён канал Telegram):

- напоминания о списаниях
- ответы на отзыв / поддержку
- сообщения администратора
- смена тарифа

## 5. Безопасность

- Ссылка подключения одноразовая, срок 15 минут
- Webhook защищён `TELEGRAM_WEBHOOK_SECRET` (заголовок `X-Telegram-Bot-Api-Secret-Token`)
- Токен бота только на сервере, не в фронтенде
- Пользователь не вводит chat id вручную
