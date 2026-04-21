# Design Decisions

> Related: [architecture.md](architecture.md) | [session-management.md](session-management.md) | [caching.md](caching.md) | [concurrency.md](concurrency.md) | [rate-limiting.md](rate-limiting.md) | [observability.md](observability.md)

This document captures the rationale behind the non-obvious architectural choices in the project. Each decision includes the trade-off acknowledged at the time of implementation.

---

## JWT Subject: `userId` over email

JWT tokens use `userId` (the immutable primary key) as the subject claim rather than email.

**Reasoning:**
- Email is mutable — it can be changed by the user or an admin. Using email as a long-lived identity claim creates a window where a token issued before an email change still resolves to the wrong identity.
- `userId` maps directly to the primary key and never changes, so token validation never requires a secondary email lookup.
- Any update flow that changes the email does not affect the validity of in-flight tokens; the token subject remains correct.

---

## Access + Refresh Tokens with Redis Backing (vs. Pure Stateless JWT)

Pure stateless JWTs cannot be revoked before expiry. If an access token is stolen or a user logs out, the token remains valid until its `exp` claim passes — there is no server-side mechanism to invalidate it.

**Access token blacklist (`blacklist:<token>` in Redis):**
- Provides true revocation on logout. The JWT filter checks this key on every authenticated request; a match results in `401` regardless of signature validity.
- TTL is set to the token's remaining lifetime, so stale entries self-clean without a background job.

**Server-tracked refresh tokens (`refresh:<token>` in Redis):**
- Only tokens the server knows about are accepted by the refresh endpoint.
- Provides full server-side session control: individual sessions can be terminated server-side (e.g. forced logout, account deactivation) without waiting for token expiry.
- The refresh endpoint issues a new access token while returning the same refresh token. The refresh token is deleted from Redis only on explicit logout.

**Short-lived access tokens:**
- Limit the exposure window for a compromised token. Even without explicit revocation, the blast radius is bounded by the token's short lifetime.

**Trade-off:** Each authenticated request involves a Redis read (blacklist check), adding a sub-millisecond round-trip. This is the same overhead already present for rate limiting and is acceptable given the correctness guarantees.

---

## Pessimistic Over Optimistic Locking for Enrollment

Enrollment is a write operation with a clear contention point — multiple students racing to claim the last seat in a course.

**Why pessimistic (`PESSIMISTIC_WRITE`):**
- Optimistic locking would require retry logic on version conflicts, adding complexity and unpredictable latency under load (repeated retries under high contention mean high latency for all contenders).
- Pessimistic locking gives a simpler, stronger guarantee: the seat-count read, comparison, and increment are atomic by holding the row lock for the duration of the transaction.
- Failure is immediate and deterministic: a student who arrives when no seats are available gets a clean rejection, not a retry loop.

**Trade-off:** Slightly reduced concurrency throughput under heavy enrollment load on the same course, in exchange for strict seat-count correctness and simpler application code.

---

## Redis-Backed Distributed Rate Limiting

Bucket4j's `LettuceBasedProxyManager<String>` stores all bucket state in Redis, keeping it entirely out of the JVM heap.

**Reasoning:**
- In-process rate limiting (e.g., Bucket4j in local mode) is per-instance. A user who exhausts their quota on instance A can bypass it by routing subsequent requests to instance B.
- Application restarts reset in-process buckets. With Redis backing, limits survive restarts — there is no window where a restart acts as a de facto quota reset.
- Per-instance limits also make horizontal scaling meaningless from an abuse-prevention standpoint.

**Trade-off:** Each rate limit check involves a Redis round-trip. In practice this is negligible given Redis's sub-millisecond latency, and the correctness guarantee is essential for any multi-instance deployment.

---

## Two-Level Caching (Caffeine + Redis) with Pub/Sub Eviction

**Why not Caffeine alone:**
Caffeine is per-instance. In a horizontally scaled deployment, a write on one node leaves stale data in every other node's heap cache. There is no built-in mechanism for cross-instance invalidation.

**Why not Redis alone:**
Redis is correct and shared, but every cache hit requires a network round-trip. For high-read workloads on stable data (course listings, user profiles), this overhead accumulates.

**Composite approach:**
- L1 (Caffeine) serves the hot path at sub-millisecond latency from the JVM heap.
- L2 (Redis) provides the shared ground truth and populates L1 on a miss.
- Writes go to both levels simultaneously.
- Cross-instance L1 invalidation is handled via Redis Pub/Sub on the `cache-evict` channel: `RedisCacheListener` receives eviction events and clears the local Caffeine cache only — L2 is already the shared source of truth.

**Trade-off:** Operational complexity (two cache backends, an eviction Pub/Sub channel). The payoff is in-process read latency for warm caches, combined with consistency guarantees that hold under horizontal scaling.

---

## Custom `PageResponse<T>` DTO

Spring's `Page<T>` serialization is unstable between framework versions and exposes internal implementation fields that change without notice.

**Reasoning:**
- A custom `PageResponse<T>` provides a stable, documented, versioned contract for all paginated API responses.
- Field names and shapes are under application control, not Spring's internal evolution.
- Simplifies consumer-side deserialization — the response shape is predictable and documented.

---

## Command / Query Service Split

Each domain area separates read and write concerns into distinct service components:

- `UserServiceImpl` / `UserQueryService`
- `CourseServiceImpl` / `CourseQueryService`
- `EnrollmentServiceImpl` / `EnrollmentQueryService`

**Reasoning:**
- `@Cacheable` is applied at the query service layer — query services are cache-eligible; command services publish events that trigger listener-driven eviction.
- Locking strategies (pessimistic write lock for enrollment) are applied in command services, isolated from read paths.
- Adding a new cache strategy or changing eviction logic does not touch the command service, and vice versa.
- The split makes each class's responsibility unambiguous and simplifies testing: query service tests verify cache contract; command service tests verify business invariants and event publishing.

---

## Separate `CourseRequest` and `CourseUpdateRequest` DTOs

`POST /courses` (create) and `PUT /courses/{id}` (full update) are structurally similar at the field level but carry different semantic intent.

**Reasoning:**
- A single shared DTO would apply identical validation constraints to both operations, making it impossible to add a create-only or update-only field without introducing conditional logic in the validator.
- `maxSeats` semantics differ between create (defaults to 20 if not provided) and update (retained if `null`, as per the current contract) — these rules belong in separate DTOs, not a shared class with branching logic.
- Future divergence (e.g., making `maxSeats` immutable after creation) is a non-breaking change to `CourseRequest` that does not affect `CourseUpdateRequest`.

---

## Two Logging Mechanisms: `@Loggable` and `LogUtil.log()`

**`@Loggable`** handles the method-entry log point — the single, unconditional emission that every annotated method has. It removes all `try/finally` boilerplate at the call site via AOP.

**`LogUtil.log()`** handles branch-level log points within the same method body — cases where the `action`, message, or context key-values differ depending on the execution path (e.g., duplicate email detected inside `createUser`, password mismatch inside `login`, role change detected inside `updateUser`). These have different `action` keys and contextual MDC fields that cannot be expressed as a single method-entry annotation without encoding branching logic into SpEL expressions, which would be fragile and untestable.

**The two are complementary, not redundant:**
- `@Loggable` handles method-level, unconditional logging.
- `LogUtil.log()` handles intra-method, conditional logging.
- Neither requires `try/finally` at the call site.
- Together, they cover all log points in the codebase without boilerplate.

---

## Why Unit Test the Service Layer

The service layer is where core business invariants live: seat-count correctness under contention, role-gated mutations, and the event-driven cache eviction contract. These are the behaviors most likely to regress and the hardest to isolate if tested only through end-to-end flows.

**Unit tests with mocked dependencies enable:**

- **Fast feedback** — no Spring context, no database, no containers. Tests run in milliseconds.
- **Event contract verification** — mutation paths assert domain event publication directly, which is the cache-eviction trigger. This validates the eviction contract at the service boundary without requiring Redis or Caffeine in scope.
- **Deterministic edge case coverage** — seat exhaustion, duplicate enrollment, unauthorized role mutation, and similar rejection paths are easy to set up and assert against mocked repositories without requiring specific database state.
- **Isolation** — a service-layer failure is immediately obvious as a service failure, not buried in an integration test failure that could be a database issue, a config issue, or a Spring Security issue.

Integration tests then verify that these invariants hold end-to-end through the HTTP stack, with real infrastructure in place.
