# Ukraine.com.ua Terminal And ENV ForCHILD UA

Цей файл пояснює найзаплутанішу частину деплою:

- що таке `/etc/virtum-booking/booking.env`;
- де саме його створювати;
- в якому терміналі виконувати команди;
- чим відрізняється твій MacBook від VPS на Ukraine.com.ua;
- чому backend не запускається просто від створення піддомену;
- чому локальний Tomcat на MacBook не є production-сервером.

Файл спеціально має `ForCHILD` у назві, щоб його було легко знайти й видалити перед передачею проекту замовнику.

## 1. Найголовніше однією фразою

Команди для production потрібно виконувати **не в локальному Tomcat на MacBook**, а **в терміналі VPS-сервера**, куди ти заходиш через SSH.

Тобто:

```text
MacBook Terminal
  -> ssh root@IP_ТВОГО_VPS
  -> після цього ти вже всередині сервера
  -> там створюєш /etc/virtum-booking/booking.env
  -> там запускаєш Java backend
```

## 2. У тебе є три різні “місця”

Це найважливіше зрозуміти.

### Місце 1: твій MacBook

Це твій ноутбук.

На ньому ти:

- відкриваєш браузер;
- відкриваєш Terminal;
- локально тестуєш backend;
- запускаєш локальний Tomcat;
- пишеш код;
- працюєш з IntelliJ;
- робиш `git push`;
- можеш виконувати `ssh root@...`, щоб зайти на VPS.

MacBook - це **не production-сервер**.

Якщо на MacBook зараз працює:

```text
http://localhost:8080
```

це бачиш тільки ти на своєму компʼютері.

Клієнти з інтернету не будуть бронювати через твій MacBook.

### Місце 2: звичайний хостинг сайту на Ukraine.com.ua

Тут лежить твій основний сайт:

```text
https://virtum-vr.com.ua
```

На звичайному хостингу зазвичай лежать:

- HTML;
- CSS;
- JS;
- картинки;
- PHP-файли;
- файли CMS.

Тут буде тільки кнопка:

```text
Забронювати
```

і підключений widget.

Але Spring Boot backend тут зазвичай не запускається.

### Місце 3: VPS на Ukraine.com.ua

Це окремий сервер.

Саме там має працювати:

- Java;
- Spring Boot backend;
- PostgreSQL;
- Nginx;
- HTTPS;
- systemd service.

Саме на VPS ти створюєш:

```text
/etc/virtum-booking/booking.env
```

Саме на VPS ти запускаєш:

```bash
systemctl restart virtum-booking
```

Саме VPS має отримувати запити:

```text
https://booking-api.virtum-vr.com.ua
```

## 3. Що таке `/etc`

`/etc` - це системна папка на Linux-сервері.

Вона не знаходиться на сайті.

Вона не знаходиться у файловому менеджері звичайного хостингу.

Вона не знаходиться всередині GitHub-проекту.

Вона знаходиться на VPS у Linux.

У Linux є головна коренева папка:

```text
/
```

Від неї ідуть системні папки:

```text
/etc
/opt
/var
/home
/usr
```

Для нашого backend ми використовуємо:

```text
/etc/virtum-booking/booking.env
```

Це означає:

```text
/                         головний корінь Linux
  etc/                    системні конфіги
    virtum-booking/       наша папка для конфігу бронювання
      booking.env         файл із секретами й налаштуваннями
```

## 4. Чому ENV кладеться саме в `/etc`

Бо ENV-файл містить секрети:

- пароль до PostgreSQL;
- Admin API Key;
- Telegram token;
- номер карти;
- інші production-налаштування.

Такі речі не треба класти:

- у GitHub;
- у папку сайту;
- у HTML;
- у JavaScript;
- у `src/main/resources`;
- у `public_html`;
- у `www`;
- у файловий менеджер сайту.

Правильне місце для конфігів серверної програми в Linux - `/etc`.

Тому ми робимо:

```text
/etc/virtum-booking/booking.env
```

А потім systemd service читає цей файл:

```ini
EnvironmentFile=/etc/virtum-booking/booking.env
```

## 5. Чи існує `/etc` на MacBook

На MacBook теж є `/etc`, але це не те, що нам потрібно.

Не створюй production ENV на MacBook у:

```text
/etc/virtum-booking/booking.env
```

Бо це буде файл на твоєму ноутбуці.

Production backend його не побачить.

Потрібно створювати `/etc/virtum-booking/booking.env` **саме на VPS** після SSH-підключення.

## 6. Як зрозуміти, в якому терміналі ти зараз

Дуже простий спосіб - дивись на prompt.

### Якщо ти на MacBook

Prompt може виглядати так:

```text
Serhiy@MacBook ~ %
```

або:

```text
serhiy@MacBook-Pro ~ %
```

або просто:

```text
%
```

Це локальний MacBook.

Тут можна виконувати:

```bash
ssh root@VPS_IP
scp file.zip root@VPS_IP:/some/path
dig booking-api.virtum-vr.com.ua +short
```

Але тут не треба виконувати:

```bash
apt install ...
systemctl restart virtum-booking
nano /etc/virtum-booking/booking.env
```

Бо це команди для Linux VPS.

### Якщо ти на VPS

Prompt може виглядати так:

```text
root@server:~#
```

або:

```text
root@vps123:~#
```

або:

```text
ubuntu@server:~$
```

Якщо бачиш `root@...#`, це майже точно сервер.

Тут треба виконувати:

```bash
apt update
apt install ...
nano /etc/virtum-booking/booking.env
systemctl restart virtum-booking
nginx -t
certbot --nginx ...
```

## 7. Який термінал відкривати

На MacBook відкрий:

```text
Applications -> Utilities -> Terminal
```

або натисни `Command + Space`, введи:

```text
Terminal
```

і натисни `Enter`.

Це буде локальний термінал MacBook.

Потім у ньому вводиш:

```bash
ssh root@ТВІЙ_VPS_IP
```

Після успішного входу цей самий термінал стає “вікном у VPS”.

Тобто фізично вікно те саме, але команди вже виконуються не на MacBook, а на сервері.

## 8. Чи це той термінал, де зараз працює Tomcat

Ні.

Термінал, де ми локально запускали Tomcat/Spring Boot на MacBook, був тільки для тесту.

Він потрібен, щоб ти побачив:

```text
http://localhost:8080
```

Це локальна версія.

Production треба запускати не там.

Production треба запускати на VPS.

Якщо локальний Tomcat на MacBook працює, це не шкодить, але він не має стосунку до реального сайту.

Можеш думати так:

```text
MacBook Tomcat = чорновик на столі
VPS Spring Boot = справжній магазин, куди приходять клієнти
```

## 9. Які команди де запускати

Ось таблиця.

| Команда | Де запускати |
|---|---|
| `ssh root@VPS_IP` | MacBook Terminal |
| `scp END16.07.2026.zip root@VPS_IP:/opt/virtum-booking/` | MacBook Terminal |
| `dig booking-api.virtum-vr.com.ua +short` | MacBook Terminal або VPS |
| `apt update` | VPS після SSH |
| `apt install ...` | VPS після SSH |
| `mkdir -p /etc/virtum-booking` | VPS після SSH |
| `nano /etc/virtum-booking/booking.env` | VPS після SSH |
| `systemctl restart virtum-booking` | VPS після SSH |
| `journalctl -u virtum-booking -f` | VPS після SSH |
| `nginx -t` | VPS після SSH |
| `certbot --nginx ...` | VPS після SSH |
| `git push` | MacBook або Codex workspace |
| `mvn test` локально | MacBook або Codex workspace |

## 10. Як виглядає правильний процес

### Крок 1. Ти відкриваєш Terminal на MacBook

Prompt:

```text
Serhiy@MacBook ~ %
```

### Крок 2. Ти підключаєшся до VPS

Вводиш:

```bash
ssh root@ТВІЙ_VPS_IP
```

### Крок 3. Ти вводиш пароль VPS

Символи не показуються. Це нормально.

### Крок 4. Ти потрапляєш на VPS

Prompt зміниться:

```text
root@server:~#
```

Ось тепер команди виконуються на VPS.

### Крок 5. На VPS створюєш ENV

```bash
mkdir -p /etc/virtum-booking
nano /etc/virtum-booking/booking.env
```

### Крок 6. Вставляєш ENV-текст

Вставляєш:

```text
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=8080
APP_TIME_ZONE=Europe/Kyiv
...
```

### Крок 7. Зберігаєш nano

Натискаєш:

```text
Control + O
Enter
Control + X
```

### Крок 8. Запускаєш backend на VPS

```bash
systemctl restart virtum-booking
```

## 11. Як знайти `/etc/virtum-booking/booking.env`

Після входу на VPS виконай:

```bash
ls /
```

Побачиш папки:

```text
bin boot dev etc home opt root usr var ...
```

Потім:

```bash
ls /etc
```

Там дуже багато системних файлів.

Створи нашу папку:

```bash
mkdir -p /etc/virtum-booking
```

Перевір:

```bash
ls /etc/virtum-booking
```

Якщо папка порожня - це нормально.

Створи файл:

```bash
nano /etc/virtum-booking/booking.env
```

Після збереження перевір:

```bash
ls -lh /etc/virtum-booking/booking.env
```

Має показати файл.

Подивитися вміст:

```bash
cat /etc/virtum-booking/booking.env
```

Але обережно: там секрети. Не скидай скрін цього файлу в публічний чат.

## 12. Чому не можна класти ENV у папку сайту

Папка сайту може бути доступна через браузер.

Наприклад:

```text
public_html
www
virtum-vr.com.ua
```

Якщо випадково покласти секрети в неправильне місце, є ризик, що хтось зможе їх скачати.

Тому:

```text
/etc/virtum-booking/booking.env
```

краще, бо це системне місце, яке не віддається браузеру.

## 13. Чому backend не запускається від піддомену

Піддомен - це просто імʼя.

Наприклад:

```text
booking-api.virtum-vr.com.ua
```

Це як вивіска на дверях.

Але за дверима має бути приміщення.

У нашому випадку приміщення - це VPS.

І всередині має працювати програма:

```text
Spring Boot backend
```

Якщо піддомен є, але VPS не налаштований, то це як вивіска без магазину.

## 14. Чому потрібен Nginx

Spring Boot працює всередині VPS на локальному порті:

```text
http://127.0.0.1:8080
```

А клієнти відкривають:

```text
https://booking-api.virtum-vr.com.ua
```

Nginx стоїть між ними:

```text
Internet
  -> Nginx на 443 HTTPS
  -> Spring Boot на 8080
```

Nginx потрібен для:

- HTTPS;
- проксі на Java backend;
- нормальної роботи домену;
- можливості оновлювати backend, не чіпаючи DNS;
- стандартного production-запуску.

## 15. Чому потрібен systemd

Якщо запустити backend просто командою:

```bash
java -jar booking.jar
```

то він працюватиме тільки поки відкрите SSH-вікно.

Закрив SSH - backend може зупинитися.

Перезавантажив VPS - backend не стартує сам.

Тому потрібен systemd service:

```text
virtum-booking.service
```

Він робить так:

- VPS включився -> backend стартував;
- backend впав -> systemd перезапустив;
- ти можеш дивитись статус;
- ти можеш дивитись логи.

## 16. Як зрозуміти, що backend реально працює на VPS

На VPS:

```bash
curl http://127.0.0.1:8080/actuator/health
```

Якщо бачиш:

```json
{"status":"UP"}
```

backend працює всередині VPS.

Потім з MacBook:

```bash
curl https://booking-api.virtum-vr.com.ua/actuator/health
```

Якщо бачиш:

```json
{"status":"UP"}
```

backend доступний з інтернету.

## 17. Міні-перевірка: де я зараз?

Введи:

```bash
pwd
```

Якщо бачиш щось типу:

```text
/Users/Serhiy
```

ти на MacBook.

Якщо бачиш:

```text
/root
```

або:

```text
/opt/virtum-booking
```

ти, скоріш за все, на VPS.

Введи:

```bash
hostname
```

На MacBook буде назва твого ноутбука.

На VPS буде назва сервера.

Введи:

```bash
whoami
```

Якщо:

```text
root
```

то ти root-користувач на VPS або root у Linux.

## 18. Що робити, якщо ти заплутався

Якщо не розумієш, де ти:

1. Введи:

```bash
pwd
```

2. Введи:

```bash
hostname
```

3. Введи:

```bash
whoami
```

4. Подивись на prompt.

5. Якщо треба повернутися з VPS на MacBook, введи:

```bash
exit
```

Після `exit` prompt має знову стати MacBook-овим.

## 19. Дуже коротко: які команди зараз будуть тобі потрібні

На MacBook:

```bash
ssh root@ТВІЙ_VPS_IP
```

Після входу на VPS:

```bash
apt update
apt install -y openjdk-17-jdk maven git nginx certbot python3-certbot-nginx postgresql postgresql-contrib unzip curl
mkdir -p /etc/virtum-booking
nano /etc/virtum-booking/booking.env
```

Після створення backend service:

```bash
systemctl restart virtum-booking
systemctl status virtum-booking
```

Для перевірки:

```bash
curl http://127.0.0.1:8080/actuator/health
curl https://booking-api.virtum-vr.com.ua/actuator/health
```

## 20. Найкоротша відповідь на твоє питання

### Питання: де знайти `/etc/virtum-booking/booking.env`?

Відповідь:

Його не треба “знаходити” в хостингу. Його треба **створити на VPS**.

Шлях:

```text
/etc/virtum-booking/booking.env
```

зʼявиться після команд:

```bash
mkdir -p /etc/virtum-booking
nano /etc/virtum-booking/booking.env
```

### Питання: в якому терміналі запускати команди?

Відповідь:

- `ssh root@VPS_IP` запускаєш у Terminal на MacBook;
- після входу через SSH усі серверні команди запускаєш у цьому ж вікні, але вони вже виконуються на VPS;
- локальний Tomcat на MacBook не використовується для production.

### Питання: це той термінал, де зараз Tomcat?

Відповідь:

Ні. Той Tomcat був локальним тестом.

Production - це VPS на Ukraine.com.ua.

### Питання: чому так складно?

Відповідь:

Бо сайт і backend - це різні речі:

- сайт показує сторінку;
- backend приймає бронювання, працює з базою, зберігає скріни оплат, відправляє Telegram, відкриває адмінку.

Backend має працювати постійно, тому йому потрібен сервер.

## 21. Безпечне правило

Якщо команда починається з:

```bash
apt
systemctl
nginx
certbot
nano /etc
mkdir -p /etc
```

то майже завжди це команда для VPS.

Якщо команда починається з:

```bash
ssh
scp
dig
open
```

то часто це команда для MacBook.

## 22. Що не треба робити

Не вставляй `booking.env` у файловий менеджер сайту.

Не шукай `/etc` у панелі сайту.

Не вставляй `ADMIN_API_KEY` у HTML.

Не вставляй `ADMIN_API_KEY` у JS.

Не думай, що `localhost:8080` на MacBook - це production.

Не запускай `apt install` у Mac Terminal до SSH.

Не запускай `systemctl restart virtum-booking` на MacBook.

## 23. Фраза, яку варто запамʼятати

```text
Усе, що стосується production backend, робиться на VPS після ssh root@VPS_IP.
```

