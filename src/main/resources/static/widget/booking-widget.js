(() => {
  const DEFAULT_CONFIG = {
    apiBase: "",
    triggerSelector: "[data-virtum-booking-open]",
    maxConcurrentBookings: 2,
    openTime: "09:30",
    closeTime: "20:30",
    breaks: [
      { start: "14:30", end: "15:30", label: "Обід" },
    ],
    slotStepMinutes: 30,
  };

  const script = document.currentScript;
  const userConfig = window.VIRTUM_BOOKING_WIDGET || {};
  const config = {
    ...DEFAULT_CONFIG,
    ...userConfig,
  };

  if (userConfig.startHour !== undefined && userConfig.openTime === undefined) {
    config.openTime = hourToTime(userConfig.startHour, DEFAULT_CONFIG.openTime);
  }

  if (userConfig.endHour !== undefined && userConfig.closeTime === undefined) {
    config.closeTime = hourToTime(userConfig.endHour, DEFAULT_CONFIG.closeTime);
  }

  config.apiBase = normalizeApiBase(config.apiBase || inferApiBase(script));
  normalizeScheduleConfig();

  const state = {
    availabilityBlocks: [],
    bookings: [],
    payment: {
      payAtClubEnabled: true,
      cardTransferEnabled: false,
      cardHolder: null,
      cardNumber: null,
      cardBank: null,
      cardTransferNote: null,
      maxProofSizeBytes: 8 * 1024 * 1024,
    },
    services: [],
    selectedService: null,
  };

  let root;
  let form;
  let serviceSelect;
  let dateInput;
  let timeInput;
  let timeSlotGrid;
  let messageNode;
  let paymentDetailsNode;
  let paymentMethodSelect;
  let paymentProofInput;
  let submitButton;

  function init() {
    buildWidget();
    bindTriggers();
    window.VirtumBookingWidget = {
      open,
      close,
      reloadServices,
    };
  }

  function buildWidget() {
    if (document.querySelector(".virtum-booking-widget")) {
      root = document.querySelector(".virtum-booking-widget");
      return;
    }

    root = document.createElement("div");
    root.className = "virtum-booking-widget";
    root.hidden = true;
    root.innerHTML = `
      <div class="virtum-booking-backdrop" data-virtum-booking-close></div>
      <section class="virtum-booking-dialog" role="dialog" aria-modal="true" aria-labelledby="virtum-booking-title">
        <header class="virtum-booking-header">
          <div>
            <p>Virtum VR</p>
            <h2 id="virtum-booking-title">Бронювання місця</h2>
          </div>
          <button class="virtum-booking-close" type="button" aria-label="Закрити" data-virtum-booking-close>x</button>
        </header>

        <form class="virtum-booking-form">
          <div class="virtum-booking-grid">
            <label>
              Послуга
              <select name="serviceSlug" required>
                <option value="">Завантаження...</option>
              </select>
            </label>
            <label>
              Дата
              <input name="date" type="date" required>
            </label>
            <div class="virtum-booking-time-field">
              <span class="virtum-booking-field-label">Час</span>
              <input name="time" type="hidden">
              <div class="virtum-booking-time-grid" role="radiogroup" aria-label="Оберіть час бронювання">
                <p class="virtum-booking-time-empty">Оберіть дату</p>
              </div>
            </div>
          </div>

          <div class="virtum-booking-grid">
            <label>
              Ім'я
              <input name="customerName" type="text" autocomplete="name" required minlength="2" maxlength="120">
            </label>
            <label>
              Телефон
              <input name="customerPhone" type="tel" autocomplete="tel" required placeholder="+380501234567">
            </label>
            <label>
              Email
              <input name="customerEmail" type="email" autocomplete="email" required>
            </label>
          </div>

          <div class="virtum-booking-grid virtum-booking-payment-grid">
            <label>
              Оплата
              <select name="paymentMethod" required>
                <option value="PAY_AT_CLUB">У клубі по приходу</option>
                <option value="CARD_TRANSFER">Переказ на карту</option>
              </select>
            </label>
            <label class="virtum-booking-proof">
              Скрін оплати
              <input name="paymentProof" type="file" accept="image/png,image/jpeg,image/webp,image/heic,image/heif">
            </label>
            <div class="virtum-booking-payment-details" aria-live="polite"></div>
          </div>

          <p class="virtum-booking-note">Після бронювання адміністратор побачить заявку в адмінці.</p>

          <div class="virtum-booking-actions">
            <button class="virtum-booking-submit" type="submit">Забронювати</button>
            <p class="virtum-booking-message" role="status"></p>
          </div>
        </form>
      </section>
    `;

    document.body.append(root);

    form = root.querySelector(".virtum-booking-form");
    serviceSelect = form.elements.serviceSlug;
    dateInput = form.elements.date;
    timeInput = form.elements.time;
    timeSlotGrid = root.querySelector(".virtum-booking-time-grid");
    messageNode = root.querySelector(".virtum-booking-message");
    paymentDetailsNode = root.querySelector(".virtum-booking-payment-details");
    paymentMethodSelect = form.elements.paymentMethod;
    paymentProofInput = form.elements.paymentProof;
    submitButton = root.querySelector(".virtum-booking-submit");

    dateInput.min = toDateValue(new Date());
    dateInput.value = firstBookableDate();

    root.querySelectorAll("[data-virtum-booking-close]").forEach((button) => {
      button.addEventListener("click", close);
    });

    serviceSelect.addEventListener("change", () => {
      state.selectedService = state.services.find((service) => service.slug === serviceSelect.value) || null;
      renderTimeSlots();
    });
    dateInput.addEventListener("change", loadBookingsForDate);
    paymentMethodSelect.addEventListener("change", renderPaymentOptions);
    form.addEventListener("submit", submitBooking);
    document.addEventListener("keydown", (event) => {
      if (event.key === "Escape" && !root.hidden) {
        close();
      }
    });
  }

  function bindTriggers() {
    // The live site shows a loading screen before the real content appears,
    // so we use event delegation instead of binding only buttons that exist at startup.
    document.addEventListener("click", (event) => {
      const trigger = findBookingTrigger(event.target);

      if (!trigger) {
        return;
      }

      event.preventDefault();
      open();
    });
  }

  function findBookingTrigger(target) {
    if (!(target instanceof Element) || root.contains(target)) {
      return null;
    }

    const explicitTrigger = target.closest(config.triggerSelector);
    if (explicitTrigger && !root.contains(explicitTrigger)) {
      return explicitTrigger;
    }

    const textTrigger = target.closest("a, button");
    if (textTrigger && /заброн|бронюван/i.test(textTrigger.textContent || "")) {
      return textTrigger;
    }

    return null;
  }

  async function open() {
    root.hidden = false;
    document.documentElement.classList.add("virtum-booking-locked");
    setMessage("");

    if (!state.services.length) {
      await reloadServices();
    }

    await reloadBookingSettings();
    await loadBookingsForDate();
    serviceSelect.focus();
  }

  function close() {
    root.hidden = true;
    document.documentElement.classList.remove("virtum-booking-locked");
  }

  async function reloadServices() {
    serviceSelect.disabled = true;
    serviceSelect.innerHTML = '<option value="">Завантаження...</option>';

    try {
      state.services = await request("/api/v1/services");
      renderServices();
    } catch (error) {
      setMessage(error.message);
      serviceSelect.innerHTML = '<option value="">Не вдалося завантажити послуги</option>';
    } finally {
      serviceSelect.disabled = false;
    }
  }

  async function reloadBookingSettings() {
    try {
      const settings = await request("/api/v1/booking-settings");
      const maxConcurrentBookings = Number(settings.maxConcurrentBookings);

      if (Number.isInteger(maxConcurrentBookings) && maxConcurrentBookings > 0) {
        config.maxConcurrentBookings = maxConcurrentBookings;
      }

      if (settings.payment) {
        state.payment = {
          ...state.payment,
          ...settings.payment,
        };
      }

      if (settings.schedule) {
        applyScheduleSettings(settings.schedule);
      }

      renderPaymentOptions();
    } catch (error) {
      // Keep the configured fallback. The backend still enforces capacity.
      renderPaymentOptions();
    }
  }

  function renderPaymentOptions() {
    const payment = state.payment;
    const cardOption = paymentMethodSelect.querySelector('option[value="CARD_TRANSFER"]');
    const clubOption = paymentMethodSelect.querySelector('option[value="PAY_AT_CLUB"]');

    clubOption.disabled = !payment.payAtClubEnabled;
    cardOption.disabled = !payment.cardTransferEnabled;

    if (paymentMethodSelect.value === "CARD_TRANSFER" && !payment.cardTransferEnabled) {
      paymentMethodSelect.value = "PAY_AT_CLUB";
    }

    if (paymentMethodSelect.value === "PAY_AT_CLUB" && !payment.payAtClubEnabled && payment.cardTransferEnabled) {
      paymentMethodSelect.value = "CARD_TRANSFER";
    }

    const isCardTransfer = paymentMethodSelect.value === "CARD_TRANSFER" && payment.cardTransferEnabled;
    paymentProofInput.disabled = !isCardTransfer;
    paymentProofInput.required = false;
    paymentProofInput.closest("label").hidden = !isCardTransfer;
    paymentDetailsNode.innerHTML = isCardTransfer ? paymentDetailsHtml(payment) : "Оплату можна внести в клубі по приходу.";
  }

  function paymentDetailsHtml(payment) {
    const details = [];

    if (payment.cardNumber) {
      details.push(`<strong>${escapeHtml(formatCardNumber(payment.cardNumber))}</strong>`);
    }

    if (payment.cardHolder) {
      details.push(`<span>${escapeHtml(payment.cardHolder)}</span>`);
    }

    if (payment.cardBank) {
      details.push(`<span>${escapeHtml(payment.cardBank)}</span>`);
    }

    if (payment.cardTransferNote) {
      details.push(`<span>${escapeHtml(payment.cardTransferNote)}</span>`);
    }

    return details.length ? details.join("") : "Реквізити карти уточніть у адміністратора.";
  }

  function renderServices() {
    if (!state.services.length) {
      serviceSelect.innerHTML = '<option value="">Активних послуг немає</option>';
      state.selectedService = null;
      renderTimeSlots();
      return;
    }

    serviceSelect.innerHTML = state.services.map((service) => `
      <option value="${escapeHtml(service.slug)}">
        ${escapeHtml(service.title)} · ${service.durationMinutes} хв · ${formatPrice(service.price)}
      </option>
    `).join("");
    state.selectedService = state.services[0];
  }

  async function loadBookingsForDate() {
    const date = dateInput.value;

    if (!date) {
      state.availabilityBlocks = [];
      state.bookings = [];
      renderTimeSlots();
      return;
    }

    timeInput.value = "";
    renderTimeSlotMessage("Завантаження...");

    try {
      const [bookings, availabilityBlocks] = await Promise.all([
        request(`/api/v1/bookings?date=${encodeURIComponent(date)}`),
        request(`/api/v1/availability-blocks?date=${encodeURIComponent(date)}`),
      ]);

      state.bookings = bookings;
      state.availabilityBlocks = availabilityBlocks;
      renderTimeSlots();
    } catch (error) {
      state.availabilityBlocks = [];
      state.bookings = [];
      renderTimeSlots();
      setMessage(error.message);
    }
  }

  function renderTimeSlots() {
    const service = state.selectedService;
    const date = dateInput.value;

    timeInput.value = "";

    if (!service || !date) {
      renderTimeSlotMessage("Оберіть послугу і дату");
      return;
    }

    const slots = buildSlots(date, service.durationMinutes);
    const availableSlots = slots.filter((slot) => !slot.disabled);

    if (!availableSlots.length) {
      renderTimeSlotMessage("Немає вільних слотів");
      return;
    }

    timeSlotGrid.innerHTML = slots.map((slot) => `
      <button
        class="virtum-booking-time-slot${slot.disabled ? " is-disabled" : ""}"
        type="button"
        data-time="${escapeHtml(slot.value)}"
        role="radio"
        aria-checked="false"
        ${slot.disabled ? "disabled" : ""}
      >
        <span>${slot.label}</span>
        <small>${slot.statusLabel}</small>
      </button>
    `).join("");

    timeSlotGrid.querySelectorAll("[data-time]").forEach((button) => {
      button.addEventListener("click", () => selectTimeSlot(button.dataset.time));
    });

    const firstAvailable = timeSlotGrid.querySelector("[data-time]:not(:disabled)");
    if (firstAvailable) {
      selectTimeSlot(firstAvailable.dataset.time);
    }
  }

  function buildSlots(dateValue, durationMinutes) {
    const slots = [];
    const today = new Date();
    const opensAt = timeToMinutes(config.openTime);
    const closesAt = timeToMinutes(config.closeTime);

    // Backend rejects overlaps too; this client-side pass simply hides bad choices early.
    for (let minute = opensAt; minute + durationMinutes <= closesAt; minute += config.slotStepMinutes) {
      const time = minutesToTime(minute);
      const startsAt = parseLocalDateTime(dateValue, time);
      const endsAt = new Date(startsAt.getTime() + durationMinutes * 60 * 1000);
      const past = startsAt <= today;
      const bookingCount = countOverlappingBookings(startsAt, endsAt);
      const booked = bookingCount >= config.maxConcurrentBookings;
      const scheduleBreak = overlappingScheduleBreak(minute, minute + durationMinutes);
      const closed = Boolean(scheduleBreak) || overlapsAvailabilityBlock(startsAt, endsAt);
      const availablePlaces = Math.max(config.maxConcurrentBookings - bookingCount, 0);

      slots.push({
        disabled: past || booked || closed,
        statusLabel: past
          ? "Недоступно"
          : booked
            ? "Зайнято"
            : scheduleBreak
              ? scheduleBreak.label
              : closed
                ? "Закрито"
                : `Вільно ${availablePlaces}/${config.maxConcurrentBookings}`,
        label: `${time} - ${minutesToTime(minute + durationMinutes)}`,
        value: time,
      });
    }

    return slots;
  }

  function countOverlappingBookings(startsAt, endsAt) {
    return state.bookings.filter((booking) => {
      const bookingStart = new Date(booking.startsAt);
      const bookingEnd = new Date(booking.endsAt);
      return bookingStart < endsAt && bookingEnd > startsAt;
    }).length;
  }

  function overlapsAvailabilityBlock(startsAt, endsAt) {
    return state.availabilityBlocks.some((block) => {
      const blockStart = new Date(block.startsAt);
      const blockEnd = new Date(block.endsAt);
      return blockStart < endsAt && blockEnd > startsAt;
    });
  }

  function overlappingScheduleBreak(startMinute, endMinute) {
    return config.breaks.find((breakItem) => {
      const breakStart = timeToMinutes(breakItem.start);
      const breakEnd = timeToMinutes(breakItem.end);

      return breakEnd > breakStart && startMinute < breakEnd && endMinute > breakStart;
    });
  }

  function renderTimeSlotMessage(message) {
    timeSlotGrid.innerHTML = `<p class="virtum-booking-time-empty">${escapeHtml(message)}</p>`;
  }

  function selectTimeSlot(time) {
    timeInput.value = time || "";

    timeSlotGrid.querySelectorAll("[data-time]").forEach((button) => {
      const selected = button.dataset.time === time;
      button.classList.toggle("is-selected", selected);
      button.setAttribute("aria-checked", String(selected));
    });
  }

  async function submitBooking(event) {
    event.preventDefault();
    setMessage("");

    if (!timeInput.value) {
      setMessage("Оберіть вільний час.");
      return;
    }

    submitButton.disabled = true;

    try {
      const data = new FormData(form);
      const booking = await request("/api/v1/bookings", {
        method: "POST",
        // Keep this payload aligned with BookingRequest on the backend.
        body: JSON.stringify({
          serviceSlug: String(data.get("serviceSlug") || "").trim(),
          customerName: String(data.get("customerName") || "").trim(),
          customerPhone: String(data.get("customerPhone") || "").trim(),
          customerEmail: String(data.get("customerEmail") || "").trim(),
          startsAt: `${data.get("date")}T${data.get("time")}:00`,
          paymentMethod: String(data.get("paymentMethod") || "PAY_AT_CLUB"),
        }),
      });
      const paymentProof = data.get("paymentProof");
      const shouldUploadProof = booking.paymentMethod === "CARD_TRANSFER"
        && paymentProof instanceof File
        && paymentProof.size > 0;

      if (shouldUploadProof) {
        await uploadPaymentProof(booking, paymentProof);
      }

      form.reset();
      dateInput.min = toDateValue(new Date());
      dateInput.value = firstBookableDate();
      renderServices();
      renderPaymentOptions();
      await loadBookingsForDate();
      setMessage(shouldUploadProof
        ? `Готово! Бронювання #${booking.id} створено, скрін оплати завантажено.`
        : `Готово! Бронювання #${booking.id} створено.`);
    } catch (error) {
      setMessage(error.message);
      await loadBookingsForDate();
    } finally {
      submitButton.disabled = false;
    }
  }

  async function uploadPaymentProof(booking, file) {
    if (file.size > state.payment.maxProofSizeBytes) {
      throw new Error("Скрін оплати завеликий. Оберіть менший файл.");
    }

    const body = new FormData();
    body.append("file", file);

    const response = await fetch(
      `${config.apiBase}/api/v1/bookings/${booking.id}/payment-proof?token=${encodeURIComponent(booking.paymentUploadToken)}`,
      {
        method: "POST",
        body,
      }
    );

    const contentType = response.headers.get("content-type") || "";
    const responseBody = contentType.includes("application/json")
      ? await response.json().catch(() => null)
      : await response.text().catch(() => "");

    if (!response.ok) {
      throw new Error(errorMessage(response.status, responseBody));
    }

    return responseBody;
  }

  async function request(path, options = {}) {
    const { headers = {}, ...requestOptions } = options;

    const response = await fetch(`${config.apiBase}${path}`, {
      ...requestOptions,
      headers: {
        Accept: "application/json",
        "Content-Type": "application/json",
        ...headers,
      },
    });

    const contentType = response.headers.get("content-type") || "";
    const body = contentType.includes("application/json")
      ? await response.json().catch(() => null)
      : await response.text().catch(() => "");

    if (!response.ok) {
      throw new Error(errorMessage(response.status, body));
    }

    return body;
  }

  function errorMessage(status, body) {
    if (status === 409) {
      const serverError = body && typeof body === "object" && body.error ? String(body.error) : "";
      const normalizedError = serverError.toLowerCase();

      if (normalizedError.includes("lunch")) {
        return "На цей час обідня пауза. Оберіть інший слот.";
      }

      if (normalizedError.includes("working hours")) {
        return "Цей час поза графіком роботи. Оберіть інший слот.";
      }

      if (normalizedError.includes("closed")) {
        return "Цей час закритий адміністратором. Оберіть інший слот.";
      }

      return "Цей час вже зайнятий. Оберіть інший слот.";
    }

    if (status === 400) {
      return "Перевірте дані у формі бронювання.";
    }

    if (body && typeof body === "object" && body.error) {
      return String(body.error);
    }

    return "Не вдалося створити бронювання. Спробуйте ще раз.";
  }

  function normalizeApiBase(value) {
    return String(value || "").replace(/\/+$/, "");
  }

  function normalizeScheduleConfig() {
    config.openTime = normalizeTime(config.openTime, DEFAULT_CONFIG.openTime);
    config.closeTime = normalizeTime(config.closeTime, DEFAULT_CONFIG.closeTime);
    config.slotStepMinutes = positiveInteger(config.slotStepMinutes, DEFAULT_CONFIG.slotStepMinutes);
    config.breaks = normalizeBreaks(config.breaks);
  }

  function applyScheduleSettings(schedule) {
    config.openTime = normalizeTime(schedule.openTime, config.openTime);
    config.closeTime = normalizeTime(schedule.closeTime, config.closeTime);
    config.slotStepMinutes = positiveInteger(schedule.slotStepMinutes, config.slotStepMinutes);

    if (schedule.breakStart && schedule.breakEnd) {
      config.breaks = normalizeBreaks([
        { start: schedule.breakStart, end: schedule.breakEnd, label: "Обід" },
      ]);
    }
  }

  function normalizeBreaks(breaks) {
    const normalized = Array.isArray(breaks)
      ? breaks
          .map((breakItem) => ({
            start: normalizeTime(breakItem.start, ""),
            end: normalizeTime(breakItem.end, ""),
            label: String(breakItem.label || "Перерва"),
          }))
          .filter((breakItem) => breakItem.start && breakItem.end && timeToMinutes(breakItem.end) > timeToMinutes(breakItem.start))
      : [];

    return normalized.length ? normalized : DEFAULT_CONFIG.breaks;
  }

  function normalizeTime(value, fallback) {
    const match = String(value || "").match(/^(\d{1,2}):(\d{2})$/);

    if (!match) {
      return fallback;
    }

    const hours = Number(match[1]);
    const minutes = Number(match[2]);

    if (!Number.isInteger(hours) || !Number.isInteger(minutes) || hours < 0 || hours > 23 || minutes < 0 || minutes > 59) {
      return fallback;
    }

    return minutesToTime(hours * 60 + minutes);
  }

  function hourToTime(value, fallback) {
    const hour = Number(value);

    if (!Number.isInteger(hour) || hour < 0 || hour > 23) {
      return fallback;
    }

    return minutesToTime(hour * 60);
  }

  function positiveInteger(value, fallback) {
    const number = Number(value);

    return Number.isInteger(number) && number > 0 ? number : fallback;
  }

  function inferApiBase(currentScript) {
    if (currentScript?.src) {
      return new URL(currentScript.src, window.location.href).origin;
    }

    return "https://booking-api.virtum-vr.com.ua";
  }

  function parseLocalDateTime(date, time) {
    return new Date(`${date}T${time}:00`);
  }

  function minutesToTime(minutes) {
    const hours = String(Math.floor(minutes / 60)).padStart(2, "0");
    const mins = String(minutes % 60).padStart(2, "0");
    return `${hours}:${mins}`;
  }

  function timeToMinutes(time) {
    const [hours, minutes] = String(time).split(":").map(Number);
    return hours * 60 + minutes;
  }

  function toDateValue(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    return `${year}-${month}-${day}`;
  }

  function firstBookableDate() {
    const date = new Date();
    const currentMinutes = date.getHours() * 60 + date.getMinutes();

    if (currentMinutes >= timeToMinutes(config.closeTime)) {
      date.setDate(date.getDate() + 1);
    }

    return toDateValue(date);
  }

  function formatPrice(price) {
    const amount = Number(price);

    if (!Number.isFinite(amount)) {
      return "";
    }

    const fractionDigits = Number.isInteger(amount) ? 0 : 2;
    const formatted = new Intl.NumberFormat("uk-UA", {
      maximumFractionDigits: fractionDigits,
    }).format(amount);

    return `${formatted} грн`;
  }

  function formatCardNumber(value) {
    return String(value || "").replace(/\s+/g, "").replace(/(.{4})/g, "$1 ").trim();
  }

  function setMessage(text) {
    messageNode.textContent = text;
  }

  function escapeHtml(value) {
    return String(value ?? "")
      .replaceAll("&", "&amp;")
      .replaceAll("<", "&lt;")
      .replaceAll(">", "&gt;")
      .replaceAll('"', "&quot;")
      .replaceAll("'", "&#039;");
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", init);
  } else {
    init();
  }
})();
