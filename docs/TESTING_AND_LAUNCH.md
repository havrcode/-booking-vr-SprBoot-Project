# Testing And Launch

Це практична інструкція: як перевірити сервіс локально, як перевірити Telegram, як відкрити адмінку і як підключити все до існуючого сайту.

## 1. Локальний backend

Запуск:

```bash
mvn spring-boot:run
```

Перевірка health:

```bash
curl http://localhost:8080/actuator/health
```

Очікувано:

```json
{"status":"UP"}
```

## 2. Public API

Список послуг:

```bash
curl http://localhost:8080/api/v1/services
```

Публічні налаштування capacity:

```bash
curl http://localhost:8080/api/v1/booking-settings
```

Очікувано за замовчуванням:

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
    "cardTransferEnabled": false
  }
}
```

Бронювання:

```bash
curl -X POST http://localhost:8080/api/v1/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "serviceSlug": "vr-party-60",
    "customerName": "Тестовий клієнт",
    "customerPhone": "+380501234567",
    "customerEmail": "test@example.com",
    "startsAt": "2026-08-01T10:30:00",
    "helmetsCount": 1,
    "paymentMethod": "PAY_AT_CLUB"
  }'
```

За замовчуванням `MAX_CONCURRENT_BOOKINGS=2`, тому два запити на той самий час по 1 шолому мають пройти, а третій має повернути `409 Conflict`. Один запит із `helmetsCount: 2` займає обидва шоломи на цей час. Слот, який перетинає обід `14:30-15:30`, також має повертати `409`.

Public endpoint зайнятих інтервалів:

```bash
curl "http://localhost:8080/api/v1/bookings?date=2026-08-01"
```

Він має повертати тільки `startsAt` і `endsAt`, без `customerName`, `customerPhone` або `customerEmail`.

## 3. Admin UI

Локально:

```text
http://localhost:8080/admin.html
```

Production:

```text
https://booking-api.virtum-vr.com.ua/admin.html
```

У полі `Admin API key` введи значення `ADMIN_API_KEY`.

Для локального dev profile за замовчуванням:

```text
dev-admin-key
```

Перевір вкладки:

- `Бронювання`: бронювання видно, його можна скасувати.
- `Бронювання`: видно спосіб/статус оплати, скрін підтвердження і кнопку `Оплачено`.
- `Послуги`: можна змінити ціну, тривалість, активність.
- `Доступність`: можна закрити майбутній час і потім знову відкрити.

## 4. Тест ручної оплати

Для тесту переказу на карту локально запусти backend з env:

```text
PAYMENT_CARD_TRANSFER_ENABLED=true
PAYMENT_CARD_HOLDER=Virtum VR
PAYMENT_CARD_NUMBER=4444555566667777
PAYMENT_CARD_BANK=Тестовий банк
```

Після цього:

1. Відкрий widget.
2. Обери `Переказ на карту`.
3. Перевір, що реквізити карти показані у формі.
4. Додай тестовий image-файл як скрін оплати.
5. Створи бронювання.
6. В адмінці відкрий `Бронювання`.
7. Перевір, що payment status став `На перевірці`.
8. Натисни `Скрін`, перевір, що файл відкрився.
9. Натисни `Оплачено`, перевір, що статус став `Оплачено`.

## 5. Тест закритого слота

1. В адмінці відкрий `Доступність`.
2. Закрий майбутній час, наприклад `2026-08-01 15:30-16:30`.
3. Відкрий widget або public API.
4. Спробуй забронювати послугу на `2026-08-01T15:30:00`.
5. Очікувано: backend повертає `409`, widget не дає обрати цей слот.
6. Видали блокування в адмінці.
7. Спробуй ще раз: бронювання має пройти.

## 6. Telegram

Створи bot через `@BotFather`, отримай token.

Дізнайся `chat_id`:

1. Напиши будь-яке повідомлення своєму боту.
2. Відкрий:

```text
https://api.telegram.org/bot<TELEGRAM_BOT_TOKEN>/getUpdates
```

3. Знайди `chat.id`.

Production ENV:

```text
MAX_CONCURRENT_BOOKINGS=2
BOOKING_OPEN_TIME=09:30
BOOKING_CLOSE_TIME=20:30
BOOKING_BREAK_START=14:30
BOOKING_BREAK_END=15:30
BOOKING_SLOT_STEP_MINUTES=60
PAYMENT_PAY_AT_CLUB_ENABLED=true
PAYMENT_CARD_TRANSFER_ENABLED=true
PAYMENT_CARD_HOLDER=<card-holder>
PAYMENT_CARD_NUMBER=<card-number>
PAYMENT_CARD_BANK=<bank-name>
PAYMENT_PROOFS_DIR=data/payment-proofs
TELEGRAM_NOTIFICATIONS_ENABLED=true
TELEGRAM_BOT_TOKEN=<token-from-botfather>
TELEGRAM_CHAT_ID=<chat-id>
```

Після цього створи тестове бронювання. У Telegram має прийти повідомлення про нове бронювання.

## 7. Підключення до virtum-vr.com.ua

Після deployment backend на:

```text
https://booking-api.virtum-vr.com.ua
```

додай перед `</head>` існуючого сайту:

```html
<link rel="stylesheet" href="https://booking-api.virtum-vr.com.ua/widget/booking-widget.css">
```

Додай перед `</body>`:

```html
<script>
  window.VIRTUM_BOOKING_WIDGET = {
    apiBase: "https://booking-api.virtum-vr.com.ua",
    triggerSelector: "[data-virtum-booking-open]",
    maxConcurrentBookings: 2,
    openTime: "09:30",
    closeTime: "20:30",
    breaks: [{ start: "14:30", end: "15:30", label: "Обід" }],
    slotStepMinutes: 60
  };
</script>
<script defer src="https://booking-api.virtum-vr.com.ua/widget/booking-widget.js"></script>
```

На існуючу кнопку `Забронювати` додай:

```html
data-virtum-booking-open
```

Приклад:

```html
<a href="#booking" class="btn pixelFont btn1" data-virtum-booking-open>Забронювати</a>
```

## 8. Final Smoke Test

Після підключення на реальному сайті:

1. Відкрий `https://virtum-vr.com.ua`.
2. Дочекайся завершення loader.
3. Натисни `Забронювати`.
4. Перевір, що modal відкрився.
5. Обери послугу, дату, час.
6. Заповни тестові дані.
7. Обери оплату в клубі і створи бронювання.
8. Повтори тест з оплатою картою і скріном, якщо `PAYMENT_CARD_TRANSFER_ENABLED=true`.
9. Перевір, що бронювання з'явилось у admin panel.
10. Познач тестову оплату як `Оплачено`.
11. Перевір, що Telegram отримав повідомлення.
12. Скасуй бронювання в admin panel.
13. Перевір, що Telegram отримав повідомлення про зміну статусу.

## 9. Automated Checks

Перед merge кожної гілки:

```bash
mvn test
```

Для widget JavaScript:

```bash
node --check src/main/resources/static/widget/booking-widget.js
node --check src/main/resources/static/admin/admin.js
```

Для Git diff:

```bash
git diff --check
```
