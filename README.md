# Library System — лабораторні роботи з ООП

Дві лабораторні роботи з однаковою предметною областю (бібліотека). Фронтенд (React + Vite).

**Предметна область (спільна для обох):** Читач шукає та замовляє книги в каталозі. Бібліотекар видає Читачеві книгу на абонемент або в читальний зал. Книга може мати один або кілька примірників.

```
ООП/
├── lab_1/      # Servlets + JDBC + Tomcat + Pulumi/AWS
├── lab_2/      # Spring Boot + JPA + Liquibase + Spring Security
└── README.md   # цей файл
```

---

## Лабораторна робота №1 — Servlets + JDBC

**Умова:** Servlets, Filters, JDBC, PostgreSQL, Keycloak/Auth0 (JWT), фронт (Angular/React/Vue), log4j, REST, Lombok, MapStruct, MVC, GOF, до 5–6 таблиць, Gradle/Maven, Tomcat, GitHub/GitLab + Pulumi + AWS ECS.

### Стек

| Шар | Технології |
|-----|------------|
| Backend | Java 17, Servlets 5 (Jakarta), JDBC, HikariCP, Lombok, MapStruct, log4j2 |
| DB | PostgreSQL 16 (DDL у [backend/src/main/resources/schema.sql](lab_1/backend/src/main/resources/schema.sql)) |
| Auth | Keycloak / Auth0 — JWT RS256, перевірка вручну через JWKS |
| Frontend | React 18 + Vite, `keycloak-js` |
| Build | Maven (бекенд), npm (фронт) |
| Server | Apache Tomcat 10.1 |
| CI/CD | GitHub Actions ([.github](lab_1/.github)), GitLab CI ([.gitlab-ci.yml](lab_1/.gitlab-ci.yml)) |
| IaC | Pulumi → AWS ECS Fargate ([infra/pulumi](lab_1/infra/pulumi)) |

### Архітектура (MVC + GOF)

- **MVC** — `Servlet` (Controller) → `Service` → `DAO` (Model), React (View).
- **Singleton** — [DatabaseConnection.java](lab_1/backend/src/main/java/com/library/config/DatabaseConnection.java) (Hikari pool).
- **Factory** — [ServiceFactory.java](lab_1/backend/src/main/java/com/library/factory/ServiceFactory.java).
- **DAO** — [BaseDao.java](lab_1/backend/src/main/java/com/library/dao/BaseDao.java) + конкретні `BookDao`, `LoanDao`, `UserDao`, `AuthorDao`.
- **Builder** — Lombok `@Builder` на entity ([model](lab_1/backend/src/main/java/com/library/model)).
- **Command** — `OrderBookCommand`, `IssueBookCommand`, `ReturnBookCommand`, `CancelOrderCommand` ([command](lab_1/backend/src/main/java/com/library/command)).
- **Strategy** — [AuthStrategy.java](lab_1/backend/src/main/java/com/library/security/AuthStrategy.java) (`JwksAuthStrategy` для Keycloak/Auth0).
- **Chain of Responsibility** — фільтри CORS → JWT ([filter](lab_1/backend/src/main/java/com/library/filter)).

### Таблиці (5)

`users`, `authors`, `books`, `book_authors` (m-n), `loans` — створюються автоматично з `schema.sql` при старті Postgres-контейнера.

### Локальний запуск

```bash
cd lab_1
docker-compose up -d postgres keycloak     # Postgres + Keycloak
cd backend && mvn clean package -DskipTests
cd ..
docker-compose up -d tomcat                # підхопить backend/target/library.war
cd frontend && npm install && npm run dev  # http://localhost:3000
```

Сервіси після старту:
- Backend (Tomcat): http://localhost:8080/library/
- Keycloak admin: http://localhost:8081 (`admin` / `admin`)
- Frontend: http://localhost:3000
- Postgres: `localhost:5432` (`library` / `library` / `library`)

Перед першим запитом створіть realm `library`, ролі `READER`, `LIBRARIAN`, клієнт `library-frontend` (public, redirect `http://localhost:3000/*`) і користувача.

### Деплой в AWS

```bash
cd lab_1/infra/pulumi
pulumi up        # ECS Fargate + RDS Postgres
```

---

## Лабораторна робота №2 — Spring Boot

**Умова:** Spring, Hibernate, Spring Data, Spring Security, Liquibase, Keycloak/Auth0 (JWT). Та сама бібліотечна предметна область.

### Стек (та що змінилося відносно lab_1)

| Шар | lab_1 | **lab_2** |
|-----|-------|-----------|
| Web | Servlets 5 (Tomcat) | **Spring Boot 3 (embedded Tomcat)** |
| ORM | JDBC + HikariCP | **Hibernate / Spring Data JPA** |
| Міграції | `schema.sql` ad-hoc | **Liquibase** (`db/changelog/*.xml`) |
| Auth | ручний JWT-фільтр | **Spring Security + OAuth2 Resource Server** |
| Identity | Keycloak / Auth0 | Keycloak / Auth0 (стратегія ролей) |
| DTO/Mapper | MapStruct | MapStruct |
| Frontend | React + Vite | React + Vite |

### Архітектура

- **Layered** — `Controller` → `Service` → `Repository` (Spring Data) → DB.
- **Strategy** — [JwtRolesStrategy.java](lab_2/backend/src/main/java/com/library/security/JwtRolesStrategy.java) має дві реалізації: `KeycloakRolesStrategy` (читає `realm_access.roles`) та `Auth0RolesStrategy` (читає кастомний claim `https://library.app/roles`). Перемикання — через env `JWT_PROVIDER`.
- **DTO + Mapper** — MapStruct ізолює API-моделі від JPA-сутностей ([mapper](lab_2/backend/src/main/java/com/library/mapper)).
- **Builder** — Lombok `@Builder` на сутностях ([domain](lab_2/backend/src/main/java/com/library/domain)).
- **Method security** — `@PreAuthorize("hasRole('LIBRARIAN')")` на видачі / прийомі книги.

### Таблиці (5)

`users`, `authors`, `books`, `book_authors`, `loans` — створюються Liquibase-міграцією `001-init-schema.xml`, наповнюються `002-seed-data.xml`.

### Життєвий цикл `Loan`

```
ORDERED ──issue──▶ ISSUED ──return──▶ RETURNED
   │
   └──cancel──▶ CANCELLED
```
`type` (`SUBSCRIPTION` / `READING_HALL`) задається в момент видачі.

### REST API

| Метод | Шлях | Роль | Опис |
|-------|------|------|------|
| GET  | `/api/me` | будь-який автент. | поточний користувач (auto-provision з JWT) |
| GET  | `/api/books?q=…` | будь-який | пошук у каталозі |
| GET  | `/api/loans` | READER | мої замовлення / видачі |
| GET  | `/api/loans?status=ORDERED` | LIBRARIAN | черга на видачу |
| POST | `/api/loans` | READER | замовити книгу |
| POST | `/api/loans/{id}/cancel` | READER (свій) | скасувати |
| POST | `/api/loans/{id}/issue` | LIBRARIAN | видати (`type`) |
| POST | `/api/loans/{id}/return` | LIBRARIAN | прийняти повернення |

### Локальний запуск

```bash
cd lab_2
docker-compose up -d postgres keycloak
cd backend && mvn clean package -DskipTests
docker-compose up -d backend             # або: java -jar target/library.jar
cd ../frontend && npm install && npm run dev
```

Порти ті самі, що в lab_1 (8080 / 8081 / 5432 / 3000). Realm `library`, ролі `READER`, `LIBRARIAN`.

### Перемикання на Auth0

```bash
JWT_PROVIDER=auth0 \
JWT_ISSUER_URI=https://YOUR_TENANT.eu.auth0.com/ \
JWT_JWKS_URI=https://YOUR_TENANT.eu.auth0.com/.well-known/jwks.json \
AUTH0_ROLES_CLAIM=https://library.app/roles \
java -jar lab_2/backend/target/library.jar
```

Action в Auth0 для проброшування ролей у токен:
```js
exports.onExecutePostLogin = async (event, api) => {
  const roles = event.authorization?.roles ?? [];
  api.idToken.setCustomClaim("https://library.app/roles", roles);
  api.accessToken.setCustomClaim("https://library.app/roles", roles);
};
```

---

## Аналіз: чи виконано умови

### Лабораторна №1

| Вимога | Стан | Де подивитися |
|--------|------|---------------|
| Servlets | ✅ | [controller](lab_1/backend/src/main/java/com/library/controller) — `BookServlet`, `LoanServlet`, `UserServlet` |
| Filters | ✅ | [filter](lab_1/backend/src/main/java/com/library/filter) — `CorsFilter`, `JwtFilter` |
| JDBC | ✅ | [dao](lab_1/backend/src/main/java/com/library/dao) + HikariCP |
| PostgreSQL | ✅ | docker-compose, `schema.sql` |
| Keycloak / Auth0 (JWT) | ✅ | `JwksAuthStrategy` через JWKS |
| Frontend (Angular/React/Vue) | ✅ | React + Vite |
| log4j | ✅ | `log4j2.xml` (формально log4j**2**, а не оригінальний log4j 1.x — вимогу прийнято виконаною) |
| REST | ✅ | сервлети повертають JSON |
| Lombok, MapStruct | ✅ | `pom.xml`, `mapper/` |
| MVC | ✅ | сервлет (C) — сервіс/DAO (M) — React (V) |
| GOF | ✅ | Singleton, Factory, DAO, Builder, Command, Strategy, Chain of Responsibility |
| ≤ 5–6 таблиць | ✅ | 5 таблиць |
| Gradle / Maven | ⚠️ | присутній **Maven**; Gradle не доданий (у формулюванні «Gradle, Maven» — за фактом достатньо одного будівельника, але якщо викладач вимагає обидва — треба додати `build.gradle`) |
| Tomcat | ✅ | `tomcat:10.1-jdk17`, WAR-пакування |
| GitHub / GitLab | ✅ | `.github/workflows`, `.gitlab-ci.yml` |
| Pulumi + AWS ECS | ✅ | `infra/pulumi` (TypeScript → ECS Fargate) |

**Слабке місце:** Gradle відсутній — якщо вимога обов’язкова, додати `build.gradle` поруч із `pom.xml`.

### Лабораторна №2

| Вимога | Стан | Де подивитися |
|--------|------|---------------|
| Spring (Boot 3) | ✅ | [LibraryApplication.java](lab_2/backend/src/main/java/com/library/LibraryApplication.java) |
| Hibernate | ✅ | JPA-сутності у [domain](lab_2/backend/src/main/java/com/library/domain) |
| Spring Data | ✅ | [repository](lab_2/backend/src/main/java/com/library/repository) |
| Spring Security | ✅ | [SecurityConfig.java](lab_2/backend/src/main/java/com/library/security/SecurityConfig.java) + `@PreAuthorize` |
| Liquibase | ✅ | `resources/db/changelog/*.xml` |
| Keycloak / Auth0 (JWT) | ✅ | `KeycloakRolesStrategy` / `Auth0RolesStrategy` через `JWT_PROVIDER` |
| Бібліотечна доменна модель | ✅ | `Book`, `Author`, `Loan` (ORDERED/ISSUED/RETURNED/CANCELLED), `type` = SUBSCRIPTION/READING_HALL |

**Слабке місце:** в умові згадано читальний зал (мабуть «у читальний зал» — обірвано); якщо викладач вимагає окремої сутності для «примірників» (`book_copies`) — її зараз немає: кількість примірників тримається полем `copies_total`/`copies_available` у `books`. Це коректне спрощення, але варто бути готовим обґрунтувати на захисті.

---

## Передумови (середовище)

- Docker + Docker Compose
- JDK 17
- Maven 3.9+
- Node.js 18+ та npm
- (опційно) Pulumi CLI + AWS credentials — лише для деплою lab_1 в AWS

## Загальна інструкція запуску

1. Клонуйте репозиторій і перейдіть у потрібну лабу:
   ```bash
   cd lab_1   # або: cd lab_2
   ```
2. Підніміть інфраструктуру (Postgres + Keycloak):
   ```bash
   docker-compose up -d postgres keycloak
   ```
3. Налаштуйте Keycloak (http://localhost:8081, `admin`/`admin`):
   - Realm: `library`
   - Ролі: `READER`, `LIBRARIAN`
   - Клієнт: `library-frontend` (public, redirect `http://localhost:3000/*`, web origin `+`)
   - Створіть користувача, призначте йому одну з ролей
4. Зберіть і запустіть backend:
   - **lab_1:** `cd backend && mvn clean package -DskipTests && cd .. && docker-compose up -d tomcat`
   - **lab_2:** `cd backend && mvn clean package -DskipTests && docker-compose up -d backend`
5. Запустіть frontend:
   ```bash
   cd frontend && npm install && npm run dev
   ```
6. Відкрийте http://localhost:3000, увійдіть через Keycloak, перевірте сценарії: пошук → замовлення (READER) → видача / повернення (LIBRARIAN).

## Зупинка

```bash
docker-compose down            # зупинити контейнери
docker-compose down -v         # + видалити том з даними Postgres
```
