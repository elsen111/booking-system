# API Reference

Base URL: `http://localhost:8080` · All endpoints are under `/api/v1` · Interactive docs: `/swagger-ui.html`

## Conventions

### Authentication

Log in to get an access token and send it on every protected request:

```
Authorization: Bearer <accessToken>
```

| Token | Lifetime | Use |
|---|---|---|
| Access token (JWT) | 15 minutes | Sent with every request |
| Refresh token (opaque) | 7 days | Exchanged for a new token pair at `/auth/refresh` |

Refresh tokens are **single use**: every refresh returns a new refresh token and revokes the old one.
Sending an already-used refresh token is treated as theft and revokes **all** of that user's refresh tokens.

### Roles

| Capability | Public | USER | ADMIN |
|---|:-:|:-:|:-:|
| Register, log in, refresh, log out | ✅ | ✅ | ✅ |
| List and read resources | | ✅ (active only) | ✅ (all) |
| Create and update resources | | | ✅ |
| Create a booking | | ✅ | ✅ |
| List bookings | | own | all |
| Read or cancel a booking | | own | any |

### Success response

Every successful response has this shape. `status` always equals the HTTP status code.

```json
{
  "success": true,
  "status": 200,
  "message": "Optional message",
  "data": {},
  "timestamp": "2026-10-11T10:15:30.123Z"
}
```

`message` is omitted when empty; `data` is omitted when there is nothing to return.

### Paginated response

List endpoints return a page inside `data`:

```json
{
  "success": true,
  "status": 200,
  "data": {
    "content": [],
    "page": 0,
    "size": 20,
    "totalElements": 42,
    "totalPages": 3,
    "last": false
  },
  "timestamp": "2026-10-11T10:15:30.123Z"
}
```

| Query parameter | Default | Description |
|---|---|---|
| `page` | `0` | Zero-based page index |
| `size` | `20` | Page size, maximum `50` |
| `sort` | endpoint specific | `property,direction`, e.g. `startTime,desc`. Repeatable. Only whitelisted properties are accepted |

### Error response

```json
{
  "success": false,
  "timestamp": "2026-10-11T10:15:30.123Z",
  "status": 409,
  "error": "Conflict",
  "message": "The resource is already booked for the selected time",
  "path": "/api/v1/bookings"
}
```

Validation errors (`400`) add a `fieldErrors` object:

```json
{
  "success": false,
  "timestamp": "2026-10-11T10:15:30.123Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/auth/register",
  "fieldErrors": {
    "email": "must be a well-formed email address",
    "password": "size must be between 8 and 72"
  }
}
```

| Status | Meaning |
|---|---|
| 400 | Validation failed, malformed JSON or parameter, invalid time range, unsupported sort property |
| 401 | Missing, invalid or expired access token; wrong credentials; invalid refresh token |
| 403 | Authenticated but not allowed (e.g. a `USER` calling an admin endpoint) |
| 404 | Not found, or not visible to the caller (other users' bookings, inactive resources for non-admins) |
| 405 | HTTP method not supported on this path |
| 409 | Conflict: email already registered, resource name taken, time slot taken, booking already cancelled |
| 429 | Rate limit exceeded |
| 500 | Unexpected server error |

### Rate limiting

Limits apply per client IP and per minute. `/actuator/**` is not limited.

| Scope | Limit |
|---|---|
| `/api/v1/auth/**` | 10 requests |
| Everything else | 60 requests |

Allowed requests carry `X-RateLimit-Remaining`. Rejected requests get status `429` and a `Retry-After` header (seconds).

### Dates and IDs

- All ids are UUIDs.
- All timestamps are ISO-8601 instants in UTC, e.g. `2026-12-01T10:00:00Z`.

---

## Auth

### POST `/auth/register`

Creates a `USER` account and logs it in. Public.

| Field | Rules |
|---|---|
| `email` | required, valid email, max 255 (stored lowercase) |
| `password` | required, 8 to 72 characters |
| `fullName` | required, max 150 |

```json
{ "email": "jane@example.com", "password": "Passw0rd!123", "fullName": "Jane Doe" }
```

**201 Created**
```json
{
  "success": true,
  "status": 201,
  "message": "Registration successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "kJ3x...vQ",
    "tokenType": "Bearer",
    "expiresIn": 900
  },
  "timestamp": "2026-10-11T10:15:30.123Z"
}
```
`expiresIn` is the access token lifetime in seconds. Errors: `400` validation, `409` email already registered.

### POST `/auth/login`

Public.

```json
{ "email": "jane@example.com", "password": "Passw0rd!123" }
```

**200 OK**: same `data` as register. Errors: `400` validation, `401` invalid email or password (also returned for disabled accounts).

### POST `/auth/refresh`

Exchanges a refresh token for a new access + refresh token pair. The old refresh token is revoked. Public.

```json
{ "refreshToken": "kJ3x...vQ" }
```

**200 OK**: same `data` as login. Error: `401` if the token is unknown, expired, revoked or reused.

### POST `/auth/logout`

Revokes the given refresh token. Idempotent: unknown tokens are ignored. Public.

```json
{ "refreshToken": "kJ3x...vQ" }
```

**200 OK**
```json
{ "success": true, "status": 200, "message": "Logged out", "timestamp": "2026-10-11T10:15:30.123Z" }
```
The access token stays valid until it expires (max 15 minutes).

---

## Resources

Bookable things such as rooms or equipment.

### Resource object

```json
{
  "id": "0f8b5c1e-3b8e-4c7a-9a55-1d2f6c7b9e10",
  "name": "Meeting Room A",
  "description": "Second floor, projector included",
  "capacity": 8,
  "active": true,
  "createdAt": "2026-10-11T10:00:00Z",
  "updatedAt": "2026-10-11T10:00:00Z"
}
```

### GET `/resources`

Paginated list. Any authenticated user. **Non-admins only see active resources**, whatever `active` they send.

| Query parameter | Description |
|---|---|
| `name` | Case-insensitive "contains" match |
| `minCapacity` | Minimum capacity |
| `active` | `true` / `false` (effective for admins) |
| `page`, `size`, `sort` | Pagination. Sortable: `name`, `capacity`, `createdAt`. Default: `name` ascending |

```
GET /api/v1/resources?name=room&minCapacity=6&sort=capacity,desc&page=0&size=10
```

**200 OK**: paginated response, `content` is an array of resource objects.

### GET `/resources/{id}`

Any authenticated user. **200 OK** with a resource object. `404` if it doesn't exist, or is inactive and the caller is not an admin.

### POST `/resources`

**ADMIN only.**

| Field | Rules |
|---|---|
| `name` | required, max 150, unique (case-insensitive) |
| `description` | optional, max 1000 |
| `capacity` | required, 1 to 1000 |

```json
{ "name": "Meeting Room A", "description": "Second floor, projector included", "capacity": 8 }
```

**201 Created** with the resource object (`active` is `true`). Errors: `400` validation, `401`, `403`, `409` name already exists.

### PUT `/resources/{id}`

**ADMIN only.** Replaces all fields. This is also how you deactivate a resource (`active: false`); resources are never hard-deleted because bookings reference them.

| Field | Rules |
|---|---|
| `name` | required, max 150, unique (case-insensitive) |
| `description` | optional, max 1000 (omit to clear) |
| `capacity` | required, 1 to 1000 |
| `active` | required |

```json
{ "name": "Meeting Room A", "description": "Renovated", "capacity": 10, "active": true }
```

**200 OK** with the updated resource. Errors: `400`, `401`, `403`, `404`, `409` name taken by another resource.

Existing bookings of a deactivated resource are kept; new bookings for it are rejected with `404`.

---

## Bookings

### Booking object

```json
{
  "id": "6f1c2d3e-8a9b-4c5d-9e0f-1a2b3c4d5e6f",
  "resourceId": "0f8b5c1e-3b8e-4c7a-9a55-1d2f6c7b9e10",
  "resourceName": "Meeting Room A",
  "userId": "b7e4a1c2-5d6f-4a8b-9c0d-2e3f4a5b6c7d",
  "userEmail": "jane@example.com",
  "startTime": "2026-12-01T10:00:00Z",
  "endTime": "2026-12-01T11:00:00Z",
  "status": "CONFIRMED",
  "createdAt": "2026-10-11T10:15:30Z"
}
```

`status` is `CONFIRMED` or `CANCELLED`.

### POST `/bookings`

Books a resource for the calling user. Any authenticated user.

| Field | Rules |
|---|---|
| `resourceId` | required, an active resource |
| `startTime` | required, in the future |
| `endTime` | required, after `startTime` |

```json
{
  "resourceId": "0f8b5c1e-3b8e-4c7a-9a55-1d2f6c7b9e10",
  "startTime": "2026-12-01T10:00:00Z",
  "endTime": "2026-12-01T11:00:00Z"
}
```

**201 Created**
```json
{
  "success": true,
  "status": 201,
  "message": "Booking confirmed",
  "data": { "id": "6f1c2d3e-...", "status": "CONFIRMED", "...": "..." },
  "timestamp": "2026-10-11T10:15:30.123Z"
}
```

| Error | Cause |
|---|---|
| 400 | Validation failed, `endTime` not after `startTime`, `startTime` in the past |
| 404 | Resource doesn't exist or is inactive |
| 409 | The resource is already booked for an overlapping time |

Time ranges are half-open (`[start, end)`): a booking 10:00 to 11:00 and another 11:00 to 12:00 don't conflict.
Cancelled bookings don't block a slot. Conflicts are enforced by a database constraint, so two simultaneous requests can never both succeed.

### GET `/bookings`

Paginated list. **Users see only their own bookings; admins see all.**

| Query parameter | Description |
|---|---|
| `resourceId` | Only bookings of this resource |
| `status` | `CONFIRMED` or `CANCELLED` |
| `from` | Bookings that end after this instant (ISO-8601) |
| `to` | Bookings that start before this instant (ISO-8601) |
| `userId` | Only bookings of this user. **Admins only**; ignored for regular users |
| `page`, `size`, `sort` | Pagination. Sortable: `startTime`, `endTime`, `createdAt`, `status`. Default: `startTime` descending |

`from` and `to` select bookings **overlapping** that window.

```
GET /api/v1/bookings?status=CONFIRMED&from=2026-12-01T00:00:00Z&to=2027-01-01T00:00:00Z&sort=startTime,asc&page=0&size=20
```

**200 OK**: paginated response, `content` is an array of booking objects.
Errors: `400` for an unsupported sort property or malformed parameter (e.g. an invalid `status` or date).

### GET `/bookings/{id}`

Owner or admin. **200 OK** with a booking object.
`404` if it doesn't exist **or belongs to someone else** (so ids can't be probed).

### PATCH `/bookings/{id}/cancel`

Owner or admin. No request body. **200 OK** with the booking, now `CANCELLED`.

| Error | Cause |
|---|---|
| 400 | The booking has already started |
| 404 | Not found, or belongs to someone else |
| 409 | Already cancelled |

---

## Operations

| Endpoint | Auth | Description |
|---|---|---|
| `GET /actuator/health` | public | Health status (also `/actuator/health/liveness` and `/readiness`) |
| `GET /actuator/info` | public | Application info |
| `GET /swagger-ui.html` | public | Swagger UI |
| `GET /v3/api-docs` | public | OpenAPI document |

---

## Example session

```bash
BASE=http://localhost:8080/api/v1

# 1. Admin logs in and creates a resource
ADMIN=$(curl -s -X POST $BASE/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"admin@example.com","password":"<ADMIN_PASSWORD>"}' | jq -r .data.accessToken)

RESOURCE=$(curl -s -X POST $BASE/resources -H "Authorization: Bearer $ADMIN" \
  -H 'Content-Type: application/json' \
  -d '{"name":"Meeting Room A","capacity":8}' | jq -r .data.id)

# 2. A user registers and books it
USER_TOKEN=$(curl -s -X POST $BASE/auth/register -H 'Content-Type: application/json' \
  -d '{"email":"jane@example.com","password":"Passw0rd!123","fullName":"Jane Doe"}' | jq -r .data.accessToken)

BOOKING=$(curl -s -X POST $BASE/bookings -H "Authorization: Bearer $USER_TOKEN" \
  -H 'Content-Type: application/json' \
  -d "{\"resourceId\":\"$RESOURCE\",\"startTime\":\"2026-12-01T10:00:00Z\",\"endTime\":\"2026-12-01T11:00:00Z\"}" \
  | jq -r .data.id)

# 3. The user lists and cancels it
curl -s "$BASE/bookings?status=CONFIRMED&sort=startTime,asc" -H "Authorization: Bearer $USER_TOKEN"
curl -s -X PATCH $BASE/bookings/$BOOKING/cancel -H "Authorization: Bearer $USER_TOKEN"

# 4. Refresh the session (the old refresh token stops working)
curl -s -X POST $BASE/auth/refresh -H 'Content-Type: application/json' \
  -d '{"refreshToken":"<refreshToken from login>"}'
```
