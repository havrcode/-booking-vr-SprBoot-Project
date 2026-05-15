# booking-vr-SprBoot

Готовий backend-проєкт бронювання VR-послуг на Spring Boot для інтеграції з `virtum-vr.com.ua`.

## Що реалізовано
- REST API для отримання послуг, створення бронювання, перегляду бронювань на день.
- Валідація payload та помилки у JSON форматі.
- Захист від перетину слотів (конфлікт часу).
- Початкові дані з VR-послугами.
- CORS увімкнено для інтеграції з frontend/віджетом.

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

## Підключення до virtum-vr.com.ua
1. Підняти цей backend на домені, наприклад `https://booking-api.virtum-vr.com.ua`.
2. На сайті викликати:
```js
fetch('https://booking-api.virtum-vr.com.ua/api/v1/services')
```
3. Для бронювання відправляти `POST /api/v1/bookings` із форми сайту.

## Наступний production крок
- Додати JWT/адмін-панель.
- Підключити PostgreSQL та Flyway.
- Додати SMS/Telegram нотифікації.
