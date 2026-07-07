# Booking Widget Install

Цей widget потрібен, щоб підключити публічне бронювання до існуючого сайту `virtum-vr.com.ua` без окремого frontend-проекту.

Окремий frontend варто робити тільки тоді, коли сторінка бронювання має стати повноцінним застосунком з власним роутингом, кабінетом клієнта або складним календарем. Для поточного етапу достатньо modal-віджета на існуючій кнопці `Забронювати`.

## 1. Підключення assets

Додай перед `</head>`:

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

## 2. Кнопка на сайті

Найкращий варіант - на існуючу кнопку `Забронювати` додати attribute:

```html
<a href="#booking" class="btn pixelFont btn1" data-virtum-booking-open>Забронювати</a>
```

Widget також намагається автоматично підхопити кнопки/посилання, у тексті яких є `Забронювати` або `Бронювання`, але explicit attribute надійніший.

Обробник кліку працює через делегування, тому widget підхопить кнопку навіть якщо сайт спочатку показує loading screen, а основний контент з'являється трохи пізніше.

## 3. Що робить widget

- відкриває modal бронювання;
- завантажує активні послуги з `GET /api/v1/services`;
- перевіряє зайняті слоти через `GET /api/v1/bookings?date=YYYY-MM-DD`;
- перевіряє закриті адміністратором слоти через `GET /api/v1/availability-blocks?date=YYYY-MM-DD`;
- відправляє бронювання через `POST /api/v1/bookings`;
- показує зрозуміле повідомлення, якщо слот уже зайнятий або закритий.

Admin API key на сайт не додається. Він потрібен тільки для адмінки.

## 4. Налаштування графіка

Години роботи зараз задаються у widget config:

```js
startHour: 10,
endHour: 21,
slotStepMinutes: 30
```

Якщо графік зміниться, онови ці значення у snippet на сайті.
