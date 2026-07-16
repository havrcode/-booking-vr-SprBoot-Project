# booking-vr-SprBoot

Готовий backend-проєкт бронювання VR-послуг на Spring Boot для інтеграції з `virtum-vr.com.ua`.

## Що реалізовано
- REST API для отримання послуг, створення бронювання, перегляду бронювань на день.
- Валідація payload та помилки у JSON форматі.
- Захист від переповнення слотів з configurable capacity через `MAX_CONCURRENT_BOOKINGS`.
- Public API для зайнятих слотів не віддає імʼя, телефон, email або коментар клієнта.
- Початкові дані з VR-послугами.
- CORS налаштовано через `app.cors.allowed-origins`.
- Flyway міграції для схеми бази даних.
- Production-профіль для PostgreSQL.
- Admin API та проста адмін-сторінка для перегляду/скасування бронювань.
- Керування VR-послугами з адмінки: створення, редагування, увімкнення/вимкнення.
- Керування доступністю з адмінки: закриття/відкриття слотів без створення фейкових бронювань.
- Ручна оплата: оплата в клубі або переказ на карту з optional upload скріну підтвердження.
- Public booking widget для підключення до кнопки `Забронювати` на існуючому сайті.
- Telegram-нотифікації про нові бронювання та зміну статусу.

## Документація
Поглиблений опис структури проекту:

```text
docs/PROJECT_STRUCTURE.md
```

План завершення і запуску:

```text
docs/PRODUCTION_READY_PLAN.md
docs/PRODUCTION_DEPLOYMENT_GUIDE_UA.md
docs/TESTING_AND_LAUNCH.md
```

## API
### `GET /api/v1/services`
Список активних послуг. `price` завжди у гривнях, `currency` завжди `UAH`.
Для публічного бронювання базовий прайс: пн-пт `400 грн/год`, сб-нд `500 грн/год`.

```json
[
  {
    "slug": "vr-party-60",
    "title": "VR-вечірка 60 хв",
    "durationMinutes": 60,
    "price": 400.00,
    "currency": "UAH",
    "active": true
  },
  {
    "slug": "vr-marathon-120",
    "title": "VR-марафон 120 хв",
    "durationMinutes": 120,
    "price": 800.00,
    "currency": "UAH",
    "active": true
  }
]
```

### `GET /api/v1/booking-settings`
Публічні налаштування бронювання:

```json
{
  "maxConcurrentBookings": 2,
  "schedule": {
    "openTime": "09:30",
    "closeTime": "20:30",
    "breakStart": "14:30",
    "breakEnd": "15:30",
    "slotStepMinutes": 60
  },
  "payment": {
    "payAtClubEnabled": true,
    "cardTransferEnabled": false,
    "cardHolder": null,
    "cardNumber": null,
    "cardBank": null,
    "cardTransferNote": "Після переказу можна додати скрін підтвердження.",
    "maxProofSizeBytes": 8388608
  }
}
```

### `POST /api/v1/bookings`
```json
{
  "serviceSlug": "vr-party-60",
  "customerName": "Іван Петренко",
  "customerPhone": "+380501234567",
  "customerComment": "День народження, будемо з дітьми",
  "startsAt": "2026-05-20T09:30:00",
  "helmetsCount": 1,
  "paymentMethod": "PAY_AT_CLUB"
}
```

`customerEmail` можна передати додатково, але для публічної форми він не обовʼязковий.

Підтримувані `paymentMethod`:

```text
PAY_AT_CLUB
CARD_TRANSFER
```

### `POST /api/v1/bookings/{id}/payment-proof?token=...`
Optional upload скріну підтвердження оплати. Використовується `multipart/form-data` поле `file`. Token повертається у відповіді створення бронювання як `paymentUploadToken`.

### `GET /api/v1/bookings?date=2026-05-20`
Список зайнятих інтервалів за день для public widget. Endpoint спеціально не повертає персональні дані клієнтів.

```json
[
  {
    "startsAt": "2026-05-20T09:30:00",
    "endsAt": "2026-05-20T10:30:00",
    "helmetsCount": 1
  }
]
```

### `GET /api/v1/booking-days`
Доступність днів для календаря на сайті. Діапазон до 62 днів.

```text
from=2026-05-20
to=2026-07-20
serviceSlug=vr-party-60
helmetsCount=1
```

```json
[
  {
    "date": "2026-05-20",
    "available": true,
    "availableSlots": 8
  }
]
```

### `GET /api/v1/availability-blocks?date=2026-05-20`
Список закритих адміністратором періодів за день. Public widget використовує цей endpoint, щоб не показувати закриті слоти як доступні.

## Локальний запуск
```bash
mvn spring-boot:run
```

Локально застосовується H2 in-memory база у PostgreSQL mode. Схема створюється через Flyway:

```bash
open http://localhost:8080/h2-console
```

JDBC URL:

```text
jdbc:h2:mem:bookingdb
```

## Тести
Звичайний test suite:

```bash
mvn test
```

Проект має два рівні integration tests:

- H2 tests для швидкої локальної перевірки;
- PostgreSQL profile test через Testcontainers.

PostgreSQL test піднімає `postgres:16-alpine`, проганяє Flyway migrations і перевіряє booking/admin flow. Якщо Docker недоступний, цей test class пропускається.

## Production запуск з PostgreSQL
1. Скопіювати `.env.example` у `.env` і задати реальний `DB_PASSWORD`.
2. Підняти PostgreSQL:
```bash
docker compose -f deploy/docker-compose.postgres.yml --env-file .env up -d
```
3. Запустити backend з production-профілем:
```bash
SPRING_PROFILES_ACTIVE=prod mvn spring-boot:run
```

Основні env-змінні:

| Змінна | Приклад |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `SERVER_PORT` | `8080` |
| `APP_TIME_ZONE` | `Europe/Kyiv` |
| `DB_HOST` | `localhost` |
| `DB_PORT` | `5432` |
| `DB_NAME` | `virtum_booking` |
| `DB_USER` | `virtum_booking` |
| `DB_PASSWORD` | `strong-password` |
| `MAX_CONCURRENT_BOOKINGS` | `2` |
| `BOOKING_OPEN_TIME` | `09:30` |
| `BOOKING_CLOSE_TIME` | `20:30` |
| `BOOKING_BREAK_START` | `14:30` |
| `BOOKING_BREAK_END` | `15:30` |
| `BOOKING_SLOT_STEP_MINUTES` | `60` |
| `PAYMENT_PAY_AT_CLUB_ENABLED` | `true` |
| `PAYMENT_CARD_TRANSFER_ENABLED` | `true` |
| `PAYMENT_CARD_HOLDER` | `Virtum VR` |
| `PAYMENT_CARD_NUMBER` | `4444555566667777` |
| `PAYMENT_CARD_BANK` | `mono/privat` |
| `PAYMENT_CARD_TRANSFER_NOTE` | `Вкажіть номер бронювання в коментарі` |
| `PAYMENT_PROOFS_DIR` | `data/payment-proofs` |
| `PAYMENT_PROOF_MAX_FILE_SIZE` | `8MB` |
| `PAYMENT_PROOF_MAX_BYTES` | `8388608` |
| `ADMIN_API_KEY` | `long-random-admin-key` |
| `TELEGRAM_NOTIFICATIONS_ENABLED` | `true` |
| `TELEGRAM_BOT_TOKEN` | `123456:bot-token` |
| `TELEGRAM_CHAT_ID` | `123456789` |

### Час і timezone

Бізнес-час бронювання рахується в timezone `Europe/Kyiv` через `APP_TIME_ZONE`. Ця зона автоматично враховує перехід України на літній і зимовий час.

Синхронізацію системного годинника має робити сервер через NTP (`systemd-timesyncd`, `chrony` або аналог). Spring Boot не змінює системний час самостійно: це потребує root-доступу і може ламати інші сервіси. Якщо під час NTP-синхронізації немає мережі, системний сервіс часу сам повторює спроби без помилки для застосунку.

`MAX_CONCURRENT_BOOKINGS` - це кількість бронювань, які можна прийняти на один і той самий часовий інтервал. Для двох активних VR-шоломів використовується `2`. Обідня пауза `14:30-15:30` і час поза графіком `09:30-20:30` блокуються backend-ом.
`BOOKING_SLOT_STEP_MINUTES=60` означає, що стартові слоти йдуть щогодини від `09:30`: `09:30`, `10:30`, `11:30` тощо.

Щоб показати клієнту реквізити карти, вистав `PAYMENT_CARD_TRANSFER_ENABLED=true` і заповни `PAYMENT_CARD_NUMBER`, `PAYMENT_CARD_HOLDER`, `PAYMENT_CARD_BANK`. Файли зі скрінами оплати зберігаються у `PAYMENT_PROOFS_DIR`; ця директорія не повинна комітитись у git.

## Підключення до virtum-vr.com.ua
1. Підняти цей backend на домені, наприклад `https://booking-api.virtum-vr.com.ua`.
2. На сайті викликати:
```js
fetch('https://booking-api.virtum-vr.com.ua/api/v1/services')
```
3. Для бронювання відправляти `POST /api/v1/bookings` із форми сайту.

Готовий JS-клієнт для форми є тут:

```text
docs/frontend/virtum-booking-api.js
```

Готовий drop-in widget для кнопки `Забронювати` описаний тут:

```text
docs/frontend/booking-widget-install.md
```

Після deployment backend буде віддавати widget assets:

```text
https://booking-api.virtum-vr.com.ua/widget/booking-widget.css
https://booking-api.virtum-vr.com.ua/widget/booking-widget.js
```

Очікувані імена полів HTML-форми:

```text
serviceSlug
customerName
customerPhone
customerComment
date
time
helmetsCount
paymentMethod
paymentProof
```

Приклад використання:

```js
import { bindBookingForm } from './virtum-booking-api.js';

bindBookingForm(document.querySelector('#booking-form'), {
  onSuccess: () => alert('Бронювання створено'),
  onError: (error) => alert(error.message),
});
```

## Reverse proxy
Приклад Nginx-конфігу для `booking-api.virtum-vr.com.ua`:

```text
deploy/nginx-booking-api.conf
```

## Admin API
Адмінські endpoints захищені header-ом:

```text
X-Admin-Api-Key: <ADMIN_API_KEY>
```

### `GET /api/v1/admin/bookings`
Повертає бронювання за період. Параметри необовʼязкові:

```text
from=2026-05-20
to=2026-05-31
status=CONFIRMED
```

Якщо `from/to` не передані, backend повертає бронювання від сьогодні на 30 днів вперед.

### `PATCH /api/v1/admin/bookings/{id}/status`
Оновлює статус бронювання.

```json
{
  "status": "CANCELLED"
}
```

Підтримувані статуси:

```text
CONFIRMED
CANCELLED
```

### `PATCH /api/v1/admin/bookings/{id}/payment-status`
Оновлює статус ручної оплати.

```json
{
  "paymentStatus": "PAID"
}
```

Підтримувані статуси:

```text
UNPAID
PENDING_REVIEW
PAID
```

### `GET /api/v1/admin/bookings/{id}/payment-proof`
Повертає завантажений клієнтом скрін підтвердження оплати. Endpoint захищений `X-Admin-Api-Key`.

### `GET /api/v1/admin/services`
Повертає всі послуги для адмінки, включно з неактивними.

Ціни в API зберігаються і повертаються у гривнях. У response для послуг і бронювань додатково повертається `currency: "UAH"`.

### `POST /api/v1/admin/services`
Створює нову VR-послугу.

```json
{
  "slug": "vr-marathon-120",
  "title": "VR-марафон 120 хв",
  "durationMinutes": 120,
  "price": 800.00,
  "active": true
}
```

### `PUT /api/v1/admin/services/{id}`
Оновлює послугу. Щоб прибрати послугу з публічного сайту, відправ `active: false`.

### `GET /api/v1/admin/availability-blocks`
Повертає закриті слоти за період:

```text
from=2026-05-20
to=2026-05-31
```

### `POST /api/v1/admin/availability-blocks`
Закриває період для бронювання:

```json
{
  "startsAt": "2026-05-20T15:30:00",
  "endsAt": "2026-05-20T17:30:00",
  "reason": "Приватна подія"
}
```

### `DELETE /api/v1/admin/availability-blocks/{id}`
Видаляє блокування і знову відкриває цей час для бронювання.

## Admin UI
Проста адмін-сторінка доступна після запуску backend:

```text
http://localhost:8080/admin.html
```

У production:

```text
https://booking-api.virtum-vr.com.ua/admin.html
```

В адмінці є три вкладки:

- `Бронювання` - перегляд, фільтрація, скасування бронювань і ручна перевірка оплати;
- `Послуги` - створення, редагування та увімкнення/вимкнення VR-послуг;
- `Доступність` - закриття/відкриття слотів, коли клуб не приймає бронювання.

## Telegram нотифікації
Backend може відправляти повідомлення в Telegram при:

- створенні нового бронювання;
- зміні статусу бронювання, наприклад `CONFIRMED` -> `CANCELLED`.

Відправка працює асинхронно: якщо Telegram відповідає повільно, створення бронювання не чекає на доставку повідомлення.

Локально нотифікації вимкнені. Для production потрібно задати env-змінні:

```text
TELEGRAM_NOTIFICATIONS_ENABLED=true
TELEGRAM_BOT_TOKEN=<token-from-botfather>
TELEGRAM_CHAT_ID=<chat-or-user-id>
```

## Наступний production крок
- Задеплоїти backend на staging-домен і підключити форму сайту `virtum-vr.com.ua` до API.
