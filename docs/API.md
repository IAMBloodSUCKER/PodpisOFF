# ПодписOFF · SubOFF API

OpenAPI 3 спецификация будет генерироваться Springdoc при запуске backend:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Базовые правила

- Base path: `/api`
- Content-Type: `application/json`
- Auth: `Authorization: Bearer <access_token>`
- Locale: `Accept-Language: ru` или `en`
- Ошибки: `{ "error": "CODE", "message": "Human readable" }`
- HTTP коды: 200 OK, 201 Created, 400 Bad Request, 401 Unauthorized, 403 Forbidden, 404 Not Found, 409 Conflict

---

## Auth

### GET `/api/auth/captcha`

**Response 200:**

```json
{
  "captchaId": "550e8400-e29b-41d4-a716-446655440000",
  "imageBase64": "data:image/png;base64,..."
}
```

---

### GET `/api/auth/username-available?username=ivan`

**Response 200:**

```json
{
  "username": "ivan",
  "available": true
}
```

---

### POST `/api/auth/register`

**Request:**

```json
{
  "username": "ivan",
  "password": "SecurePass123",
  "recoveryKey": "correct horse battery staple",
  "captchaId": "550e8400-e29b-41d4-a716-446655440000",
  "captchaAnswer": "42",
  "termsAccepted": true,
  "locale": "ru"
}
```

**Response 201:**

```json
{
  "accessToken": "eyJhbG...",
  "expiresIn": 3600,
  "username": "ivan"
}
```

---

### POST `/api/auth/login`

**Request:**

```json
{
  "username": "ivan",
  "password": "SecurePass123",
  "rememberMe": true
}
```

**Response 200:**

```json
{
  "accessToken": "eyJhbG...",
  "expiresIn": 3600,
  "username": "ivan",
  "plan": "free",
  "locale": "ru"
}
```

---

### POST `/api/auth/recover-password`

**Request:**

```json
{
  "username": "ivan",
  "recoveryKey": "correct horse battery staple",
  "newPassword": "NewSecurePass456"
}
```

**Response 200:** `{ "success": true }`

---

### GET `/api/auth/me`

**Response 200:**

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "username": "ivan",
  "email": null,
  "plan": "free",
  "planExpiresAt": null,
  "timezone": "Europe/Moscow",
  "locale": "ru",
  "subscriptionCount": 3,
  "subscriptionLimit": 5
}
```

---

## Subscriptions

### GET `/api/subscriptions`

Query params (optional):

- `status` — active | paused | **off**
- `category` — streaming, software, ...

**Response 200:**

```json
{
  "items": [
    {
      "id": "...",
      "name": "Netflix",
      "amount": 799.00,
      "currency": "RUB",
      "billingPeriod": "monthly",
      "billingDay": 15,
      "nextBillingAt": "2026-08-15",
      "category": "streaming",
      "status": "active",
      "sharePercent": 100,
      "cancelUrl": "https://netflix.com/account",
      "note": "Семейный тариф",
      "monthlyEquivalent": 799.00,
      "createdAt": "2026-07-01T10:00:00Z"
    }
  ],
  "total": 1
}
```

---

### POST `/api/subscriptions`

**Request:**

```json
{
  "name": "Cursor",
  "amount": 1990.00,
  "currency": "RUB",
  "billingPeriod": "monthly",
  "billingDay": 5,
  "category": "software",
  "sharePercent": 100,
  "cancelUrl": "https://cursor.com/settings",
  "note": "Pro plan"
}
```

**Response 403** (free limit):

```json
{
  "error": "PLAN_LIMIT",
  "message": "Free plan allows up to 5 subscriptions. Upgrade to Pro."
}
```

---

### PATCH `/api/subscriptions/{id}/status`

**Request:**

```json
{
  "status": "off"
}
```

UI label RU: «Выключить (OFF)». EN: «Turn OFF».

---

## Dashboard

### GET `/api/dashboard/summary`

**Response 200:**

```json
{
  "monthlyTotal": 4280.50,
  "yearlyTotal": 51366.00,
  "currency": "RUB",
  "activeCount": 6,
  "offCount": 2,
  "byCategory": [
    { "category": "streaming", "monthlyTotal": 1598.00, "count": 2 }
  ]
}
```

---

### GET `/api/dashboard/upcoming?days=30`

**Response 200:**

```json
{
  "items": [
    {
      "subscriptionId": "...",
      "name": "Cursor",
      "amount": 1990.00,
      "currency": "RUB",
      "nextBillingAt": "2026-07-05",
      "daysUntil": 0
    }
  ]
}
```

---

## Reminders, Billing, Export, System

См. полные спецификации в README и предыдущей версии docs — эндпоинты без изменений:

- `GET/PUT /api/subscriptions/{id}/reminders`
- `POST /api/billing/create-payment`
- `POST /api/billing/webhook`
- `GET /api/billing/status`
- `GET /api/export/subscriptions.csv`
- `GET /actuator/health`
