-- Library System schema (5 tables)

CREATE TABLE IF NOT EXISTS users (
    id          BIGSERIAL PRIMARY KEY,
    sub         VARCHAR(128) UNIQUE NOT NULL,
    email       VARCHAR(255) UNIQUE NOT NULL,
    full_name   VARCHAR(255),
    role        VARCHAR(32) NOT NULL DEFAULT 'READER',  -- READER | LIBRARIAN
    created_at  TIMESTAMP DEFAULT now()
);

CREATE TABLE IF NOT EXISTS authors (
    id          BIGSERIAL PRIMARY KEY,
    full_name   VARCHAR(255) NOT NULL,
    country     VARCHAR(64)
);

CREATE TABLE IF NOT EXISTS books (
    id               BIGSERIAL PRIMARY KEY,
    title            VARCHAR(255) NOT NULL,
    isbn             VARCHAR(32) UNIQUE,
    year             INT,
    total_copies     INT NOT NULL DEFAULT 1,
    available_copies INT NOT NULL DEFAULT 1
);
CREATE INDEX IF NOT EXISTS books_title_idx ON books USING gin (to_tsvector('simple', title));

CREATE TABLE IF NOT EXISTS book_authors (
    book_id    BIGINT REFERENCES books(id) ON DELETE CASCADE,
    author_id  BIGINT REFERENCES authors(id) ON DELETE CASCADE,
    PRIMARY KEY (book_id, author_id)
);

-- Lifecycle: ORDERED  -> ISSUED   -> RETURNED
--                    \-> CANCELLED
-- type only meaningful when ISSUED: SUBSCRIPTION (home) | READING_HALL (on-site)
CREATE TABLE IF NOT EXISTS loans (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT NOT NULL REFERENCES users(id),
    book_id      BIGINT NOT NULL REFERENCES books(id),
    librarian_id BIGINT REFERENCES users(id),
    status       VARCHAR(16) NOT NULL DEFAULT 'ORDERED',
    type         VARCHAR(16),
    ordered_at   TIMESTAMP NOT NULL DEFAULT now(),
    issued_at    TIMESTAMP,
    due_at       TIMESTAMP,
    returned_at  TIMESTAMP
);
CREATE INDEX IF NOT EXISTS loans_user_idx   ON loans(user_id);
CREATE INDEX IF NOT EXISTS loans_status_idx ON loans(status);

-- demo data
INSERT INTO authors(full_name, country) VALUES
    ('Тарас Шевченко', 'Ukraine'),
    ('Леся Українка', 'Ukraine'),
    ('George Orwell', 'UK')
ON CONFLICT DO NOTHING;

INSERT INTO books(title, isbn, year, total_copies, available_copies) VALUES
    ('Кобзар', '978-1', 1840, 3, 3),
    ('Лісова пісня', '978-2', 1911, 2, 2),
    ('1984', '978-3', 1949, 5, 5)
ON CONFLICT DO NOTHING;

INSERT INTO book_authors(book_id, author_id)
SELECT b.id, a.id FROM books b, authors a
WHERE (b.title='Кобзар' AND a.full_name='Тарас Шевченко')
   OR (b.title='Лісова пісня' AND a.full_name='Леся Українка')
   OR (b.title='1984' AND a.full_name='George Orwell')
ON CONFLICT DO NOTHING;
