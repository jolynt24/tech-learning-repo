# Wellness API

A REST API built with Quarkus for tracking daily wellness. Handles user registration, login, JWT auth, and daily entries covering sleep, water, workouts, reading, hobbies, mood, and meals — plus goal tracking, streak calculation, and analytics. Backed by PostgreSQL with Redis caching.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Framework | Quarkus 3.x |
| Language | Java 17 |
| ORM | Hibernate ORM + Panache (active record) |
| Database | PostgreSQL |
| Cache | Redis |
| Auth | SmallRye JWT (RS256) |
| Security | Quarkus Security JPA |
| Migrations | Flyway |
| Build | Gradle |
| Boilerplate | Lombok |
| Observability | Micrometer + Prometheus, SmallRye Health |

---

## Prerequisites

- Java 17+
- Docker and Docker Compose

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

This starts:
- PostgreSQL on port `5432` (`wellness_db` / `wellness_user` / `wellness_pass`)
- Redis on port `6379`

Flyway runs automatically on startup and creates all tables, indexes, and triggers via `src/main/resources/db/V1__init.sql`.

### 3. Run the app

```bash
./gradlew quarkusDev
```

The API is available at `http://localhost:8080`.
Swagger UI: `http://localhost:8080/q/swagger-ui`
Dev UI: `http://localhost:8080/q/dev`
Health check: `http://localhost:8080/q/health`
Metrics (Prometheus): `http://localhost:8080/q/metrics`

---

## Authentication

The API uses JWT Bearer tokens. All endpoints under `/api/entries`, `/api/goals`, and `/api/analytics` require:

```
Authorization: Bearer <access_token>
```

Tokens are RS256-signed. Access tokens expire in 1 hour. Refresh tokens expire in 7 days and carry a `"type": "refresh"` claim — they only work on `POST /api/auth/refresh`.

---

## API Reference

### Auth — `/api/auth`

#### Register

```
POST /api/auth/register
```

Creates a new user account. Username must be unique (min 3 chars). Email must be unique and valid format. Password must be at least 8 characters.

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

#### Login

```
POST /api/auth/login
```

Authenticates with **email + password** and returns an access token (1h) and refresh token (7d).

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "alice@example.com", "password": "secret123"}'
```

**Response `200`**
```json
{
  "token": "<access_token>",
  "refreshToken": "<refresh_token>",
  "user": "alice",
  "expiresIn": "2026-03-15T13:00:00Z"
}
```

---

#### Refresh Token

```
POST /api/auth/refresh
```

Issues a new token pair. Must use the **refresh token** (not the access token).

```bash
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Authorization: Bearer <refresh_token>"
```

**Response `200`** — same shape as login.

---

#### Get Current User

```
GET /api/auth/me
```

```bash
curl http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer <access_token>"
```

**Response `200`**
```json
{ "id": 1, "username": "alice", "email": "alice@example.com" }
```

---

#### Update Profile

```
PUT /api/auth/profile
```

Updates email or password. Provide one field at a time.

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

All endpoints require auth. Dates use `YYYY-MM-DD` format. Each user can have one entry per date.

#### Create Entry

```
POST /api/entries
```

All fields except `entryDate` are optional. `entryDate` defaults to today.

```bash
curl -X POST http://localhost:8080/api/entries \
  -H "Authorization: Bearer <access_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "entryDate": "2026-03-15",
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

```
GET /api/entries/{date}
```

```bash
curl http://localhost:8080/api/entries/2026-03-15 \
  -H "Authorization: Bearer <access_token>"
```

**Response `200`** — entry for that date. `404` if not found.

---

#### Update Entry

```
PUT /api/entries/{date}
```

Updates fields on an existing entry. A non-null `meals` list replaces all existing meals.

```bash
curl -X PUT http://localhost:8080/api/entries/2026-03-15 \
  -H "Authorization: Bearer <access_token>" \
  -H "Content-Type: application/json" \
  -d '{ "waterMl": 2500, "moodRating": 5 }'
```

**Response `200`** — updated entry. `404` if not found.

---

#### Delete Entry

```
DELETE /api/entries/{date}
```

```bash
curl -X DELETE http://localhost:8080/api/entries/2026-03-15 \
  -H "Authorization: Bearer <access_token>"
```

**Response `204`**. `404` if not found.

---

#### Get Entries by Date Range

```
GET /api/entries/range?from=YYYY-MM-DD&to=YYYY-MM-DD
```

```bash
curl "http://localhost:8080/api/entries/range?from=2026-03-01&to=2026-03-15" \
  -H "Authorization: Bearer <access_token>"
```

**Response `200`** — array of entries ordered by date ascending.

---

#### Get Latest Entries

```
GET /api/entries/latest?limit=7
```

Returns the most recent N entries. `limit` defaults to `7`.

```bash
curl "http://localhost:8080/api/entries/latest?limit=5" \
  -H "Authorization: Bearer <access_token>"
```

**Response `200`** — array of entries, newest first.

---

### Goals — `/api/goals`

Set targets for wellness metrics. Each goal has a type, target value, frequency, and optional end date.

`goalType` values: `SLEEP`, `WATER`, `WORKOUT`, `READING`, `HOBBY`
`goalFrequency` values: `DAILY`, `WEEKLY`, `MONTHLY`

#### Create Goal

```
POST /api/goals
```

```bash
curl -X POST http://localhost:8080/api/goals \
  -H "Authorization: Bearer <access_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "goalType": "SLEEP",
    "target": 7.5,
    "goalFrequency": "DAILY",
    "startDate": "2026-03-01",
    "endDate": "2026-06-01"
  }'
```

**Response `201`**
```json
{
  "id": 1,
  "goalLabel": "Sleep",
  "goalMetrics": "hours",
  "target": 7.5,
  "goalFrequency": "Daily",
  "frequencyDays": 1,
  "active": true,
  "startDate": "2026-03-01",
  "endDate": "2026-06-01",
  "createdAt": "2026-03-15T09:00:00Z"
}
```

---

#### Get All Goals

```
GET /api/goals
```

```bash
curl http://localhost:8080/api/goals \
  -H "Authorization: Bearer <access_token>"
```

**Response `200`** — array of goals ordered by start date.

---

#### Get Goal by ID

```
GET /api/goals/{id}
```

```bash
curl http://localhost:8080/api/goals/1 \
  -H "Authorization: Bearer <access_token>"
```

**Response `200`** — goal. `404` if not found or belongs to another user.

---

#### Update Goal

```
PATCH /api/goals/{id}
```

All fields optional — only provided fields are updated.

```bash
curl -X PATCH http://localhost:8080/api/goals/1 \
  -H "Authorization: Bearer <access_token>" \
  -H "Content-Type: application/json" \
  -d '{ "target": 8.0, "active": false }'
```

**Response `200`** — updated goal. `404` if not found.

---

#### Delete Goal

```
DELETE /api/goals/{id}
```

```bash
curl -X DELETE http://localhost:8080/api/goals/1 \
  -H "Authorization: Bearer <access_token>"
```

**Response `204`**. `404` if not found.

---

### Analytics — `/api/analytics`

#### Current Streaks

```
GET /api/analytics/streaks
```

Returns the current streak (consecutive days goal was met) for each active goal type.

```bash
curl http://localhost:8080/api/analytics/streaks \
  -H "Authorization: Bearer <access_token>"
```

**Response `200`**
```json
[
  {
    "goalType": "SLEEP",
    "currentStreak": 5,
    "longestStreak": 5,
    "activeToday": true,
    "calculatedAt": "2026-03-15T09:00:00Z"
  }
]
```

---

#### Trend Analysis

```
GET /api/analytics/trends?metric=SLEEP&period=30
```

Returns average, min, max, trend direction, and daily data points for a metric over the past N days.

`metric` values: `SLEEP`, `WATER`, `WORKOUT`, `READING`, `HOBBY`
`period`: number of days to look back (e.g. `7`, `30`, `90`)

```bash
curl "http://localhost:8080/api/analytics/trends?metric=SLEEP&period=30" \
  -H "Authorization: Bearer <access_token>"
```

**Response `200`**
```json
{
  "metric": "SLEEP",
  "period": 30,
  "average": 7.2,
  "min": 5.5,
  "max": 9.0,
  "trendDirection": "INCREASING",
  "dataPoints": [
    { "time": "2026-02-14", "value": 6.5 },
    { "time": "2026-02-15", "value": 7.0 }
  ]
}
```

`trendDirection` values: `INCREASING`, `DECREASING`, `STABLE`

---

#### Period Summary

```
GET /api/analytics/summary?period=WEEK
```

Returns aggregated stats for the past week or month, including streak information.

`period` values: `WEEK` (7 days), `MONTH` (30 days)

```bash
curl "http://localhost:8080/api/analytics/summary?period=WEEK" \
  -H "Authorization: Bearer <access_token>"
```

**Response `200`**
```json
{
  "period": "WEEK",
  "startDate": "2026-03-08",
  "endDate": "2026-03-15",
  "totalEntries": 6,
  "avgSleepHours": 7.3,
  "avgWaterMl": 1950,
  "workoutDays": 4,
  "totalWorkoutDuration": 120,
  "totalReadingMinutes": 140,
  "totalHobbyMinutes": 200,
  "avgMoodRating": 4.2,
  "streaks": [...]
}
```

---

## Scheduled Jobs

Three background jobs run automatically:

| Job | Schedule | What it does |
|-----|----------|-------------|
| `calculateDailyStreaks` | 1 AM daily | Checks yesterday's entries against each user's active goals; invalidates streak caches |
| `cleanupExpiredGoals` | 1 AM daily | Deactivates goals where `endDate < today` |
| `warmupPopularCaches` | Every 6 hours | Pre-calculates WEEK and MONTH summaries for all users with active goals |

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
| 1003 | 401 | Invalid credentials / user not found |
| 1004 | 401 | Forbidden access |
| 2001 | 500 | Database operation failed |
| 3001 | 400 | Bad request (e.g. no fields provided for profile update) |
| 3002 | 400 | Validation error |
| 4001 | 409 | Duplicate entry for that date |
| 4002 | 404 | Entry not found |
| 4003 | 404 | Goal not found |
| 4004 | 500 | JSON processing issue |

---

## Running Tests

```bash
./gradlew test
```

Tests use `@QuarkusTest` and run against the real database (PostgreSQL must be running). Each test class creates its own users and data — no shared state.

Test files:

| File | Coverage |
|------|---------|
| `AuthResourceTest` | Register, login, refresh, me, profile update |
| `EntryResourceTest` | Create, get, update, delete, range, latest |
| `GoalResourceTest` | Create, get, update, delete, goal progress stub |
| `AnalyticsResourceTest` | Streaks, trends, summary — all edge cases |
| `CrossUserIsolationTest` | Verifies users cannot access each other's data |

---

## Build

```bash
# Run in dev mode (live reload)
./gradlew quarkusDev

# Standard JAR
./gradlew build
java -jar build/quarkus-app/quarkus-run.jar

# Über-JAR (single fat jar)
./gradlew build -Dquarkus.package.jar.type=uber-jar
java -jar build/*-runner.jar

# Native executable (requires GraalVM)
./gradlew build -Dquarkus.native.enabled=true

# Native build via Docker (no GraalVM needed locally)
./gradlew build -Dquarkus.native.enabled=true -Dquarkus.native.container-build=true
```
