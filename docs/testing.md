# Testing

> Related: [concurrency.md](concurrency.md) | [caching.md](caching.md) | [cicd.md](cicd.md)

---

## Overview

The test suite has two distinct layers: **service-layer unit tests** with mocked dependencies, and **controller-layer integration tests** backed by real PostgreSQL and Redis via Testcontainers. Both layers share base classes that eliminate setup boilerplate from individual test classes.

---

## Unit Tests

Unit tests cover all three core command services. They use Mockito mocks for repositories, event publishers, and auth utilities — no Spring context, no database, no Redis.

### Coverage

**`CourseUnitTests`**
- Create, update, patch, deactivate
- Ownership validation (non-owner rejection)
- Domain event publishing on all mutations

**`UserUnitTests`**
- Create (including inactive-user reactivation path)
- Update, patch (including `patchMe` flows for self-service mutations)
- Password change
- ADMIN-guarded role mutation paths
- Domain event publishing on mutations

**`EnrollmentUnitTests`**
- Enroll (new enrollment and reactivation of previously inactive enrollment)
- Unenroll
- Seat availability and boundary conditions: zero seats, at-capacity
- Duplicate enrollment guard
- `DataIntegrityViolationException` safety-net handling
- Domain event publishing on enroll/unenroll

### Base class: `BaseUnitTest`

All unit test classes extend `BaseUnitTest`, which provides:

- Pre-configured Mockito mocks for `AuthUtil`, `ApplicationEventPublisher`, and `ExceptionUtil`
- Fixture builders: `buildUser(Long id)` and `buildUser(Long id, Role role)` shared across all three test classes
- Shared assertion helpers: `captureEvent`, `captureEvents`, `verifyNoEventPublished`, `mockCurrentUser`, `mockNotFoundException`, `mockBadRequestException`

### What unit tests assert

Unit tests verify three categories:

1. **Repository interactions** — that the correct repository methods are called with the correct arguments (e.g. `save`, `existsByEmail`, `findByEmail`).
2. **Event publishing** — that domain events (`UserUpdatedEvent`, `CourseUpdatedEvent`, `EnrollmentChangedEvent`) are published after mutating operations. These events are the triggers for cache eviction; asserting their publication at the service boundary validates the eviction contract without requiring cache infrastructure in scope.
3. **Edge cases and failure paths** — not-found lookups, seat exhaustion, duplicate enrollments, unauthorized role changes, and other rejection scenarios.

---

## Integration Tests

Integration tests run with a full `SpringBootTest` context plus PostgreSQL 15 and Redis 7 containers managed by Testcontainers. All HTTP requests go through MockMvc with full Spring Security in effect.

### Coverage

**`UserFlowIT`**
- Registration (including email reactivation path)
- Login: success, wrong password, inactive user
- Token issuance: `accessToken` + `refreshToken` on login response
- Token refresh: new access token issued, same refresh token returned
- Refresh edge cases: expired/unknown/tampered/empty refresh token rejected
- Logout: access token blacklisted, refresh token deleted
- Post-logout rejection: old tokens rejected with `401`
- Cross-user logout isolation: logout does not affect other users' sessions
- Full session lifecycle: login → access → refresh → continued access → logout → rejection
- Malformed `Authorization` header handling
- Role-based access control (RBAC) at the HTTP boundary
- Paginated listing with correct `PageResponse` shape
- Filtering by name, email, isActive — including combined filters
- Full update, partial update, password change, account deactivation

**`CourseFlowIT`**
- Course creation (INSTRUCTOR role only)
- Ownership-guarded update and patch (non-owner returns `404`)
- ADMIN vs. non-ADMIN retrieval paths
- Own courses listing (`/courses/my`)
- Active-only course browsing with pagination
- Filtering by title, instructorId, isActive
- Soft deactivation

**`EnrollmentFlowIT`**
- Enroll and unenroll
- Duplicate enrollment detection (`409 Conflict`)
- Full-course rejection
- Enrollment reactivation path
- Own enrollment listing (STUDENT)
- Course enrollment listing (INSTRUCTOR)
- Role-invalid access (`403 Forbidden`)
- Concurrent enrollment validation (see [concurrency.md](concurrency.md))

**`CacheFlowIT`**
- L1 (Caffeine) cache hit behavior — second request does not hit the database
- L1 miss → L2 (Redis) hit → L1 population
- Write/update-driven eviction — stale data not served after mutation
- Stampede protection (`@Cacheable(sync = true)`) — concurrent requests do not produce duplicate DB hits
- Non-cached behavior for `page != 0` requests
- Distinct cache keys for different filter combinations
- Updated data visibility after eviction

### Base class: `BaseIntegrationTest`

All integration test classes extend `BaseIntegrationTest`, which provides:

- A `MockMvc` instance configured with full Spring Security support
- Database reset and seed fixtures before each test: a `testUser` (INSTRUCTOR), a `testCourse`, and a pre-generated access token
- Role-switching helpers: `setStudentToken()`, `setInstructorToken()`, `setAdminToken()`
- Entity builders: `buildUser()`, `buildCourse()`, `buildEnrollment()`, `buildThisManyUsers()` (for pagination fixture setup)
- `toJson()` serialization and a reusable `auth()` `RequestPostProcessor`

`BaseIntegrationTest` extends `TestContainerConfig`, which starts the PostgreSQL and Redis containers and injects their connection properties via `@DynamicPropertySource`.

---

## Test Profiles and Maven Configuration

### Spring profiles

**`test` profile** (`application-test.properties`):
- JWT fallback secret (tests run without setting `JWT_SECRET` in the environment)
- PostgreSQL dialect + `create-drop` schema mode for the test application context
- Testcontainers start PostgreSQL 15 and Redis 7; connection properties are injected dynamically
- `RateLimitFilter` is active (it is only disabled under the `mock-redis` profile)

**`mock-redis` profile** (`application-mock-redis.properties`):
- Caching disabled (`spring.cache.type=none`)
- Redis repositories disabled
- Intended for alternate local wiring; **not** used by the default integration suite

### Maven profiles

| Maven profile | Active Spring profiles | Use case |
|---|---|---|
| `default` | `test,mock-redis` | Fast local runs without a real Redis |
| `fast` | `test,mock-redis` | Same as default |
| `real` | `test` | Full Testcontainers suite; used by CI/CD |

The integration tests pin `@ActiveProfiles("test")`, so the verified path in CI is always the Testcontainers-backed PostgreSQL + Redis stack.

---

## Running Tests

```bash
# Full suite (unit + integration, Testcontainers)
./mvnw clean verify

# Full suite explicitly using the 'real' profile (used in CI)
./mvnw clean verify -Preal

# Specific unit test class
./mvnw test -Dtest=CourseUnitTests
./mvnw test -Dtest=UserUnitTests
./mvnw test -Dtest=EnrollmentUnitTests

# Specific integration test class
./mvnw -Dit.test=CourseFlowIT failsafe:integration-test failsafe:verify
./mvnw -Dit.test=UserFlowIT failsafe:integration-test failsafe:verify
./mvnw -Dit.test=EnrollmentFlowIT failsafe:integration-test failsafe:verify
./mvnw -Dit.test=CacheFlowIT failsafe:integration-test failsafe:verify
```
