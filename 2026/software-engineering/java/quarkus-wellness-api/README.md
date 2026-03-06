# Wellness API

A REST API built with Quarkus for tracking daily wellness. Handles user registration, login, JWT auth, and daily entries covering sleep, water, workouts, reading, hobbies, mood, and meals — all backed by PostgreSQL.

## Tech Stack

- **Quarkus** — framework
- **Hibernate ORM + Panache** — database access
- **PostgreSQL** — primary database
- **Redis** — available for caching
- **SmallRye JWT** — token signing and verification (RSA)
- **Quarkus Security JPA** — role-based access control
- **Gradle** — build tool
- **Lombok** — boilerplate reduction

---

## Prerequisites

- Java 21+
- Docker (for PostgreSQL and Redis)

---

## Setup

### 1. Generate RSA key pair

The app uses RSA keys to sign and verify JWTs. Generate them once before running:

```bash
openssl genrsa -out src/main/resources/privateKey.pem 2048
openssl rsa -pubout -in src/main/resources/privateKey.pem -out src/main/resources/publicKey.pem
```

### 2. Start dependencies

```bash
docker-compose up -d
```

This starts PostgreSQL on port `5432` and Redis on port `6379`.

### 3. Run the app

```bash
./gradlew quarkusDev
```

The API is available at `http://localhost:8080`. Quarkus Dev UI is at `http://localhost:8080/q/dev/`.

---

## API Reference

All entry endpoints require a valid access token (`Authorization: Bearer <token>`) and the `user` role.

### Auth — `/api/auth`

### Register

```bash
POST /api/auth/register
```

Creates a new user account. Username and email must be unique (case-insensitive). Password must be at least 8 characters.

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username": "alice", "email": "alice@example.com", "password": "secret123"}'
```

**Response `201`**
```json
{ "id": 1, "username": "alice", "email": "alice@example.com" }
```

---

### Login

```bash
POST /api/auth/login
```

Authenticates a user and returns an access token (1 hour) and refresh token (7 days).

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "alice", "password": "secret123"}'
```

**Response `200`**
```json
{
  "token": "<access_token>",
  "refreshToken": "<refresh_token>",
  "user": "alice",
  "expiresIn": "2026-03-04T13:00:00Z"
}
```

---

### Get Current User

```bash
GET /api/auth/me
```

Returns the authenticated user's profile. Requires a valid access token.

```bash
curl http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer <access_token>"
```

**Response `200`**
```json
{ "id": 1, "username": "alice", "email": "alice@example.com" }
```

---

### Refresh Token

```bash
POST /api/auth/refresh
```

Issues a new access + refresh token pair. Must be called with a **refresh token** (not an access token).

```bash
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Authorization: Bearer <refresh_token>"
```

**Response `200`** — same shape as login response.

---

### Update Profile

```bash
PUT /api/auth/profile
```

Updates the authenticated user's email or password. Provide one field at a time.

```bash
# Update email
curl -X PUT http://localhost:8080/api/auth/profile \
  -H "Authorization: Bearer <access_token>" \
  -H "Content-Type: application/json" \
  -d '{"email": "newalice@example.com"}'

# Update password
curl -X PUT http://localhost:8080/api/auth/profile \
  -H "Authorization: Bearer <access_token>" \
  -H "Content-Type: application/json" \
  -d '{"password": "newsecret456"}'
```

**Response `200`** — updated user profile.

---

### Entries — `/api/entries`

All endpoints require auth (`user` role). Dates use `YYYY-MM-DD` format.

#### Create Entry

```bash
POST /api/entries
```

Creates a daily wellness entry. `entryDate` defaults to today. All fields except `entryDate` are optional. Meals is a list — each meal requires `mealType` and `description`.

```bash
curl -X POST http://localhost:8080/api/entries \
  -H "Authorization: Bearer <access_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "entryDate": "2026-03-06",
    "sleepHours": 7.5,
    "sleepQuality": 4,
    "waterMl": 2000,
    "workoutDone": true,
    "workoutType": "Running",
    "workoutDurationMin": 30,
    "readingMinutes": 20,
    "readingPages": 15,
    "readingBook": "Clean Code",
    "hobbyActivity": "Guitar",
    "hobbyDurationMin": 45,
    "moodRating": 4,
    "notes": "Good day overall",
    "meals": [
      { "mealType": "BREAKFAST", "description": "Oats and fruit", "calories": 400 },
      { "mealType": "LUNCH", "description": "Chicken salad", "calories": 550 }
    ]
  }'
```

`mealType` values: `BREAKFAST`, `LUNCH`, `DINNER`, `SNACK`

**Response `201`** — full entry with meals.

---

#### Get Entry by Date

```bash
GET /api/entries/{date}
```

```bash
curl http://localhost:8080/api/entries/2026-03-06 \
  -H "Authorization: Bearer <access_token>"
```

**Response `200`** — entry for that date. `404` if not found.

---

#### Update Entry

```bash
PUT /api/entries/{date}
```

Replaces fields on an existing entry. Meals list replaces existing meals.

```bash
curl -X PUT http://localhost:8080/api/entries/2026-03-06 \
  -H "Authorization: Bearer <access_token>" \
  -H "Content-Type: application/json" \
  -d '{ "waterMl": 2500, "moodRating": 5 }'
```

**Response `200`** — updated entry. `404` if not found.

---

#### Delete Entry

```bash
DELETE /api/entries/{date}
```

```bash
curl -X DELETE http://localhost:8080/api/entries/2026-03-06 \
  -H "Authorization: Bearer <access_token>"
```

**Response `204`** — no content. `404` if not found.

---

#### Get Entries by Date Range

```bash
GET /api/entries/range?from=YYYY-MM-DD&to=YYYY-MM-DD
```

```bash
curl "http://localhost:8080/api/entries/range?from=2026-03-01&to=2026-03-06" \
  -H "Authorization: Bearer <access_token>"
```

**Response `200`** — array of entries.

---

#### Get Latest Entries

```bash
GET /api/entries/latest?limit=7
```

Returns the most recent N entries. `limit` defaults to `7`.

```bash
curl "http://localhost:8080/api/entries/latest?limit=5" \
  -H "Authorization: Bearer <access_token>"
```

**Response `200`** — array of entries, newest first.

---

## Error Responses

All errors return a consistent JSON structure:

```json
{ "code": 1003, "message": "Invalid Credentials" }
```

| Code | HTTP | Meaning |
|------|------|---------|
| 1001 | 409 | Username or email already exists |
| 1002 | 401 | Login error |
| 1003 | 401 | Invalid credentials |
| 2001 | 500 | Database error |
| 3001 | 400 | Bad request (e.g. no fields provided for profile update) |
| 4001 | 409 | Duplicate entry for that date |
| 4002 | 404 | Entry not found |

---

## Build

```bash
# Standard JAR
./gradlew build
java -jar build/quarkus-app/quarkus-run.jar

# Über-JAR
./gradlew build -Dquarkus.package.jar.type=uber-jar
java -jar build/*-runner.jar

# Native executable (requires GraalVM)
./gradlew build -Dquarkus.native.enabled=true

# Native build without GraalVM (uses Docker)
./gradlew build -Dquarkus.native.enabled=true -Dquarkus.native.container-build=true
```
