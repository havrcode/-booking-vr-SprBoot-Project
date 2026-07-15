# Production Ready Plan

Цей документ описує, що саме входить у завершену версію сервісу бронювання Virtum VR.

## Назва сервісу

Технічно це `booking API` або `booking service`: окремий backend-сервіс, який:

- зберігає послуги, ціни, бронювання і закриті слоти;
- контролює кількість одночасних бронювань через capacity;
- веде ручну оплату: оплата в клубі або переказ на карту зі скріном підтвердження;
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
                 ├─ Payment proof file storage
                 └─ Telegram Bot API
```

## Що вже закрито в коді

- Public services API: `GET /api/v1/services`.
- Public booking settings API: `GET /api/v1/booking-settings`.
- Public booking API: `POST /api/v1/bookings`.
- Public payment proof upload API: `POST /api/v1/bookings/{id}/payment-proof`.
- Public day bookings API: `GET /api/v1/bookings?date=YYYY-MM-DD` повертає тільки зайняті інтервали без персональних даних клієнтів.
- Public availability blocks API: `GET /api/v1/availability-blocks?date=YYYY-MM-DD`.
- Admin bookings API: перегляд, скасування бронювань, перегляд скрінів оплати і зміна payment status.
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
MAX_CONCURRENT_BOOKINGS=1
PAYMENT_PAY_AT_CLUB_ENABLED=true
PAYMENT_CARD_TRANSFER_ENABLED=true
PAYMENT_CARD_HOLDER=<card-holder>
PAYMENT_CARD_NUMBER=<card-number>
PAYMENT_CARD_BANK=<bank-name>
PAYMENT_CARD_TRANSFER_NOTE=<short-note-for-client>
PAYMENT_PROOFS_DIR=data/payment-proofs
PAYMENT_PROOF_MAX_FILE_SIZE=8MB
PAYMENT_PROOF_MAX_BYTES=8388608
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
- `GET /api/v1/booking-settings` повертає production capacity.
- `GET /api/v1/booking-settings` повертає актуальні payment settings без admin секретів.
- `GET /api/v1/bookings?date=YYYY-MM-DD` не повертає імʼя, телефон або email клієнта.
- `POST /api/v1/bookings` створює бронювання.
- Бронювання того самого часу дозволяється тільки до ліміту `MAX_CONCURRENT_BOOKINGS`; наступне повертає `409`.
- Скасоване бронювання не блокує слот.
- Закритий адміністратором слот повертає `409` при бронюванні.
- Public widget не показує зайняті або закриті слоти як доступні.
- Public widget дозволяє обрати оплату в клубі або переказ на карту.
- Якщо `PAYMENT_CARD_TRANSFER_ENABLED=true`, widget показує реквізити карти.
- Клієнт може завантажити скрін підтвердження оплати.
- Admin UI показує payment status і дозволяє відкрити скрін.
- Admin UI дозволяє позначити бронювання як `PAID`.
- Admin UI дозволяє редагувати ціни.
- Admin UI дозволяє вимикати послуги.
- Admin UI дозволяє закривати і відкривати слоти.
- Telegram отримує повідомлення про нове бронювання.
- Telegram отримує повідомлення про скасування бронювання.
- Production працює з PostgreSQL, не з H2.
- Admin API key не потрапляє на публічний сайт.
- `MAX_CONCURRENT_BOOKINGS` відповідає реальній кількості одночасних клієнтів/груп, які клуб може прийняти.

## Що можна додати пізніше

Це не блокує launch, але може бути наступним етапом:

- окрема сутність `rooms` або `vr-stations`, якщо треба не тільки capacity, а й бронювання конкретного фізичного VR-місця;
- SMS або email-підтвердження клієнту;
- автоматична онлайн-оплата через платіжний шлюз;
- календарний dashboard по днях/тижнях;
- audit log для дій адміністратора.
