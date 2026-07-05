# ПодписOFF · SubOFF

<p align="center">
  <strong>RU:</strong> ПодписOFF — выключи лишние подписки<br>
  <strong>EN:</strong> SubOFF — turn off subscriptions you forgot about
</p>

**ПодписOFF** (международное имя **SubOFF**) — веб-приложение для учёта подписок: Netflix, VPN, Cursor, Яндекс Плюс и всё остальное в одном месте. Показывает, сколько денег уходит в месяц и в год, и напоминает перед списанием.

> **ПодписOFF** = «Подписка» + «OFF» (выключить). Короткий мемный бренд для РФ.  
> **SubOFF** — то же самое для англоязычной аудитории и GitHub/App Store.

---

## Содержание

1. [Ответы на главные вопросы](#ответы-на-главные-вопросы)
2. [Что это за приложение](#что-это-за-приложение)
3. [Сценарии использования](#сценарии-использования)
4. [Технологии и архитектура](#технологии-и-архитектура)
5. [Модель данных](#модель-данных)
6. [API — эндпоинты](#api--эндпоинты)
7. [Монетизация и оплата](#монетизация-и-оплата)
8. [Деплой на Vultr](#деплой-на-vultr)
9. [Roadmap MVP](#roadmap-mvp)
10. [Структура репозитория](#структура-репозитория)

Подробнее:

- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — схемы, потоки запросов, безопасность
- [`docs/API.md`](docs/API.md) — полный справочник API
- [`docs/DEPLOYMENT.md`](docs/DEPLOYMENT.md) — Vultr, Docker, домен, SSL, бэкапы
- [`docs/PAYMENTS.md`](docs/PAYMENTS.md) — приём оплаты, самозанятость, чеки

---

## Брендинг

| | RU | EN |
|---|-----|-----|
| **Название** | ПодписOFF | SubOFF |
| **Слоган** | Выключи лишние подписки | Turn off forgotten subscriptions |
| **Домен (цель)** | podpisoff.ru | suboff.app |
| **GitHub repo** | `PodpisOFF` или `SubOFF` | |
| **Package Java** | `com.podpisoff` | |
| **БД** | `podpisoff` | |

---

## Ответы на главные вопросы

### Веб или App Store / Google Play?

**Рекомендация: начать с веб-приложения (PWA).**

| Вариант | Плюсы | Минусы | Когда |
|---------|-------|--------|-------|
| **Web + PWA** | Быстро, один код, не нужен review Apple/Google, обновления мгновенно | Нет иконки в Store «из коробки» (но можно «Добавить на экран») | **MVP — сейчас** |
| **Обёртка (Capacitor)** | Одна кодовая база → Store | Всё равно нужны аккаунты разработчика | Фаза 2, если попросят пользователи |
| **Нативное приложение** | Лучший UX в Store | Два стека (iOS + Android), дорого для одного dev | Не сейчас |

**PWA** — это обычный сайт, который на телефоне можно установить как приложение (иконка на рабочем столе, полноэкранный режим). Для ПодписOFF этого достаточно: формы, списки, push/email-напоминания.

### Что нужно для App Store / Google Play (если позже)?

**Google Play (проще):**

- Аккаунт разработчика — **$25 один раз**
- APK/AAB (собирается через Capacitor из того же React)
- Иконка, скриншоты, описание
- Политика конфиденциальности (URL)
- Ревью 1–3 дня

**Apple App Store (сложнее):**

- Apple Developer Program — **$99 в год**
- Mac или CI (GitHub Actions + Mac runner) для сборки
- Строгий review (1–7 дней)
- Политика конфиденциальности, объяснение зачем приложению каждое разрешение

**Вывод:** не трать время и деньги на Store, пока нет 100+ активных пользователей в вебе.

### Vultr подойдёт для деплоя?

**Да.** [Vultr](https://console.vultr.com/) — нормальный VPS-хостинг для Docker-приложения.

Минимальная конфигурация для старта:

| Ресурс | Рекомендация | Цена (~) |
|--------|--------------|----------|
| VPS | 1 vCPU, 2 GB RAM, 55 GB SSD | $10–12/мес |
| Домен | `podpisoff.ru` | ~300–700 ₽/год |
| SSL | Let's Encrypt (бесплатно) | $0 |
| Бэкапы БД | Vultr Automated Backups или cron + pg_dump | +20% к VPS |

На одном VPS крутится: PostgreSQL + Spring Boot + Nginx (фронт + reverse proxy). Подробная инструкция — [`docs/DEPLOYMENT.md`](docs/DEPLOYMENT.md).

### Как хранить данные?

**PostgreSQL** на том же VPS (или Vultr Managed Database, если вырастешь).

- Пользователи, подписки, напоминания, настройки — всё в PostgreSQL
- Пароли — только **BCrypt-хэш**, как в [TheGreatHike](../TheGreatHike)
- JWT для сессий (access token + optional refresh)
- Бэкап: ежедневный `pg_dump` на Vultr Object Storage или второй диск

### Регистрация — как в TheGreatHike?

**Да, тот же подход** — проверенный и достаточный для MVP:

- Логин (username) + пароль
- BCrypt-хэш пароля
- JWT после входа
- «Запомнить меня» (длинный refresh token)
- Восстановление пароля по **ключевой фразе** (recovery key) — без email на старте
- Математическая капча при регистрации (как в TheGreatHike)

Email **не обязателен** на MVP. Позже добавишь опциональный email для напоминаний о списании.

### Как принимать платёж? Куда?

Для **России + самозанятость (НПД)** — оптимальный путь:

| Этап | Решение |
|------|---------|
| **MVP (бесплатно)** | Только free tier, без оплаты |
| **Первые платящие** | **ЮKassa** (работает с самозанятыми) |
| **Альтернатива** | CloudPayments, Robokassa, Tinkoff Acquiring |

**Что нужно сделать:**

1. Оформить **самозанятость** в приложении «Мой налог» (если ещё нет)
2. Зарегистрироваться в [ЮKassa](https://yookassa.ru/) как самозанятый
3. Подключить **рекуррентные платежи** (автопродление Pro) или разовую оплату «Pro на месяц»
4. ЮKassa сама отправляет чеки в ФНС — тебе не нужна отдельная касса

**Куда приходят деньги:** на твою карту/счёт, указанный при регистрации в ЮKassa. Комиссия ~3–3,5%.

Для **международных пользователей** (позже): Stripe или Lemon Squeezy (они выступают merchant of record — проще с налогами).

Подробно — [`docs/PAYMENTS.md`](docs/PAYMENTS.md).

---

## Что это за приложение

### Проблема

Люди подписываются на десятки сервисов и забывают:

- сколько платят в сумме;
- когда спишут деньги;
- на что подписались «на пробу» и не отменили.

### Решение

ПодписOFF — личный дашборд подписок:

- список всех подписок с ценой и периодом (месяц / год / неделя);
- **итого в месяц и в год**;
- **напоминание** за N дней до списания;
- категории (стриминг, софт, VPN, игры…);
- статус: активна / на паузе / **OFF** (отменена);
- заметка: «где отменить» (ссылка на настройки сервиса).

### Не медицинский / не финансовый совет

ПодписOFF — **учётный инструмент**. Не подключается к банку и не списывает деньги сам. Пользователь сам вносит данные.

---

## Сценарии использования

### 1. «Сколько я вообще плачу?»

Пользователь добавляет 8 подписок → видит: **4 280 ₽/мес · 51 360 ₽/год**.

### 2. «Забыл про пробный период»

Добавил подписку с датой первого списания → за 3 дня push/email: «Cursor спишет 1 990 ₽ 12 июля».

### 3. «Хочу срезать расходы»

Фильтр по категории «Стриминг» → видит 3 сервиса → переводит один в **OFF** → сумма пересчитывается.

### 4. «Делим Netflix с соседом»

Подписка с полем «моя доля» (50% от 799 ₽ = 399 ₽) — учитывается в статистике.

### 5. «Годовая vs месячная»

Подписка «VPN 3 990 ₽/год» автоматически показывает эквивалент **~333 ₽/мес** в общей сумме.

---

## Технологии и архитектура

### Стек (как TheGreatHike, но проще — monolith)

| Слой | Технология |
|------|------------|
| Backend | **Java 17**, **Spring Boot 3.4** (modular monolith) |
| API | REST, **OpenAPI 3** (Springdoc) |
| Auth | JWT (jjwt), BCrypt, recovery key |
| DB | **PostgreSQL 16** |
| Migrations | Flyway |
| Frontend | **React 19** + TypeScript + Vite |
| UI | CSS modules или Tailwind |
| Deploy | **Docker Compose**, Nginx |
| Notifications | Email (SMTP / Resend) + опционально Telegram Bot API |

### Почему monolith, а не микросервисы

TheGreatHike — gateway + auth + tracking. Для ПодписOFF на старте **один Spring Boot JAR** быстрее в разработке и деплое. Модули внутри (`auth`, `subscriptions`, `notifications`) — границы как в TheGreatHike, но один процесс.

```
┌─────────────┐     HTTPS      ┌──────────────────────────────────┐
│   Browser   │ ─────────────► │  Nginx :443                      │
│   (PWA)     │                │  ├─ /      → React static        │
└─────────────┘                │  └─ /api/* → Spring Boot :8080   │
                               └──────────────┬───────────────────┘
                                              │
                               ┌──────────────▼───────────────────┐
                               │  PostgreSQL :5432                  │
                               └──────────────────────────────────┘
```

Подробные диаграммы — [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

---

## Модель данных

### Основные таблицы

```
users
  id              UUID PK
  username        VARCHAR(64) UNIQUE
  password_hash   TEXT
  recovery_key_hash TEXT
  email           VARCHAR(255) NULL      -- опционально, для напоминаний
  plan            ENUM(free, pro)        -- тариф
  plan_expires_at TIMESTAMP NULL
  timezone        VARCHAR(64)          -- Europe/Moscow
  locale          VARCHAR(8)           -- ru | en
  created_at      TIMESTAMP

subscriptions
  id              UUID PK
  user_id         UUID FK → users
  name            VARCHAR(128)           -- «Netflix», «Cursor»
  amount          DECIMAL(12,2)          -- сумма платежа
  currency        CHAR(3) DEFAULT 'RUB'
  billing_period  ENUM(weekly, monthly, yearly, custom)
  billing_day     SMALLINT NULL          -- день месяца (1-31)
  next_billing_at DATE NULL              -- следующее списание
  category        VARCHAR(64)            -- streaming, software, ...
  status          ENUM(active, paused, off)   -- OFF = отменена
  share_percent   SMALLINT DEFAULT 100   -- моя доля (%)
  cancel_url      TEXT NULL              -- ссылка «где отменить»
  note            TEXT NULL
  created_at      TIMESTAMP
  updated_at      TIMESTAMP

reminders
  id              UUID PK
  subscription_id UUID FK
  days_before     SMALLINT               -- 1, 3, 7
  channel         ENUM(email, push, telegram)
  sent_at         TIMESTAMP NULL

notification_log
  id, user_id, type, payload, sent_at   -- аудит отправок
```

### Лимиты тарифов

| | Free | Pro |
|---|------|-----|
| Подписок | до 5 | без лимита |
| Напоминаний | 1 на подписку | несколько + email |
| Экспорт CSV | нет | да |
| Категории | базовые | свои |
| Цена | 0 | **149 ₽/мес** |

---

## API — эндпоинты

Базовый URL: `https://podpisoff.ru/api` (локально: `http://localhost:8080/api`).

Авторизация: `Authorization: Bearer <JWT>` (кроме auth и health).

### Auth (как TheGreatHike)

| Метод | Путь | Описание |
|-------|------|----------|
| GET | `/auth/captcha` | Капча для регистрации |
| GET | `/auth/username-available?username=` | Проверка занятости логина |
| POST | `/auth/register` | Регистрация |
| POST | `/auth/login` | Вход → JWT |
| POST | `/auth/logout` | Выход (invalidate refresh) |
| POST | `/auth/recover-password` | Сброс по recovery key |
| GET | `/auth/me` | Текущий пользователь |

### Subscriptions

| Метод | Путь | Описание |
|-------|------|----------|
| GET | `/subscriptions` | Список подписок пользователя |
| POST | `/subscriptions` | Создать |
| GET | `/subscriptions/{id}` | Одна подписка |
| PUT | `/subscriptions/{id}` | Обновить |
| DELETE | `/subscriptions/{id}` | Удалить |
| PATCH | `/subscriptions/{id}/status` | active / paused / **off** |

### Dashboard

| Метод | Путь | Описание |
|-------|------|----------|
| GET | `/dashboard/summary` | Итого мес/год, по категориям |
| GET | `/dashboard/upcoming` | Ближайшие списания (7/30 дней) |

### Reminders

| Метод | Путь | Описание |
|-------|------|----------|
| GET | `/subscriptions/{id}/reminders` | Напоминания подписки |
| PUT | `/subscriptions/{id}/reminders` | Настроить (Pro) |

### Billing (Pro)

| Метод | Путь | Описание |
|-------|------|----------|
| POST | `/billing/create-payment` | Создать платёж ЮKassa |
| POST | `/billing/webhook` | Webhook от ЮKassa |
| GET | `/billing/status` | Статус подписки Pro |

### Export (Pro)

| Метод | Путь | Описание |
|-------|------|----------|
| GET | `/export/subscriptions.csv` | CSV всех подписок |

### System

| Метод | Путь | Описание |
|-------|------|----------|
| GET | `/actuator/health` | Health check |

Примеры тел запросов — [`docs/API.md`](docs/API.md).

---

## Монетизация и оплата

### Модель

- **Freemium:** бесплатно до 5 подписок
- **Pro:** 149 ₽/мес или 1 490 ₽/год (−17%)

### Путь пользователя к оплате

```
Free user → упёрся в лимит / хочет email-напоминания
  → «Оформить Pro»
  → POST /billing/create-payment
  → редирект на страницу ЮKassa
  → оплата картой / SBP
  → webhook → plan = pro, plan_expires_at = +30 days
  → чек в «Мой налог» автоматически
```

### Самозанятость

Тебе достаточно статуса **НПД** + договор с ЮKassa. Не нужно ООО/ИП на старте. Лимит дохода самозанятого — 2,4 млн ₽/год (для ПодписOFF на старте более чем достаточно).

---

## Деплой на Vultr

Краткий чеклист:

1. Создать VPS на [console.vultr.com](https://console.vultr.com/) (Ubuntu 24.04, 2 GB RAM)
2. Установить Docker + Docker Compose
3. Клонировать репозиторий, заполнить `.env`
4. `docker compose up -d --build`
5. Настроить домен `podpisoff.ru` (A-запись → IP VPS)
6. Caddy или Nginx + Certbot для HTTPS
7. Cron: бэкап PostgreSQL

Полная инструкция — [`docs/DEPLOYMENT.md`](docs/DEPLOYMENT.md).

---

## Roadmap MVP

### Фаза 0 — документация ✅

- [x] README, архитектура, API, деплой, платежи

### Фаза 1 — MVP (3–4 недели)

- [ ] Spring Boot monolith: auth (как TheGreatHike)
- [ ] CRUD подписок
- [ ] Dashboard: сумма мес/год, upcoming
- [ ] React UI: список, форма, дашборд
- [ ] Docker Compose local
- [ ] PWA manifest + mobile layout
- [ ] i18n: RU (ПодписOFF) + EN (SubOFF)

### Фаза 2 — напоминания (1–2 недели)

- [ ] Scheduler: проверка `next_billing_at`
- [ ] Email-напоминания (SMTP)
- [ ] Опционально: Telegram bot только для алертов

### Фаза 3 — монетизация (1 неделя)

- [ ] ЮKassa интеграция
- [ ] Лимиты Free vs Pro
- [ ] Landing + политика конфиденциальности

### Фаза 4 — продакшен

- [ ] Деплой на Vultr
- [ ] Мониторинг (health + uptime)
- [ ] Первые 10 пользователей (друзья, Habr, Reddit)

---

## Структура репозитория

```
PodpisOFF/
├── README.md                 ← ты здесь
├── docs/
│   ├── ARCHITECTURE.md
│   ├── API.md
│   ├── DEPLOYMENT.md
│   └── PAYMENTS.md
├── backend/                  ← Spring Boot monolith (будет)
│   ├── src/main/java/com/podpisoff/
│   └── pom.xml
├── frontend/                 ← React PWA (будет)
│   ├── src/
│   └── package.json
├── docker-compose.yml
├── deploy/
│   ├── docker-compose.prod.yml
│   └── Caddyfile
├── .env.example
└── LICENSE
```

---

## Быстрый старт через Docker

```bash
git clone https://github.com/IAMBloodSUCKER/PodpisOFF.git
cd PodpisOFF
docker compose up --build -d
```

- Приложение: http://localhost:3000  
- API: http://localhost:8080/api  
- PostgreSQL: localhost:5433  

Остановить контейнеры:

```bash
docker compose down
```

Продакшен-стек (пример с Caddy):

```bash
docker compose -f deploy/docker-compose.prod.yml up -d
```

---

## Лицензия

MIT
