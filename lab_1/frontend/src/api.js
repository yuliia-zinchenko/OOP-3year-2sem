import { keycloak } from "./keycloak.js";

async function request(path, opts = {}) {
  const headers = {
    "Content-Type": "application/json",
    Authorization: `Bearer ${keycloak.token}`,
    ...(opts.headers || {}),
  };
  const res = await fetch(path, { ...opts, headers });
  if (!res.ok) {
    let msg = `${res.status} ${res.statusText}`;
    try { const j = await res.json(); if (j.error) msg = j.error; } catch {}
    throw new Error(msg);
  }
  return res.status === 204 ? null : res.json();
}

export const api = {
  me: () => request("/api/me"),
  searchBooks: (q) => request("/api/books" + (q ? `?q=${encodeURIComponent(q)}` : "")),

  // Reader
  myLoans: () => request("/api/loans"),
  order: (bookId) =>
    request("/api/loans", { method: "POST", body: JSON.stringify({ bookId }) }),
  cancel: (id) => request(`/api/loans/${id}/cancel`, { method: "POST" }),

  // Librarian
  pendingOrders: () => request("/api/loans?status=ORDERED"),
  issue: (id, type) =>
    request(`/api/loans/${id}/issue`, { method: "POST", body: JSON.stringify({ type }) }),
  acceptReturn: (id) => request(`/api/loans/${id}/return`, { method: "POST" }),
};
