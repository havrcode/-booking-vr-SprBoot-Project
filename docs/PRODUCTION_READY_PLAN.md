# Production Ready Plan

Цей документ описує, що саме входить у завершену версію сервісу бронювання Virtum VR.

## Назва сервісу

Технічно це `booking API` або `booking service`: окремий backend-сервіс, який:

- зберігає послуги, ціни, бронювання і закриті слоти;
- віддає public API для сайту `virtum-vr.com.ua`;
- віддає public booking widget для кнопки `Забронювати`;
- має admin panel для управління бронюваннями, послугами і доступністю;
- надсилає Telegram-нотифікації адміністратору.

## Цільова архітектура

```text
virtum-vr.com.ua
  └─ кнопка "Забронювати"
       └─ booking widget assets
            └─ https://booking-api.virtum-vr.com.ua
                 ├─ Public API
                 ├─ Admin API
                 ├─ Admin UI
                 ├─ PostgreSQL
                 └─ Telegram Bot API
```

## Що вже закрито в коді

- Public services API: `GET /api/v1/services`.
- Public booking API: `POST /api/v1/bookings`.
- Public day bookings API: `GET /api/v1/bookings?date=YYYY-MM-DD`.
- Public availability blocks API: `GET /api/v1/availability-blocks?date=YYYY-MM-DD`.
- Admin bookings API: перегляд і скасування бронювань.
- Admin services API: створення, редагування, увімкнення/вимкнення послуг і цін.
- Admin availability API: закриття і відкриття слотів.
- Admin UI: вкладки `Бронювання`, `Послуги`, `Доступність`.
- Public widget: modal-форма бронювання для існуючого сайту.
- Telegram notifications: нове бронювання і зміна статусу.
- PostgreSQL production profile.
- Flyway migrations.
- CORS для `virtum-vr.com.ua`.
- Nginx config example.

## Що треба зробити на сервері

1. Створити DNS record:

```text
booking-api.virtum-vr.com.ua -> server IP
```

2. Поставити PostgreSQL або підняти його через:

```bash
docker compose -f deploy/docker-compose.postgres.yml --env-file .env up -d
```

3. Задати production ENV:

```text
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=8080
DB_HOST=localhost
DB_PORT=5432
DB_NAME=virtum_booking
DB_USER=virtum_booking
DB_PASSWORD=<strong-password>
ADMIN_API_KEY=<long-random-admin-key>
TELEGRAM_NOTIFICATIONS_ENABLED=true
TELEGRAM_BOT_TOKEN=<token-from-botfather>
TELEGRAM_CHAT_ID=<admin-chat-id>
```

4. Запустити backend.

5. Налаштувати Nginx + HTTPS за прикладом:

```text
deploy/nginx-booking-api.conf
```

6. Перевірити:

```text
https://booking-api.virtum-vr.com.ua/actuator/health
https://booking-api.virtum-vr.com.ua/admin.html
https://booking-api.virtum-vr.com.ua/widget/booking-widget.js
```

7. Підключити widget assets до `virtum-vr.com.ua`.

## Acceptance Checklist

- `GET /api/v1/services` повертає активні послуги.
- `POST /api/v1/bookings` створює бронювання.
- Повторне бронювання того самого часу повертає `409`.
- Скасоване бронювання не блокує слот.
- Закритий адміністратором слот повертає `409` при бронюванні.
- Public widget не показує зайняті або закриті слоти як доступні.
- Admin UI дозволяє редагувати ціни.
- Admin UI дозволяє вимикати послуги.
- Admin UI дозволяє закривати і відкривати слоти.
- Telegram отримує повідомлення про нове бронювання.
- Telegram отримує повідомлення про скасування бронювання.
- Production працює з PostgreSQL, не з H2.
- Admin API key не потрапляє на публічний сайт.

## Що можна додати пізніше

Це не блокує launch, але може бути наступним етапом:

- окрема сутність `rooms` або `vr-stations`, якщо треба бронювати конкретні фізичні VR-місця;
- SMS або email-підтвердження клієнту;
- онлайн-оплата;
- календарний dashboard по днях/тижнях;
- audit log для дій адміністратора.
