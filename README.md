# booking-vr-SprBoot

Готовий backend-проєкт бронювання VR-послуг на Spring Boot для інтеграції з `virtum-vr.com.ua`.

## Що реалізовано
- REST API для отримання послуг, створення бронювання, перегляду бронювань на день.
- Валідація payload та помилки у JSON форматі.
- Захист від перетину слотів (конфлікт часу).
- Початкові дані з VR-послугами.
- CORS налаштовано через `app.cors.allowed-origins`.
- Flyway міграції для схеми бази даних.
- Production-профіль для PostgreSQL.

## API
### `GET /api/v1/services`
Список активних послуг.

### `POST /api/v1/bookings`
```json
{
  "serviceSlug": "vr-party-60",
  "customerName": "Іван Петренко",
  "customerPhone": "+380501234567",
  "customerEmail": "ivan@example.com",
  "startsAt": "2026-05-20T14:00:00"
}
```

### `GET /api/v1/bookings?date=2026-05-20`
Список бронювань за день.

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
| `DB_HOST` | `localhost` |
| `DB_PORT` | `5432` |
| `DB_NAME` | `virtum_booking` |
| `DB_USER` | `virtum_booking` |
| `DB_PASSWORD` | `strong-password` |

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

Очікувані імена полів HTML-форми:

```text
serviceSlug
customerName
customerPhone
customerEmail
date
time
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

## Наступний production крок
- Додати JWT/адмін-панель для перегляду та керування бронюваннями.
- Додати SMS/Telegram нотифікації.
- Додати інтеграційні тести для PostgreSQL профілю.
