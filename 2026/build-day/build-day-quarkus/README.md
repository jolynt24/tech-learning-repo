# Build Day: Quarkus — Step-by-Step Workshop Guide

Welcome to **Build Day: Quarkus**! This guide walks you through building REST APIs with Quarkus — from your first "Hello World" to a production-style application with a database, authentication, and analytics. No prior Quarkus experience needed.

---

## Table of Contents

1. [What is Quarkus?](#1-what-is-quarkus)
2. [Prerequisites & Setup](#2-prerequisites--setup)
3. [Part 1 — Hello World REST API](#3-part-1--hello-world-rest-api)
4. [Part 2 — Connecting to a Database](#4-part-2--connecting-to-a-database)
5. [Part 3 — Querying Data with Panache](#5-part-3--querying-data-with-panache)
6. [Part 4 — Building a Real-World API (Wellness Tracker)](#6-part-4--building-a-real-world-api-wellness-tracker)
7. [Part 5 — JWT Authentication](#7-part-5--jwt-authentication)
8. [Part 6 — Goals & Analytics Endpoints](#8-part-6--goals--analytics-endpoints)
9. [Part 7 — Testing](#9-part-7--testing)
10. [Part 8 — Database Migrations with Flyway](#10-part-8--database-migrations-with-flyway)
11. [Stretch Goals](#11-stretch-goals)
12. [Quick Reference](#12-quick-reference)

---

## 1. What is Quarkus?

Quarkus is a **Java framework built for the cloud**. It compiles fast, starts in milliseconds, and uses familiar Java standards like Jakarta EE and MicroProfile — so if you know Spring, most concepts translate directly.

**Key things you'll use today:**

| Feature | What it does |
|---|---|
| `quarkus-rest` | Build REST endpoints |
| `hibernate-orm-panache` | Simplified database access |
| `quarkus-security-jpa` | User auth backed by a DB |
| `smallrye-jwt` | Issue and verify JWT tokens |
| `quarkus-flyway` | Database migrations |
| Dev Mode | Live reload on save, like nodemon |

---

## 2. Prerequisites & Setup

### Required tools

- **Java 21+** — check with `java -version`
- **Gradle 8+** — bundled via the `./gradlew` wrapper (no install needed)
- **Docker** — for running PostgreSQL locally
- An IDE — IntelliJ IDEA recommended (free Community edition works fine)

### Clone the workshop repo

```bash
git clone <repo-url>
cd tech-learning-repo/2026/software-engineering/java
```

There are two projects inside:

```
quarkus-freecodecamp-tutorial/   ← Parts 1–3 (Films API, beginner-friendly)
quarkus-wellness-api/            ← Parts 4–8 (Wellness Tracker, advanced)
```

### Start the database (Part 1–3)

The Films API uses MySQL. Start it with Docker:

```bash
docker run --name sakila-mysql \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=sakila \
  -p 3306:3306 \
  -d mysql:8
```

> **Tip:** The Sakila sample database is a classic MySQL demo dataset — it has films, actors, and rentals. You can import the schema from https://dev.mysql.com/doc/index-other.html

### Start the database (Parts 4–8)

The Wellness API uses PostgreSQL:

```bash
docker run --name wellness-postgres \
  -e POSTGRES_USER=wellness_user \
  -e POSTGRES_PASSWORD=wellness_pass \
  -e POSTGRES_DB=wellness_db \
  -p 5432:5432 \
  -d postgres:16
```

---

## 3. Part 1 — Hello World REST API

**Goal:** Run your first Quarkus app and hit an endpoint.

### Step 1 — Start dev mode

```bash
cd quarkus-freecodecamp-tutorial
./gradlew quarkusDev
```

Watch the terminal. Quarkus starts in under 2 seconds. Visit http://localhost:8080/q/dev/ — this is the **Dev UI**, a live dashboard showing your endpoints, config, and extensions.

### Step 2 — Your first endpoint

Open `src/main/java/org/fractalschema/app/FilmResource.java`. It already has:

```java
@Path("/")
public class FilmResource {

    @GET
    @Path("/helloworld")
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "Hello World!";
    }
}
```

Test it:

```bash
curl http://localhost:8080/helloworld
# Hello World!
```

**Key concepts:**
- `@Path` — maps a class or method to a URL
- `@GET` — the HTTP verb
- `@Produces` — declares the response content type

### Step 3 — Add your own endpoint

Add this method inside `FilmResource`:

```java
@GET
@Path("/greet/{name}")
@Produces(MediaType.TEXT_PLAIN)
public String greet(@PathParam("name") String name) {
    return "Hello, " + name + "!";
}
```

Save the file. Quarkus **hot-reloads instantly** — no restart needed.

```bash
curl http://localhost:8080/greet/Jolyn
# Hello, Jolyn!
```

> **Notice:** You didn't restart the server. That's Quarkus dev mode — it detects changes and recompiles automatically.

---

## 4. Part 2 — Connecting to a Database

**Goal:** Wire up Hibernate ORM and query a real database.

### Step 1 — Check application.properties

Open `src/main/resources/application.properties`:

```properties
quarkus.datasource.db-kind=mysql
quarkus.datasource.username=root
quarkus.datasource.password=root
quarkus.datasource.jdbc.url=jdbc:mysql://localhost:3306/sakila
```

This is the **only place** you configure your database. No XML, no `@Bean` setup.

### Step 2 — Look at the Film entity

Open `src/main/java/org/fractalschema/app/model/Film.java`. This is a standard JPA entity — a Java class that maps to a database table.

```java
@Entity
@Table(name = "film")
public class Film {

    @Id
    @Column(name = "film_id")
    private Short id;

    @Column(name = "title")
    private String title;

    @Column(name = "length")
    private Short length;

    // ... getters, constructors
}
```

**Key concepts:**
- `@Entity` — tells Hibernate this class is a table
- `@Table(name = "film")` — maps to the `film` table
- `@Id` — the primary key column
- `@Column` — maps a field to a column

### Step 3 — Try the film endpoint

```bash
curl http://localhost:8080/film/1
# ACADEMY DINOSAUR
```

The `FilmRepository` uses a plain `EntityManager` to query by ID:

```java
public Optional<Film> getFilm(Short filmId) {
    return Optional.ofNullable(entityManager.find(Film.class, filmId));
}
```

---

## 5. Part 3 — Querying Data with Panache

**Goal:** Use JPQL and Panache for more powerful queries.

### Step 1 — Paged results

Try the paged endpoint:

```bash
curl http://localhost:8080/pagedFilms/0/60
```

This returns the first 20 films longer than 60 minutes. Look at the repository:

```java
public List<Film> paged(long page, Short minLength) {
    return entityManager.createQuery(
        "SELECT new Film(f.id, f.title, f.length) FROM Film f " +
        "WHERE f.length > :minLength ORDER BY f.length", Film.class)
        .setParameter("minLength", minLength)
        .setFirstResult((int) (page * PAGE_SIZE))
        .setMaxResults(PAGE_SIZE)
        .getResultList();
}
```

**Key concepts:**
- `setFirstResult` / `setMaxResults` — pagination
- Named constructors in JPQL (`SELECT new Film(...)`) — fetch only what you need

### Step 2 — Actors with JOIN FETCH

```bash
curl http://localhost:8080/actors/A/90
```

This returns all films starting with "A", longer than 90 min, with their full cast:

```java
"SELECT DISTINCT f FROM Film f JOIN FETCH f.actorList " +
"WHERE f.title LIKE :prefix AND f.length > :minLength ORDER BY f.length DESC"
```

> `JOIN FETCH` tells Hibernate to load the related `actorList` in the **same query**, avoiding the N+1 problem.

### Step 3 — Transactional updates

```bash
curl http://localhost:8080/update/120/4.99
```

This updates rental rates for all films longer than 120 min. The `@Transactional` annotation on the repository method ensures the update is committed atomically:

```java
@Transactional
public int updateRentalRate(Short minLength, BigDecimal rentalRate) {
    return entityManager.createQuery(
        "UPDATE Film f SET f.rentalRate = :rentalRate WHERE f.length > :minLength")
        ...
        .executeUpdate();
}
```

---

## 6. Part 4 — Building a Real-World API (Wellness Tracker)

**Goal:** Understand the structure of the Wellness API and start the app.

### Step 1 — Start dev mode for the Wellness API

```bash
cd ../quarkus-wellness-api
./gradlew quarkusDev
```

### Step 2 — Explore the project structure

```
src/main/java/org/fractalschema/
├── auth/           ← User entity, AuthResource, AuthService
├── entries/        ← DailyEntry entity, EntryResource, EntryService
├── goals/          ← Goal entity, GoalResource, GoalService
├── analytics/      ← AnalyticsResource, AnalyticsService
├── dto/
│   ├── request/    ← Input objects (RegisterRequest, CreateEntryRequest...)
│   └── response/   ← Output objects (UserResponse, EntryResponse...)
├── enums/          ← GoalType, Period, MealType...
└── exceptions/     ← CustomExceptions, CustomExceptionMapper
```

This is a **layered architecture**:
- **Resource** — handles HTTP, delegates to Service
- **Service** — business logic
- **Entity** — database model
- **DTO** — data transfer objects (what the API accepts/returns)

### Step 3 — The DailyEntry entity

The `DailyEntry` entity (`entries/DailyEntry.java`) tracks a user's full wellness log for one day:

- Sleep hours and quality
- Water intake (ml)
- Workout details
- Reading minutes and pages
- Hobby activity
- Mood rating (1–5)
- Notes
- A list of `Meal` objects

It extends `PanacheEntity` — this gives it a built-in `id` field and static query methods (more on that below).

---

## 7. Part 5 — JWT Authentication

**Goal:** Understand how user registration, login, and protected endpoints work.

### Step 1 — Register a user

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username": "alice", "email": "alice@example.com", "password": "secret123"}'
```

### Step 2 — Log in and get a token

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "alice", "password": "secret123"}'
```

You'll get back an `AuthResponse` containing an `accessToken`. Copy it — you'll need it for all other requests.

### Step 3 — How it works

The `User` entity uses `quarkus-security-jpa` annotations:

```java
@UserDefinition
public class User extends PanacheEntity {

    @Username
    private String username;

    @Password(PasswordType.MCF)
    private String password;  // bcrypt hash, never plaintext

    @Roles
    private String roles;     // e.g. "user"
}
```

The `AuthService` issues JWTs using SmallRye JWT. The `application.properties` points to RSA key files:

```properties
smallrye.jwt.sign.key.location=privateKey.pem
mp.jwt.verify.publickey.location=publicKey.pem
```

### Step 4 — Call a protected endpoint

```bash
TOKEN="<paste your token here>"

curl http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer $TOKEN"
```

### Step 5 — How endpoints are protected

Any method (or class) annotated with `@RolesAllowed("user")` requires a valid JWT. Without a token, you get `401 Unauthorized`.

```java
@GET @Path("/me") @RolesAllowed("user")
public Response getUserInfo() {
    UserResponse userResponse = authService.me();
    return Response.ok(userResponse).build();
}
```

---

## 8. Part 6 — Goals & Analytics Endpoints

**Goal:** Explore the CRUD goals API and the analytics endpoints.

### Step 1 — Create a goal

```bash
curl -X POST http://localhost:8080/api/goals \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "goalType": "SLEEP",
    "targetValue": 8.0,
    "frequency": "DAILY",
    "startDate": "2026-04-08"
  }'
```

The `GoalResource` follows the same pattern as `EntryResource` — all five standard operations: `POST`, `GET`, `GET /{id}`, `PATCH /{id}`, `DELETE /{id}`.

### Step 2 — Log a daily entry

```bash
curl -X POST http://localhost:8080/api/entries \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "entryDate": "2026-04-08",
    "sleepHours": 7.5,
    "sleepQuality": 4,
    "waterMl": 2000,
    "workoutDone": true,
    "workoutType": "Running",
    "workoutDurationMin": 30,
    "moodRating": 4
  }'
```

### Step 3 — Get analytics

```bash
# Current streaks (how many consecutive days you've hit your goals)
curl http://localhost:8080/api/analytics/streaks \
  -H "Authorization: Bearer $TOKEN"

# Trend analysis for a specific metric
curl "http://localhost:8080/api/analytics/trends?metric=SLEEP&period=7" \
  -H "Authorization: Bearer $TOKEN"

# Period summary
curl "http://localhost:8080/api/analytics/summary?period=WEEKLY" \
  -H "Authorization: Bearer $TOKEN"
```

### Step 4 — Use Swagger UI

Quarkus auto-generates interactive API docs. Visit:

```
http://localhost:8080/q/swagger-ui
```

You can try every endpoint directly from the browser, including setting your Bearer token once and using it across all requests.

---

## 9. Part 7 — Testing

**Goal:** Run the existing tests and understand how Quarkus testing works.

### Step 1 — Run the tests

```bash
# From quarkus-wellness-api:
./gradlew test
```

### Step 2 — Look at an integration test

Open `src/test/java/org/fractalschema/AuthResourceTest.java`.

```java
@QuarkusTest
public class AuthResourceTest {

    @Test
    void testRegisterAndLogin() {
        // Register
        given()
            .contentType(ContentType.JSON)
            .body(new RegisterRequest("testuser", "test@example.com", "password123"))
        .when()
            .post("/api/auth/register")
        .then()
            .statusCode(201)
            .body("username", equalTo("testuser"));

        // Login
        String token =
            given()
                .contentType(ContentType.JSON)
                .body(new LoginRequest("testuser", "password123"))
            .when()
                .post("/api/auth/login")
            .then()
                .statusCode(200)
                .extract().path("accessToken");

        assertNotNull(token);
    }
}
```

**Key concepts:**
- `@QuarkusTest` — starts the full app in test mode, including the database
- `RestAssured` — fluent HTTP test client (built into Quarkus test support)
- The `%test` profile in `application.properties` automatically runs Flyway migrations, so you start with a clean schema

### Step 3 — Continuous testing

In dev mode, press `r` in the terminal to enable **continuous testing** — tests re-run automatically on every save.

---

## 10. Part 8 — Database Migrations with Flyway

**Goal:** Understand how schema changes are managed safely.

### Step 1 — Find the migration files

```bash
ls src/main/resources/db/
```

Flyway migration files follow the naming pattern `V<version>__<description>.sql`:

```
V1__create_users.sql
V2__create_daily_entries.sql
V3__create_goals.sql
...
```

### Step 2 — How Flyway is configured

In `application.properties`:

```properties
quarkus.flyway.migrate-at-start=true
quarkus.flyway.locations=classpath:db
quarkus.flyway.baseline-on-migrate=true
```

Every time the app starts, Flyway checks which migrations have already run (tracked in a `flyway_schema_history` table) and applies any new ones. You never write `CREATE TABLE` by hand during development — you write a migration file.

### Step 3 — Add a migration (stretch)

Create a new file `src/main/resources/db/V10__add_step_count.sql`:

```sql
ALTER TABLE daily_entries ADD COLUMN step_count INT;
```

Restart the app — Flyway applies it automatically.

---

## 11. Stretch Goals

Pick one (or more) to work on:

**Beginner**
- Add a `DELETE /film/{filmId}` endpoint to the Films API
- Add a `GET /api/entries/latest` query that returns the last N entries (already scaffolded — check `EntryResource:56`)

**Intermediate**
- Implement `GET /api/goals/progress` — it currently returns `501 Not Implemented` (see `GoalResource:54`)
- Add pagination to `GET /api/entries/range`

**Advanced**
- Add a `CacheService` call to `AnalyticsService` to cache streak results (the `CacheService` class exists at `cache/CacheService.java`)
- Write a `CrossUserIsolationTest` — verify that User A cannot see User B's entries (the test file is already scaffolded at `src/test/java/org/fractalschema/CrossUserIsolationTest.java`)
- Build a native executable: `./gradlew build -Dquarkus.native.enabled=true`

---

## 12. Quick Reference

### Dev mode commands

| Command | What it does |
|---|---|
| `./gradlew quarkusDev` | Start app with live reload |
| `r` (in dev mode) | Toggle continuous testing |
| `h` (in dev mode) | Show all dev mode shortcuts |
| `./gradlew test` | Run all tests once |
| `./gradlew build` | Package the app as a JAR |

### Common annotations

| Annotation | Where | What it does |
|---|---|---|
| `@Path("/foo")` | Class / Method | Maps URL path |
| `@GET`, `@POST`, `@PUT`, `@PATCH`, `@DELETE` | Method | HTTP verb |
| `@PathParam("id")` | Parameter | Reads `{id}` from path |
| `@QueryParam("limit")` | Parameter | Reads `?limit=...` from query string |
| `@RolesAllowed("user")` | Class / Method | Requires auth |
| `@Transactional` | Method | Wraps DB operation in a transaction |
| `@ApplicationScoped` | Class | One instance per application (singleton) |
| `@Inject` | Field | Dependency injection |
| `@Valid` | Parameter | Triggers Bean Validation on the request body |

### Panache cheat sheet

```java
// Static finders (from PanacheEntity)
User.findById(1L);
User.findByUsername("alice");      // custom static method
User.listAll();
User.count();

// Instance operations
user.persist();
user.delete();

// Queries
User.find("email", "alice@example.com").firstResult();
User.list("roles = ?1", "admin");
```

### Useful URLs in dev mode

| URL | What it is |
|---|---|
| `http://localhost:8080/q/dev/` | Dev UI dashboard |
| `http://localhost:8080/q/swagger-ui` | Interactive API docs |
| `http://localhost:8080/q/health` | Health check endpoints |

---

Happy building! If you get stuck, check the Quarkus docs at https://quarkus.io/guides/ — every extension has a dedicated guide.
