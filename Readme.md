# Course Management API

A well-structured REST API built with **Java 21** and **Spring Boot** for managing users, courses, and enrollments — applying production-oriented patterns including JWT authentication, role-based access control, two-level distributed caching, Redis-backed rate limiting, and full request observability.

---

## Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Getting Started](#getting-started)
- [API Reference](#api-reference)
- [Request Pipeline](#request-pipeline)
- [Rate Limiting](#rate-limiting)
- [Caching](#caching)
- [Concurrency Control](#concurrency-control)
- [Observability](#observability)
- [Error Handling](#error-handling)
- [Design Decisions](#design-decisions)
- [Testing](#testing)
- [Roadmap](#roadmap)

---

## Features

| Area               | Details                                                                                                                                                                                                                                            |
|--------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Auth**           | JWT-based authentication & authorization                                                                                                                                                                                                           |
| **Access Control** | Role-based permissions — `ADMIN`, `INSTRUCTOR`, `STUDENT`                                                                                                                                                                                          |
| **Courses**        | Full CRUD, pagination, filtering, search, seat tracking                                                                                                                                                                                            |
| **Enrollments**    | Enroll/unenroll with seat validation and concurrency safety                                                                                                                                                                                        |
| **Rate Limiting**  | Role + endpoint-aware distributed token bucket limiting via Bucket4j + Redis (shared across instances)                                                                                                                                             |
| **Caching**        | Two-level caching: Caffeine (L1, in-process) + Redis (L2, distributed) with cross-instance eviction sync                                                                                                                                           |
| **Observability**  | Per-request trace IDs, MDC-based structured logging, annotation-driven log instrumentation                                                                                                                                                         |
| **Error Handling** | Global exception handler with consistent JSON error responses                                                                                                                                                                                      |
| **Validation**     | Input validation and sanitization across all endpoints                                                                                                                                                                                             |
| **Testing**        | Service-layer unit coverage (`CourseService`, `UserService`, `EnrollmentService`) with shared `BaseUnitTest`; full MockMvc integration tests for all three controllers with role-based, pagination, filtering, security, and concurrency scenarios |

---

## Tech Stack

- **Java 21**
- **Spring Boot 4.0.5** — web framework
- **Spring Security** — authentication & authorization
- **Spring Data JPA** — database access layer
- **Spring Cache + Caffeine** — L1 in-memory caching
- **Spring Cache + Redis** — L2 distributed caching
- **Spring AOP** — annotation-driven logging aspect
- **JJWT 0.12.7** — JWT generation and validation
- **Bucket4j** — token bucket rate limiting
- **Bucket4j Redis module** — distributed bucket storage via Lettuce `ProxyManager`
- **Redis** — L2 cache, distributed rate limit state, cache eviction pub/sub
- **PostgreSQL** — relational database
- **Spring Boot Actuator** — health and info endpoints (`/actuator/health`, `/actuator/info`) (authentication required via JWT)
- **Logstash Logback Encoder** — structured JSON logging
- **Lombok** — boilerplate reduction
- **Maven** — build tool

---

## Architecture

```
src/main/java/com/nishant/coursemanagement/
│
├── config/          # Security, cache, Redis, and rate-limiting configuration
├── controller/      # REST endpoints (users, courses, enrollments)
├── service/         # Business logic (split into command + query services)
├── repository/      # Spring Data JPA repositories
├── entity/          # JPA entities (User, Course, Enrollment)
├── dto/             # Request/response models and PageResponse wrapper
├── mapper/          # Entity ↔ DTO conversion
├── exception/       # Custom exceptions, global handler, error response factory
├── security/        # JWT utilities, properties, auth helpers
├── filter/          # OncePerRequestFilter chain (Trace → RateLimit → JWT)
├── cache/           # CompositeCacheManager, CompositeCache, custom key builders
├── event/           # Domain events (events/) and their cache-eviction listeners (listeners/)
├── log/             # Logging infrastructure
│   ├── annotation/  # @Loggable annotation and LogLevel enum
│   ├── aspect/      # LoggingAspect — AOP-driven MDC population and log emission
│   └── util/        # LogUtil — MDC helpers and inline LogUtil.log() for branch-level logs
└── util/            # Shared utilities (Sanitizer, StringUtil, etc.)
```

### Domain Model

```
ADMIN
  └──[manages]──► User (can update any user, including their role)

INSTRUCTOR
  └──[creates & controls]──► Course (title, description, maxSeats, enrolledStudents, isActive)
                                  ▲           + availableSeats [computed, not persisted]
STUDENT                           │
  └──[enrolls via]──► Enrollment (student ↔ course, isActive)
```

---

## Getting Started

### Prerequisites

- Java 21+
- Maven 3.8+
- PostgreSQL
- Redis

### 1. Clone

```bash
git clone https://github.com/nishant-dd-24/course-management-api.git
cd course-management-api
```

### 2. Configure environment variables

```bash
export SPRING_DATASOURCE_PASSWORD=your_db_password
export JWT_SECRET=$(echo -n "your_secret_key_min_32_chars" | base64)
export JWT_EXPIRATION_SECONDS=3600
export SPRING_REDIS_HOST=localhost
export SPRING_REDIS_PORT=6379
```

### 3. Run

```bash
./mvnw spring-boot:run
```

The API starts at `http://localhost:8080`.

---

## API Reference

All endpoints except `/users/login` and `/users/register` require authentication via JWT.

All protected endpoints require the header:
```
Authorization: Bearer <jwt_token>
```

---

### Authentication

| Method | Endpoint          | Access | Description                          |
|--------|-------------------|--------|--------------------------------------|
| `POST` | `/users/login`    | Public | Authenticate and receive a JWT token |
| `POST` | `/users/register` | Public | Register a new user                  |

**Login request:**
```json
{
  "email": "user@example.com",
  "password": "yourpassword"
}
```

**Register request:**
```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "yourpassword"
}
```

`POST /users/register` always creates a user with role `STUDENT`.

**Login response:**
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "expiresIn": 3600,
  "expiresAt": "2026-04-05T14:46:23.082550103Z",
  "user": {
    "id": 22,
    "name": "John Doe",
    "email": "john@example.com",
    "role": "STUDENT",
    "isActive": true
  }
}
```

---

### Users

| Method   | Endpoint                    | Role          | Description                            |
|----------|-----------------------------|---------------|----------------------------------------|
| `GET`    | `/users`                    | ADMIN         | List all users (paginated, filterable) |
| `GET`    | `/users/{id}`               | ADMIN         | Get user by ID                         |
| `PUT`    | `/users/{id}`               | ADMIN         | Full update of a user                  |
| `PATCH`  | `/users/{id}`               | ADMIN         | Partial update of a user               |
| `DELETE` | `/users/{id}`               | ADMIN         | Deactivate a user (soft delete)        |
| `GET`    | `/users/my`                 | Authenticated | Get own profile                        |
| `PUT`    | `/users/my`                 | Authenticated | Update own profile                     |
| `PATCH`  | `/users/my`                 | Authenticated | Partial update of own profile          |
| `DELETE` | `/users/my`                 | Authenticated | Deactivate own account (soft delete)   |
| `POST`   | `/users/my/change-password` | Authenticated | Change password                        |

**Query parameters for `GET /users`:**

| Param      | Type    | Description                    |
|------------|---------|--------------------------------|
| `name`     | string  | Filter by name (partial match) |
| `email`    | string  | Filter by email                |
| `isActive` | boolean | Filter by active status        |
| `page`     | int     | Page number (0-indexed)        |
| `size`     | int     | Page size (default: 5)         |
| `sort`     | string  | Sort field (default: `id`)     |

---

### Courses

| Method   | Endpoint               | Role                                | Description                                                                                           |
|----------|------------------------|-------------------------------------|-------------------------------------------------------------------------------------------------------|
| `POST`   | `/courses`             | INSTRUCTOR                          | Create a new course                                                                                   |
| `GET`    | `/courses`             | ADMIN                               | ADMIN only — list all courses (paginated, filterable)                                                 |
| `GET`    | `/courses/{id}`        | ADMIN                               | Get any course by ID                                                                                  |
| `GET`    | `/courses/active/{id}` | Authenticated (no role restriction) | Get an active course by ID                                                                            |
| `GET`    | `/courses/active`      | Authenticated (no role restriction) | Browse all active courses                                                                             |
| `GET`    | `/courses/my`          | INSTRUCTOR                          | Get own courses                                                                                       |
| `PUT`    | `/courses/{id}`        | INSTRUCTOR                          | Updates title and description; `maxSeats` is updated only if provided (`null` retains existing value) |
| `PATCH`  | `/courses/{id}`        | INSTRUCTOR                          | Partial update — any of: title, description, maxSeats (min: 1)                                        |
| `DELETE` | `/courses/{id}`        | INSTRUCTOR                          | Deactivate a course (soft delete)                                                                     |

**Course response fields:**

| Field            | Description                                             |
|------------------|---------------------------------------------------------|
| `id`             | Course ID                                               |
| `title`          | Course title                                            |
| `description`    | Course description                                      |
| `instructorId`   | ID of the owning instructor                             |
| `maxSeats`       | Total seat capacity                                     |
| `availableSeats` | Computed: `maxSeats - enrolledStudents` (not persisted) |
| `isActive`       | Whether the course is active                            |

**Course request note:**

- `maxSeats` defaults to `20` if not provided in `POST /courses`

**Query parameters for `GET /courses`:**

| Param                  | Type    | Description                     |
|------------------------|---------|---------------------------------|
| `title`                | string  | Filter by title (partial match) |
| `isActive`             | boolean | Filter by active status         |
| `instructorId`         | long    | Filter by instructor            |
| `page`, `size`, `sort` | —       | Standard pagination             |

---

### Enrollments

| Method   | Endpoint                  | Role       | Description                  |
|----------|---------------------------|------------|------------------------------|
| `POST`   | `/enrollments/{courseId}` | STUDENT    | Enroll in a course           |
| `DELETE` | `/enrollments/{courseId}` | STUDENT    | Unenroll from a course       |
| `GET`    | `/enrollments/my`         | STUDENT    | Get own enrollments          |
| `GET`    | `/enrollments/{id}`       | INSTRUCTOR | Get enrollments for a course |

**Query parameters for `GET /enrollments/my` and `GET /enrollments/{id}`:**

| Param      | Type    | Description                 |
|------------|---------|-----------------------------|
| `isActive` | boolean | Filter by active status     |
| `page`     | int     | Page number (0-indexed)     |
| `size`     | int     | Page size (default: `5`)    |
| `sort`     | string  | Sort field (default: `id`)  |

**Enrollment response fields:**

| Field       | Description             |
|-------------|-------------------------|
| `id`        | Enrollment ID           |
| `studentId` | Enrolled student ID     |
| `courseId`  | Enrolled course ID      |

---

### Pagination Response

All paginated endpoints return a consistent `PageResponse<T>` envelope:

```json
{
  "content": [...],
  "pageNumber": 0,
  "pageSize": 5,
  "totalElements": 18,
  "numberOfElements": 5,
  "totalPages": 4,
  "first": true,
  "last": false,
  "hasNext": true,
  "hasPrevious": false,
  "hasContent": true
}
```

---

## Request Pipeline

Every request passes through a three-stage filter chain before reaching the controller:

```
Client Request
      │
      ▼
┌─────────────────┐
│  Trace Filter   │  Generates/propagates X-Trace-Id, stores in MDC
└────────┬────────┘
         │
         ▼
┌──────────────────────┐
│  Rate Limit Filter   │  Resolves identity, checks distributed token bucket (Redis)
└────────┬─────────────┘
         │
         ▼
┌─────────────────┐
│   JWT Filter    │  Validates token, loads SecurityContext
│                 │  (skips /users/login and /users/register)
└────────┬────────┘
         │
         ▼
    Controller
```

---

## Rate Limiting

Rate limiting is implemented using **Bucket4j** (token bucket algorithm) backed by a **Redis `ProxyManager<String>`** via Lettuce. Buckets are stored in Redis, making rate limit state shared across all instances and persistent across restarts. Limits are resolved per-request based on two axes:

### Role-based limits (per minute)

| Role       | Requests/min |
|------------|--------------|
| ADMIN      | 100          |
| INSTRUCTOR | 50           |
| STUDENT    | 20           |
| Anonymous  | 10           |

### Endpoint-specific limits (per minute)

| Endpoint               | Limit |
|------------------------|-------|
| `POST /users/login`    | 5     |
| `POST /enrollments/**` | 10    |
| `GET /courses/**`      | 100   |
| All others             | 20    |

**Identity resolution:** When a valid JWT is present, the bucket key is `userId:ROLE:normalizedPath:method`. For unauthenticated requests, the IP address is used as fallback. Path normalization replaces numeric IDs with `{id}` (e.g. `/courses/42` → `/courses/{id}`) to prevent per-ID bucket explosion.

**Cross-instance consistency:** Because buckets live in Redis, a user who exhausts their quota on instance A cannot bypass the limit by hitting instance B. Limits also survive application restarts — there is no reset window on redeploy.

**Response headers:**
```
X-RateLimit-Limit: 20
X-RateLimit-Remaining: 17
Retry-After: 42        ← only on 429 responses
```

---

## Caching

Caching is applied at the **query service layer** using Spring's caching abstraction. The implementation uses a **two-level `CompositeCacheManager`**: Caffeine as L1 (in-process, sub-millisecond) and Redis as L2 (distributed, shared across instances). The cache targets read-heavy operations on courses and users.

### Read / write flow

```
Read:
  L1 hit → return immediately
  L1 miss → check L2
    L2 hit → populate L1, return
    L2 miss → load from DB, write to both L1 and L2, return

Write:
  Mutating operations evict the relevant entry from both L1 and L2.
```

This means the database is only reached when neither cache level has a valid entry.

### Cache stampede protection

`@Cacheable(sync = true)` is set on all cache definitions. Under concurrent load, only one thread computes the value for a given key; all others wait on that result rather than each racing to the database. This is enforced at the `CompositeCache` level so it applies to both L1 and L2 reads.

### Cache key design

Keys are constructed by dedicated utility classes to ensure stability across requests:

```
title=java|isActive=true|instructorId=1|page=0|size=5|sort=id:ASC
```

This prevents cache misses from input formatting differences (e.g., `%java%` vs `java`) and includes all pagination/sort parameters.

### What is cached

Only **courses** and **users** are cached. Enrollment data is not cached directly, but enrollment mutations (enroll/unenroll) affect the `enrolledStudents` field on the `Course` entity — any enrollment change publishes an event that evicts the relevant course cache entries to prevent stale seat counts being served.

### Cache eviction

Eviction is handled via **domain events** published on mutating operations (create, update, deactivate, enroll, unenroll), keeping cache state consistent without manual cache management at the controller level.

### Distributed Cache Eviction

L2 (Redis) is inherently consistent — all instances share the same store, so a write on one node is immediately visible to all others. The problem is L1: each instance holds its own Caffeine heap cache, and evicting a key locally does nothing for the other nodes.

This is solved via **Redis Pub/Sub** on the `cache-evict` channel:

1. A mutating operation calls `CompositeCache.evict()` on the local instance — this clears both L1 and L2 immediately.
2. A `CacheEvictEvent` (carrying `cacheName`, `key`, and an `evictAll` flag for full-cache clears) is published to the `cache-evict` Redis channel.
3. All other instances have a `RedisCacheListener` subscribed to that channel. On receipt, it evicts the same key (or clears the entire cache if `evictAll=true`) from their local Caffeine L1 only — L2 is already consistent.

Net result: no instance serves a stale in-process entry after a mutation, and the cross-instance invalidation is asynchronous and zero-cost on the write path.

### Key configuration

- **L1 (Caffeine):** 10 minute TTL (write-based expiry), 10,000 max entries, shared across all cache names
- **L2 (Redis):** TTL matched to L1 (10 minutes); values serialized via `GenericJacksonJsonRedisSerializer` with default typing enabled, so polymorphic types round-trip correctly without requiring explicit type hints at each call site

---

## Concurrency Control

Enrollment operations use **pessimistic write locking** (`PESSIMISTIC_WRITE`) via JPA to prevent race conditions when multiple students attempt to enroll in a course simultaneously. The course row is locked at the database level for the duration of the transaction, ensuring that the seat count read, comparison, and increment are atomic.

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT c FROM Course c WHERE c.id = :courseId AND c.isActive = true")
Optional<Course> findByIdForUpdate(@Param("courseId") Long courseId);
```

Note: the `isActive = true` filter means attempting to enroll in an inactive course results in a `404 Not Found` rather than a seat-full error.

`DataIntegrityViolationException` is handled by the global exception handler as a safety net for duplicate enrollment attempts under race conditions.

**Trade-off:** Slightly reduced throughput under high concurrent enrollment on the same course, in exchange for strict seat-count correctness.

### Concurrency validation

The concurrency guarantee is verified by `EnrollmentFlowIT` using two real concurrent-request tests (`ConcurrencyTests`). Each test spawns 5 threads via `ExecutorService`, holds them behind a `CountDownLatch` until all are ready, then releases them simultaneously against a course with 2 available seats:

- `shouldAllowOnlyLimitedEnrollments_whenConcurrentRequests` — asserts that exactly 2 requests succeed, that the active enrollment count in the database equals 2, and that `enrolledStudents` on the course reflects the correct value
- `shouldRejectExcessEnrollments_whenConcurrentRequests` — asserts that exactly 2 requests succeed and that all remaining requests receive a 4xx error

These tests run against H2 with real JPA pessimistic locking in place, giving confidence that the seat-boundary guarantee holds under actual concurrent load.

---

## Observability

### Request tracing

`TraceFilter` runs first in the filter chain:

- Reads `X-Trace-Id` from the incoming request, or generates a new UUID if absent
- Stores the trace ID in MDC under the key `traceId`
- Propagates the trace ID back in the response via `X-Trace-Id`

Because `logstash-logback-encoder` is configured with `includeMdc=true`, the trace ID is automatically included in every JSON log entry for the duration of the request — no manual passing required.

### Structured JSON logging

Logging is configured via `logback-spring.xml` using `LogstashEncoder`. Every log line is emitted as a JSON object.

### Annotation-driven log instrumentation

Log instrumentation is handled through a dedicated `log` package with three layers:

**`@Loggable` (annotation)** — placed on any method to declare its logging intent. Supported fields:

| Field                | Description                                                                    |
|----------------------|--------------------------------------------------------------------------------|
| `action`             | Machine-readable action key written to MDC (e.g. `"DELETE_USER"`)              |
| `message`            | Human-readable log message; falls back to `action` if blank                    |
| `level`              | Log level — `DEBUG`, `INFO`, `WARN`, `ERROR` (default: `INFO`)                 |
| `extras`             | SpEL expressions evaluated against method parameters (e.g. `"#request.email"`) |
| `extraKeys`          | MDC key names corresponding to each `extras` expression by index               |
| `includeCurrentUser` | When `true`, resolves the authenticated user and writes `currentUserId` to MDC |

**`LoggingAspect` (AOP)** — intercepts all `@Loggable`-annotated methods. On each invocation it populates MDC from the annotation metadata, calls the method, then adds `status` and `durationMs` before emitting the log line. MDC is always cleared in a `finally` block regardless of outcome.

**`LogUtil.log()` (inline helper)** — used directly inside method bodies for branch-level log points (e.g. duplicate email, password mismatch, role change) that cannot be expressed as a single method-entry annotation. Accepts action, message, and variadic key-value pairs; always clears MDC in its own `finally` block.

Together, the two approaches cover all log points without any `try/finally` boilerplate at the call site:

```java
// Method-entry log — declared via annotation
@Loggable(action = "DELETE_USER", level = WARN, includeCurrentUser = true,
          extras = {"#id"}, extraKeys = {"deletedUserId"})
public void deleteUser(Long id) { ... }

// Branch-level log — emitted inline
LogUtil.log(log, WARN, "LOGIN_FAILED", "Login failed",
            "reason", "INVALID_PASSWORD", "userId", user.getId());
```

### Log output

Below are real examples from a login request:

```json
{"timestamp":"2026-04-06T12:29:54.909189587+05:30","@version":"1","message":"Login successful","logger":"com.nishant.coursemanagement.service.user.UserServiceImpl","thread":"http-nio-8080-exec-1","level":"INFO","level_value":20000,"traceId":"0eaaf0b2-9128-4872-a3a7-a3a6ec9b7459","path":"/users/login","action":"LOGIN_SUCCESS","method":"POST","userId":"22","clientIp":"0:0:0:0:0:0:0:1","app":"course-management"}

{"timestamp":"2026-04-06T12:29:54.924532492+05:30","@version":"1","message":"LOGIN_ATTEMPT","logger":"com.nishant.coursemanagement.log.aspect.LoggingAspect","thread":"http-nio-8080-exec-1","level":"INFO","level_value":20000,"traceId":"0eaaf0b2-9128-4872-a3a7-a3a6ec9b7459","path":"/users/login","method":"POST","durationMs":"143","clientIp":"0:0:0:0:0:0:0:1","status":"SUCCESS","app":"course-management"}
```

Key points about the log structure:

- `action` is the machine-readable key used for querying and alerting; `message` is the human-readable description
- `status` (`SUCCESS` / `FAILED`) and `durationMs` are added automatically by `LoggingAspect` on every `@Loggable`-annotated method
- `traceId`, `method`, `path`, and `clientIp` are set once by `TraceFilter` and are present on all log entries for the full request lifetime
- MDC fields appear as flat top-level keys in the JSON output alongside standard fields
- The `app` field is injected globally via `customFields` in `logback-spring.xml`
- `LogUtil.clear()` only removes the keys it added — it does not call `MDC.clear()` — so `traceId` and other request-scoped fields set by `TraceFilter` remain intact. `TraceFilter` itself calls `MDC.clear()` in its own `finally` block at the very end as the outermost filter.

---

## Error Handling

All errors are handled by a global `@ControllerAdvice` and return a consistent JSON structure.

### HTTP status codes

| Scenario                                           | Status                      |
|----------------------------------------------------|-----------------------------|
| `@Valid` validation failure                        | `400 Bad Request`           |
| Invalid enum value / type mismatch / bad argument  | `400 Bad Request`           |
| Authentication required                            | `401 Unauthorized`          |
| Access denied                                      | `403 Forbidden`             |
| Resource not found                                 | `404 Not Found`             |
| Method not allowed                                 | `405 Method Not Allowed`    |
| Duplicate resource (e.g. email already registered) | `409 Conflict`              |
| Rate limit exceeded                                | `429 Too Many Requests`     |
| Unexpected server error                            | `500 Internal Server Error` |

### Error response schema

```json
{
  "traceId": "2cf5dd29-3bb0-44b3-921a-2ac07aa09f4a",
  "path": "/users/abc",
  "method": "GET",
  "status": 400,
  "message": "Invalid value 'abc' for parameter 'id'. Expected type: Long",
  "errorCode": "BAD_REQUEST",
  "timestamp": "2026-04-03T08:52:47.856580784",
  "errors": {}
}
```

The `errors` map is populated **only** for `@Valid` validation failures (`MethodArgumentNotValidException`), where it contains field-level messages. For all other error cases it is an empty `{}`.

### Dual 401/403 handling

Spring Security intercepts some auth failures before the request reaches the controller layer. Two dedicated handlers cover this:

- **`CustomAuthenticationEntryPoint`** — handles `401 Unauthorized` when Spring Security rejects a request due to a missing or invalid JWT, before it reaches the controller
- **`CustomAccessDeniedHandler`** — handles `403 Forbidden` when Spring Security blocks an authenticated user due to insufficient role, before it reaches the controller

The `GlobalExceptionHandler` covers the equivalent cases that do reach the controller (`UnauthorizedException`, `AuthorizationDeniedException`). Both paths use the same `ErrorResponseWriter`, so the JSON response shape is identical regardless of which layer catches the error.

---

## Design Decisions

### Why use `userId` instead of email as JWT subject?

- `userId` is immutable — email can be changed by the user or an admin, making it an unreliable long-lived identity claim
- Using email as the subject creates a window where a token issued before an email change still resolves to the wrong identity
- `userId` maps directly to the primary key, so token validation never requires a secondary email lookup
- Avoids any stale-identity issue during update flows where email and the in-flight token would temporarily be out of sync

---

### Why pessimistic over optimistic locking?

Enrollment is a write operation with a clear contention point — multiple students racing to claim the last seat in a course. Optimistic locking would require retry logic on version conflicts, adding complexity and unpredictable latency under load. Pessimistic locking (`PESSIMISTIC_WRITE`) gives a simpler, stronger guarantee by holding the row lock for the duration of the transaction.

**Trade-off:** Slightly reduced concurrency throughput under heavy enrollment load on the same course, in exchange for strict seat-count correctness.

---

### Why Redis-backed distributed rate limiting?

Bucket4j's `LettuceBasedProxyManager<String>` stores all bucket state in Redis via a `StatefulRedisConnection<String, byte[]>`, keeping it entirely out of the JVM heap. Rate limits are shared across every instance — a user who exhausts their quota on one node cannot bypass it by hitting another. Limits also survive application restarts, eliminating the window where a restart was a de facto quota reset.

**Trade-off:** Each rate limit check now involves a Redis round-trip. In practice this is negligible given Redis's sub-millisecond latency, and the correctness guarantees are essential for any horizontally scaled deployment.

---

### Why two-level caching (Caffeine + Redis)?

Caffeine alone doesn't survive horizontal scaling — each instance has its own heap cache, so a write on one node leaves stale data on every other. Redis alone is correct but adds a network hop to every cache hit.

The composite approach gets both: L1 (Caffeine) serves the hot path at nanosecond latency; L2 (Redis) provides the shared ground truth and populates L1 on a miss. Writes go to both levels simultaneously. Cross-instance L1 invalidation is handled via Redis Pub/Sub (see [Distributed Cache Eviction](#distributed-cache-eviction)): the `RedisCacheListener` receives eviction events and clears only the local Caffeine cache — L2 is already the shared source of truth and needs no cross-instance notification.

**Trade-off:** Operational complexity (two cache backends, an eviction pub/sub channel). The payoff is read latency that stays in-process for warm caches, combined with consistency guarantees that hold under horizontal scaling.

---

### Why a custom `PageResponse` DTO?

Spring's `Page<T>` serialization is unstable and includes internal fields that change between versions. A custom `PageResponse<T>` gives a stable, documented contract for all paginated responses.

---

### Why split command and query services?

Each domain area (`UserService` / `UserQueryService`, etc.) separates read and write concerns, making caching and locking strategies easier to apply precisely. Query services are cache-eligible; command services manage eviction.

---

### Why separate `CourseRequest` and `CourseUpdateRequest` DTOs?

`POST /courses` (create) and `PUT /courses/{id}` (full update) look identical at the field level but carry different semantic intent. Sharing a single DTO would mean the same validation constraints apply to both operations and that any future divergence (e.g., making `maxSeats` immutable after creation) requires adding conditional logic inside a shared class.

Using `CourseUpdateRequest` for the PUT path makes the contract explicit, keeps validation requirements decoupled, and ensures that adding a create-only or update-only field is a non-breaking change to the other operation.

---

### Why two logging mechanisms (`@Loggable` + `LogUtil.log()`)?

`@Loggable` handles the method-entry log point — the single, unconditional emission that every method has. `LogUtil.log()` handles branch-level log points within the same method body (e.g. duplicate email detected, password mismatch, role change on update) that carry different `action` keys and context depending on the execution path. Using `@Loggable` alone for these would require either one annotation per branch (not possible) or encoding branching logic into SpEL (fragile). The two mechanisms are complementary: the annotation removes all `try/finally` boilerplate at method entry; the helper removes it at every inline call site.

---

### Why unit test the service layer?

The service layer is where the core invariants live — seat-count correctness under contention, role-gated mutations, and event-driven cache eviction sequencing. These are the behaviors most likely to regress and the hardest to isolate if tested only through end-to-end flows.

Unit tests with mocked repositories and event publishers keep those checks deterministic and fast:

- **Event-driven architecture:** mutation paths assert domain-event publication directly (`UserUpdatedEvent`, `CourseUpdatedEvent`, `EnrollmentChangedEvent`), which is the cache-eviction trigger.
- **Caching complexity:** eviction behavior is verified at the contract boundary (event published or not) without requiring Redis/Caffeine in unit scope.
- **Concurrency guarantees:** enrollment seat-boundary logic (full course, reactivation, zero-floor decrement) is validated in service tests while DB lock mechanics remain an integration concern.
- **Role enforcement:** ADMIN-only role mutation paths are asserted explicitly in update and patch flows.

The result is a fast suite that validates business invariants independently of infrastructure availability.

---

## Testing

### What is covered

**Service-layer unit tests** cover all three core command services:

- `CourseUnitTests` — create, update, patch, deactivate; ownership validation (non-owner rejection); event publishing on mutations
- `UserUnitTests` — create (including inactive-user reactivation), update, patch (including `patchMe` flows), deactivate; password change; admin-guarded mutations; event publishing
- `EnrollmentUnitTests` — enroll (new and reactivation paths), unenroll; seat availability and boundary conditions (zero seats, at-capacity); duplicate enrollment guard; `DataIntegrityViolationException` safety net; event publishing

**Controller-layer integration tests** (MockMvc-based, with a real `SpringBootTest` context) cover all three controllers end-to-end:

- `UserFlowIT` — registration, login (success, wrong password, inactive user), role-based access control, paginated listing, filtering by name/email/isActive, full update, partial update, password change, and account deactivation
- `CourseFlowIT` — course creation (INSTRUCTOR only), ownership-guarded update and patch, full-coverage retrieval (admin vs. non-admin, own courses, active-only listing), title/instructorId/isActive filtering with pagination, and soft deactivation
- `EnrollmentFlowIT` — enroll, unenroll, duplicate detection, full-course rejection, own enrollment retrieval, instructor-scoped course enrollments, unauthorized and role-invalid access, and multithreaded concurrency validation (see below)

### Test structure

**Unit tests** extend a shared `BaseUnitTest`, which provides:

- Pre-configured Mockito mocks for `AuthUtil`, `ApplicationEventPublisher`, and `ExceptionUtil`; each test class provides its own repository mocks
- `buildUser(Long id)` and `buildUser(Long id, Role role)` fixture builders shared across all three test classes
- Shared helpers: `captureEvent`, `captureEvents`, `verifyNoEventPublished`, `mockCurrentUser`, `mockNotFoundException`, `mockBadRequestException`

**Integration tests** extend a shared `BaseIntegrationTest`, which provides:

- A `MockMvc` instance configured with full Spring Security support
- Database reset and seed fixtures before each test: a `testUser` (INSTRUCTOR), a `testCourse`, and a pre-generated JWT token
- Role-switching helpers: `setStudentToken()`, `setInstructorToken()`, `setAdminToken()`
- Entity builders: `buildUser()`, `buildCourse()`, `buildEnrollment()`, and `buildThisManyUsers()` for pagination setup
- `toJson()` serialization and a reusable `auth()` `RequestPostProcessor`

This keeps individual test classes focused on behavior assertions rather than setup noise.

### What is verified

Each unit test suite covers three categories:

- **Repository interactions** — that the correct repository methods are called with the correct arguments (e.g. `save`, `existsByEmail`, `findByEmail`)
- **Event publishing** — that domain events are published after mutating operations, which is the trigger for cache eviction
- **Edge cases and failure scenarios** — not-found lookups, seat exhaustion, duplicate enrollments, unauthorized role changes, and other rejection paths

Integration tests additionally verify:

- **HTTP-level behavior** — correct status codes, response shapes, and field values across all endpoints
- **Role enforcement at the HTTP boundary** — `401 Unauthorized`, `403 Forbidden`, and ownership-based `404 Not Found` for non-owner mutations
- **Pagination and filtering** — correct `content.length()`, `totalElements`, `pageNumber`, and `pageSize` fields; filter combinations (title + instructorId + isActive, name + email + isActive)
- **Concurrency correctness** — see [Concurrency Control](#concurrency-control)

### Test configuration

The project includes a dedicated `application-test.properties` for test defaults. It uses:

- **H2 in-memory database** — no PostgreSQL instance required
- **Caching disabled** (`spring.cache.type=none`) — no cache manager is registered at all; caching infrastructure is fully excluded from the test context
- **JWT fallback secret** — `app.jwt.secret=${JWT_SECRET:dGVzdC1zZWNyZXQtc2VjcmV0LXNlY3JldC1rZXktMTIzNDU2}` (base64-encoded), so no environment variable is required to run tests locally. Expiration also defaults via `app.jwt.expiration-seconds=${JWT_EXPIRATION_SECONDS:3600}`

### How to run

```bash
./mvnw verify
```

To run tests for a specific class:

```bash
./mvnw test -Dtest=CourseUnitTests
./mvnw test -Dtest=UserUnitTests
./mvnw test -Dtest=EnrollmentUnitTests
./mvnw test -Dtest=CourseFlowIT
./mvnw test -Dtest=UserFlowIT
./mvnw test -Dtest=EnrollmentFlowIT
```

---

## Roadmap

The following are needed before this project could be considered production-ready:

**Correctness & reliability**
- [ ] Advanced input validation and edge-case hardening

**Operability**
- [ ] Dockerization + `docker-compose` setup (including Redis and PostgreSQL)
- [ ] Prometheus metrics integration (Actuator already active)
- [ ] Swagger / OpenAPI documentation

**Scalability**
- [ ] Database indexing and query analysis for larger datasets

**Observability**
- [ ] OpenTelemetry distributed tracing integration
- [ ] API abuse detection and alerting

---

## Author

**Nishantkumar Dwivedi**  
[github.com/nishant-dd-24](https://github.com/nishant-dd-24)