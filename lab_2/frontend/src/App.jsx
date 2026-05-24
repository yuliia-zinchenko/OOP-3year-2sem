import { useEffect, useState } from "react";
import { initAuth, keycloak } from "./keycloak.js";
import { api } from "./api.js";
import "./styles.css";

export default function App() {
  const [ready, setReady] = useState(false);
  const [me, setMe] = useState(null);
  const [err, setErr] = useState(null);

  useEffect(() => {
    initAuth()
      .then(async (ok) => {
        if (!ok) return;
        setReady(true);
        setMe(await api.me());
      })
      .catch((e) => setErr(e.message));
  }, []);

  if (err) return <div className="app"><div className="alert">{err}</div></div>;
  if (!ready || !me) return <div className="loader">Завантаження…</div>;

  return (
    <div className="app">
      <header className="app-header">
        <h1>📚 Бібліотека <span className="header-tag">Spring</span></h1>
        <div className="user-info">
          <span>{me.email}</span>
          <span className="role-badge">{me.role}</span>
          <button className="btn btn-ghost btn-sm" onClick={() => keycloak.logout()}>
            Вийти
          </button>
        </div>
      </header>
      {me.role === "LIBRARIAN" ? <LibrarianView /> : <ReaderView />}
    </div>
  );
}

function ReaderView() {
  const [q, setQ] = useState("");
  const [books, setBooks] = useState([]);
  const [loans, setLoans] = useState([]);
  const [err, setErr] = useState(null);

  async function refresh() {
    try {
      const [b, l] = await Promise.all([api.searchBooks(q), api.myLoans()]);
      setBooks(b);
      setLoans(l);
    } catch (e) {
      setErr(e.message);
    }
  }

  useEffect(() => { refresh(); }, []);

  const act = (fn) => fn().then(refresh).catch((e) => setErr(e.message));

  return (
    <>
      {err && <div className="alert">{err}</div>}

      <section className="section">
        <h2>Каталог</h2>
        <form
          className="search-bar"
          onSubmit={(e) => { e.preventDefault(); refresh(); }}
        >
          <input
            className="input"
            placeholder="Пошук за назвою, ISBN або автором…"
            value={q}
            onChange={(e) => setQ(e.target.value)}
          />
          <button className="btn btn-primary">Шукати</button>
        </form>

        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>Книга</th>
                <th>Автори</th>
                <th>Доступно</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {books.length === 0 ? (
                <tr><td colSpan={4}><div className="empty">Нічого не знайдено.</div></td></tr>
              ) : books.map((b) => (
                <tr key={b.id}>
                  <td>
                    <div className="book-title">{b.title}</div>
                    <div className="book-meta">{b.year}</div>
                  </td>
                  <td>{(b.authorNames || []).join(", ")}</td>
                  <td>
                    <span className="copies">
                      <span className={"copies-dot" + (b.availableCopies === 0 ? " empty" : "")} />
                      {b.availableCopies}/{b.totalCopies}
                    </span>
                  </td>
                  <td style={{ textAlign: "right" }}>
                    <button
                      className="btn btn-primary btn-sm"
                      disabled={b.availableCopies === 0}
                      onClick={() => act(() => api.order(b.id))}
                    >
                      Замовити
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      <section className="section">
        <h2>Мої замовлення та видачі</h2>
        {loans.length === 0 ? (
          <div className="empty">Немає активних записів.</div>
        ) : (
          <ul className="list">
            {loans.map((l) => (
              <li key={l.id}>
                <div>
                  <div>
                    {l.bookTitle || `Книга #${l.bookId}`}{" "}
                    <span className={`status status-${l.status}`}>{statusLabel(l.status)}</span>
                  </div>
                  <div className="meta">
                    {l.type && typeLabel(l.type)}
                    {l.dueAt && <> · до {new Date(l.dueAt).toLocaleDateString()}</>}
                  </div>
                </div>
                {l.status === "ORDERED" && (
                  <button className="btn btn-danger btn-sm" onClick={() => act(() => api.cancel(l.id))}>
                    Скасувати
                  </button>
                )}
              </li>
            ))}
          </ul>
        )}
      </section>
    </>
  );
}

function LibrarianView() {
  const [orders, setOrders] = useState([]);
  const [err, setErr] = useState(null);

  async function refresh() {
    try { setOrders(await api.pendingOrders()); }
    catch (e) { setErr(e.message); }
  }
  useEffect(() => { refresh(); }, []);

  const act = (fn) => fn().then(refresh).catch((e) => setErr(e.message));

  return (
    <section className="section">
      <h2>Замовлення на видачу</h2>
      {err && <div className="alert">{err}</div>}
      {orders.length === 0 ? (
        <div className="empty">Немає очікуючих замовлень.</div>
      ) : (
        <ul className="list">
          {orders.map((l) => (
            <li key={l.id}>
              <div>
                <div className="book-title">{l.bookTitle || `Книга #${l.bookId}`}</div>
                <div className="meta">Замовлення #{l.id} · читач {l.readerEmail}</div>
              </div>
              <div className="btn-group">
                <button className="btn btn-sm" onClick={() => act(() => api.issue(l.id, "SUBSCRIPTION"))}>
                  Абонемент
                </button>
                <button className="btn btn-primary btn-sm" onClick={() => act(() => api.issue(l.id, "READING_HALL"))}>
                  Читальний зал
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

const statusLabel = (s) => ({
  ORDERED: "Замовлено",
  ISSUED: "Видано",
  RETURNED: "Повернено",
  CANCELLED: "Скасовано",
}[s] || s);

const typeLabel = (t) => (t === "SUBSCRIPTION" ? "абонемент" : "читальний зал");
