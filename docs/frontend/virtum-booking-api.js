const DEFAULT_API_BASE = "https://booking-api.virtum-vr.com.ua";

function apiBase() {
  const configured = window.VIRTUM_BOOKING_API_BASE || DEFAULT_API_BASE;
  return String(configured).replace(/\/+$/, "");
}

async function request(path, options = {}) {
  const response = await fetch(`${apiBase()}${path}`, {
    headers: {
      Accept: "application/json",
      "Content-Type": "application/json",
      ...options.headers,
    },
    ...options,
  });

  const contentType = response.headers.get("content-type") || "";
  const body = contentType.includes("application/json")
    ? await response.json().catch(() => null)
    : await response.text().catch(() => "");

  if (!response.ok) {
    const error = new Error(errorMessage(response.status, body));
    error.status = response.status;
    error.body = body;
    throw error;
  }

  return body;
}

export function listServices() {
  return request("/api/v1/services");
}

export function bookingSettings() {
  return request("/api/v1/booking-settings");
}

export function listBookingsByDate(date) {
  return request(`/api/v1/bookings?date=${encodeURIComponent(date)}`);
}

export function createBooking(payload) {
  return request("/api/v1/bookings", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export async function uploadPaymentProof(booking, file) {
  if (!booking?.id || !booking?.paymentUploadToken || !file) {
    return null;
  }

  const body = new FormData();
  body.append("file", file);

  const response = await fetch(
    `${apiBase()}/api/v1/bookings/${booking.id}/payment-proof?token=${encodeURIComponent(booking.paymentUploadToken)}`,
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
    const error = new Error(errorMessage(response.status, responseBody));
    error.status = response.status;
    error.body = responseBody;
    throw error;
  }

  return responseBody;
}

export function toLocalIsoDateTime(date, time) {
  if (!date || !time) {
    return "";
  }

  return `${date}T${time.length === 5 ? `${time}:00` : time}`;
}

export function formatPriceUah(price) {
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

export function readBookingForm(form) {
  const data = new FormData(form);

  return {
    serviceSlug: String(data.get("serviceSlug") || "").trim(),
    customerName: String(data.get("customerName") || "").trim(),
    customerPhone: String(data.get("customerPhone") || "").trim(),
    customerEmail: String(data.get("customerEmail") || "").trim(),
    startsAt: toLocalIsoDateTime(data.get("date"), data.get("time")),
    paymentMethod: String(data.get("paymentMethod") || "PAY_AT_CLUB"),
  };
}

export function bindBookingForm(form, callbacks = {}) {
  const submitButton = form.querySelector("[type='submit']");

  form.addEventListener("submit", async (event) => {
    event.preventDefault();

    submitButton?.setAttribute("disabled", "disabled");

    try {
      const booking = await createBooking(readBookingForm(form));
      const data = new FormData(form);
      const paymentProof = data.get("paymentProof");

      if (booking.paymentMethod === "CARD_TRANSFER" && paymentProof instanceof File && paymentProof.size > 0) {
        await uploadPaymentProof(booking, paymentProof);
      }

      callbacks.onSuccess?.(booking);
      form.reset();
    } catch (error) {
      callbacks.onError?.(error);
    } finally {
      submitButton?.removeAttribute("disabled");
    }
  });
}

function errorMessage(status, body) {
  if (status === 409) {
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
