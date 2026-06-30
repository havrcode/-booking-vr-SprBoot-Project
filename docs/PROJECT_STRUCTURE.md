# Project Structure

Цей документ пояснює, як влаштований backend бронювання Virtum VR, які пакети за що відповідають, і як рухається запит від сайту до бази даних.

## High-level Architecture

Проект побудований як Spring Boot REST API:

```text
Website / Admin page
        |
        v
Spring MVC Controller
        |
        v
BookingService
        |
        v
Spring Data Repository
        |
        v
Database: H2 locally, PostgreSQL in production
```

Основна ідея:

- публічний сайт створює бронювання і дивиться зайняті слоти;
- адмінська частина переглядає бронювання та скасовує їх;
- бізнес-логіка живе в `BookingService`;
- структура бази контролюється Flyway migrations;
- production-конфіг відокремлений у `application-prod.yml`.

## Repository Root

```text
.
├── README.md
├── pom.xml
├── .env.example
├── deploy/
├── docs/
└── src/
```

### `pom.xml`

Maven-конфіг проекту. Тут визначені:

- Spring Boot parent;
- Java version;
- dependencies;
- build plugin.

Важливі dependencies:

- `spring-boot-starter-web` - REST API і статичні сторінки.
- `spring-boot-starter-data-jpa` - робота з БД через JPA/Hibernate.
- `spring-boot-starter-validation` - валідація DTO через annotations.
- `spring-boot-starter-actuator` - health/info endpoints.
- `flyway-core` і `flyway-database-postgresql` - міграції БД.
- `h2` - локальна in-memory база.
- `postgresql` - production database driver.
- `spring-boot-starter-test` - інтеграційні тести.

### `.env.example`

Шаблон environment variables для production/staging запуску.

Не коміть реальний `.env`: він ігнорується через `.gitignore`.

### `deploy/`

Інфраструктурні приклади для deployment:

```text
deploy/
├── docker-compose.postgres.yml
└── nginx-booking-api.conf
```

- `docker-compose.postgres.yml` піднімає PostgreSQL.
- `nginx-booking-api.conf` прокидує `booking-api.virtum-vr.com.ua` на Spring Boot app на порту `8080`.

### `docs/`

Документація та frontend snippets:

```text
docs/
├── PROJECT_STRUCTURE.md
└── frontend/
    └── virtum-booking-api.js
```

- `PROJECT_STRUCTURE.md` - цей файл.
- `frontend/virtum-booking-api.js` - готовий JS-клієнт для інтеграції форми на сайті `virtum-vr.com.ua`.

## Source Tree

```text
src/
├── main/
│   ├── java/ua/com/virtum/booking/
│   └── resources/
└── test/
    └── java/ua/com/virtum/booking/
```

## Java Packages

```text
ua.com.virtum.booking
├── BookingVrApplication.java
├── config/
├── controller/
├── dto/
├── entity/
├── exception/
├── notification/
├── repository/
└── service/
```

### `BookingVrApplication.java`

Spring Boot entry point.

Запускає application context і включає component scanning для всіх класів у `ua.com.virtum.booking`.

## `config/`

Пакет для технічних налаштувань.

```text
config/
├── AsyncConfig.java
├── AdminApiConfig.java
├── AdminApiKeyInterceptor.java
├── AdminProperties.java
├── CorsConfig.java
├── CorsProperties.java
└── DataSeeder.java
```

### `CorsConfig.java`

Налаштовує CORS для `/api/**`.

Дозволені origins беруться з:

```yaml
app:
  cors:
    allowed-origins:
```

У production дозволені тільки:

- `https://virtum-vr.com.ua`
- `https://www.virtum-vr.com.ua`

Локально також дозволені dev origins:

- `http://localhost:3000`
- `http://localhost:5173`

### `CorsProperties.java`

Configuration properties class для `app.cors`.

Spring автоматично підставляє значення з YAML у цей клас.

### `AdminApiConfig.java`

Підключає interceptor для всіх шляхів:

```text
/api/v1/admin/**
```

Це означає, що public API не потребує admin key, а admin API потребує.

### `AdminApiKeyInterceptor.java`

Перевіряє header:

```text
X-Admin-Api-Key: <ADMIN_API_KEY>
```

Якщо ключ відсутній або неправильний, endpoint повертає:

```json
{
  "status": 401,
  "error": "Admin API key is missing or invalid."
}
```

Це простий production-ready старт для одного адміністратора. Коли зʼявиться повноцінна адмін-панель з користувачами, цей механізм можна замінити на Spring Security + JWT.

### `AdminProperties.java`

Configuration properties class для:

```yaml
app:
  admin:
    api-key:
```

У production значення приходить з env:

```text
ADMIN_API_KEY
```

### `DataSeeder.java`

Створює початкові VR-послуги, якщо таблиця послуг порожня.

Зараз seed data:

- `vr-party-60`
- `vr-quest-90`
- `vr-kids-45`

Якщо треба змінити перелік послуг, найпростіший шлях зараз - оновити цей seeder. Для більш дорослого production flow краще зробити окремий admin endpoint для керування послугами або Flyway seed migration.

## `controller/`

Пакет REST controllers.

```text
controller/
├── BookingController.java
└── AdminBookingController.java
```

### `BookingController.java`

Public API для сайту.

Endpoints:

```text
GET  /api/v1/services
POST /api/v1/bookings
GET  /api/v1/bookings?date=YYYY-MM-DD
```

Використовується сайтом `virtum-vr.com.ua` та JS-клієнтом з `docs/frontend/virtum-booking-api.js`.

### `AdminBookingController.java`

Admin API для менеджера/адміністратора.

Endpoints:

```text
GET   /api/v1/admin/bookings
PATCH /api/v1/admin/bookings/{id}/status
```

Підтримувані query params для списку:

```text
from=2026-05-20
to=2026-05-31
status=CONFIRMED
```

Якщо `from/to` не передані, backend повертає бронювання від сьогодні на 30 днів вперед.

## `dto/`

DTO означає Data Transfer Object. Це обʼєкти, які входять у API або виходять з API.

```text
dto/
├── AdminBookingResponse.java
├── BookingResponse.java
├── CreateBookingRequest.java
└── UpdateBookingStatusRequest.java
```

### `CreateBookingRequest.java`

Payload для створення бронювання:

```json
{
  "serviceSlug": "vr-party-60",
  "customerName": "Іван Петренко",
  "customerPhone": "+380501234567",
  "customerEmail": "ivan@example.com",
  "startsAt": "2026-05-20T14:00:00"
}
```

Валідація:

- `serviceSlug` не порожній;
- `customerName` 2-120 символів;
- `customerPhone` має простий phone pattern;
- `customerEmail` має email format;
- `startsAt` має бути в майбутньому.

### `BookingResponse.java`

Public response для сайту.

Містить:

- id;
- service data;
- customer data;
- start/end time;
- status.

### `AdminBookingResponse.java`

Розширений response для адмінки.

Додатково містить:

- `createdAt`;
- повний статус;
- усі дані клієнта.

### `UpdateBookingStatusRequest.java`

Payload для зміни статусу:

```json
{
  "status": "CANCELLED"
}
```

## `entity/`

JPA entities, які відповідають таблицям у базі.

```text
entity/
├── Booking.java
├── BookingStatus.java
└── VrService.java
```

### `VrService.java`

Таблиця:

```text
vr_services
```

Описує послугу:

- `slug` - стабільний код для API;
- `title` - назва;
- `durationMinutes` - тривалість;
- `price` - ціна;
- `active` - чи показувати на сайті.

### `Booking.java`

Таблиця:

```text
bookings
```

Описує бронювання:

- service;
- customer name/phone/email;
- startsAt;
- endsAt;
- status;
- createdAt.

Важливо: `service` завантажується lazy. Тому методи, які маплять booking у DTO і читають `b.getService()`, мають працювати всередині transaction.

### `BookingStatus.java`

Підтримувані статуси:

```text
CONFIRMED
CANCELLED
```

Скасовані бронювання не блокують слот для нового бронювання.

## `repository/`

Spring Data repositories.

```text
repository/
├── BookingRepository.java
└── VrServiceRepository.java
```

### `VrServiceRepository.java`

Методи:

- знайти активні послуги;
- знайти активну послугу за slug.

### `BookingRepository.java`

Методи:

- знайти confirmed booking, що перетинається з новим слотом;
- знайти confirmed bookings за день для public API;
- знайти всі bookings за період для Admin API;
- знайти bookings за період і статусом.

Назви методів довгі, бо Spring Data будує SQL query з імені методу.

## `service/`

```text
service/
└── BookingService.java
```

Це головний шар бізнес-логіки.

Відповідає за:

- список послуг;
- створення бронювання;
- перевірку перетину слотів;
- список бронювань на день;
- admin list;
- зміну статусу бронювання.

### Booking Creation Flow

```text
POST /api/v1/bookings
        |
        v
CreateBookingRequest validation
        |
        v
BookingService.create()
        |
        v
Find active service by slug
        |
        v
Calculate endsAt from durationMinutes
        |
        v
Check overlap with CONFIRMED bookings
        |
        v
Save Booking
        |
        v
Return BookingResponse
```

### Slot Conflict Logic

Новий слот конфліктує з існуючим, якщо:

```text
existing.startsAt < new.endsAt
AND
existing.endsAt > new.startsAt
AND
existing.status = CONFIRMED
```

Це дозволяє:

- заборонити перетин часу;
- дозволити бронювання рівно після завершення попереднього;
- не блокувати слот скасованим бронюванням.

## `exception/`

```text
exception/
├── BadRequestException.java
├── ConflictException.java
├── GlobalExceptionHandler.java
└── NotFoundException.java
```

### `GlobalExceptionHandler.java`

Централізовано перетворює Java exceptions у JSON responses.

Приклади:

- `NotFoundException` -> `404`;
- `ConflictException` -> `409`;
- `BadRequestException` -> `400`;
- validation errors -> `400` з `fields`.

## Resources

```text
src/main/resources/
├── application.yml
├── application-prod.yml
├── db/migration/
└── static/
```

### `application.yml`

Default local profile:

- H2 in-memory database;
- H2 console enabled;
- Flyway enabled;
- local CORS origins;
- default dev admin key.

### `application-prod.yml`

Production profile:

- PostgreSQL datasource;
- H2 console disabled;
- Flyway enabled;
- restricted CORS origins;
- `ADMIN_API_KEY` from env;
- actuator health/info.

### `db/migration/`

Flyway migrations:

```text
V1__init_booking_schema.sql
V2__add_booking_status.sql
```

Rules:

- existing migrations should not be edited after production deployment;
- schema changes should be added as `V3__...sql`, `V4__...sql`, etc.;
- entity changes and migrations must stay aligned.

### `static/`

Static files served by Spring Boot.

```text
static/
├── admin.html
└── admin/
    ├── admin.css
    └── admin.js
```

Admin page URL:

```text
http://localhost:8080/admin.html
```

In production:

```text
https://booking-api.virtum-vr.com.ua/admin.html
```

The page calls the Admin API and sends:

```text
X-Admin-Api-Key
```

## Tests

```text
src/test/java/ua/com/virtum/booking/
├── BookingApiIntegrationTests.java
├── BookingVrApplicationTests.java
├── PostgresProfileIntegrationTests.java
└── notification/
    └── BookingNotificationServiceTests.java
```

### `BookingApiIntegrationTests.java`

Covers:

- public services endpoint;
- static admin page availability;
- booking creation;
- conflict `409`;
- admin list protection with `401`;
- admin list with valid key;
- cancellation;
- re-booking a cancelled slot.

### `PostgresProfileIntegrationTests.java`

Runs the production database profile against a real PostgreSQL container.

It verifies:

- `application-prod.yml` can boot with PostgreSQL settings;
- Flyway migrations apply to PostgreSQL;
- seeded services are available;
- booking creation works;
- admin booking list works;
- status update works.

The test uses Testcontainers:

```text
postgres:16-alpine
```

If Docker is unavailable, the class is skipped via:

```java
@Testcontainers(disabledWithoutDocker = true)
```

### `BookingVrApplicationTests.java`

Basic Spring context load test.

### `notification/BookingNotificationServiceTests.java`

Checks Telegram notification behavior without calling the real Telegram API.

It verifies:

- disabled notifications do nothing;
- enabled notifications call the configured Telegram endpoint;
- Telegram failures are logged and do not break booking creation.

## Configuration Cheat Sheet

Local:

```bash
mvn spring-boot:run
```

Production:

```bash
SPRING_PROFILES_ACTIVE=prod mvn spring-boot:run
```

Important env vars:

```text
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=8080
DB_HOST=localhost
DB_PORT=5432
DB_NAME=virtum_booking
DB_USER=virtum_booking
DB_PASSWORD=...
ADMIN_API_KEY=...
TELEGRAM_NOTIFICATIONS_ENABLED=false
TELEGRAM_BOT_TOKEN=...
TELEGRAM_CHAT_ID=...
```

## What To Change For Common Tasks

### Add a new public booking field

Touch these places:

1. `CreateBookingRequest.java`
2. `Booking.java`
3. Flyway migration `V3__...sql`
4. `BookingService.create()`
5. `BookingResponse.java` or `AdminBookingResponse.java`
6. `BookingApiIntegrationTests.java`

### Add a new booking status

Touch these places:

1. `BookingStatus.java`
2. Admin UI label in `static/admin/admin.js`
3. Business rules in `BookingService` if the status affects slot blocking
4. Tests

### Change CORS domains

Touch:

```text
application.yml
application-prod.yml
```

The Java class `CorsConfig.java` reads those values automatically.

### Change admin auth

Current implementation:

```text
AdminApiKeyInterceptor.java
```

Future implementation:

- add Spring Security;
- add login endpoint;
- issue JWT;
- replace API-key interceptor.

### Change booking services

Current quick path:

```text
DataSeeder.java
```

Better future path:

- add `AdminServiceController`;
- add endpoints to create/update/deactivate services;
- add tests and admin UI controls.

### `AsyncConfig.java`

Enables async execution for background tasks.

Currently used for booking notifications:

```text
BookingService
    -> BookingNotificationService
        -> notificationTaskExecutor
            -> TelegramBookingNotifier
```

This keeps the booking API responsive even when Telegram is slow or temporarily unavailable.

## Development Flow

Recommended Git flow:

1. Work in a branch like `codex/some-feature`.
2. Run tests.
3. Push branch.
4. Create PR.
5. Merge PR into `main`.
6. Pull latest `main` before starting the next branch.

This keeps every development step understandable and reversible.

## `notification/`

Пакет для зовнішніх повідомлень.

```text
notification/
├── BookingNotificationService.java
├── BookingNotifier.java
└── TelegramBookingNotifier.java
```

### `BookingNotifier.java`

Інтерфейс каналу нотифікацій.

Зараз є Telegram, але цей інтерфейс дозволяє потім додати:

- SMS;
- email;
- Viber;
- внутрішній CRM webhook.

### `BookingNotificationService.java`

Оркестратор нотифікацій.

`BookingService` не знає деталей Telegram API. Він просто викликає:

```java
notificationService.bookingCreated(...)
notificationService.bookingStatusChanged(...)
```

Важлива поведінка: якщо Telegram недоступний або повернув помилку, бронювання не ламається. Помилка логується, але користувач все одно отримує створене бронювання.

Notification methods are asynchronous. Booking creation returns without waiting for Telegram delivery.

### `TelegramBookingNotifier.java`

Реалізація відправки повідомлень у Telegram Bot API.

Вмикається тільки якщо задані всі параметри:

```text
TELEGRAM_NOTIFICATIONS_ENABLED=true
TELEGRAM_BOT_TOKEN=...
TELEGRAM_CHAT_ID=...
```

Повідомлення відправляються при:

- створенні бронювання;
- зміні статусу бронювання.

## Notification Configuration

Local default:

```yaml
app:
  notifications:
    telegram:
      enabled: false
```

Production:

```yaml
app:
  notifications:
    telegram:
      enabled: ${TELEGRAM_NOTIFICATIONS_ENABLED:false}
      bot-token: ${TELEGRAM_BOT_TOKEN:}
      chat-id: ${TELEGRAM_CHAT_ID:}
```

Для отримання `TELEGRAM_CHAT_ID` найпростіший практичний шлях:

1. Створити bot через BotFather.
2. Написати повідомлення боту.
3. Отримати chat id через Telegram Bot API `getUpdates`.
4. Записати значення у `.env`.
