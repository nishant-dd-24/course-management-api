# Course Management API

A production-grade course management platform built with **Java 21**, **Spring Boot**, and a **React + Vite** frontend. The backend exposes a REST API for users, courses, and enrollments with JWT authentication, role-based access control, distributed caching, Redis-backed rate limiting, pessimistic concurrency control, and structured observability.

> **Deployment Status:** The deployment infrastructure is fully implemented, and the repository contains a complete production-ready deployment pipeline. However, the production deployment is currently **inactive** and not hosted. Any URLs referenced in this documentation (e.g., `api.nishantdd.dev`) are for configuration examples and historical context only.

---

## Key Features

| Area | Implementation |
|---|---|
| **Auth** | JWT access + refresh tokens; Redis-backed blacklist and refresh store; explicit logout invalidation |
| **Access Control** | Role-based permissions — `ADMIN`, `INSTRUCTOR`, `STUDENT` |
| **Rate Limiting** | Role + endpoint-aware distributed token bucket via Bucket4j + Redis |
| **Caching** | Two-level: Caffeine (L1) + Redis (L2); cross-instance eviction via Pub/Sub |
| **Concurrency** | Pessimistic write locking on enrollment seat claims |
| **Observability** | Per-request trace IDs, MDC-based structured JSON logging, `@Loggable` instrumentation |
| **Error Handling** | Global exception handler; consistent JSON error schema; dual 401/403 coverage |
| **Frontend** | React 19 SPA with role-aware dashboard, courses, enrollments, and admin user management |
| **Testing** | Service-layer unit tests + MockMvc integration tests with PostgreSQL + Redis Testcontainers |

---

## Tech Stack

| Layer | Technology |
|---|---|
| **Backend runtime** | Java 21, Spring Boot 4.0.5 |
| **Backend security** | Spring Security + JJWT 0.12.7 |
| **Database** | PostgreSQL + Spring Data JPA |
| **Cache L1** | Caffeine via Spring Cache |
| **Cache L2** | Redis via Spring Cache |
| **Rate Limiting** | Bucket4j + Bucket4j Redis (Lettuce `ProxyManager`) |
| **Session Store** | Redis (blacklist, refresh tokens, rate limit state, eviction Pub/Sub) |
| **Logging** | Logstash Logback Encoder + Spring AOP |
| **Testing** | JUnit 5 + Mockito + Testcontainers (PostgreSQL 15, Redis 7) |
| **Build (backend)** | Maven |
| **Frontend** | React 19, Vite 8, Tailwind CSS 4 |
| **Ops** | Spring Boot Actuator, Docker Compose, Nginx, GitHub Actions |

---

## Repository Structure

```
course-management-api/
├── src/                      # Spring Boot backend
├── frontend/                 # React + Vite SPA
├── docs/                     # Technical documentation
├── scripts/                  # Infrastructure bootstrap and deployment scripts
├── docker-compose.yml        # Production stack (Postgres, Redis, Nginx, app-blue/green)
├── docker-compose.dev.yml    # Local dev stack with hot reload
├── Dockerfile                # Production API image
├── Dockerfile.dev            # Dev API image
├── nginx.conf                # TLS termination, API proxy, static frontend
├── .env.example              # Environment variable template
└── pom.xml
```

### Backend package layout

```
src/main/java/com/nishant/coursemanagement/
├── bootstrap/       # Admin user bootstrap on startup
├── config/          # Security, cache, Redis, rate limiting, OpenAPI
├── controller/      # REST endpoints (users, courses, enrollments)
├── service/         # Business logic (command + query services)
├── repository/      # Spring Data JPA repositories
├── entity/          # JPA entities (User, Course, Enrollment)
├── dto/             # Request/response models and PageResponse wrapper
├── mapper/          # Entity ↔ DTO conversion
├── exception/       # Custom exceptions, global handler, error response factory
├── security/        # JWT utilities, properties, auth helpers
├── filter/          # OncePerRequestFilter chain (Trace → RateLimit → JWT)
├── cache/           # CompositeCacheManager, CompositeCache, key builders
├── event/           # Domain events and cache-eviction listeners
├── log/             # @Loggable annotation, LoggingAspect (AOP), LogUtil
└── util/            # Shared utilities (Sanitizer, StringUtil, etc.)
```

For architecture details, see [docs/architecture.md](docs/architecture.md).

---

## Quick Start

### Prerequisites

- **Backend:** Java 21+, Maven 3.8+
- **Frontend:** Node.js 22+ (matches CI), npm
- **Infrastructure:** PostgreSQL 15 and Redis 7 — run locally or via Docker Compose

### 1. Clone

```bash
git clone https://github.com/nishant-dd-24/course-management-api.git
cd course-management-api
```

### 2. Configure environment

```bash
cp .env.example .env
```

Open `.env` and set the required values:

| Variable | Required | Notes |
|---|---|---|
| `SPRING_DATASOURCE_USERNAME` | No | PostgreSQL username; defaults to `course_user` |
| `SPRING_DATASOURCE_PASSWORD` | Yes | PostgreSQL password |
| `JWT_SECRET` | Yes | Base64-encoded secret, min 32 raw bytes (`openssl rand -base64 32`) |
| `APP_ADMIN_PASSWORD` | Optional | Password for the bootstrapped admin account; defaults to `12345678` — override for any non-local deployment |
| All others | No | Safe defaults for local development |

Full variable reference: [docs/deployment.md](docs/deployment.md)

### 3. Run with Docker (recommended)

**Development (API hot reload):**

```bash
docker compose -f docker-compose.dev.yml up --build
```

Starts PostgreSQL, Redis, and the Spring Boot app with source mounted for DevTools restart. API available at `http://localhost:8080`.

**Production topology (VPS-style stack):**

```bash
docker compose up -d
```

Starts `postgres`, `redis`, `nginx`, and `app-blue`. The `scripts/bootstrap.sh` script automates the initial Let's Encrypt certificate setup. Traffic is served on ports 80/443 — not on `localhost:8080`. See [docs/deployment.md](docs/deployment.md).

### 4. Run the frontend locally

With the API running on port 8080:

```bash
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173`. The dev server uses `VITE_API_BASE_URL=http://localhost:8080` from `frontend/.env.development`. CORS is preconfigured for `http://localhost:5173`.

See [frontend/README.md](frontend/README.md) for page-level details and build instructions.

---

## Manual Setup (without Docker)

### Provision database

```sql
CREATE USER course_user WITH PASSWORD 'your_db_password';
CREATE DATABASE course_db OWNER course_user;
```

Ensure PostgreSQL and Redis are running locally.

### Export environment variables

```bash
set -a && source .env && set +a
```

For a non-Docker run, also set connection hosts if needed:

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/course_db
export SPRING_DATASOURCE_USERNAME=course_user
export SPRING_DATA_REDIS_HOST=localhost
export SPRING_DATA_REDIS_PORT=6379
```

### Start the API

```bash
./mvnw spring-boot:run
```

API starts at `http://localhost:8080`. An `ADMIN` user is bootstrapped automatically on first start using the configured admin credentials.

---

## Production Deployment Architecture

The project includes a deployment architecture designed for hosting on a VPS (previously a **DigitalOcean Droplet**) behind **Nginx** (TLS terminator + reverse proxy). Deployment uses a **blue-green strategy** with zero-downtime cutover.

```
Browser → app.nishantdd.dev → Nginx (static React SPA)
Client  → api.nishantdd.dev  → Nginx → Active app container (app-blue or app-green) → Spring Boot API
                                                                                    → PostgreSQL
                                                                                    → Redis
```

- Traffic switches only after the incoming container passes `/actuator/health/readiness`
- Rollback is automatic on health-check failure
- HTTPS via Let's Encrypt; HTTP → HTTPS redirect enforced at Nginx
- The CI pipeline builds the frontend and copies `frontend/dist` to the host server alongside infra files

Full details: [docs/deployment.md](docs/deployment.md)

---

## CI/CD

Automated via **GitHub Actions** (workflow currently disabled as `.github/workflows/deploy.yml.removed` since the production environment is inactive).

| Step | Action |
|---|---|
| Trigger | Push to `main` |
| Test | `./mvnw clean verify -Preal` (Testcontainers) |
| Build | Frontend (`npm ci && npm run build`) + Docker image tagged with `github.sha`, pushed to Docker Hub |
| Deploy | SCP artifacts to host server → SSH executes `scripts/deploy.sh` |

The deploy script handles the full blue-green cutover sequence with built-in rollback. See [docs/cicd.md](docs/cicd.md).

---

## API Overview

All endpoints except `/users/login`, `/users/register`, `/users/refresh`, `/actuator/health/**`, `/swagger-ui/**`, `/v3/api-docs/**`, `/`, and `/docs` require:

```
Authorization: Bearer <access_token>
```

| Domain | Endpoints |
|---|---|
| Auth | `POST /users/login`, `/users/register`, `/users/refresh`, `/users/logout` |
| Users | `GET\|PUT\|PATCH\|DELETE /users`, `/users/{id}`, `/users/my`, `POST /users/my/change-password` |
| Courses | `GET\|POST\|PUT\|PATCH\|DELETE /courses`, `/courses/active`, `/courses/my`, `POST /courses/{id}/activate` |
| Enrollments | `POST\|DELETE /enrollments/{courseId}`, `GET /enrollments/my`, `GET /enrollments/{id}` |
| Ops | `GET /actuator/health/**` (public), `/actuator/info` (authenticated), `/swagger-ui/index.html` (public), `/v3/api-docs` (public) |

Full request/response schemas, role requirements, query parameters, and lifecycle notes: [docs/api.md](docs/api.md)

### Swagger / OpenAPI

- Swagger UI: `/swagger-ui/index.html` (also reachable via `/` and `/docs`)
- OpenAPI JSON: `/v3/api-docs`
- For protected endpoints in Swagger, authorize with `Bearer <access_token>`

---

## Testing

```bash
# Full suite (unit + integration, Testcontainers)
./mvnw clean verify

# CI-equivalent profile
./mvnw clean verify -Preal

# Specific unit test class
./mvnw test -Dtest=CourseUnitTests

# Specific integration test class
./mvnw -Dit.test=CacheFlowIT failsafe:integration-test failsafe:verify
```

Coverage spans service-layer unit tests and controller-layer integration tests (real PostgreSQL + Redis via Testcontainers). See [docs/testing.md](docs/testing.md).

---

## Troubleshooting

| Symptom | Likely cause | Fix |
|---|---|---|
| App fails to start with JWT error | `JWT_SECRET` not set | Add a Base64 secret to `.env` (`openssl rand -base64 32`) |
| Frontend cannot reach API | API not running or wrong base URL | Confirm API on `:8080`; check `frontend/.env.development` |
| CORS errors in browser | Origin not allowed | Backend allows `http://localhost:5173` and `https://app.nishantdd.dev` only |
| `docker compose up` fails on TLS | Missing Let's Encrypt certs | Use `docker-compose.dev.yml` locally, or provision certs on the host |
| Integration tests hang or fail | Docker not available for Testcontainers | Ensure Docker daemon is running; use `-Preal` for full Redis coverage |

---

## Documentation Index

| File | Contents |
|---|---|
| [docs/architecture.md](docs/architecture.md) | Package structure, service split, domain model |
| [docs/api.md](docs/api.md) | Full endpoint reference, schemas, lifecycle notes |
| [docs/request-flow.md](docs/request-flow.md) | Filter chain, per-request processing pipeline |
| [docs/session-management.md](docs/session-management.md) | JWT design, Redis token store, logout invalidation |
| [docs/rate-limiting.md](docs/rate-limiting.md) | Token bucket strategy, role/endpoint limits |
| [docs/caching.md](docs/caching.md) | Two-level cache, eviction, cross-instance Pub/Sub sync |
| [docs/concurrency.md](docs/concurrency.md) | Pessimistic locking, seat-count correctness |
| [docs/observability.md](docs/observability.md) | Trace filter, structured logging, `@Loggable` |
| [docs/error-handling.md](docs/error-handling.md) | Global handler, error schema, 401/403 dual coverage |
| [docs/deployment.md](docs/deployment.md) | Infrastructure, Docker topology, Nginx, HTTPS, env vars |
| [docs/cicd.md](docs/cicd.md) | GitHub Actions pipeline, deploy script, rollback |
| [docs/testing.md](docs/testing.md) | Unit and integration test structure, coverage, profiles |
| [docs/design-decisions.md](docs/design-decisions.md) | Rationale for major architectural choices |
| [frontend/README.md](frontend/README.md) | Frontend setup, pages, auth flow, build |

---

## Future Scope

- [ ] **Enrollment state machine** — `PENDING_PAYMENT → ACTIVE → CANCELLED / EXPIRED` with enforced state guards
- [ ] **Course lifecycle management** — `DRAFT → PUBLISHED → ARCHIVED` states with enrollment restrictions
- [ ] **Payment integration** — Idempotent payment initiation and webhook ingestion
- [ ] **Async event pipeline** — Post-mutation side effects off the request thread
- [ ] **Background job processing** — Scheduled cleanup and retry queues
- [ ] **Instructor analytics** — Aggregated enrollment metrics per course
- [ ] **Observability uplift** — Prometheus metrics + OpenTelemetry tracing
- [ ] **Frontend test coverage** — Component and integration tests for the React SPA
- [ ] **Resilience hardening** — Failure simulation for Redis/DB outages and request storms

---

**Author:** Nishantkumar Dwivedi — [github.com/nishant-dd-24](https://github.com/nishant-dd-24)

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.
