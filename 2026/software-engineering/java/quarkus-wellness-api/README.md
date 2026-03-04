# Wellness API

A REST authentication API built with Quarkus. Handles user registration, login, JWT token issuance and refresh, and profile management backed by PostgreSQL.

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

Base path: `/api/auth`

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
