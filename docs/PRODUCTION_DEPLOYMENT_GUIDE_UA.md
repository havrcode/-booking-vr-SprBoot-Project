# Production Deployment Guide UA

Це покрокова інструкція для запуску `booking-vr-SprBoot-Project` як робочого production-сервісу для `virtum-vr.com.ua`, а не просто локального тесту.

## 1. Цільова схема

```text
virtum-vr.com.ua
  -> кнопка "Забронювати"
  -> widget з booking-api.virtum-vr.com.ua
  -> Spring Boot backend
  -> PostgreSQL
  -> payment proof file storage
  -> Telegram notifications
  -> admin panel
```

Існуючий сайт не має сам зберігати бронювання. Він тільки відкриває widget. Уся логіка бронювання, оплати, адмінки і Telegram живе в цьому Spring Boot backend.

## 2. Корисні файли в проекті

- `README.md` - короткий опис запуску, API, ENV і адмінки.
- `docs/PRODUCTION_READY_PLAN.md` - production checklist і що вже готово в коді.
- `docs/TESTING_AND_LAUNCH.md` - локальні і фінальні smoke-тести.
- `docs/frontend/booking-widget-install.md` - як підключити widget до існуючого сайту.
- `docs/frontend/virtum-booking-api.js` - JS helper, якщо сайт підключає свою форму замість готового widget.
- `src/main/resources/application-prod.yml` - production-профіль з PostgreSQL, CORS, оплатою і Telegram.
- `deploy/docker-compose.postgres.yml` - PostgreSQL через Docker Compose.
- `deploy/nginx-booking-api.conf` - приклад Nginx reverse proxy.

## 3. DNS

Створи DNS record:

```text
booking-api.virtum-vr.com.ua A <IP_твого_сервера>
```

Після цього backend має бути доступний як:

```text
https://booking-api.virtum-vr.com.ua
```

Адмінка:

```text
https://booking-api.virtum-vr.com.ua/admin.html
```

## 4. Admin API key

Адмінський ключ не видається GitHub або Telegram. Ти сам його генеруєш і кладеш у production ENV змінну `ADMIN_API_KEY`.

На сервері:

```bash
openssl rand -hex 32
```

Приклад результату:

```text
b8d4f3c02f7d0d4f7d8a0b4c5e0123456789abcdef0123456789abcdef
```

Це і є твій `ADMIN_API_KEY`.

Важливо:

- не коміть цей ключ у git;
- не вставляй його в код сайту `virtum-vr.com.ua`;
- не передавай його public frontend-у;
- вводь його вручну тільки в admin panel;
- локальний тестовий ключ `dev-admin-key` не можна використовувати в production.

Admin API захищений header-ом:

```text
X-Admin-Api-Key: <ADMIN_API_KEY>
```

У web-адмінці цей ключ вводиться у поле `Admin API key`.

## 5. Production ENV

На сервері створи файл:

```text
/etc/virtum-booking/booking.env
```

Приклад:

```text
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=8080

DB_HOST=127.0.0.1
DB_PORT=5432
DB_NAME=virtum_booking
DB_USER=virtum_booking
DB_PASSWORD=replace_with_strong_password

MAX_CONCURRENT_BOOKINGS=2
BOOKING_OPEN_TIME=09:30
BOOKING_CLOSE_TIME=20:30
BOOKING_BREAK_START=14:30
BOOKING_BREAK_END=15:30
BOOKING_SLOT_STEP_MINUTES=60

PAYMENT_PAY_AT_CLUB_ENABLED=true
PAYMENT_CARD_TRANSFER_ENABLED=true
PAYMENT_CARD_HOLDER=Virtum VR
PAYMENT_CARD_NUMBER=replace_with_card_number
PAYMENT_CARD_BANK=mono / privat / інший банк
PAYMENT_CARD_TRANSFER_NOTE=Вкажіть номер бронювання в коментарі до платежу

PAYMENT_PROOFS_DIR=/var/lib/virtum-booking/payment-proofs
PAYMENT_PROOF_MAX_FILE_SIZE=8MB
PAYMENT_PROOF_MAX_BYTES=8388608

ADMIN_API_KEY=replace_with_long_random_key

TELEGRAM_NOTIFICATIONS_ENABLED=true
TELEGRAM_BOT_TOKEN=replace_with_bot_token
TELEGRAM_CHAT_ID=replace_with_chat_id
```

`MAX_CONCURRENT_BOOKINGS=2` означає, що на один і той самий час можна прийняти два активні VR-шоломи. Клієнт може забронювати `1` або `2` шоломи одним бронюванням. Графік `09:30-20:30`, `14:30-15:30` закрито як обідня пауза, а стартові слоти йдуть щогодини від `09:30`.

## 6. PostgreSQL

### Варіант A: PostgreSQL через Docker Compose

У проекті вже є:

```text
deploy/docker-compose.postgres.yml
```

Запуск:

```bash
docker compose -f deploy/docker-compose.postgres.yml --env-file /etc/virtum-booking/booking.env up -d
```

Перевірка:

```bash
docker ps
docker logs virtum-booking-postgres
```

Цей compose створює:

- database: `virtum_booking`;
- user: `virtum_booking`;
- password: з `DB_PASSWORD`;
- persistent volume: `virtum_booking_pgdata`.

### Варіант B: PostgreSQL встановлений вручну

Увійди в PostgreSQL:

```bash
sudo -u postgres psql
```

Створи базу і користувача:

```sql
CREATE DATABASE virtum_booking;
CREATE USER virtum_booking WITH ENCRYPTED PASSWORD 'replace_with_strong_password';
GRANT ALL PRIVILEGES ON DATABASE virtum_booking TO virtum_booking;
\c virtum_booking
GRANT CREATE, USAGE ON SCHEMA public TO virtum_booking;
```

Таблиці вручну створювати не треба. Spring Boot сам застосує Flyway migrations при старті.

## 7. Директорія для скрінів оплат

Створи директорію:

```bash
sudo mkdir -p /var/lib/virtum-booking/payment-proofs
```

Якщо backend запускається від користувача `virtum-booking`:

```bash
sudo chown -R virtum-booking:virtum-booking /var/lib/virtum-booking
```

Саме сюди будуть складатись скріни підтвердження оплати.

Важливо: цю директорію треба backup-ити разом із PostgreSQL.

## 8. Збірка backend

На сервері потрібна Java 17.

Збірка:

```bash
mvn clean package
```

Jar буде тут:

```text
target/booking-vr-sprboot-1.0.0.jar
```

Скопіюй jar у production-директорію, наприклад:

```text
/opt/virtum-booking/booking-vr-sprboot-1.0.0.jar
```

## 9. Systemd service

Створи файл:

```text
/etc/systemd/system/virtum-booking.service
```

Вміст:

```ini
[Unit]
Description=Virtum VR Booking API
After=network.target

[Service]
User=virtum-booking
WorkingDirectory=/opt/virtum-booking
EnvironmentFile=/etc/virtum-booking/booking.env
ExecStart=/usr/bin/java -jar /opt/virtum-booking/booking-vr-sprboot-1.0.0.jar
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
```

Запуск:

```bash
sudo systemctl daemon-reload
sudo systemctl enable virtum-booking
sudo systemctl start virtum-booking
sudo systemctl status virtum-booking
```

Логи:

```bash
journalctl -u virtum-booking -f
```

Перевірка локально на сервері:

```bash
curl http://127.0.0.1:8080/actuator/health
```

Очікувано:

```json
{"status":"UP"}
```

## 10. Nginx і HTTPS

Nginx має проксувати:

```text
https://booking-api.virtum-vr.com.ua -> http://127.0.0.1:8080
```

У проекті є приклад:

```text
deploy/nginx-booking-api.conf
```

Важлива правка для скрінів оплати: у Nginx постав хоча б `10m`:

```nginx
client_max_body_size 10m;
```

Backend дозволяє файл до `8MB`, тому `1m` буде замало.

Сертифікат через Let’s Encrypt:

```bash
sudo certbot --nginx -d booking-api.virtum-vr.com.ua
```

Перевір:

```text
https://booking-api.virtum-vr.com.ua/actuator/health
https://booking-api.virtum-vr.com.ua/admin.html
https://booking-api.virtum-vr.com.ua/widget/booking-widget.js
```

## 11. Telegram bot

### Як отримати TELEGRAM_BOT_TOKEN

1. Відкрий Telegram.
2. Знайди `@BotFather`.
3. Напиши `/newbot`.
4. Введи назву, наприклад `Virtum Booking`.
5. Введи username, який закінчується на `bot`, наприклад `VirtumBookingNotifyBot`.
6. BotFather дасть token.

Приклад token:

```text
123456789:AAHxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

Це значення треба покласти в:

```text
TELEGRAM_BOT_TOKEN=123456789:AAHxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

### Як отримати TELEGRAM_CHAT_ID

1. Відкрий свого нового бота.
2. Натисни `Start` або напиши `/start`.
3. Відкрий у браузері:

```text
https://api.telegram.org/bot<TELEGRAM_BOT_TOKEN>/getUpdates
```

4. У JSON знайди:

```json
"chat":{"id":123456789}
```

Це число і є `TELEGRAM_CHAT_ID`.

Якщо хочеш повідомлення в Telegram-групу:

1. Додай бота в групу.
2. Напиши будь-яке повідомлення в групі.
3. Знову відкрий `getUpdates`.
4. Візьми `chat.id` саме з групового повідомлення.

Для груп `chat.id` часто відʼємний. Це нормально.

Production ENV:

```text
TELEGRAM_NOTIFICATIONS_ENABLED=true
TELEGRAM_BOT_TOKEN=<token-from-botfather>
TELEGRAM_CHAT_ID=<chat-id>
```

Перевірка: створи тестове бронювання. У Telegram має прийти повідомлення.

## 12. Підключення widget до сайту

На `virtum-vr.com.ua` перед `</head>` додай:

```html
<link rel="stylesheet" href="https://booking-api.virtum-vr.com.ua/widget/booking-widget.css">
```

Перед `</body>` додай:

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

На кнопку бронювання додай attribute:

```html
data-virtum-booking-open
```

Приклад:

```html
<a href="#booking" class="btn pixelFont btn1" data-virtum-booking-open>Забронювати</a>
```

Admin API key на сайт не додається. Widget працює тільки через public API.

## 13. CORS

У production-профілі вже дозволені:

```text
https://virtum-vr.com.ua
https://www.virtum-vr.com.ua
```

Це налаштовано в:

```text
src/main/resources/application-prod.yml
```

Якщо frontend буде на іншому домені, його треба додати в `app.cors.allowed-origins`.

## 14. Оплата

Підтримуються два варіанти:

- оплата в клубі при приході клієнта;
- переказ на карту зі скріном підтвердження.

Керується через ENV:

```text
PAYMENT_PAY_AT_CLUB_ENABLED=true
PAYMENT_CARD_TRANSFER_ENABLED=true
PAYMENT_CARD_HOLDER=Virtum VR
PAYMENT_CARD_NUMBER=replace_with_card_number
PAYMENT_CARD_BANK=mono / privat / інший банк
PAYMENT_CARD_TRANSFER_NOTE=Вкажіть номер бронювання в коментарі до платежу
```

Якщо `PAYMENT_CARD_TRANSFER_ENABLED=true`, widget покаже клієнту реквізити карти і дозволить завантажити скрін.

В адмінці можна:

- відкрити скрін;
- побачити статус `UNPAID`, `PENDING_REVIEW` або `PAID`;
- натиснути `Оплачено`.

## 15. Admin panel після запуску

Відкрий:

```text
https://booking-api.virtum-vr.com.ua/admin.html
```

Введи `ADMIN_API_KEY`.

Перевір вкладки:

- `Бронювання` - список бронювань, скасування, оплата, скріни.
- `Послуги` - назви, ціни в грн, тривалість, активність.
- `Доступність` - закриття/відкриття слотів.

## 16. Final smoke test

Після підключення до реального сайту:

1. Відкрий `https://virtum-vr.com.ua`.
2. Натисни `Забронювати`.
3. Перевір, що modal відкрився.
4. Обери послугу, дату і час.
5. Створи бронювання з оплатою в клубі.
6. Створи бронювання з переказом на карту і скріном.
7. Перевір, що обидва бронювання видно в admin panel.
8. Перевір, що Telegram отримав повідомлення.
9. Познач оплату як `Оплачено`.
10. Скасуй тестове бронювання.
11. Перевір, що Telegram отримав повідомлення про зміну статусу.
12. Перевір, що скасований слот більше не блокує час.

## 17. Acceptance checklist

- `GET /api/v1/services` повертає активні послуги.
- Ціни показуються в грн, API повертає `currency: "UAH"`.
- `GET /api/v1/booking-settings` повертає правильний capacity і payment settings.
- `GET /api/v1/bookings?date=YYYY-MM-DD` не повертає імʼя, телефон або email клієнта.
- `POST /api/v1/bookings` створює бронювання.
- Повторне бронювання того самого часу блокується після досягнення `MAX_CONCURRENT_BOOKINGS`.
- Закритий адміністратором слот не можна забронювати.
- Widget не показує зайняті або закриті слоти як доступні.
- Клієнт може вибрати оплату в клубі або переказ на карту.
- Клієнт може завантажити скрін підтвердження оплати.
- Admin UI дозволяє переглянути скрін і позначити оплату як `PAID`.
- Admin UI дозволяє редагувати послуги і ціни.
- Admin UI дозволяє закривати і відкривати слоти.
- Telegram отримує повідомлення про нове бронювання.
- Telegram отримує повідомлення про скасування бронювання.
- Production працює з PostgreSQL, не з H2.
- `ADMIN_API_KEY` не потрапляє на публічний сайт.
- Скріншоти оплат і PostgreSQL backup-яться.

## 18. Що не блокує запуск

Це можна додати пізніше:

- окрема сутність `rooms` або `vr-stations`;
- SMS або email-підтвердження клієнту;
- автоматична онлайн-оплата через LiqPay/WayForPay/інший шлюз;
- календарний dashboard по днях/тижнях;
- audit log для дій адміністратора.

Для першого production-запуску критично мати PostgreSQL, HTTPS, правильний `ADMIN_API_KEY`, CORS, Telegram, backup бази і папку для скрінів оплат.
