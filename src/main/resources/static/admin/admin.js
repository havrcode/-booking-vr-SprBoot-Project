const apiKeyInput = document.querySelector("#api-key");
const keyForm = document.querySelector("#key-form");
const bookingTab = document.querySelector("#booking-tab");
const servicesTab = document.querySelector("#services-tab");
const bookingsView = document.querySelector("#bookings-view");
const servicesView = document.querySelector("#services-view");
const fromDateInput = document.querySelector("#from-date");
const toDateInput = document.querySelector("#to-date");
const statusFilter = document.querySelector("#status-filter");
const refreshButton = document.querySelector("#refresh-button");
const rows = document.querySelector("#booking-rows");
const message = document.querySelector("#message");
const confirmedCount = document.querySelector("#confirmed-count");
const cancelledCount = document.querySelector("#cancelled-count");
const totalCount = document.querySelector("#total-count");
const serviceForm = document.querySelector("#service-form");
const serviceIdInput = document.querySelector("#service-id");
const serviceSlugInput = document.querySelector("#service-slug");
const serviceTitleInput = document.querySelector("#service-title");
const serviceDurationInput = document.querySelector("#service-duration");
const servicePriceInput = document.querySelector("#service-price");
const serviceActiveInput = document.querySelector("#service-active");
const serviceSubmitButton = document.querySelector("#service-submit-button");
const serviceResetButton = document.querySelector("#service-reset-button");
const serviceRows = document.querySelector("#service-rows");

const API_KEY_STORAGE = "virtum.adminApiKey";

let activeView = "bookings";
let services = [];

init();

function init() {
  const today = new Date();
  const nextWeek = new Date(today);
  nextWeek.setDate(today.getDate() + 7);

  fromDateInput.value = toDateInputValue(today);
  toDateInput.value = toDateInputValue(nextWeek);
  apiKeyInput.value = localStorage.getItem(API_KEY_STORAGE) || "";

  keyForm.addEventListener("submit", (event) => {
    event.preventDefault();
    localStorage.setItem(API_KEY_STORAGE, apiKeyInput.value.trim());
    setMessage("Ключ збережено.");
    loadActiveView();
  });

  bookingTab.addEventListener("click", () => selectView("bookings"));
  servicesTab.addEventListener("click", () => selectView("services"));
  refreshButton.addEventListener("click", loadBookings);
  serviceForm.addEventListener("submit", saveService);
  serviceResetButton.addEventListener("click", resetServiceForm);

  loadBookings();
}

function selectView(view) {
  activeView = view;
  const isBookings = view === "bookings";

  bookingTab.classList.toggle("active", isBookings);
  servicesTab.classList.toggle("active", !isBookings);
  bookingTab.setAttribute("aria-selected", String(isBookings));
  servicesTab.setAttribute("aria-selected", String(!isBookings));
  bookingsView.hidden = !isBookings;
  servicesView.hidden = isBookings;

  loadActiveView();
}

function loadActiveView() {
  if (activeView === "services") {
    loadServices();
    return;
  }

  loadBookings();
}

async function loadBookings() {
  const apiKey = getApiKey();

  if (!apiKey) {
    renderRows([]);
    setMessage("Додайте Admin API key.");
    return;
  }

  refreshButton.disabled = true;
  setMessage("Завантаження...");

  try {
    const params = new URLSearchParams();
    params.set("from", fromDateInput.value);
    params.set("to", toDateInput.value);

    if (statusFilter.value) {
      params.set("status", statusFilter.value);
    }

    const response = await fetch(`/api/v1/admin/bookings?${params}`, {
      headers: { "X-Admin-Api-Key": apiKey },
    });

    const body = await parseBody(response);

    if (!response.ok) {
      throw new Error(errorMessage(body, "Не вдалося завантажити бронювання."));
    }

    renderRows(body);
    setMessage("");
  } catch (error) {
    renderRows([]);
    setMessage(error.message);
  } finally {
    refreshButton.disabled = false;
  }
}

async function cancelBooking(id) {
  const apiKey = getApiKey();

  if (!apiKey) {
    setMessage("Додайте Admin API key.");
    return;
  }

  const button = document.querySelector(`[data-cancel-id="${id}"]`);
  button.disabled = true;

  try {
    const response = await fetch(`/api/v1/admin/bookings/${id}/status`, {
      method: "PATCH",
      headers: {
        "Content-Type": "application/json",
        "X-Admin-Api-Key": apiKey,
      },
      body: JSON.stringify({ status: "CANCELLED" }),
    });

    const body = await parseBody(response);

    if (!response.ok) {
      throw new Error(errorMessage(body, "Не вдалося скасувати бронювання."));
    }

    await loadBookings();
    setMessage("Бронювання скасовано.");
  } catch (error) {
    setMessage(error.message);
    button.disabled = false;
  }
}

async function loadServices() {
  const apiKey = getApiKey();

  if (!apiKey) {
    renderServiceRows([]);
    setMessage("Додайте Admin API key.");
    return;
  }

  serviceSubmitButton.disabled = true;
  setMessage("Завантаження...");

  try {
    const response = await fetch("/api/v1/admin/services", {
      headers: { "X-Admin-Api-Key": apiKey },
    });
    const body = await parseBody(response);

    if (!response.ok) {
      throw new Error(errorMessage(body, "Не вдалося завантажити послуги."));
    }

    services = body;
    renderServiceRows(services);
    setMessage("");
  } catch (error) {
    services = [];
    renderServiceRows([]);
    setMessage(error.message);
  } finally {
    serviceSubmitButton.disabled = false;
  }
}

async function saveService(event) {
  event.preventDefault();

  const apiKey = getApiKey();

  if (!apiKey) {
    setMessage("Додайте Admin API key.");
    return;
  }

  const serviceId = serviceIdInput.value;
  const isUpdate = Boolean(serviceId);
  serviceSubmitButton.disabled = true;

  try {
    const response = await fetch(isUpdate ? `/api/v1/admin/services/${serviceId}` : "/api/v1/admin/services", {
      method: isUpdate ? "PUT" : "POST",
      headers: {
        "Content-Type": "application/json",
        "X-Admin-Api-Key": apiKey,
      },
      body: JSON.stringify(servicePayload()),
    });

    const body = await parseBody(response);

    if (!response.ok) {
      throw new Error(errorMessage(body, "Не вдалося зберегти послугу."));
    }

    await loadServices();
    resetServiceForm();
    setMessage(isUpdate ? "Послугу оновлено." : "Послугу створено.");
  } catch (error) {
    setMessage(error.message);
  } finally {
    serviceSubmitButton.disabled = false;
  }
}

async function toggleService(id) {
  const apiKey = getApiKey();
  const service = services.find((item) => String(item.id) === String(id));

  if (!apiKey || !service) {
    setMessage("Додайте Admin API key.");
    return;
  }

  const button = document.querySelector(`[data-toggle-service-id="${id}"]`);
  button.disabled = true;

  try {
    const response = await fetch(`/api/v1/admin/services/${id}`, {
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
        "X-Admin-Api-Key": apiKey,
      },
      body: JSON.stringify({
        slug: service.slug,
        title: service.title,
        durationMinutes: service.durationMinutes,
        price: service.price,
        active: !service.active,
      }),
    });

    const body = await parseBody(response);

    if (!response.ok) {
      throw new Error(errorMessage(body, "Не вдалося змінити статус послуги."));
    }

    await loadServices();
    setMessage(body.active ? "Послугу увімкнено." : "Послугу вимкнено.");
  } catch (error) {
    setMessage(error.message);
    button.disabled = false;
  }
}

function editService(id) {
  const service = services.find((item) => String(item.id) === String(id));

  if (!service) {
    return;
  }

  serviceIdInput.value = service.id;
  serviceSlugInput.value = service.slug;
  serviceTitleInput.value = service.title;
  serviceDurationInput.value = service.durationMinutes;
  servicePriceInput.value = service.price;
  serviceActiveInput.checked = service.active;
  serviceSubmitButton.textContent = "Оновити";
  serviceSlugInput.focus();
}

function resetServiceForm() {
  serviceForm.reset();
  serviceIdInput.value = "";
  serviceActiveInput.checked = true;
  serviceSubmitButton.textContent = "Створити";
}

function servicePayload() {
  return {
    slug: serviceSlugInput.value.trim().toLowerCase(),
    title: serviceTitleInput.value.trim(),
    durationMinutes: Number(serviceDurationInput.value),
    price: Number(servicePriceInput.value),
    active: serviceActiveInput.checked,
  };
}

function renderRows(bookings) {
  updateSummary(bookings);

  if (!bookings.length) {
    rows.innerHTML = '<tr><td colspan="7" class="empty">Бронювань немає</td></tr>';
    return;
  }

  rows.innerHTML = bookings.map((booking) => {
    const start = new Date(booking.startsAt);
    const end = new Date(booking.endsAt);
    const isCancelled = booking.status === "CANCELLED";

    return `
      <tr>
        <td>${formatDate(start)}</td>
        <td>${formatTime(start)}-${formatTime(end)}</td>
        <td>
          <strong>${escapeHtml(booking.serviceTitle)}</strong>
          <div class="secondary">${booking.durationMinutes} хв · ${formatPrice(booking.price)}</div>
        </td>
        <td>
          <div class="client">
            <strong>${escapeHtml(booking.customerName)}</strong>
            <span class="secondary">#${booking.id}</span>
          </div>
        </td>
        <td>
          <div class="contact">
            <span>${escapeHtml(booking.customerPhone)}</span>
            <span class="secondary">${escapeHtml(booking.customerEmail)}</span>
          </div>
        </td>
        <td>${statusBadge(booking.status)}</td>
        <td>
          ${isCancelled ? "" : `<button class="danger-button" data-cancel-id="${booking.id}">Скасувати</button>`}
        </td>
      </tr>
    `;
  }).join("");

  rows.querySelectorAll("[data-cancel-id]").forEach((button) => {
    button.addEventListener("click", () => cancelBooking(button.dataset.cancelId));
  });
}

function renderServiceRows(items) {
  if (!items.length) {
    serviceRows.innerHTML = '<tr><td colspan="6" class="empty">Послуг немає</td></tr>';
    return;
  }

  serviceRows.innerHTML = items.map((service) => `
    <tr class="${service.active ? "" : "inactive-row"}">
      <td>
        <strong>${escapeHtml(service.title)}</strong>
        <div class="secondary">#${service.id}</div>
      </td>
      <td><code>${escapeHtml(service.slug)}</code></td>
      <td>${service.durationMinutes} хв</td>
      <td>${formatPrice(service.price)}</td>
      <td>${serviceBadge(service.active)}</td>
      <td>
        <div class="row-actions">
          <button class="secondary-button" data-edit-service-id="${service.id}" type="button">Редагувати</button>
          <button class="${service.active ? "danger-button" : "secondary-button"}" data-toggle-service-id="${service.id}" type="button">
            ${service.active ? "Вимкнути" : "Увімкнути"}
          </button>
        </div>
      </td>
    </tr>
  `).join("");

  serviceRows.querySelectorAll("[data-edit-service-id]").forEach((button) => {
    button.addEventListener("click", () => editService(button.dataset.editServiceId));
  });

  serviceRows.querySelectorAll("[data-toggle-service-id]").forEach((button) => {
    button.addEventListener("click", () => toggleService(button.dataset.toggleServiceId));
  });
}

function updateSummary(bookings) {
  const confirmed = bookings.filter((booking) => booking.status === "CONFIRMED").length;
  const cancelled = bookings.filter((booking) => booking.status === "CANCELLED").length;

  confirmedCount.textContent = confirmed;
  cancelledCount.textContent = cancelled;
  totalCount.textContent = bookings.length;
}

function statusBadge(status) {
  const cssClass = status === "CANCELLED" ? "cancelled" : "confirmed";
  const label = status === "CANCELLED" ? "Скасовано" : "Підтверджено";
  return `<span class="badge ${cssClass}">${label}</span>`;
}

function serviceBadge(active) {
  const cssClass = active ? "confirmed" : "cancelled";
  const label = active ? "Активна" : "Неактивна";
  return `<span class="badge ${cssClass}">${label}</span>`;
}

function getApiKey() {
  return (apiKeyInput.value || localStorage.getItem(API_KEY_STORAGE) || "").trim();
}

function setMessage(text) {
  message.textContent = text;
}

async function parseBody(response) {
  const contentType = response.headers.get("content-type") || "";
  return contentType.includes("application/json")
    ? response.json().catch(() => null)
    : response.text().catch(() => "");
}

function errorMessage(body, fallback) {
  if (!body) {
    return fallback;
  }

  if (body.fields) {
    return Object.entries(body.fields)
      .map(([field, error]) => `${field}: ${error}`)
      .join("; ");
  }

  return body.error || fallback;
}

function toDateInputValue(date) {
  return date.toISOString().slice(0, 10);
}

function formatDate(date) {
  return new Intl.DateTimeFormat("uk-UA", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  }).format(date);
}

function formatTime(date) {
  return new Intl.DateTimeFormat("uk-UA", {
    hour: "2-digit",
    minute: "2-digit",
  }).format(date);
}

function formatPrice(price) {
  return new Intl.NumberFormat("uk-UA", {
    style: "currency",
    currency: "UAH",
    maximumFractionDigits: 2,
  }).format(price);
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}
