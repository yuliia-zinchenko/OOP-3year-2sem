# Library System — лабораторна робота з ООП

Бекенд на чистих **Servlets + JDBC** (без Spring), фронтенд на React.

## Стек

| Шар | Технології |
|-----|-----------|
| Backend | Java 17, Servlets 5, JDBC, HikariCP, Lombok, MapStruct, log4j2 |
| DB | PostgreSQL 16 |
| Auth | Keycloak / Auth0 (JWT, RS256) |
| Frontend | React 18 + Vite |
| Build | Maven (backend), Gradle (можна замінити), npm (frontend) |
| Server | Apache Tomcat 10.1 |
| CI/CD | GitHub Actions, GitLab CI |
| IaC | Pulumi (TypeScript) → AWS ECS Fargate |

## Структура

```
lab_1/
├── backend/        # Java + Maven + Servlets
├── frontend/       # React + Vite
├── infra/pulumi/   # AWS ECS deployment
├── docker-compose.yml   # Postgres + Keycloak локально
└── .github/.gitlab/     # CI
```

## Архітектура (MVC + GOF)

- **MVC**: Servlets = Controller, Service+DAO = Model, React = View
- **Singleton**: `DatabaseConnection` (HikariCP pool)
- **Factory**: `ServiceFactory` створює сервіси за типом
- **DAO**: абстрактний `BaseDao<T>` + конкретні реалізації
- **Builder**: Lombok `@Builder` на entity
- **Command**: `Command` interface для дій над книгами
- **Strategy**: `AuthStrategy` (Keycloak vs Auth0)
- **Chain of Responsibility**: фільтри (CORS → JWT → Role)

## Таблиці (5)

1. `users` — користувачі (sub з JWT)
2. `authors` — автори
3. `books` — книги
4. `book_authors` — many-to-many
5. `loans` — видачі книг

## Запуск локально

```bash
docker-compose up -d            # Postgres + Keycloak
cd backend && mvn package        # збирає WAR
# деплой target/library.war у Tomcat 10
cd frontend && npm i && npm run dev
```

## Деплой в AWS

```bash
cd infra/pulumi && pulumi up
```
