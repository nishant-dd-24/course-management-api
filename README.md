# Course Management API

A production-grade REST API built with **Java 21** and **Spring Boot** for managing users, courses, and enrollments. Applies production-oriented engineering patterns throughout: JWT authentication with server-side revocation, role-based access control, two-level distributed caching, Redis-backed rate limiting, pessimistic concurrency control, and structured observability.

**Production:** [`https://api.nishantdd.dev`](https://api.nishantdd.dev)

---

## Key Features

| Area               | Implementation                                                                                          |
|--------------------|---------------------------------------------------------------------------------------------------------|
| **Auth**           | JWT access + refresh tokens; Redis-backed blacklist and refresh store; explicit logout invalidation     |
| **Access Control** | Role-based permissions — `ADMIN`, `INSTRUCTOR`, `STUDENT`                                               |
| **Rate Limiting**  | Role + endpoint-aware distributed token bucket via Bucket4j + Redis; shared across instances            |
| **Caching**        | Two-level: Caffeine (L1, in-process) + Redis (L2, distributed); cross-instance eviction via Pub/Sub     |
| **Concurrency**    | Pessimistic write locking on enrollment seat claims; validated under real concurrent load               |
| **Observability**  | Per-request trace IDs, MDC-based structured JSON logging, annotation-driven log instrumentation         |
| **Error Handling** | Global exception handler; consistent JSON error schema; dual 401/403 coverage for Spring Security layer |
| **Testing**        | Service-layer unit tests + MockMvc integration tests backed by PostgreSQL + Redis Testcontainers        |

---

## Tech Stack

| Layer         | Technology                                                            |
|---------------|-----------------------------------------------------------------------|
| Runtime       | Java 21                                                               |
| Framework     | Spring Boot 4.0.5                                                     |
| Security      | Spring Security + JJWT 0.12.7                                         |
| Database      | PostgreSQL + Spring Data JPA                                          |
| Cache L1      | Caffeine via Spring Cache                                             |
| Cache L2      | Redis via Spring Cache                                                |
| Rate Limiting | Bucket4j + Bucket4j Redis (Lettuce `ProxyManager`)                    |
| Session Store | Redis (blacklist, refresh tokens, rate limit state, eviction Pub/Sub) |
| Logging       | Logstash Logback Encoder (structured JSON) + Spring AOP               |
| Testing       | JUnit 5 + Mockito + Testcontainers (PostgreSQL 15, Redis 7)           |
| Build         | Maven                                                                 |
| Ops           | Spring Boot Actuator                                                  |

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
├── event/           # Domain events and cache-eviction listeners
├── log/             # @Loggable annotation, LoggingAspect (AOP), LogUtil
└── util/            # Shared utilities (Sanitizer, StringUtil, etc.)
```

### Domain Model

```
ADMIN
  └─[manages]──► User (can update any user, including role)

INSTRUCTOR
  └─[creates & controls]──► Course (title, description, maxSeats, enrolledStudents, isActive)
                                            │      + availableSeats [computed, not persisted]
STUDENT                                     │
  └─[enrolls via]──► Enrollment (student ↔ course, isActive)
```

For a full breakdown of package responsibilities, service command/query split, and design rationale, see [docs/architecture.md](docs/architecture.md).

---

## Quick Start

### Prerequisites

- Java 21+, Maven 3.8+
- PostgreSQL, Redis (or Docker — see step 2)

### 1. Clone

```bash
git clone https://github.com/nishant-dd-24/course-management-api.git
cd course-management-api
```

### 2. Configure environment

```bash
cp .env.example .env
```

Open `.env` and fill in the required values:

| Variable                     | Required | Notes                                       |
|------------------------------|----------|---------------------------------------------|
| `SPRING_DATASOURCE_PASSWORD` | Yes      | Your PostgreSQL password                    |
| `JWT_SECRET`                 | Yes      | Base64-encoded secret, min 32 raw bytes     |
| `APP_ADMIN_PASSWORD`         | Yes      | Password for the bootstrapped admin account |
| All others                   | No       | Defaults are safe for local development     |

Full variable reference: [docs/deployment.md](docs/deployment.md)

### 3. Run with Docker *(recommended)*

**Development (hot reload):**
```bash
docker compose -f docker-compose.dev.yml up --build
```

Mounts local source into the container and enables Spring DevTools for automatic restart on classpath changes.

**Production topology:**
```bash
docker compose up -d
```

Brings up `postgres`, `redis`, `nginx`, and `app-blue`. API is available at `http://localhost:8080`. An `ADMIN` user is bootstrapped automatically on first start.

---

### Manual Setup (without Docker)

### 4. Provision database

```sql
CREATE USER course_user WITH PASSWORD 'your_db_password';
CREATE DATABASE course_db OWNER course_user;
```

### 5. Export environment variables

```bash
set -a && source .env && set +a
```

### 6. Run

```bash
./mvnw spring-boot:run
```

API starts at `http://localhost:8080`. An `ADMIN` user is bootstrapped automatically on first start.

---

## Docker

### Development (hot reload)

```bash
docker compose -f docker-compose.dev.yml up --build
```

Mounts local source into the container; enables Spring DevTools for automatic restart on classpath changes.

### Production topology

```bash
docker compose up -d
```

Brings up `postgres`, `redis`, `nginx`, and `app-blue`. `app-green` is profile-gated and activated during blue-green cutover. Requires Let's Encrypt certs at `/etc/letsencrypt` on the host.

Create a `.env` file in the project root before starting either compose configuration. See [docs/deployment.md](docs/deployment.md) for the full `.env` reference and networking details.

---

## Production Deployment

Hosted on a **DigitalOcean Droplet** behind **Nginx** (TLS terminator + reverse proxy). Deployment uses a **blue-green strategy** with zero-downtime cutover.

```
Client → DNS (api.nishantdd.dev) → DigitalOcean Droplet
  → Nginx container (80/443)
  → Active app container (app-blue or app-green)
  → Spring Boot API (:8080)
```

- Traffic switches only after the incoming container passes `/actuator/health/readiness`
- Rollback is automatic on health-check failure
- HTTPS via Let's Encrypt; HTTP → HTTPS redirect enforced at Nginx

Full details: [docs/deployment.md](docs/deployment.md)

---

## CI/CD

Automated via **GitHub Actions** (`.github/workflows/deploy.yml`).

| Step    | Action                                                      |
|---------|-------------------------------------------------------------|
| Trigger | Push to `main`                                              |
| Test    | `./mvnw clean verify -Preal` (Testcontainers)               |
| Build   | Docker image tagged with `github.sha`, pushed to Docker Hub |
| Deploy  | SCP artifacts to droplet → SSH executes `scripts/deploy.sh` |

The deploy script handles the full blue-green cutover sequence with built-in rollback. See [docs/cicd.md](docs/cicd.md).

---

## API Overview

All endpoints except `/users/login`, `/users/register`, `/users/refresh`, `/actuator/health/**`, `/swagger-ui/**`, and `/v3/api-docs/**` require:

```
Authorization: Bearer <access_token>
```

| Domain      | Endpoints                                                                                                                        |
|-------------|----------------------------------------------------------------------------------------------------------------------------------|
| Auth        | `POST /users/login`, `/users/register`, `/users/refresh`, `/users/logout`                                                        |
| Users       | `GET                                                                                                                             |PUT|PATCH|DELETE /users`, `/users/{id}`, `/users/my` |
| Courses     | `GET                                                                                                                             |POST|PUT|PATCH|DELETE /courses`, `/courses/active`, `/courses/my` |
| Enrollments | `POST                                                                                                                            |DELETE /enrollments/{courseId}`, `GET /enrollments/my`, `GET /enrollments/{id}` |
| Ops         | `GET /actuator/health/**` (public), `/actuator/info` (authenticated), `/swagger-ui/index.html` (public), `/v3/api-docs` (public) |

Full request/response schemas, role requirements, query parameters, and lifecycle notes: [docs/api.md](docs/api.md)

### Swagger / OpenAPI

- Swagger UI: `/swagger-ui/index.html`
- OpenAPI JSON: `/v3/api-docs`
- For protected endpoints in Swagger, authorize with `Bearer <access_token>`

---

## Testing

```bash
# Full suite (unit + integration, Testcontainers)
./mvnw clean verify

# Specific unit test class
./mvnw test -Dtest=CourseUnitTests

# Specific integration test class
./mvnw -Dit.test=CacheFlowIT failsafe:integration-test failsafe:verify
```

Coverage spans service-layer unit tests (mocked dependencies, event publishing assertions) and controller-layer integration tests (real PostgreSQL + Redis via Testcontainers, full request lifecycle including concurrency). See [docs/testing.md](docs/testing.md).

---

## Documentation Index

| File                                                     | Contents                                                   |
|----------------------------------------------------------|------------------------------------------------------------|
| [docs/architecture.md](docs/architecture.md)             | Package structure, service split, domain model             |
| [docs/api.md](docs/api.md)                               | Full endpoint reference, schemas, lifecycle notes          |
| [docs/request-flow.md](docs/request-flow.md)             | Filter chain, per-request processing pipeline              |
| [docs/session-management.md](docs/session-management.md) | JWT design, Redis token store, logout invalidation         |
| [docs/rate-limiting.md](docs/rate-limiting.md)           | Token bucket strategy, role/endpoint limits, Redis backing |
| [docs/caching.md](docs/caching.md)                       | Two-level cache, eviction, cross-instance Pub/Sub sync     |
| [docs/concurrency.md](docs/concurrency.md)               | Pessimistic locking, seat-count correctness, validation    |
| [docs/observability.md](docs/observability.md)           | Trace filter, structured logging, `@Loggable` + `LogUtil`  |
| [docs/error-handling.md](docs/error-handling.md)         | Global handler, error schema, 401/403 dual coverage        |
| [docs/deployment.md](docs/deployment.md)                 | Infrastructure, Docker topology, Nginx, HTTPS, env vars    |
| [docs/cicd.md](docs/cicd.md)                             | GitHub Actions pipeline, deploy script, rollback           |
| [docs/testing.md](docs/testing.md)                       | Unit and integration test structure, coverage, profiles    |
| [docs/design-decisions.md](docs/design-decisions.md)     | Rationale for all major architectural choices              |

---

## Future Scope

- [ ] **Enrollment state machine** — Transition enrollments through `PENDING_PAYMENT → ACTIVE → CANCELLED / EXPIRED` with enforced state guards, timeout jobs for stale pending states, and event emission on each transition
- [ ] **Course lifecycle management** — Introduce `DRAFT → PUBLISHED → ARCHIVED` states; restrict enrollment to `PUBLISHED` courses and propagate state transitions through the cache and access control layers
- [ ] **Payment integration** — Idempotent payment initiation (client-supplied keys), retry-safe processing, and webhook ingestion with at-least-once delivery guarantees and duplicate event rejection
- [ ] **Async event pipeline** — Move post-mutation side effects (notifications, audit trails) off the request thread using an async event bus; isolate handler failures from the primary transaction
- [ ] **Background job processing** — Scheduled cleanup for expired enrollments, retry queues for failed async handlers, and temporal consistency under job overlap (idempotent job design)
- [ ] **Instructor analytics** — Aggregated enrollment and engagement metrics per course; read-optimized with cache-aside strategy and pre-aggregated counters to avoid expensive query-time rollups
- [ ] **Observability uplift** — Prometheus metrics via Actuator (groundwork already in place) + OpenTelemetry distributed tracing for cross-service and cross-layer latency attribution
- [ ] **Resilience hardening** — Structured failure simulation (Redis unavailability, DB latency injection, concurrent request storms) to surface and close gaps in circuit-breaking, fallback, and timeout behavior

---

**Author:** Nishantkumar Dwivedi — [github.com/nishant-dd-24](https://github.com/nishant-dd-24)