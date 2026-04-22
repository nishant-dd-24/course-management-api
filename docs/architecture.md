# Architecture

> Related: [request-flow.md](request-flow.md) | [design-decisions.md](design-decisions.md)

---

## Package Structure

```
src/main/java/com/nishant/coursemanagement/
│
├── config/          # Security, cache, Redis, and rate-limiting configuration
├── controller/      # REST endpoints (users, courses, enrollments)
├── service/         # Business logic (split into command + query services)
├── repository/      # Spring Data JPA repositories
├── entity/          # JPA entities (User, Course, Enrollment)
├── dto/             # Request/response models; SearchRequest DTOs with validation;
│                    #   domain sort enums (UserSortBy, CourseSortBy, EnrollmentSortBy);
│                    #   SortDirection enum; PageResponse wrapper
├── mapper/          # Entity ↔ DTO conversion; PageableMapper (SearchRequest → Pageable)
├── exception/       # Custom exceptions, global handler, error response factory
├── security/        # JWT utilities, properties, auth helpers
├── filter/          # OncePerRequestFilter chain (Trace → RateLimit → JWT)
├── cache/           # CompositeCacheManager, CompositeCache, custom key builders
├── event/           # Domain events (events/) and cache-eviction listeners (listeners/)
├── log/             # Logging infrastructure
│   ├── annotation/  # @Loggable annotation and LogLevel enum
│   ├── aspect/      # LoggingAspect — AOP-driven MDC population and log emission
│   └── util/        # LogUtil — MDC helpers and inline LogUtil.log() for branch-level logs
└── util/            # Shared utilities (Sanitizer, StringUtil, etc.)
```

---

## Domain Model

```
ADMIN
  └─[manages]──► User (can update any user, including their role)

INSTRUCTOR
  └─[creates & controls]──► Course (title, description, maxSeats, enrolledStudents, isActive)
                                            │      + availableSeats [computed, not persisted]
STUDENT                                     │
  └─[enrolls via]──► Enrollment (student ↔ course, isActive)
```

### Entity notes

- `Course.availableSeats` is computed as `maxSeats - enrolledStudents` and is never stored. It is projected onto the response DTO at mapping time.
- `Enrollment`, `User`, and `Course` all use soft delete: deactivation sets `isActive = false` rather than removing the row.
- `Course.enrolledStudents` is incremented/decremented transactionally during enroll/unenroll operations, with a pessimistic write lock held for the duration. See [concurrency.md](concurrency.md).

---

## Service Layer: Command / Query Split

Each domain area is split into a command service and a query service:

| Domain | Command Service | Query Service |
|---|---|---|
| User | `UserServiceImpl` | `UserQueryService` |
| Course | `CourseServiceImpl` | `CourseQueryService` |
| Enrollment | `EnrollmentServiceImpl` | `EnrollmentQueryService` |

**Command services** handle all mutating operations (create, update, deactivate, enroll, unenroll). They publish domain events after each mutation, which trigger cache eviction in the query layer.

**Query services** handle read operations and use `@Cacheable` where applicable. Cache eviction is event-driven via listeners that call `CompositeCache.evict()/clear()`, keeping HTTP and command logic decoupled from cache invalidation mechanics.

This separation makes caching and locking strategies easier to apply precisely — query services are cache-eligible; command services manage eviction. See [design-decisions.md](design-decisions.md) for the rationale behind this split.

---

## Request Processing Pipeline

Every HTTP request passes through a three-stage filter chain before reaching a controller:

```
Trace Filter → Rate Limit Filter → JWT Filter → Controller
```

For per-filter responsibilities and the full pipeline diagram, see [request-flow.md](request-flow.md).

---

## Cache Architecture

Caching uses a custom `CompositeCacheManager` that wraps both a Caffeine (`L1`) and a Redis (`L2`) backend. The `CompositeCache` implementation coordinates reads and writes across both levels.

For the full read/write flow, eviction strategy, and cross-instance invalidation via Redis Pub/Sub, see [caching.md](caching.md).

---

## Event-Driven Cache Eviction

Mutating operations do not call eviction methods directly. Instead, command services publish domain events:

| Event | Published by |
|---|---|
| `UserUpdatedEvent` | `UserServiceImpl` on update/patch/deactivate |
| `CourseUpdatedEvent` | `CourseServiceImpl` on update/patch/deactivate |
| `EnrollmentChangedEvent` | `EnrollmentServiceImpl` on enroll/unenroll |

Listeners in `event/listeners/` subscribe to these events and call `CompositeCache.evict()` for the relevant cache names and keys. This decouples business logic from cache management.

`EnrollmentChangedEvent` also triggers course cache eviction because enrollment mutations modify `enrolledStudents` on the `Course` entity, which would cause stale `availableSeats` values to be served if the course cache were not invalidated.

---

## Infrastructure Topology (Production)

```
Client
  → DNS (api.nishantdd.dev)
  → DigitalOcean Droplet (Linux VPS)
  → Nginx container (ports 80/443)
  → Active app container (app-blue or app-green)
  → Spring Boot API (:8080)
  → PostgreSQL container
  → Redis container
```

All containers run on `app-network` (Docker bridge). Inter-service communication uses Docker DNS names (`postgres`, `redis`, `app-blue`, `app-green`). See [deployment.md](deployment.md) for the full infrastructure and blue-green deployment setup.
