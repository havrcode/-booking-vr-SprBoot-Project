# Ukraine.com.ua Beginner Deployment Guide UA

Це максимально детальна інструкція саме під твою ситуацію:

- основний сайт уже є: `https://virtum-vr.com.ua`;
- піддомен уже створений: `booking-api.virtum-vr.com.ua`;
- `Admin API Key` уже згенерований;
- backend `booking-vr-SprBoot-Project` уже готовий у коді;
- потрібно зробити так, щоб backend реально запрацював у production, а сайт почав звертатися до нього.

Цей файл написаний максимально просто: що натиснути, що вставити, які команди виконати, що має вийти в результаті.

## 0. Найважливіше перед стартом

Піддомен `booking-api.virtum-vr.com.ua` - це тільки адреса.

Він сам по собі не запускає backend.

Потрібно, щоб десь був сервер, на якому постійно працює Java-програма:

```text
booking-api.virtum-vr.com.ua
  -> VPS сервер
  -> Nginx
  -> Spring Boot backend на порті 8080
  -> PostgreSQL база
```

Звичайний хостинг для сайту зазвичай працює з файлами сайту: HTML, CSS, JS, PHP, картинки.

Spring Boot backend - це не просто файл для браузера. Його треба запускати як серверну програму.

Тому для цього проекту потрібен VPS/VDS або інший сервер із SSH-доступом.

## 1. Що в тебе вже є

У тебе вже є:

1. Домен:

```text
virtum-vr.com.ua
```

2. Піддомен:

```text
booking-api.virtum-vr.com.ua
```

3. Backend-проект:

```text
booking-vr-SprBoot-Project
```

4. Admin API Key.

5. Готовий widget бронювання в backend:

```text
/widget/booking-widget.js
/widget/booking-widget.css
```

6. Адмінка:

```text
/admin.html
```

## 2. Що ще потрібно зробити

Тобі потрібно:

1. Замовити або відкрити VPS на Ukraine.com.ua.
2. Дізнатися IP-адресу VPS.
3. Направити піддомен `booking-api.virtum-vr.com.ua` на IP VPS через DNS A-record.
4. Зайти на VPS через SSH.
5. Встановити Java, Maven, PostgreSQL, Nginx, Certbot.
6. Створити PostgreSQL базу.
7. Завантажити backend на VPS.
8. Створити ENV-файл на VPS.
9. Зібрати `.jar` файл backend.
10. Запустити backend як systemd service.
11. Налаштувати Nginx.
12. Випустити HTTPS-сертифікат.
13. Перевірити API.
14. Підключити widget до основного сайту.
15. Відкрити адмінку і перевірити бронювання.

## 3. Що підготувати в нотатках перед початком

Відкрий на MacBook програму Notes або будь-який текстовий файл і створи собі список.

Назви його, наприклад:

```text
Virtum VR production дані
```

Запиши туди такі поля:

```text
VPS IP =
VPS root password =
PostgreSQL password =
Admin API Key =
Telegram Bot Token =
Telegram Chat ID =
Card holder =
Card number =
Card bank =
```

Поки деякі поля порожні - це нормально.

## 4. Admin API Key

Твій ключ, який ти згенерував на MacBook у Terminal, підходить.

Його не треба генерувати саме на сервері.

`Admin API Key` - це просто довгий секретний пароль для адмінки.

Його треба буде вставити на VPS у ENV-файл:

```text
ADMIN_API_KEY=твій_ключ
```

Важливо:

- не вставляй `ADMIN_API_KEY` у HTML сайту;
- не вставляй `ADMIN_API_KEY` у JavaScript widget;
- не відправляй `ADMIN_API_KEY` іншому розробнику без потреби;
- не коміть `ADMIN_API_KEY` у GitHub;
- вводь його тільки в адмінці `https://booking-api.virtum-vr.com.ua/admin.html`.

Якщо хочеш згенерувати новий ключ, на MacBook можна виконати:

```bash
openssl rand -hex 32
```

Приклад вигляду:

```text
7f7ad9f2342ce85d9df30c4f6f4a0c4a2e0f9cdbb9c0c3ab4e9787d9c4e6faaa
```

Це нормальний ключ.

## 5. Замовлення VPS на Ukraine.com.ua

Назви кнопок у панелі Ukraine.com.ua можуть трохи відрізнятися, але логіка буде така.

1. Відкрий у браузері:

```text
https://www.ukraine.com.ua
```

2. Натисни кнопку входу в кабінет.

Зазвичай це:

```text
Увійти
```

або:

```text
Панель керування
```

3. Увійди в акаунт.

4. У лівому або верхньому меню знайди розділ:

```text
VPS
```

або:

```text
Віртуальні сервери
```

або:

```text
Сервери
```

5. Натисни:

```text
Замовити VPS
```

або:

```text
Створити сервер
```

6. Обери операційну систему:

```text
Ubuntu 24.04 LTS
```

Якщо `Ubuntu 24.04` немає, бери:

```text
Ubuntu 22.04 LTS
```

7. Для старту вистачить малого VPS.

Орієнтир:

```text
1 CPU
1-2 GB RAM
20+ GB SSD
```

Якщо різниця в ціні невелика, краще взяти:

```text
2 GB RAM
```

8. Далі натисни:

```text
Замовити
```

або:

```text
Продовжити
```

9. Оплати рахунок.

10. Після створення VPS у панелі має зʼявитися сторінка сервера.

Знайди там:

```text
IP-адреса
```

або:

```text
IPv4
```

Скопіюй IP у свої нотатки:

```text
VPS IP = ...
```

11. Там само знайди дані доступу:

```text
root password
```

або:

```text
Пароль root
```

або кнопку:

```text
Змінити пароль
```

Запиши пароль у нотатки.

## 6. DNS: направити booking-api на VPS

Цей крок потрібен, щоб:

```text
booking-api.virtum-vr.com.ua
```

відкривав саме твій VPS.

### 6.1. Відкрити DNS домену

1. У кабінеті Ukraine.com.ua знайди меню:

```text
Домени
```

або:

```text
Мої домени
```

2. Натисни на домен:

```text
virtum-vr.com.ua
```

3. Знайди вкладку або кнопку:

```text
DNS
```

або:

```text
DNS-записи
```

або:

```text
Керування DNS
```

або:

```text
Редагувати DNS
```

### 6.2. Створити A-запис

Тобі потрібен запис:

```text
Тип: A
Імʼя/Host: booking-api
Значення/Value/IP: IP твого VPS
TTL: можна залишити стандартний
```

Приклад:

```text
A    booking-api    123.123.123.123
```

Де `123.123.123.123` - це IP твого VPS.

### 6.3. Якщо запис для booking-api уже є

Якщо ти вже створив піддомен і бачиш там запис типу `A` або `CNAME`, перевір:

- якщо `A` вказує не на VPS IP - зміни його на VPS IP;
- якщо `CNAME` веде на shared hosting - видали або заміни на `A`;
- для `booking-api` має лишитись нормальний `A` запис на VPS.

### 6.4. Зберегти DNS

Натисни:

```text
Зберегти
```

або:

```text
Додати
```

або:

```text
Оновити
```

Після цього почекай від 5 хвилин до 1 години.

На MacBook перевір:

```bash
dig booking-api.virtum-vr.com.ua +short
```

Якщо команда показує IP VPS - DNS готовий.

Якщо нічого не показує - почекай ще.

Якщо показує не той IP - DNS-запис зроблений не туди.

## 7. Підключення до VPS з MacBook

Відкрий на MacBook:

```text
Terminal
```

Виконай:

```bash
ssh root@ТВІЙ_VPS_IP
```

Наприклад:

```bash
ssh root@123.123.123.123
```

Якщо питає:

```text
Are you sure you want to continue connecting?
```

напиши:

```text
yes
```

Натисни `Enter`.

Потім введи root password.

Коли пароль вводиш, символи можуть не показуватись. Це нормально.

Якщо зайшло успішно, ти побачиш щось схоже:

```text
root@server:~#
```

Це означає: ти вже всередині VPS.

## 8. Оновити сервер

На VPS виконай:

```bash
apt update
apt upgrade -y
```

Якщо питає підтвердження - погоджуйся.

## 9. Встановити потрібні програми

На VPS виконай:

```bash
apt install -y openjdk-17-jdk maven git nginx certbot python3-certbot-nginx postgresql postgresql-contrib unzip curl
```

Перевір Java:

```bash
java -version
```

Має бути Java 17 або вище.

Перевір Maven:

```bash
mvn -version
```

Перевір Nginx:

```bash
systemctl status nginx
```

Вийти зі статусу можна клавішею:

```text
q
```

## 10. Налаштувати час сервера

На VPS:

```bash
timedatectl set-timezone Europe/Kyiv
timedatectl set-ntp true
timedatectl
```

У результаті має бути щось схоже:

```text
Time zone: Europe/Kyiv
System clock synchronized: yes
```

Якщо `synchronized` не `yes` одразу - не панікуй. Сервер може синхронізуватися трохи пізніше.

## 11. Створити PostgreSQL базу

На VPS зайди в PostgreSQL:

```bash
sudo -u postgres psql
```

Ти побачиш:

```text
postgres=#
```

Тепер створи користувача.

Заміни `ТУТ_СИЛЬНИЙ_ПАРОЛЬ_БАЗИ` на свій пароль.

Пароль краще згенерувати на MacBook або VPS:

```bash
openssl rand -hex 24
```

У PostgreSQL встав:

```sql
CREATE USER virtum_booking WITH PASSWORD 'ТУТ_СИЛЬНИЙ_ПАРОЛЬ_БАЗИ';
```

Потім:

```sql
CREATE DATABASE virtum_booking OWNER virtum_booking;
```

Потім:

```sql
\q
```

Запиши пароль у нотатки:

```text
PostgreSQL password = ...
```

## 12. Створити папки для backend

На VPS:

```bash
mkdir -p /opt/virtum-booking
mkdir -p /var/lib/virtum-booking/payment-proofs
```

Пояснення:

- `/opt/virtum-booking` - тут буде програма;
- `/var/lib/virtum-booking/payment-proofs` - тут будуть скріни оплат.

## 13. Завантажити проект на VPS

Є два варіанти.

Рекомендую варіант A через GitHub.

### Варіант A: через GitHub

На VPS:

```bash
cd /opt/virtum-booking
git clone -b codex/mobile-calendar-pricing https://github.com/havrcode/-booking-vr-SprBoot-Project.git source
```

Перейди в проект:

```bash
cd /opt/virtum-booking/source
```

Перевір, що файли є:

```bash
ls
```

Ти маєш побачити:

```text
pom.xml
src
docs
README.md
```

### Варіант B: через архів

Якщо GitHub не хочеш використовувати, можна завантажити архів `END16.07.2026.zip`.

На MacBook:

```bash
scp /Users/Serhiy/Documents/Codex/2026-07-13/prior-conversation-with-codex-conversation-role-2/outputs/END16.07.2026.zip root@ТВІЙ_VPS_IP:/opt/virtum-booking/
```

На VPS:

```bash
cd /opt/virtum-booking
unzip END16.07.2026.zip
mv END16.07.2026 source
cd source
```

## 14. Зібрати backend у jar

На VPS:

```bash
cd /opt/virtum-booking/source
mvn -DskipTests package
```

Після завершення перевір:

```bash
ls target/*.jar
```

Там має бути `.jar` файл.

Скопіюй його:

```bash
cp target/*.jar /opt/virtum-booking/booking.jar
```

Перевір:

```bash
ls -lh /opt/virtum-booking/booking.jar
```

## 15. Створити ENV-файл

ENV-файл не кладеться в папку сайту.

ENV-файл кладеться на VPS:

```text
/etc/virtum-booking/booking.env
```

Створи папку:

```bash
mkdir -p /etc/virtum-booking
```

Відкрий файл:

```bash
nano /etc/virtum-booking/booking.env
```

Встав туди текст нижче.

Заміни всі значення, де написано `ЗАМІНИ_...`.

```text
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=8080
APP_TIME_ZONE=Europe/Kyiv
TZ=Europe/Kyiv
JAVA_TOOL_OPTIONS=-Duser.timezone=Europe/Kyiv

DB_HOST=127.0.0.1
DB_PORT=5432
DB_NAME=virtum_booking
DB_USER=virtum_booking
DB_PASSWORD=ЗАМІНИ_НА_ПАРОЛЬ_POSTGRESQL

MAX_CONCURRENT_BOOKINGS=2
BOOKING_OPEN_TIME=09:30
BOOKING_CLOSE_TIME=20:30
BOOKING_BREAK_START=14:30
BOOKING_BREAK_END=15:30
BOOKING_SLOT_STEP_MINUTES=60

PAYMENT_PAY_AT_CLUB_ENABLED=true
PAYMENT_CARD_TRANSFER_ENABLED=true
PAYMENT_CARD_HOLDER=ЗАМІНИ_НА_ІМЯ_ВЛАСНИКА_КАРТИ
PAYMENT_CARD_NUMBER=ЗАМІНИ_НА_НОМЕР_КАРТИ
PAYMENT_CARD_BANK=ЗАМІНИ_НА_БАНК
PAYMENT_CARD_TRANSFER_NOTE=Після переказу можна додати скрін підтвердження.

PAYMENT_PROOFS_DIR=/var/lib/virtum-booking/payment-proofs
PAYMENT_PROOF_MAX_FILE_SIZE=8MB
PAYMENT_PROOF_MAX_BYTES=8388608

ADMIN_API_KEY=ЗАМІНИ_НА_ТВІЙ_ADMIN_API_KEY

TELEGRAM_NOTIFICATIONS_ENABLED=false
TELEGRAM_BOT_TOKEN=
TELEGRAM_CHAT_ID=
```

Поки Telegram можна залишити вимкненим:

```text
TELEGRAM_NOTIFICATIONS_ENABLED=false
```

Коли Telegram буде готовий, увімкнеш.

### Як зберегти файл у nano

1. Натисни:

```text
Control + O
```

2. Натисни `Enter`.

3. Натисни:

```text
Control + X
```

### Закрити доступ до ENV-файлу

На VPS:

```bash
chmod 600 /etc/virtum-booking/booking.env
```

## 16. Перший тестовий запуск вручну

Перед systemd краще один раз запустити вручну.

На VPS:

```bash
cd /opt/virtum-booking
set -a
source /etc/virtum-booking/booking.env
set +a
java -jar /opt/virtum-booking/booking.jar
```

Якщо все добре, в логах має бути:

```text
Tomcat started on port 8080
Started BookingVrApplication
```

Відкрий ще одне SSH-вікно або нову вкладку Terminal і перевір:

```bash
curl http://127.0.0.1:8080/actuator/health
```

Має бути:

```json
{"status":"UP"}
```

Зупини ручний запуск:

```text
Control + C
```

## 17. Створити systemd service

Systemd потрібен, щоб backend:

- сам запускався після перезавантаження VPS;
- сам перезапускався, якщо впаде;
- працював постійно.

Створи файл:

```bash
nano /etc/systemd/system/virtum-booking.service
```

Встав:

```ini
[Unit]
Description=Virtum VR Booking API
After=network.target postgresql.service

[Service]
WorkingDirectory=/opt/virtum-booking
EnvironmentFile=/etc/virtum-booking/booking.env
ExecStart=/usr/bin/java -jar /opt/virtum-booking/booking.jar
Restart=always
RestartSec=10
SuccessExitStatus=143

[Install]
WantedBy=multi-user.target
```

Збережи:

```text
Control + O
Enter
Control + X
```

Увімкни service:

```bash
systemctl daemon-reload
systemctl enable --now virtum-booking
```

Перевір:

```bash
systemctl status virtum-booking
```

Має бути:

```text
active (running)
```

Вийти зі статусу:

```text
q
```

Подивитися логи:

```bash
journalctl -u virtum-booking -f
```

Вийти з логів:

```text
Control + C
```

Перевір API:

```bash
curl http://127.0.0.1:8080/actuator/health
curl http://127.0.0.1:8080/api/v1/services
```

## 18. Налаштувати Nginx для піддомену

Nginx буде приймати запити з інтернету:

```text
https://booking-api.virtum-vr.com.ua
```

і передавати їх backend-у:

```text
http://127.0.0.1:8080
```

Створи файл:

```bash
nano /etc/nginx/sites-available/booking-api.virtum-vr.com.ua
```

Встав:

```nginx
server {
    listen 80;
    listen [::]:80;
    server_name booking-api.virtum-vr.com.ua;

    client_max_body_size 10m;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;

        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        proxy_connect_timeout 10s;
        proxy_send_timeout 30s;
        proxy_read_timeout 30s;
    }
}
```

Збережи файл.

Увімкни сайт:

```bash
ln -s /etc/nginx/sites-available/booking-api.virtum-vr.com.ua /etc/nginx/sites-enabled/
```

Перевір Nginx:

```bash
nginx -t
```

Якщо пише:

```text
syntax is ok
test is successful
```

перезавантаж Nginx:

```bash
systemctl reload nginx
```

Перевір з MacBook або VPS:

```bash
curl http://booking-api.virtum-vr.com.ua/actuator/health
```

Має бути:

```json
{"status":"UP"}
```

## 19. HTTPS через Certbot

Коли HTTP уже працює, робимо HTTPS.

На VPS:

```bash
certbot --nginx -d booking-api.virtum-vr.com.ua
```

Certbot може спитати email. Введи свій email.

Може спитати:

```text
Agree?
```

Пиши:

```text
Y
```

Якщо питає, чи робити redirect HTTP -> HTTPS, обери redirect.

Після цього перевір:

```bash
curl https://booking-api.virtum-vr.com.ua/actuator/health
```

Має бути:

```json
{"status":"UP"}
```

Перевір services:

```bash
curl https://booking-api.virtum-vr.com.ua/api/v1/services
```

Там має бути:

```text
VR-вечірка 60 хв
VR-марафон 120 хв
```

## 20. Telegram bot token

Цей крок можна зробити після основного запуску.

### 20.1. Створити бота

1. Відкрий Telegram.

2. У пошуку знайди:

```text
@BotFather
```

3. Відкрий офіційного BotFather.

4. Натисни:

```text
Start
```

5. Напиши:

```text
/newbot
```

6. BotFather попросить назву бота.

Напиши, наприклад:

```text
Virtum VR Booking
```

7. Потім BotFather попросить username.

Username має закінчуватись на `bot`.

Наприклад:

```text
virtum_vr_booking_bot
```

або:

```text
virtum_booking_zh_bot
```

8. BotFather дасть token.

Він виглядає приблизно так:

```text
1234567890:AAHxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

Скопіюй його в нотатки:

```text
Telegram Bot Token = ...
```

### 20.2. Дізнатися chat id

1. Відкрий свого нового бота.

2. Натисни:

```text
Start
```

3. Напиши боту будь-яке повідомлення, наприклад:

```text
test
```

4. На MacBook або VPS відкрий URL, підставивши token:

```text
https://api.telegram.org/botТВІЙ_BOT_TOKEN/getUpdates
```

Приклад:

```text
https://api.telegram.org/bot1234567890:AAHxxxx/getUpdates
```

5. У відповіді знайди:

```json
"chat":{"id":123456789}
```

Це і є:

```text
TELEGRAM_CHAT_ID
```

### 20.3. Увімкнути Telegram у ENV

На VPS:

```bash
nano /etc/virtum-booking/booking.env
```

Зміни:

```text
TELEGRAM_NOTIFICATIONS_ENABLED=true
TELEGRAM_BOT_TOKEN=твій_token
TELEGRAM_CHAT_ID=твій_chat_id
```

Збережи файл.

Перезапусти backend:

```bash
systemctl restart virtum-booking
```

Перевір:

```bash
systemctl status virtum-booking
```

## 21. Підключити widget до основного сайту

Тепер backend працює на:

```text
https://booking-api.virtum-vr.com.ua
```

Потрібно додати widget на основний сайт:

```text
https://virtum-vr.com.ua
```

### 21.1. Відкрити файли сайту на Ukraine.com.ua

1. Увійди в кабінет Ukraine.com.ua.

2. Знайди розділ:

```text
Хостинг
```

або:

```text
Мої сайти
```

3. Обери сайт:

```text
virtum-vr.com.ua
```

4. Знайди:

```text
Файловий менеджер
```

або:

```text
FTP
```

або:

```text
Файли сайту
```

5. Відкрий кореневу папку сайту.

Вона може називатися:

```text
www
```

або:

```text
public_html
```

або:

```text
virtum-vr.com.ua
```

### 21.2. Куди вставити код widget

Якщо сайт статичний HTML, шукай головний файл:

```text
index.html
```

Якщо сайт на CMS або конструкторі, шукай місце для коду перед `</body>`.

Це може називатися:

```text
HTML-код
```

або:

```text
Custom code
```

або:

```text
Код перед </body>
```

або:

```text
Footer scripts
```

### 21.3. Код для вставки на сайт

Встав перед `</body>`:

```html
<link rel="stylesheet" href="https://booking-api.virtum-vr.com.ua/widget/booking-widget.css">

<script>
  window.VIRTUM_BOOKING_WIDGET = {
    apiBase: "https://booking-api.virtum-vr.com.ua",
    triggerSelector: "[data-virtum-booking-open]"
  };
</script>

<script defer src="https://booking-api.virtum-vr.com.ua/widget/booking-widget.js"></script>
```

### 21.4. Додати кнопку бронювання

На кнопці, яка має відкривати бронювання, додай атрибут:

```html
data-virtum-booking-open
```

Приклад:

```html
<button type="button" data-virtum-booking-open>
  Забронювати
</button>
```

Якщо кнопка вже існує, наприклад:

```html
<a href="#booking" class="btn">Забронювати</a>
```

зроби:

```html
<a href="#" class="btn" data-virtum-booking-open>Забронювати</a>
```

Після збереження відкрий сайт і натисни кнопку.

Має відкритися вікно бронювання.

## 22. Відкрити адмінку

У браузері відкрий:

```text
https://booking-api.virtum-vr.com.ua/admin.html
```

Там буде поле:

```text
Ключ Admin API
```

Встав свій `ADMIN_API_KEY`.

Потім натискай вкладки:

```text
Бронювання
Послуги
Доступність
```

В адмінці ти зможеш:

- дивитися бронювання;
- скасовувати бронювання;
- змінювати статус оплати;
- дивитися скріни підтвердження оплати;
- закривати години вручну;
- редагувати послуги.

## 23. Фінальна перевірка після запуску

### 23.1. API health

Відкрий:

```text
https://booking-api.virtum-vr.com.ua/actuator/health
```

Має бути:

```json
{"status":"UP"}
```

### 23.2. Послуги

Відкрий:

```text
https://booking-api.virtum-vr.com.ua/api/v1/services
```

Має бути видно:

```text
VR-вечірка 60 хв
VR-марафон 120 хв
```

Ціни мають бути в `UAH`.

### 23.3. Widget на сайті

Відкрий:

```text
https://virtum-vr.com.ua
```

Натисни:

```text
Забронювати
```

Перевір:

- календар відкрився;
- послуги видно кнопками;
- ціни в грн;
- час із `09:30`;
- обід тільки `14:30-15:30`;
- можна вибрати 1 або 2 шоломи;
- email не обовʼязковий;
- є необовʼязковий коментар.

### 23.4. Тестове бронювання

Створи тестове бронювання:

- день: майбутній день;
- послуга: `VR-вечірка 60 хв`;
- шоломи: `1`;
- імʼя: `Тест`;
- телефон: свій;
- коментар: `Тестове бронювання`.

Після цього відкрий адмінку і перевір, що бронювання зʼявилось.

Після тесту можеш скасувати його в адмінці.

## 24. Як оновлювати backend після нових змін у GitHub

На VPS:

```bash
cd /opt/virtum-booking/source
git pull
mvn -DskipTests package
cp target/*.jar /opt/virtum-booking/booking.jar
systemctl restart virtum-booking
systemctl status virtum-booking
```

Перевір:

```bash
curl https://booking-api.virtum-vr.com.ua/actuator/health
```

## 25. Корисні команди на VPS

Статус backend:

```bash
systemctl status virtum-booking
```

Перезапустити backend:

```bash
systemctl restart virtum-booking
```

Зупинити backend:

```bash
systemctl stop virtum-booking
```

Запустити backend:

```bash
systemctl start virtum-booking
```

Логи backend:

```bash
journalctl -u virtum-booking -f
```

Перевірити Nginx:

```bash
nginx -t
```

Перезапустити Nginx:

```bash
systemctl reload nginx
```

Перевірити PostgreSQL:

```bash
systemctl status postgresql
```

## 26. Типові проблеми

### Проблема: `https://booking-api.virtum-vr.com.ua` не відкривається

Перевір DNS:

```bash
dig booking-api.virtum-vr.com.ua +short
```

Має показати IP VPS.

Якщо не показує - DNS ще не оновився або запис неправильний.

### Проблема: `502 Bad Gateway`

Це означає, що Nginx працює, але backend не відповідає.

Перевір:

```bash
systemctl status virtum-booking
journalctl -u virtum-booking -n 100
```

### Проблема: backend не стартує через базу

Перевір ENV:

```bash
nano /etc/virtum-booking/booking.env
```

Особливо:

```text
DB_HOST
DB_PORT
DB_NAME
DB_USER
DB_PASSWORD
```

Перевір PostgreSQL:

```bash
systemctl status postgresql
```

### Проблема: адмінка пише, що ключ неправильний

Перевір:

1. Який ключ у файлі:

```bash
nano /etc/virtum-booking/booking.env
```

2. Чи після зміни ENV ти перезапустив backend:

```bash
systemctl restart virtum-booking
```

3. Чи вводиш ключ без пробілів на початку/в кінці.

### Проблема: widget на сайті не відкривається

Перевір, чи на кнопці є:

```html
data-virtum-booking-open
```

Перевір, чи підключений script:

```html
https://booking-api.virtum-vr.com.ua/widget/booking-widget.js
```

Відкрий цей URL у браузері. Якщо файл відкривається - backend віддає widget.

### Проблема: CORS

У production backend дозволяє:

```text
https://virtum-vr.com.ua
https://www.virtum-vr.com.ua
```

Якщо сайт відкривається з іншого домену або тестового домену, треба додати його в `application-prod.yml` або ENV/конфіг.

## 27. Що точно не робити

Не клади `booking.env` у папку сайту.

Не клади `ADMIN_API_KEY` у HTML.

Не клади `ADMIN_API_KEY` у JavaScript.

Не використовуй `dev-admin-key` у production.

Не запускай production на H2.

Не видаляй PostgreSQL базу після запуску.

Не роби `git reset --hard` на сервері, якщо не розумієш, що саме зміниться.

## 28. Міні-чекліст запуску

Позначай галочками:

```text
[ ] VPS створений
[ ] IP VPS записаний
[ ] DNS A-record booking-api -> VPS IP
[ ] SSH на VPS працює
[ ] Java встановлена
[ ] PostgreSQL встановлений
[ ] База virtum_booking створена
[ ] Проект завантажений у /opt/virtum-booking/source
[ ] booking.jar створений
[ ] /etc/virtum-booking/booking.env створений
[ ] systemd service virtum-booking active/running
[ ] Nginx налаштований
[ ] HTTPS certbot успішний
[ ] /actuator/health повертає UP
[ ] /api/v1/services повертає послуги
[ ] widget підключений на virtum-vr.com.ua
[ ] кнопка Забронювати відкриває widget
[ ] тестове бронювання створюється
[ ] адмінка відкривається
[ ] Admin API Key працює
[ ] Telegram увімкнений, якщо потрібен
```

## 29. Куди дивитися в проекті

Основні файли:

```text
docs/PRODUCTION_DEPLOYMENT_GUIDE_UA.md
docs/TESTING_AND_LAUNCH.md
docs/frontend/booking-widget-install.md
src/main/resources/application-prod.yml
deploy/nginx-booking-api.conf
```

Цей файл є дуже детальним варіантом саме під Ukraine.com.ua і твою поточну ситуацію.
