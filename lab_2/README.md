# Library System — лабораторна №2

Та сама предметна область, що й у `lab_1` (Читач шукає та замовляє Книги, Бібліотекар видає на абонемент / у читальний зал, Книги мають кілька примірників), але реалізована на сучасному Spring-стеку.

## Стек

| Шар | lab_1 | **lab_2** |
|-----|-------|-----------|
| Web | Servlets 5 (Tomcat) | **Spring Boot 3 (embedded)** |
| ORM | JDBC + HikariCP | **Hibernate / Spring Data JPA** |
| Міграції | `schema.sql` | **Liquibase** (`db/changelog/*.xml`) |
| Auth | ручний JWT-фільтр | **Spring Security + OAuth2 Resource Server** |
| Identity | Keycloak / Auth0 | Keycloak / Auth0 (через стратегію ролей) |
| Frontend | React + Vite | React + Vite (без змін) |
| DB | PostgreSQL 16 | PostgreSQL 16 |

## Структура

```
lab_2/
├── backend/                # Spring Boot 3 + JPA + Liquibase + Security
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/
│       ├── java/com/library/
│       │   ├── LibraryApplication.java
│       │   ├── domain/         # JPA-сутності (UserAccount, Book, Author, Loan, ...)
│       │   ├── repository/     # Spring Data JPA репозиторії
│       │   ├── dto/            # DTO + request-моделі
│       │   ├── mapper/         # MapStruct
│       │   ├── service/        # BookService, LoanService
│       │   ├── controller/     # REST endpoints
│       │   └── security/       # SecurityConfig, JwtRolesStrategy (Keycloak/Auth0)
│       └── resources/
│           ├── application.yml
│           └── db/changelog/   # Liquibase changelog
├── frontend/               # React + Vite (повторно з lab_1)
└── docker-compose.yml      # Postgres + Keycloak + backend
```

## Архітектура

- **Layered**: Controller → Service → Repository (Spring Data) → DB.
- **Strategy**: `JwtRolesStrategy` має дві реалізації — `KeycloakRolesStrategy` (читає `realm_access.roles`) та `Auth0RolesStrategy` (читає кастомний namespaced claim `https://library.app/roles`). Перемикання — змінною середовища `JWT_PROVIDER`.
- **DTO + Mapper**: MapStruct ізолює транспортні моделі від JPA-сутностей.
- **Builder**: Lombok `@Builder` на сутностях.
- **Method security**: `@PreAuthorize("hasRole('LIBRARIAN')")` на ендпоінтах видачі / прийому повернення.

## Таблиці (5)

`users`, `authors`, `books`, `book_authors` (m-n), `loans` — створюються Liquibase-міграцією `001-init-schema.xml`, наповнюються `002-seed-data.xml`.

## Життєвий цикл `Loan`

```
ORDERED ──issue──▶ ISSUED ──return──▶ RETURNED
   │
   └──cancel──▶ CANCELLED
```
`type` (SUBSCRIPTION / READING_HALL) задається у момент видачі.

## REST API

| Метод | Шлях | Роль | Опис |
|-------|------|------|------|
| GET | `/api/me` | будь-який автентифікований | поточний користувач (auto-provision на основі JWT) |
| GET | `/api/books?q=…` | будь-який | пошук у каталозі |
| GET | `/api/loans` | READER | мої замовлення / видачі |
| GET | `/api/loans?status=ORDERED` | LIBRARIAN | черга на видачу |
| POST | `/api/loans` | READER | замовити книгу |
| POST | `/api/loans/{id}/cancel` | READER (свій) | скасувати замовлення |
| POST | `/api/loans/{id}/issue` | LIBRARIAN | видати (`type`) |
| POST | `/api/loans/{id}/return` | LIBRARIAN | прийняти повернення |

## Запуск локально

```bash
docker-compose up -d postgres keycloak
cd backend && mvn package
java -jar target/library.jar          # або: docker-compose up backend
cd ../frontend && npm i && npm run dev
```

Realm `library` у Keycloak (8081):
- ролі: `READER`, `LIBRARIAN`
- клієнт `library-frontend` (public, redirect `http://localhost:3000/*`)

## Перемикання на Auth0

```bash
JWT_PROVIDER=auth0 \
JWT_ISSUER_URI=https://YOUR_TENANT.eu.auth0.com/ \
JWT_JWKS_URI=https://YOUR_TENANT.eu.auth0.com/.well-known/jwks.json \
AUTH0_ROLES_CLAIM=https://library.app/roles \
java -jar backend/target/library.jar
```
Auth0 Action для додавання ролі у токен:
```js
exports.onExecutePostLogin = async (event, api) => {
  const roles = event.authorization?.roles ?? [];
  api.idToken.setCustomClaim("https://library.app/roles", roles);
  api.accessToken.setCustomClaim("https://library.app/roles", roles);
};
```
