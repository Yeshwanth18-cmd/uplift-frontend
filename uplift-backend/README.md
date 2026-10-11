# UpLift Backend

Spring Boot starter backend for UpLift.

## Requirements

- Java 21
- Maven 3.9+

## Run immediately

This starter uses an H2 in-memory database by default, so you do not need PostgreSQL credentials yet.

### Windows PowerShell

```powershell
mvn spring-boot:run
```

If your checkout includes Maven Wrapper, you can instead run:

```powershell
.\mvnw.cmd spring-boot:run
```

API health:
http://localhost:8080/api/health

Swagger:
http://localhost:8080/swagger-ui/index.html

H2 console:
http://localhost:8080/h2-console

JDBC URL:
jdbc:h2:mem:upliftdb

User:
sa

Password:
(empty)

## MySQL

The default profile uses H2 in-memory storage for local development and tests.
To use MySQL, create the `uplift_db` database, provide `DB_USERNAME` and
`DB_PASSWORD`, then activate the `mysql` profile:

```powershell
$env:SPRING_PROFILES_ACTIVE = "mysql"
$env:DB_USERNAME = "your-database-user"
$env:DB_PASSWORD = "your-database-password"
.\mvnw.cmd spring-boot:run
```

Optionally set `DB_URL` to override the default MySQL connection URL. Set
`JWT_SECRET` to a unique high-entropy value before deploying outside local
development.

## Marketplace MVP API

- `POST /api/auth/register`, `POST /api/auth/login`
- `GET /api/projects`, `POST /api/projects`, `GET /api/projects/{id}`
- `PATCH /api/projects/{id}/status` to cancel an open project or complete an active one
- `POST /api/projects/{id}/proposals` to submit a freelancer proposal
- `GET /api/proposals/mine`, `GET /api/proposals/inbox`
- `PATCH /api/proposals/{id}/status` to accept or reject a pending proposal
- `GET /api/dashboard` for role-specific marketplace statistics

All marketplace endpoints require the bearer token returned by authentication.
Public registration supports client and freelancer accounts; administrator
accounts must be provisioned separately.
