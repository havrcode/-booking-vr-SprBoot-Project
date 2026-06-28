const apiKeyInput = document.querySelector("#api-key");
const keyForm = document.querySelector("#key-form");
const fromDateInput = document.querySelector("#from-date");
const toDateInput = document.querySelector("#to-date");
const statusFilter = document.querySelector("#status-filter");
const refreshButton = document.querySelector("#refresh-button");
const rows = document.querySelector("#booking-rows");
const message = document.querySelector("#message");
const confirmedCount = document.querySelector("#confirmed-count");
const cancelledCount = document.querySelector("#cancelled-count");
const totalCount = document.querySelector("#total-count");

const API_KEY_STORAGE = "virtum.adminApiKey";

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
    loadBookings();
  });

  refreshButton.addEventListener("click", loadBookings);
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
      throw new Error(body?.error || "Не вдалося завантажити бронювання.");
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
      throw new Error(body?.error || "Не вдалося скасувати бронювання.");
    }

    setMessage("Бронювання скасовано.");
    await loadBookings();
  } catch (error) {
    setMessage(error.message);
    button.disabled = false;
  }
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
    maximumFractionDigits: 0,
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

