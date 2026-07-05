# Деплой ПодписOFF на Vultr

## Домены

| Язык | Домен |
|------|-------|
| RU (основной) | **podpisoff.ru** |
| EN (международный) | **suboff.app** (опционально, redirect на тот же VPS) |

Оба домена могут указывать на один VPS. Frontend выбирает locale по домену или настройке пользователя.

---

## Что понадобится

| Что | Зачем | Стоимость |
|-----|-------|-----------|
| Vultr VPS | Сервер | ~$10–12/мес |
| Домен podpisoff.ru | RU-бренд | ~500 ₽/год |
| Email SMTP | Напоминания | Resend free tier |
| ЮKassa | Оплата Pro | комиссия ~3% |

## 1. Создать VPS

1. [console.vultr.com](https://console.vultr.com/) → Cloud Compute
2. Ubuntu 24.04 LTS, 2 GB RAM (~$12/mo)
3. SSH Key, hostname: `podpisoff-prod`

## 2. Настройка сервера

```bash
ssh root@YOUR_VPS_IP
apt update && apt upgrade -y
apt install -y docker.io docker-compose-v2 git ufw fail2ban
ufw allow OpenSSH && ufw allow 80/tcp && ufw allow 443/tcp && ufw enable
adduser podpisoff && usermod -aG docker podpisoff
```

## 3. Деплой

```bash
su - podpisoff
git clone https://github.com/YOUR_USER/PodpisOFF.git
cd PodpisOFF
cp .env.example .env
nano .env
docker compose -f deploy/docker-compose.prod.yml up -d --build
```

## 4. DNS

```
A    @      YOUR_VPS_IP    # podpisoff.ru
A    www    YOUR_VPS_IP
```

## 5. SSL (Caddy)

```
podpisoff.ru {
    reverse_proxy frontend:80
}
podpisoff.ru/api/* {
    reverse_proxy backend:8080
}
```

## 6. Бэкапы

```bash
0 3 * * * docker exec podpisoff-postgres pg_dump -U podpisoff podpisoff | gzip > ~/backups/podpisoff-$(date +\%Y\%m\%d).sql.gz
```

## 7. Чеклист

- [ ] HTTPS на podpisoff.ru
- [ ] Health: `https://podpisoff.ru/actuator/health`
- [ ] Секреты не в git
- [ ] Политика конфиденциальности
- [ ] ЮKassa webhook (когда подключишь оплату)
