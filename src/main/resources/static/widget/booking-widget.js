(() => {
  const DEFAULT_CONFIG = {
    apiBase: "",
    triggerSelector: "[data-virtum-booking-open]",
    startHour: 10,
    endHour: 21,
    slotStepMinutes: 30,
  };

  const script = document.currentScript;
  const userConfig = window.VIRTUM_BOOKING_WIDGET || {};
  const config = {
    ...DEFAULT_CONFIG,
    ...userConfig,
  };

  config.apiBase = normalizeApiBase(config.apiBase || inferApiBase(script));

  const state = {
    availabilityBlocks: [],
    bookings: [],
    services: [],
    selectedService: null,
  };

  let root;
  let form;
  let serviceSelect;
  let dateInput;
  let timeSelect;
  let messageNode;
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
            <label>
              Час
              <select name="time" required>
                <option value="">Оберіть дату</option>
              </select>
            </label>
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
    timeSelect = form.elements.time;
    messageNode = root.querySelector(".virtum-booking-message");
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

    timeSelect.disabled = true;
    timeSelect.innerHTML = '<option value="">Завантаження...</option>';

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
    } finally {
      timeSelect.disabled = false;
    }
  }

  function renderTimeSlots() {
    const service = state.selectedService;
    const date = dateInput.value;

    if (!service || !date) {
      timeSelect.innerHTML = '<option value="">Оберіть послугу і дату</option>';
      return;
    }

    const slots = buildSlots(date, service.durationMinutes);
    const availableSlots = slots.filter((slot) => !slot.disabled);

    if (!availableSlots.length) {
      timeSelect.innerHTML = '<option value="">Немає вільних слотів</option>';
      return;
    }

    timeSelect.innerHTML = slots.map((slot) => `
      <option value="${slot.value}" ${slot.disabled ? "disabled" : ""}>
        ${slot.label}${slot.disabledReason}
      </option>
    `).join("");

    const firstAvailable = timeSelect.querySelector("option:not(:disabled)");
    if (firstAvailable) {
      timeSelect.value = firstAvailable.value;
    }
  }

  function buildSlots(dateValue, durationMinutes) {
    const slots = [];
    const today = new Date();
    const opensAt = config.startHour * 60;
    const closesAt = config.endHour * 60;

    // Backend rejects overlaps too; this client-side pass simply hides bad choices early.
    for (let minute = opensAt; minute + durationMinutes <= closesAt; minute += config.slotStepMinutes) {
      const time = minutesToTime(minute);
      const startsAt = parseLocalDateTime(dateValue, time);
      const endsAt = new Date(startsAt.getTime() + durationMinutes * 60 * 1000);
      const past = startsAt <= today;
      const booked = overlapsBooking(startsAt, endsAt);
      const closed = overlapsAvailabilityBlock(startsAt, endsAt);

      slots.push({
        disabled: past || booked || closed,
        disabledReason: past ? " - недоступно" : booked ? " - зайнято" : closed ? " - закрито" : "",
        label: `${time} - ${minutesToTime(minute + durationMinutes)}`,
        value: time,
      });
    }

    return slots;
  }

  function overlapsBooking(startsAt, endsAt) {
    return state.bookings.some((booking) => {
      const bookingStart = new Date(booking.startsAt);
      const bookingEnd = new Date(booking.endsAt);
      return bookingStart < endsAt && bookingEnd > startsAt;
    });
  }

  function overlapsAvailabilityBlock(startsAt, endsAt) {
    return state.availabilityBlocks.some((block) => {
      const blockStart = new Date(block.startsAt);
      const blockEnd = new Date(block.endsAt);
      return blockStart < endsAt && blockEnd > startsAt;
    });
  }

  async function submitBooking(event) {
    event.preventDefault();
    setMessage("");

    if (!timeSelect.value) {
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
        }),
      });

      form.reset();
      dateInput.min = toDateValue(new Date());
      dateInput.value = firstBookableDate();
      renderServices();
      await loadBookingsForDate();
      setMessage(`Готово! Бронювання #${booking.id} створено.`);
    } catch (error) {
      setMessage(error.message);
      await loadBookingsForDate();
    } finally {
      submitButton.disabled = false;
    }
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

      if (serverError.toLowerCase().includes("closed")) {
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

  function toDateValue(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    return `${year}-${month}-${day}`;
  }

  function firstBookableDate() {
    const date = new Date();

    if (date.getHours() >= config.endHour) {
      date.setDate(date.getDate() + 1);
    }

    return toDateValue(date);
  }

  function formatPrice(price) {
    return new Intl.NumberFormat("uk-UA", {
      style: "currency",
      currency: "UAH",
      maximumFractionDigits: 0,
    }).format(price);
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
