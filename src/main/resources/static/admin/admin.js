const apiKeyInput = document.querySelector("#api-key");
const keyForm = document.querySelector("#key-form");
const bookingTab = document.querySelector("#booking-tab");
const servicesTab = document.querySelector("#services-tab");
const availabilityTab = document.querySelector("#availability-tab");
const bookingsView = document.querySelector("#bookings-view");
const servicesView = document.querySelector("#services-view");
const availabilityView = document.querySelector("#availability-view");
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
const availabilityForm = document.querySelector("#availability-form");
const availabilityStartsAtInput = document.querySelector("#availability-starts-at");
const availabilityEndsAtInput = document.querySelector("#availability-ends-at");
const availabilityReasonInput = document.querySelector("#availability-reason");
const availabilitySubmitButton = document.querySelector("#availability-submit-button");
const availabilityFromDateInput = document.querySelector("#availability-from-date");
const availabilityToDateInput = document.querySelector("#availability-to-date");
const availabilityRefreshButton = document.querySelector("#availability-refresh-button");
const availabilityRows = document.querySelector("#availability-rows");

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
  availabilityFromDateInput.value = toDateInputValue(today);
  availabilityToDateInput.value = toDateInputValue(nextWeek);
  resetAvailabilityForm();
  apiKeyInput.value = localStorage.getItem(API_KEY_STORAGE) || "";

  keyForm.addEventListener("submit", (event) => {
    event.preventDefault();
    localStorage.setItem(API_KEY_STORAGE, apiKeyInput.value.trim());
    setMessage("Ключ збережено.");
    loadActiveView();
  });

  bookingTab.addEventListener("click", () => selectView("bookings"));
  servicesTab.addEventListener("click", () => selectView("services"));
  availabilityTab.addEventListener("click", () => selectView("availability"));
  refreshButton.addEventListener("click", loadBookings);
  serviceForm.addEventListener("submit", saveService);
  serviceResetButton.addEventListener("click", resetServiceForm);
  availabilityForm.addEventListener("submit", saveAvailabilityBlock);
  availabilityRefreshButton.addEventListener("click", loadAvailabilityBlocks);

  loadBookings();
}

function selectView(view) {
  activeView = view;
  const isBookings = view === "bookings";
  const isServices = view === "services";
  const isAvailability = view === "availability";

  bookingTab.classList.toggle("active", isBookings);
  servicesTab.classList.toggle("active", isServices);
  availabilityTab.classList.toggle("active", isAvailability);
  bookingTab.setAttribute("aria-selected", String(isBookings));
  servicesTab.setAttribute("aria-selected", String(isServices));
  availabilityTab.setAttribute("aria-selected", String(isAvailability));
  bookingsView.hidden = !isBookings;
  servicesView.hidden = !isServices;
  availabilityView.hidden = !isAvailability;

  loadActiveView();
}

function loadActiveView() {
  if (activeView === "availability") {
    loadAvailabilityBlocks();
    return;
  }

  if (activeView === "services") {
    loadServices();
    return;
  }

  loadBookings();
}

async function loadAvailabilityBlocks() {
  const apiKey = getApiKey();

  if (!apiKey) {
    renderAvailabilityRows([]);
    setMessage("Додайте Admin API key.");
    return;
  }

  availabilityRefreshButton.disabled = true;
  availabilitySubmitButton.disabled = true;
  setMessage("Завантаження...");

  try {
    const params = new URLSearchParams();
    params.set("from", availabilityFromDateInput.value);
    params.set("to", availabilityToDateInput.value);

    const response = await fetch(`/api/v1/admin/availability-blocks?${params}`, {
      headers: { "X-Admin-Api-Key": apiKey },
    });
    const body = await parseBody(response);

    if (!response.ok) {
      throw new Error(errorMessage(body, "Не вдалося завантажити закриті слоти."));
    }

    renderAvailabilityRows(body);
    setMessage("");
  } catch (error) {
    renderAvailabilityRows([]);
    setMessage(error.message);
  } finally {
    availabilityRefreshButton.disabled = false;
    availabilitySubmitButton.disabled = false;
  }
}

async function saveAvailabilityBlock(event) {
  event.preventDefault();

  const apiKey = getApiKey();

  if (!apiKey) {
    setMessage("Додайте Admin API key.");
    return;
  }

  availabilitySubmitButton.disabled = true;

  try {
    const response = await fetch("/api/v1/admin/availability-blocks", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-Admin-Api-Key": apiKey,
      },
      body: JSON.stringify(availabilityPayload()),
    });
    const body = await parseBody(response);

    if (!response.ok) {
      throw new Error(errorMessage(body, "Не вдалося закрити цей час."));
    }

    resetAvailabilityForm();
    await loadAvailabilityBlocks();
    setMessage("Час закрито для бронювання.");
  } catch (error) {
    setMessage(error.message);
  } finally {
    availabilitySubmitButton.disabled = false;
  }
}

async function deleteAvailabilityBlock(id) {
  const apiKey = getApiKey();

  if (!apiKey) {
    setMessage("Додайте Admin API key.");
    return;
  }

  const button = document.querySelector(`[data-delete-availability-id="${id}"]`);
  button.disabled = true;

  try {
    const response = await fetch(`/api/v1/admin/availability-blocks/${id}`, {
      method: "DELETE",
      headers: { "X-Admin-Api-Key": apiKey },
    });
    const body = await parseBody(response);

    if (!response.ok) {
      throw new Error(errorMessage(body, "Не вдалося відкрити цей час."));
    }

    await loadAvailabilityBlocks();
    setMessage("Час знову доступний для бронювання.");
  } catch (error) {
    setMessage(error.message);
    button.disabled = false;
  }
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

async function updatePaymentStatus(id, paymentStatus) {
  const apiKey = getApiKey();

  if (!apiKey) {
    setMessage("Додайте Admin API key.");
    return;
  }

  const button = document.querySelector(`[data-payment-paid-id="${id}"]`);
  if (button) {
    button.disabled = true;
  }

  try {
    const response = await fetch(`/api/v1/admin/bookings/${id}/payment-status`, {
      method: "PATCH",
      headers: {
        "Content-Type": "application/json",
        "X-Admin-Api-Key": apiKey,
      },
      body: JSON.stringify({ paymentStatus }),
    });

    const body = await parseBody(response);

    if (!response.ok) {
      throw new Error(errorMessage(body, "Не вдалося оновити оплату."));
    }

    await loadBookings();
    setMessage("Статус оплати оновлено.");
  } catch (error) {
    setMessage(error.message);
    if (button) {
      button.disabled = false;
    }
  }
}

async function openPaymentProof(id) {
  const apiKey = getApiKey();

  if (!apiKey) {
    setMessage("Додайте Admin API key.");
    return;
  }

  const previewWindow = window.open("", "_blank");

  try {
    const response = await fetch(`/api/v1/admin/bookings/${id}/payment-proof`, {
      headers: { "X-Admin-Api-Key": apiKey },
    });

    if (!response.ok) {
      const body = await parseBody(response);
      throw new Error(errorMessage(body, "Не вдалося відкрити скрін оплати."));
    }

    const blob = await response.blob();
    const url = URL.createObjectURL(blob);

    if (previewWindow) {
      previewWindow.location = url;
    } else {
      window.open(url, "_blank");
    }
  } catch (error) {
    if (previewWindow) {
      previewWindow.close();
    }
    setMessage(error.message);
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

function resetAvailabilityForm() {
  const startsAt = roundToNextBookingSlot(new Date());
  const endsAt = new Date(startsAt);
  endsAt.setHours(startsAt.getHours() + 1);

  availabilityStartsAtInput.value = toDateTimeInputValue(startsAt);
  availabilityEndsAtInput.value = toDateTimeInputValue(endsAt);
  availabilityReasonInput.value = "";
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

function availabilityPayload() {
  return {
    startsAt: normalizeDateTimeLocalValue(availabilityStartsAtInput.value),
    endsAt: normalizeDateTimeLocalValue(availabilityEndsAtInput.value),
    reason: availabilityReasonInput.value.trim(),
  };
}

function renderRows(bookings) {
  updateSummary(bookings);

  if (!bookings.length) {
    rows.innerHTML = '<tr><td colspan="8" class="empty">Бронювань немає</td></tr>';
    return;
  }

  rows.innerHTML = bookings.map((booking) => {
    const start = new Date(booking.startsAt);
    const end = new Date(booking.endsAt);
    const isCancelled = booking.status === "CANCELLED";
    const isPaid = booking.paymentStatus === "PAID";
    const hasPaymentProof = booking.paymentProof && booking.paymentProof.uploaded;

    return `
      <tr>
        <td>${formatDate(start)}</td>
        <td>${formatTime(start)}-${formatTime(end)}</td>
        <td>
          <strong>${escapeHtml(booking.serviceTitle)}</strong>
          <div class="secondary">${booking.durationMinutes} хв · ${formatPrice(booking.price)}</div>
          <div class="secondary">${helmetLabel(booking.helmetsCount || 1)}</div>
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
        <td>
          <div class="payment">
            <span>${paymentMethodLabel(booking.paymentMethod)}</span>
            ${paymentStatusBadge(booking.paymentStatus)}
            ${hasPaymentProof ? `<button class="secondary-button compact-button" data-proof-id="${booking.id}" type="button">Скрін</button>` : ""}
          </div>
        </td>
        <td>${statusBadge(booking.status)}</td>
        <td>
          <div class="row-actions">
            ${isCancelled || isPaid ? "" : `<button class="secondary-button" data-payment-paid-id="${booking.id}" type="button">Оплачено</button>`}
            ${isCancelled ? "" : `<button class="danger-button" data-cancel-id="${booking.id}" type="button">Скасувати</button>`}
          </div>
        </td>
      </tr>
    `;
  }).join("");

  rows.querySelectorAll("[data-cancel-id]").forEach((button) => {
    button.addEventListener("click", () => cancelBooking(button.dataset.cancelId));
  });

  rows.querySelectorAll("[data-payment-paid-id]").forEach((button) => {
    button.addEventListener("click", () => updatePaymentStatus(button.dataset.paymentPaidId, "PAID"));
  });

  rows.querySelectorAll("[data-proof-id]").forEach((button) => {
    button.addEventListener("click", () => openPaymentProof(button.dataset.proofId));
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

function renderAvailabilityRows(items) {
  if (!items.length) {
    availabilityRows.innerHTML = '<tr><td colspan="5" class="empty">Закритих слотів немає</td></tr>';
    return;
  }

  availabilityRows.innerHTML = items.map((block) => {
    const start = new Date(block.startsAt);
    const end = new Date(block.endsAt);
    const createdAt = new Date(block.createdAt);

    return `
      <tr>
        <td>${formatDate(start)}</td>
        <td>${formatTime(start)}-${formatTime(end)}</td>
        <td>${escapeHtml(block.reason || "Закрито адміністратором")}</td>
        <td>${formatDate(createdAt)} ${formatTime(createdAt)}</td>
        <td>
          <button class="secondary-button" data-delete-availability-id="${block.id}" type="button">Відкрити</button>
        </td>
      </tr>
    `;
  }).join("");

  availabilityRows.querySelectorAll("[data-delete-availability-id]").forEach((button) => {
    button.addEventListener("click", () => deleteAvailabilityBlock(button.dataset.deleteAvailabilityId));
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

function paymentStatusBadge(status) {
  const labels = {
    UNPAID: "Не оплачено",
    PENDING_REVIEW: "На перевірці",
    PAID: "Оплачено",
  };
  const cssClass = {
    UNPAID: "unpaid",
    PENDING_REVIEW: "pending",
    PAID: "paid",
  }[status] || "unpaid";

  return `<span class="badge ${cssClass}">${labels[status] || status}</span>`;
}

function paymentMethodLabel(method) {
  if (method === "CARD_TRANSFER") {
    return "Карта";
  }

  return "У клубі";
}

function helmetLabel(count) {
  const value = Number(count) || 1;
  return value === 1 ? "1 шолом" : `${value} шоломи`;
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

function toDateTimeInputValue(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  const hours = String(date.getHours()).padStart(2, "0");
  const minutes = String(date.getMinutes()).padStart(2, "0");
  return `${year}-${month}-${day}T${hours}:${minutes}`;
}

function normalizeDateTimeLocalValue(value) {
  return value.length === 16 ? `${value}:00` : value;
}

function roundToNextBookingSlot(date) {
  const rounded = new Date(date);
  rounded.setSeconds(0, 0);
  const minutes = rounded.getMinutes();
  if (minutes < 30) {
    rounded.setMinutes(30);
  } else {
    rounded.setHours(rounded.getHours() + 1, 30);
  }
  return rounded;
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

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}
