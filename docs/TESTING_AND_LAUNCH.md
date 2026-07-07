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

Бронювання:

```bash
curl -X POST http://localhost:8080/api/v1/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "serviceSlug": "vr-party-60",
    "customerName": "Test Customer",
    "customerPhone": "+380501234567",
    "customerEmail": "test@example.com",
    "startsAt": "2026-08-01T14:00:00"
  }'
```

Повторний запит на той самий час має повернути `409 Conflict`.

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
- `Послуги`: можна змінити ціну, тривалість, активність.
- `Доступність`: можна закрити майбутній час і потім знову відкрити.

## 4. Тест закритого слота

1. В адмінці відкрий `Доступність`.
2. Закрий майбутній час, наприклад `2026-08-01 15:00-16:00`.
3. Відкрий widget або public API.
4. Спробуй забронювати послугу на `2026-08-01T15:00:00`.
5. Очікувано: backend повертає `409`, widget не дає обрати цей слот.
6. Видали блокування в адмінці.
7. Спробуй ще раз: бронювання має пройти.

## 5. Telegram

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
TELEGRAM_NOTIFICATIONS_ENABLED=true
TELEGRAM_BOT_TOKEN=<token-from-botfather>
TELEGRAM_CHAT_ID=<chat-id>
```

Після цього створи тестове бронювання. У Telegram має прийти повідомлення про нове бронювання.

## 6. Підключення до virtum-vr.com.ua

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
    startHour: 10,
    endHour: 21,
    slotStepMinutes: 30
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

## 7. Final Smoke Test

Після підключення на реальному сайті:

1. Відкрий `https://virtum-vr.com.ua`.
2. Дочекайся завершення loader.
3. Натисни `Забронювати`.
4. Перевір, що modal відкрився.
5. Обери послугу, дату, час.
6. Заповни тестові дані.
7. Створи бронювання.
8. Перевір, що бронювання з'явилось у admin panel.
9. Перевір, що Telegram отримав повідомлення.
10. Скасуй бронювання в admin panel.
11. Перевір, що Telegram отримав повідомлення про зміну статусу.

## 8. Automated Checks

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
