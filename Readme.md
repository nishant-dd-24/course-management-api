# Course Management API

A well-structured REST API built with **Java 21** and **Spring Boot** for managing users, courses, and enrollments — applying production-oriented patterns including JWT authentication, role-based access control, intelligent caching, in-process rate limiting, and full request observability.

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
- [Roadmap](#roadmap)

---

## Features

| Area               | Details                                                                               |
|--------------------|---------------------------------------------------------------------------------------|
| **Auth**           | JWT-based authentication & authorization                                              |
| **Access Control** | Role-based permissions — `ADMIN`, `INSTRUCTOR`, `STUDENT`                             |
| **Courses**        | Full CRUD, pagination, filtering, search, seat tracking                               |
| **Enrollments**    | Enroll/unenroll with seat validation and concurrency safety                           |
| **Rate Limiting**  | Role + endpoint-aware in-process token bucket limiting via Bucket4j (single-instance) |
| **Caching**        | Caffeine-backed in-memory caching with custom key generation (single-instance)        |
| **Observability**  | Per-request trace IDs, MDC-based structured logging                                   |
| **Error Handling** | Global exception handler with consistent JSON error responses                         |
| **Validation**     | Input validation and sanitization across all endpoints                                |

---

## Tech Stack

- **Java 21**
- **Spring Boot 4.0.5** — web framework
- **Spring Security** — authentication & authorization
- **Spring Data JPA** — database access layer
- **Spring Cache + Caffeine** — in-memory caching
- **JJWT 0.12.7** — JWT generation and validation
- **Bucket4j** — token bucket rate limiting
- **PostgreSQL** — relational database
- **Spring Boot Actuator** — health and info endpoints (`/actuator/health`, `/actuator/info`)
- **Logstash Logback Encoder** — structured JSON logging
- **Lombok** — boilerplate reduction
- **Maven** — build tool

---

## Architecture

```
src/main/java/com/nishant/coursemanagement/
│
├── config/          # Security, Jackson, cache configuration
├── controller/      # REST endpoints (users, courses, enrollments)
├── service/         # Business logic (split into command + query services)
├── repository/      # Spring Data JPA repositories
├── entity/          # JPA entities (User, Course, Enrollment)
├── dto/             # Request/response models and PageResponse wrapper
├── mapper/          # Entity ↔ DTO conversion
├── exception/       # Custom exceptions, global handler, error response factory
├── security/        # JWT utilities, properties, auth helpers
├── filter/          # OncePerRequestFilter chain (Trace → RateLimit → JWT)
├── cache/           # Custom cache key builders
├── event/           # Domain events (events/) and their cache-eviction listeners (listeners/)
└── util/            # Shared utilities
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

### 1. Clone

```bash
git clone https://github.com/nishant-dd-24/course-management-api.git
cd course-management-api
```

### 2. Configure environment variables

```bash
export SPRING_DATASOURCE_PASSWORD=your_db_password
export JWT_SECRET=your_secret_key_min_32_chars
export JWT_EXPIRATION_SECONDS=3600
```

### 3. Run

```bash
./mvnw spring-boot:run
```

The API starts at `http://localhost:8080`.

---

## API Reference

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
    "role": "STUDENT"
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

| Param    | Type    | Description                    |
|----------|---------|--------------------------------|
| `name`   | string  | Filter by name (partial match) |
| `email`  | string  | Filter by email                |
| `active` | boolean | Filter by active status        |
| `page`   | int     | Page number (0-indexed)        |
| `size`   | int     | Page size (default: 5)         |
| `sort`   | string  | Sort field (default: `id`)     |

---

### Courses

| Method   | Endpoint                     | Role       | Description                                                    |
|----------|------------------------------|------------|----------------------------------------------------------------|
| `POST`   | `/courses`                   | INSTRUCTOR | Create a new course                                            |
| `GET`    | `/courses`                   | ADMIN      | List all courses (paginated, filterable)                       |
| `GET`    | `/courses/{id}`              | ADMIN      | Get any course by ID                                           |
| `GET`    | `/courses/active/{id}`       | Public     | Get an active course by ID                                     |
| `GET`    | `/courses/available-courses` | Public     | Browse all active courses                                      |
| `GET`    | `/courses/my`                | INSTRUCTOR | Get own courses                                                |
| `PUT`    | `/courses/{id}`              | INSTRUCTOR | Full update of a course (title, description, maxSeats)         |
| `PATCH`  | `/courses/{id}`              | INSTRUCTOR | Partial update — any of: title, description, maxSeats (min: 1) |
| `DELETE` | `/courses/{id}`              | INSTRUCTOR | Deactivate a course (soft delete)                              |

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

**Query parameters for `GET /courses`:**

| Param                  | Type    | Description                     |
|------------------------|---------|---------------------------------|
| `title`                | string  | Filter by title (partial match) |
| `active`               | boolean | Filter by active status         |
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
│  Rate Limit Filter   │  Resolves identity, checks token bucket
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

Rate limiting is implemented using **Bucket4j** (token bucket algorithm) with a **Caffeine** cache for bucket storage. Limits are resolved per-request based on two axes:

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

**Identity resolution:** When a valid JWT is present, the bucket key is `email:ROLE:normalizedPath:method`. For unauthenticated requests, the IP address is used as fallback. Path normalization replaces numeric IDs with `{id}` (e.g. `/courses/42` → `/courses/{id}`) to prevent per-ID bucket explosion.

**Response headers:**
```
X-RateLimit-Limit: 20
X-RateLimit-Remaining: 17
Retry-After: 42        ← only on 429 responses
```

---

## Caching

Caching is applied at the **query service layer** using Spring's caching abstraction backed by **Caffeine**. The cache targets read-heavy operations on courses and enrollments.

### Cache key design

Keys are constructed by dedicated utility classes to ensure stability across requests:

```
title=java|active=true|instructorId=1|page=0|size=5|sort=id:ASC
```

This prevents cache misses from input formatting differences (e.g., `%java%` vs `java`) and includes all pagination/sort parameters.

### What is cached

Only **courses** and **users** are cached. Enrollment data is not cached directly, but enrollment mutations (enroll/unenroll) do affect the `enrolledStudents` field on the `Course` entity — so any enrollment change publishes an event that evicts the relevant course cache entries to prevent stale seat counts being served.

### Cache eviction

Eviction is handled via **domain events** published on mutating operations (create, update, deactivate, enroll, unenroll), keeping cache state consistent without manual cache management at the controller level.

### Key configuration

- `sync = true` on all cache definitions to prevent **cache stampede** under concurrent load
- Single global Caffeine config: **10 minute TTL** (write-based expiry), **10,000 max entries**, shared across all cache names

---

## Concurrency Control

Enrollment operations use **pessimistic write locking** (`PESSIMISTIC_WRITE`) via JPA to prevent race conditions when multiple students attempt to enroll in a course simultaneously. The course row is locked at the database level for the duration of the transaction, ensuring that the seat count read, comparison, and increment are atomic.

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT c FROM Course c WHERE c.id = :courseId AND c.isActive = true")
Optional<Course> findByIdForUpdate(@Param("courseId") Long courseId);
```

Note: the `isActive = true` filter means attempting to enroll in an inactive course results in a `404 Not Found` rather than a seat-full error.

A `DataIntegrityViolationException` catch provides an additional safety net at the database level for any duplicate enrollment attempts.

**Trade-off:** Slightly reduced throughput under high concurrent enrollment on the same course, in exchange for strict seat-count correctness.

---

## Observability

### Request tracing

`TraceFilter` runs first in the filter chain:

- Reads `X-Trace-Id` from the incoming request, or generates a new UUID if absent
- Stores the trace ID in MDC under the key `traceId`
- Propagates the trace ID back in the response via `X-Trace-Id`

Because `logstash-logback-encoder` is configured with `includeMdc=true`, the trace ID is automatically included in every JSON log entry for the duration of the request — no manual passing required.

### Structured JSON logging

Logging is configured via `logback-spring.xml` using `LogstashEncoder`. Every log line is emitted as a JSON object. Below are real examples from a login request:

```json
{"timestamp":"2026-04-05T23:41:25.024365205+05:30","@version":"1","message":"Login attempt","logger":"com.nishant.coursemanagement.service.user.UserServiceImpl","thread":"http-nio-8080-exec-2","level":"INFO","level_value":20000,"traceId":"bb2d5b6d-1fbd-4d0f-88a1-c3605aa6a307","action":"LOGIN_ATTEMPT","userId":"22","clientIp":"0:0:0:0:0:0:0:1","app":"course-management"}

{"timestamp":"2026-04-05T23:41:25.087230552+05:30","@version":"1","message":"Login successful","logger":"com.nishant.coursemanagement.service.user.UserServiceImpl","thread":"http-nio-8080-exec-2","level":"INFO","level_value":20000,"traceId":"bb2d5b6d-1fbd-4d0f-88a1-c3605aa6a307","action":"LOGIN_SUCCESS","userId":"22","clientIp":"0:0:0:0:0:0:0:1","app":"course-management"}
```

Key points about the log structure:

- `@version` and `level_value` are added automatically by `LogstashEncoder`
- Timestamps include nanosecond precision and local timezone offset
- `traceId`, `method`, `path`, and `clientIp` are set once by `TraceFilter` at the start of every request and are present on all log entries for the full request lifetime
- MDC fields (`action`, `userId`, `courseId`, etc.) appear as flat top-level keys alongside the standard fields
- The `app` field is injected globally via `customFields` in `logback-spring.xml` and appears on every log entry

Contextual fields (`action`, `userId`, `courseId`, etc.) are added to MDC explicitly using `LogUtil` before each log call and cleared immediately after in a `finally` block. `LogUtil.clear()` only removes the keys it added — it does not call `MDC.clear()` — so `traceId` and the other request-scoped fields set by `TraceFilter` remain intact for the full duration of the request. `TraceFilter` itself calls `MDC.clear()` in its own `finally` block at the very end as the outermost filter.

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

### Why pessimistic over optimistic locking?

Enrollment is a write operation with a clear contention point — multiple students racing to claim the last seat in a course. Optimistic locking would require retry logic on version conflicts, adding complexity and unpredictable latency under load. Pessimistic locking (`PESSIMISTIC_WRITE`) gives a simpler, stronger guarantee by holding the row lock for the duration of the transaction.

**Trade-off:** Slightly reduced concurrency throughput under heavy enrollment load on the same course, in exchange for strict seat-count correctness.

---

### Why in-process rate limiting over Redis?

Bucket4j + Caffeine gives zero-latency limit checks with no external dependency. For a single-instance deployment, this is the right default. The design is structured so that switching to a Redis-backed `ProxyManager` requires only a configuration change.

**Trade-off:** Rate limit state resets on application restart and is not shared across instances.

---

### Why a custom `PageResponse` DTO?

Spring's `Page<T>` serialization is unstable and includes internal fields that change between versions. A custom `PageResponse<T>` gives a stable, documented contract for all paginated responses.

---

### Why split command and query services?

Each domain area (`UserService` / `UserQueryService`, etc.) separates read and write concerns, making caching and locking strategies easier to apply precisely. Query services are cache-eligible; command services manage eviction.

---

## Roadmap

The following are needed before this project could be considered production-ready:

**Correctness & reliability**
- [ ] Unit and integration test coverage
- [ ] Advanced input validation and edge-case hardening

**Operability**
- [ ] Dockerization + `docker-compose` setup
- [ ] Prometheus metrics integration (Actuator already active)
- [ ] Swagger / OpenAPI documentation

**Scalability** *(current in-memory solutions break under horizontal scaling)*
- [ ] Distributed rate limiting via Redis + Bucket4j `ProxyManager`
- [ ] Distributed caching (Caffeine → Redis or two-level)
- [ ] Database indexing and query analysis for larger datasets

**Observability**
- [ ] OpenTelemetry distributed tracing integration
- [ ] API abuse detection and alerting

---

## Author

**Nishantkumar Dwivedi**  
[github.com/nishant-dd-24](https://github.com/nishant-dd-24)