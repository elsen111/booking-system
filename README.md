<div align="center">

# 📅 Booking API

**Book shared resources with role-based access, rotating JWT sessions and zero double bookings.**

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-build-02303A?logo=gradle&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-ready-2496ED?logo=docker&logoColor=white)
![Liquibase](https://img.shields.io/badge/Liquibase-migrations-2962FF?logo=liquibase&logoColor=white)
![OpenAPI](https://img.shields.io/badge/OpenAPI-Swagger-85EA2D?logo=swagger&logoColor=black)
![JWT](https://img.shields.io/badge/Auth-JWT-000000?logo=jsonwebtokens&logoColor=white)

[🚀 Quick start](#quick-start) · [📖 API reference](docs/API.md) · [⚙️ Configuration](#configuration) · [☁️ Deployment](#deployment)

</div>

---

## 📚 Table of contents

- [✨ Features](#features)
- [🧰 Tech stack](#tech-stack)
- [🚀 Quick start](#quick-start)
- [⚙️ Configuration](#configuration)
- [🗂️ Project structure](#project-structure)
- [🧠 Design decisions](#design-decisions)
- [🧪 Tests](#tests)
- [☁️ Deployment](#deployment)
- [🩺 Troubleshooting](#troubleshooting)

Full endpoint reference: **[docs/API.md](docs/API.md)** 📖

<a id="features"></a>
## ✨ Features

| | Feature | Description |
|---|---|---|
| 👥 | **Role-based access** | `USER` books resources and manages their own bookings; `ADMIN` manages resources and sees everything |
| 🔐 | **JWT authentication** | Short-lived access tokens plus **rotating refresh tokens** (stored hashed, with reuse detection) |
| 🚫 | **No double bookings** | Enforced by a PostgreSQL exclusion constraint, not only by application code |
| 📄 | **Pagination, sorting, filtering** | On all list endpoints, with whitelisted sort properties |
| 🛡️ | **Rate limiting** | Per client IP, stricter on `/auth/**` |
| 📦 | **Consistent responses** | `ApiResponse` for success, `ErrorResponse` for errors, both carry the HTTP status |
| 📘 | **OpenAPI / Swagger UI** | With bearer authentication built in |
| 🐳 | **Docker ready** | Dockerfile, Compose setup and a Render blueprint |
| ✅ | **Integration tests** | Run against a real PostgreSQL (Testcontainers) |

<a id="tech-stack"></a>
## 🧰 Tech stack

| | Technology |
|---|---|
| ☕ | Java 21, Gradle |
| 🍃 | Spring Boot 3.5 (Web, Data JPA, Security, Validation, Actuator) |
| 🐘 | PostgreSQL 16 |
| 🧬 | Liquibase (YAML changelogs) |
| 🔄 | MapStruct, Lombok |
| 🔑 | jjwt (JWT) |
| ⏱️ | Bucket4j + Caffeine (rate limiting) |
| 📘 | springdoc-openapi |
| 🧪 | JUnit 5, Testcontainers |

<a id="quick-start"></a>
## 🚀 Quick start

Requirements: 🐳 Docker. ☕ JDK 21 is only needed to run from the IDE or Gradle.

The application lives in the `booking-api/` folder, so run every command from there:

```bash
cd booking-api
cp .env.example .env          # then edit the values (at least the passwords and JWT_SECRET)
docker compose up --build -d  # PostgreSQL + the API
docker compose logs -f app    # wait for "Started BookingApiApplication"
```

| 🔗 URL | Description |
|---|---|
| http://localhost:8080/swagger-ui.html | 📘 Swagger UI (use **Authorize** with an access token) |
| http://localhost:8080/v3/api-docs | 📄 OpenAPI document |
| http://localhost:8080/actuator/health | 🩺 Health check |

👑 A bootstrap admin is created on first start from `ADMIN_EMAIL` / `ADMIN_PASSWORD`. Log in with it via `POST /api/v1/auth/login`.

### 💻 Develop from the IDE instead

The container and `bootRun` both use port 8080, so run only the database:

```bash
cd booking-api
docker compose stop app        # if the app container is running
docker compose up -d postgres
./gradlew bootRun              # reads .env from the booking-api folder
```

> 💡 In IntelliJ, open the `booking-api` folder as the project and set the run configuration's working directory to it, so `.env` is found.

### 🛠️ Useful commands

```bash
docker compose up --build -d   # 🔁 rebuild and restart after code changes
docker compose logs -f app     # 📜 follow the app logs
docker compose down            # ⏹️ stop everything, keep the data
docker compose down -v         # 🗑️ stop everything and delete the database volume
```

<a id="configuration"></a>
## ⚙️ Configuration

Configuration comes from environment variables. Locally they are read from `.env` (template: `.env.example`);
in deployed environments they are set on the platform. `.env` is git-ignored 🙈 and excluded from the Docker image.
Real environment variables always win over `.env`.

| Variable | Required in prod | Description |
|---|:-:|---|
| `SPRING_PROFILES_ACTIVE` | ✅ | `prod` in deployed environments |
| `DB_HOST`, `DB_PORT`, `DB_NAME` | ✅ | PostgreSQL location (or one full JDBC URL via `DB_URL`) |
| `DB_USERNAME`, `DB_PASSWORD` | ✅ | Database credentials |
| `JWT_SECRET` | ✅ | HMAC secret, at least 32 characters (`openssl rand -base64 48`) |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | ✅ | Bootstrap admin, created on startup if missing |
| `PORT` | ➖ | HTTP port (default 8080, set automatically by most platforms) |
| `APP_CORS_ALLOWED_ORIGINS` | ➖ | Comma-separated origins, only if CORS is enabled |

> 🔒 The `prod` profile has no defaults for secrets, so the app refuses to start when one is missing.

Token lifetimes and rate limits live under `app.*` in `application.yaml`:

| Property | Default |
|---|---|
| `app.jwt.access-token-ttl` | ⏳ 15m |
| `app.jwt.refresh-token-ttl` | 🔄 7d |
| `app.rate-limit.auth-per-minute` | 🔑 10 |
| `app.rate-limit.general-per-minute` | 🌐 60 |

<a id="project-structure"></a>
## 🗂️ Project structure

Repository layout 📁:

```
booking-system/
├── README.md
├── docs/
│   └── API.md              📖 endpoint reference
└── booking-api/            🍃 the Spring Boot project (run all commands from here)
    ├── src/
    ├── Dockerfile
    ├── docker-compose.yml
    ├── build.gradle
    ├── .env.example
    └── ...
```

Inside `booking-api/src/main`, package-by-feature 📦 with layers inside each feature:

```
java/bookingapi/
├── BookingApiApplication.java
├── config/       ⚙️  Security, OpenAPI, JPA auditing, scheduling, admin bootstrap, typed properties
├── common/       🧱  BaseEntity, ApiResponse / ErrorResponse / PageResponse, exceptions, PageableSanitizer
├── security/     🔐  JWT service + filter, rate-limit filter, 401/403/429 writer, AuthenticatedUser
├── auth/         🪪  controller, dto, service (+impl), RefreshToken entity and cleanup job
├── user/         👤  User, Role, repository
├── resource/     🏢  domain, dto, mapper, repository (+specifications), service (+impl), controller
└── booking/      📅  domain, dto, mapper, repository (+specifications), service (+impl), controller

resources/
├── application.yaml
├── application-prod.yaml
└── db/changelog/    🧬 Liquibase master + numbered YAML changesets
```

<a id="design-decisions"></a>
## 🧠 Design decisions

- 🧱 **Plain entities, logic in services.** Entities only describe table columns; builders and MapStruct mappers create them. The trade-off is that rules like "can't cancel twice" live in the service layer.
- 🧬 **Liquibase owns the schema.** Hibernate only validates it (`ddl-auto: validate`).
- 🚫 **Double booking:** a service check gives a friendly error, but the `EXCLUDE USING gist` constraint on `(resource_id, time range)` is the real guarantee under concurrent requests. Cancelled bookings don't block a slot, and back-to-back bookings are allowed (ranges are half-open).
- 🔄 **Refresh tokens:** random opaque values stored as SHA-256 hashes and rotated on every use. Presenting an already-used token revokes all of that user's tokens.
- 🔐 **Authorization in two layers:** coarse rules (public / authenticated / admin) in `SecurityConfig`; data rules (booking ownership) in the service layer. Accessing someone else's booking returns `404`, not `403`, so ids can't be probed.
- 🗃️ **Soft delete for resources:** bookings reference resources, so an admin deactivates a resource (`active: false`) instead of deleting it. Regular users only see active resources.
- ⏳ **Stateless access tokens:** a disabled user keeps access until the access token expires (15 minutes); refreshing is blocked immediately.
- 🛡️ **Rate limiting** uses in-memory buckets per instance. If you run several instances, switch Bucket4j to its Redis integration.

<a id="tests"></a>
## 🧪 Tests

```bash
cd booking-api
./gradlew test      # 🐳 needs Docker: Testcontainers starts PostgreSQL
```

| ✅ Covered |
|---|
| 🪪 Registration, login, refresh rotation and reuse detection |
| 👥 Role-based access (401 / 403 / 404 rules) |
| 📅 Booking conflicts, back-to-back bookings, cancellation and ownership |
| 📄 Pagination, sorting and filtering |
| 🛡️ Rate limiting |

<a id="deployment"></a>
## ☁️ Deployment

### 🟣 Render

The app is in a subfolder, so the blueprint must live in the **repository root** and point at it:

1. 📄 Place `render.yaml` in the repository root and add `rootDir: booking-api` to the web service:
```yaml
   services:
     - type: web
       name: booking-api
       runtime: docker
       rootDir: booking-api
       # ...rest unchanged
```
2. 📤 Push the repository to GitHub.
3. 🧩 In Render: **New → Blueprint**, select the repository.
4. 🔑 Provide `ADMIN_EMAIL` and `ADMIN_PASSWORD` when prompted. The database variables are wired automatically and `JWT_SECRET` is generated.
5. 🩺 After the deploy, check `/actuator/health` and `/swagger-ui.html`.

> ⚠️ **Free-tier notes:** free web services spin down after a period of inactivity (the first request afterwards is slow),
> and the free database expires after 30 days. Use a paid plan for anything long-lived.

### 🚂 Railway or any Docker host

Use the same `Dockerfile` (set the root directory to `booking-api`) and the variables from the configuration table.
Point `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME` and `DB_PASSWORD` at your PostgreSQL instance, or set `DB_URL` to a full JDBC URL.

<a id="troubleshooting"></a>
## 🩺 Troubleshooting

| 😕 Symptom | 💡 Cause and fix |
|---|---|
| Only the `postgres` container exists | Run `docker compose up --build -d` from the `booking-api` folder; check `docker compose ps -a` |
| `no configuration file provided` | You're in the repository root: `cd booking-api` first |
| App container exits right away | `docker compose logs app`; usually a missing variable in `.env` |
| Database authentication failed after editing `.env` | The volume keeps the old credentials: `docker compose down -v` |
| `Address already in use` on 8080 | The container and `bootRun` can't run together: `docker compose stop app` |
| `429 Too Many Requests` | Rate limit reached; wait for the `Retry-After` seconds |
| App refuses to start in `prod` | A required variable is missing (see the configuration table) |
| 404 on the API doc link | The path is case-sensitive: it must be `docs/API.md` |

---

<div align="center">

Made with ☕ and 🍃 · See **[docs/API.md](docs/API.md)** for every endpoint 📖

</div>
