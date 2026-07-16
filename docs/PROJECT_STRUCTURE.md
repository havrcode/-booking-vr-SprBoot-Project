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
- адмінська частина переглядає бронювання, скасовує їх і керує VR-послугами;
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
├── PRODUCTION_READY_PLAN.md
├── PROJECT_STRUCTURE.md
├── TESTING_AND_LAUNCH.md
└── frontend/
    ├── booking-widget-install.md
    └── virtum-booking-api.js
```

- `PRODUCTION_READY_PLAN.md` - production-ready roadmap, acceptance checklist і deployment кроки.
- `PROJECT_STRUCTURE.md` - цей файл.
- `TESTING_AND_LAUNCH.md` - практична інструкція для локального тестування, Telegram і підключення сайту.
- `frontend/booking-widget-install.md` - інструкція для підключення modal-віджета до існуючого сайту.
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
├── DataSeeder.java
├── NotificationConfig.java
└── TelegramNotificationProperties.java
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
- `vr-arena-120`
- `vr-quest-90`
- `vr-kids-45`

Цей seeder лишається тільки стартовим bootstrap-механізмом. Нові production-послуги краще додавати через адмінку.

## `controller/`

Пакет REST controllers.

```text
controller/
├── AdminAvailabilityBlockController.java
├── AdminBookingController.java
├── AdminVrServiceController.java
├── AvailabilityBlockController.java
└── BookingController.java
```

### `BookingController.java`

Public API для сайту.

Endpoints:

```text
GET  /api/v1/services
POST /api/v1/bookings
GET  /api/v1/bookings?date=YYYY-MM-DD
GET  /api/v1/booking-days?from=YYYY-MM-DD&to=YYYY-MM-DD&serviceSlug=...&helmetsCount=1
```

Використовується сайтом `virtum-vr.com.ua` та JS-клієнтом з `docs/frontend/virtum-booking-api.js`.

### `AvailabilityBlockController.java`

Public API для закритих адміністратором слотів.

Endpoints:

```text
GET /api/v1/availability-blocks?date=YYYY-MM-DD
```

Public widget використовує цей endpoint разом з `/api/v1/bookings`, щоб показувати реальну доступність часу.

Public response не розкриває внутрішню причину блокування. Reason видно тільки в Admin API.

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

### `AdminVrServiceController.java`

Admin API для керування VR-послугами.

Endpoints:

```text
GET  /api/v1/admin/services
POST /api/v1/admin/services
PUT  /api/v1/admin/services/{id}
```

Фізичного delete немає навмисно: старі бронювання мають лишатися привʼязаними до своїх послуг. Щоб прибрати послугу з публічного сайту, admin update ставить:

```json
{
  "active": false
}
```

### `AdminAvailabilityBlockController.java`

Admin API для ручного керування доступністю.

Endpoints:

```text
GET    /api/v1/admin/availability-blocks
POST   /api/v1/admin/availability-blocks
DELETE /api/v1/admin/availability-blocks/{id}
```

Це не клієнтське бронювання. Це службове блокування календаря: технічна пауза, приватна подія, неробочий час або інша причина, коли слот не можна продавати.

## `dto/`

DTO означає Data Transfer Object. Це обʼєкти, які входять у API або виходять з API.

```text
dto/
├── AdminBookingResponse.java
├── AdminVrServiceResponse.java
├── AvailabilityBlockResponse.java
├── BookingResponse.java
├── CreateBookingRequest.java
├── SaveAvailabilityBlockRequest.java
├── SaveVrServiceRequest.java
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
  "startsAt": "2026-05-20T09:30:00",
  "helmetsCount": 1
}
```

Валідація:

- `serviceSlug` не порожній;
- `customerName` 2-120 символів;
- `customerPhone` має простий phone pattern;
- `customerEmail` має email format;
- `startsAt` має бути в майбутньому.
- `helmetsCount` optional, за замовчуванням `1`, максимум дорівнює `MAX_CONCURRENT_BOOKINGS`.

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

### `SaveVrServiceRequest.java`

Payload для створення або оновлення VR-послуги:

```json
{
  "slug": "vr-arena-120",
  "title": "VR-арена 120 хв",
  "durationMinutes": 120,
  "price": 2200.00,
  "active": true
}
```

Валідація:

- `slug` тільки lowercase letters, numbers і hyphens;
- `title` не порожній, до 255 символів;
- `durationMinutes` від 15 до 480;
- `price` більше 0, до 2 знаків після коми, завжди у гривнях.
- `active` обовʼязковий boolean.

### `SaveAvailabilityBlockRequest.java`

Payload для закриття часу:

```json
{
  "startsAt": "2026-05-20T15:30:00",
  "endsAt": "2026-05-20T17:30:00",
  "reason": "Приватна подія"
}
```

Валідація:

- `startsAt` і `endsAt` мають бути в майбутньому;
- `endsAt` має бути після `startsAt`;
- `reason` не довший за 255 символів.

### `AdminVrServiceResponse.java`

Response для адмінки з повними даними послуги:

- id;
- slug;
- title;
- durationMinutes;
- price;
- currency (`UAH`);
- active.

## `entity/`

JPA entities, які відповідають таблицям у базі.

```text
entity/
├── AvailabilityBlock.java
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
- `price` - ціна у гривнях;
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

### `AvailabilityBlock.java`

Таблиця:

```text
availability_blocks
```

Описує період, який адміністратор закрив для бронювання:

- startsAt;
- endsAt;
- reason;
- createdAt.

Backend перевіряє цю таблицю під час створення бронювання. Якщо новий booking перетинається з block, API повертає `409 Conflict`.

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
├── AvailabilityBlockRepository.java
├── BookingRepository.java
└── VrServiceRepository.java
```

### `VrServiceRepository.java`

Методи:

- знайти активні послуги;
- знайти активну послугу за slug;
- знайти всі послуги для адмінки;
- перевірити duplicate slug перед create/update.

### `BookingRepository.java`

Методи:

- знайти confirmed booking, що перетинається з новим слотом;
- знайти confirmed bookings за день для public API;
- знайти всі bookings за період для Admin API;
- знайти bookings за період і статусом.

Назви методів довгі, бо Spring Data будує SQL query з імені методу.

### `AvailabilityBlockRepository.java`

Методи:

- знайти blocks, які перетинаються з новим booking slot;
- знайти blocks за період для admin UI.

## `service/`

```text
service/
└── BookingService.java
```

Це головний шар бізнес-логіки.

Відповідає за:

- список послуг;
- admin list/create/update для послуг;
- створення бронювання;
- перевірку перетину слотів;
- перевірку перетину з availability blocks;
- список бронювань на день;
- admin list бронювань;
- зміну статусу бронювання;
- admin list/create/delete для закритих слотів.

### Service Management Flow

```text
POST /api/v1/admin/services
        |
        v
SaveVrServiceRequest validation
        |
        v
BookingService.createService()
        |
        v
Normalize slug
        |
        v
Check duplicate slug
        |
        v
Save VrService
        |
        v
Return AdminVrServiceResponse
```

Неактивні послуги не видаляються з бази. Вони лишаються доступними для старих бронювань, але `GET /api/v1/services` приховує їх від публічного сайту.

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
Check overlap with availability blocks
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
V3__add_availability_blocks.sql
V4__add_manual_payment_fields.sql
V5__ukrainian_service_titles.sql
```

Правила:

- наявні міграції не редагуються після production deployment;
- зміни схеми додаються новими файлами `V6__...sql`, `V7__...sql` тощо;
- зміни entity і міграції мають лишатися узгодженими.

### `static/`

Static files served by Spring Boot.

```text
static/
├── admin.html
├── admin/
    ├── admin.css
    └── admin.js
└── widget/
    ├── booking-widget.css
    └── booking-widget.js
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

Admin UI має три вкладки:

- бронювання з фільтрами та скасуванням;
- послуги зі створенням, редагуванням і перемикачем active;
- доступність зі створенням і видаленням закритих слотів.

Public booking widget URL після deployment:

```text
https://booking-api.virtum-vr.com.ua/widget/booking-widget.css
https://booking-api.virtum-vr.com.ua/widget/booking-widget.js
```

Widget підключається до кнопки на існуючому сайті через:

```html
data-virtum-booking-open
```

Він не використовує Admin API key. Публічна частина викликає тільки `/api/v1/services`, `/api/v1/booking-days?from=...&to=...`, `/api/v1/bookings?date=...`, `/api/v1/availability-blocks?date=...` і `POST /api/v1/bookings`.

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
- re-booking a cancelled slot;
- admin service create/update/deactivate flow;
- admin availability block create/list/delete flow;
- static admin page and booking widget assets.

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

Production path:

```text
Admin page -> Services tab
```

Code touched by service-management changes:

- `AdminVrServiceController.java`;
- `SaveVrServiceRequest.java`;
- `AdminVrServiceResponse.java`;
- `BookingService.java`;
- `VrServiceRepository.java`;
- `static/admin.html`;
- `static/admin/admin.js`;
- `BookingApiIntegrationTests.java`.

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
