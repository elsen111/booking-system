# Booking API

REST API for booking shared resources (meeting rooms, equipment, desks) with role-based access control.
Built with Spring Boot 3.5, PostgreSQL, JWT authentication with rotating refresh tokens, Liquibase migrations and OpenAPI docs.

Full endpoint reference: [API.md](API.md)

## Features

- **Roles:** `USER` books resources and manages their own bookings; `ADMIN` manages resources and sees all bookings
- **JWT authentication** with short-lived access tokens and **rotating refresh tokens** (stored hashed, reuse detection)
- **No double bookings:** enforced by a PostgreSQL exclusion constraint, not only by application code
- **Pagination, sorting and filtering** on list endpoints (sort properties are whitelisted)
- **Rate limiting** per client IP, stricter on `/auth/**`
- **Consistent responses:** `ApiResponse` for success and `ErrorResponse` for errors, both carry the HTTP status
- **OpenAPI / Swagger UI** with bearer authentication
- **Docker** image, Docker Compose setup and a Render blueprint
- **Integration tests** against a real PostgreSQL (Testcontainers)

## Tech stack

Java 21 · Gradle · Spring Boot (Web, Data JPA, Security, Validation, Actuator) · PostgreSQL 16 · Liquibase (YAML) ·
MapStruct · Lombok · jjwt · Bucket4j + Caffeine · springdoc-openapi · JUnit 5 + Testcontainers

## Quick start

Requirements: Docker. JDK 21 is only needed to run from the IDE or Gradle.

```bash
cp .env.example .env          # then edit the values (at least the passwords and JWT_SECRET)
docker compose up --build -d  # PostgreSQL + the API
docker compose logs -f app    # wait for "Started BookingApiApplication"
```

| URL | Description |
|---|---|
| http://localhost:8080/swagger-ui.html | Swagger UI (use **Authorize** with an access token) |
| http://localhost:8080/v3/api-docs | OpenAPI document |
| http://localhost:8080/actuator/health | Health check |

A bootstrap admin is created on first start from `ADMIN_EMAIL` / `ADMIN_PASSWORD`. Log in with it via `POST /api/v1/auth/login`.

### Develop from the IDE instead

The container and `bootRun` both use port 8080, so run only the database:

```bash
docker compose stop app        # if the app container is running
docker compose up -d postgres
./gradlew bootRun              # reads .env from the project root
```

In IntelliJ, set the run configuration's working directory to the project root so `.env` is found.

### Useful commands

```bash
docker compose up --build -d   # rebuild and restart after code changes
docker compose logs -f app     # follow the app logs
docker compose down            # stop everything, keep the data
docker compose down -v         # stop everything and delete the database volume
```

## Configuration

Configuration comes from environment variables. Locally they are read from `.env` (template: `.env.example`);
in deployed environments they are set on the platform. `.env` is git-ignored and excluded from the Docker image.
Real environment variables always win over `.env`.

| Variable | Required in prod | Description |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | yes | `prod` in deployed environments |
| `DB_HOST`, `DB_PORT`, `DB_NAME` | yes | PostgreSQL location (or one full JDBC URL via `DB_URL`) |
| `DB_USERNAME`, `DB_PASSWORD` | yes | Database credentials |
| `JWT_SECRET` | yes | HMAC secret, at least 32 characters (`openssl rand -base64 48`) |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | yes | Bootstrap admin, created on startup if missing |
| `PORT` | no | HTTP port (default 8080, set automatically by most platforms) |
| `APP_CORS_ALLOWED_ORIGINS` | no | Comma-separated origins, only if CORS is enabled |

The `prod` profile has no defaults for secrets, so the app refuses to start when one is missing.
Token lifetimes and rate limits are configured under `app.*` in `application.yaml`:

| Property | Default |
|---|---|
| `app.jwt.access-token-ttl` | 15m |
| `app.jwt.refresh-token-ttl` | 7d |
| `app.rate-limit.auth-per-minute` | 10 |
| `app.rate-limit.general-per-minute` | 60 |

## Project structure

Package-by-feature, with layers inside each feature.

```
src/main/java/bookingapi/
├── BookingApiApplication.java
├── config/              Security, OpenAPI, JPA auditing, scheduling, admin bootstrap, typed properties
├── common/              BaseEntity, ApiResponse / ErrorResponse / PageResponse, exceptions, PageableSanitizer
├── security/            JWT service + filter, rate-limit filter, 401/403/429 writer, AuthenticatedUser
├── auth/                controller, dto, service (+impl), RefreshToken entity and cleanup job
├── user/                User, Role, repository
├── resource/            domain, dto, mapper, repository (+specifications), service (+impl), controller
└── booking/             domain, dto, mapper, repository (+specifications), service (+impl), controller

src/main/resources/
├── application.yaml
├── application-prod.yaml
└── db/changelog/        Liquibase master + numbered YAML changesets
```

## Design decisions

- **Plain entities, logic in services.** Entities only describe table columns; builders and MapStruct mappers create them. The trade-off is that rules like "can't cancel twice" live in the service layer.
- **Liquibase owns the schema.** Hibernate only validates it (`ddl-auto: validate`).
- **Double booking:** a service check gives a friendly error, but the `EXCLUDE USING gist` constraint on `(resource_id, time range)` is the real guarantee under concurrent requests. Cancelled bookings don't block a slot, and back-to-back bookings are allowed (ranges are half-open).
- **Refresh tokens:** random opaque values stored as SHA-256 hashes and rotated on every use. Presenting an already-used token revokes all of that user's tokens.
- **Authorization in two layers:** coarse rules (public / authenticated / admin) in `SecurityConfig`; data rules (booking ownership) in the service layer. Accessing someone else's booking returns `404`, not `403`, so ids can't be probed.
- **Soft delete for resources:** bookings reference resources, so an admin deactivates a resource (`active: false`) instead of deleting it. Regular users only see active resources.
- **Stateless access tokens:** a disabled user keeps access until the access token expires (15 minutes); refreshing is blocked immediately.
- **Rate limiting** uses in-memory buckets per instance. If you run several instances, switch Bucket4j to its Redis integration.

## Tests

```bash
./gradlew test      # needs Docker: Testcontainers starts PostgreSQL
```

Integration tests cover registration and login, refresh rotation and reuse detection, role-based access,
booking conflicts and ownership, pagination and sorting, and rate limiting.

## Deployment

### Render

1. Push the repository to GitHub (including `gradlew`, `gradle/wrapper/`, `Dockerfile` and `render.yaml`).
2. In Render: **New → Blueprint**, select the repository.
3. Provide `ADMIN_EMAIL` and `ADMIN_PASSWORD` when prompted. The database variables are wired automatically and `JWT_SECRET` is generated.
4. After the deploy, check `/actuator/health` and `/swagger-ui.html`.

Free-tier notes: free web services spin down after a period of inactivity (the first request afterwards is slow),
and the free database expires after 30 days. Use a paid plan for anything long-lived.

### Railway or any Docker host

Use the same `Dockerfile` and set the variables from the configuration table. Point `DB_HOST`, `DB_PORT`,
`DB_NAME`, `DB_USERNAME` and `DB_PASSWORD` at your PostgreSQL instance, or set `DB_URL` to a full JDBC URL.

## Troubleshooting

| Symptom | Cause and fix |
|---|---|
| Only the `postgres` container exists | Run `docker compose up --build -d` from the project root; check `docker compose ps -a` |
| App container exits right away | `docker compose logs app`; usually a missing variable in `.env` |
| Database authentication failed after editing `.env` | The volume keeps the old credentials: `docker compose down -v` |
| `Address already in use` on 8080 | The container and `bootRun` can't run together: `docker compose stop app` |
| `429 Too Many Requests` | Rate limit reached; wait for the `Retry-After` seconds |
| App refuses to start in `prod` | A required variable is missing (see the configuration table) |
