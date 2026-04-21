# Caching

> Related: [architecture.md](architecture.md) | [design-decisions.md](design-decisions.md)

---

## Overview

Caching is applied at the **query service layer** using Spring's caching abstraction. The implementation uses a custom **two-level `CompositeCacheManager`**: Caffeine as L1 (in-process, sub-millisecond) and Redis as L2 (distributed, shared across instances).

Only **courses** and **users** are cached. Enrollment data is not cached directly, but enrollment mutations affect the `enrolledStudents` field on `Course`, so enrollment events trigger course cache eviction to prevent stale `availableSeats` values being served.

---

## Read / Write Flow

```
Read:
  L1 hit → return immediately (sub-millisecond, no network)
  L1 miss → check L2
    L2 hit → populate L1, return
    L2 miss → load from DB, write to both L1 and L2, return

Write (any mutating operation):
  Evict the relevant entry from both L1 and L2 simultaneously.
  Publish cache-evict event to Redis Pub/Sub channel for cross-instance L1 invalidation.
```

The database is only reached when neither cache level has a valid entry.

---

## Cache Stampede Protection

`@Cacheable(sync = true)` is set on all cache definitions. Under concurrent load, only one thread computes the value for a given key; all other threads wait on that result rather than each racing to the database. This is enforced at the `CompositeCache` level and applies to both L1 and L2 reads. The integration test `CacheFlowIT` validates this behavior directly. See [testing.md](testing.md).

---

## Cache Key Design

Keys are constructed by dedicated utility classes to ensure stability across requests. A representative course-list key:

```
title=java|isActive=true|instructorId=1|page=0|size=5|sort=id:ASC
```

All pagination and sort parameters are included in the key. Input normalization is applied before key construction (e.g., leading/trailing wildcards are stripped before the key is built), preventing cache misses caused by equivalent queries with different raw input formatting.

---

## Caching Conditions

Cache conditions on list endpoints are explicit in `@Cacheable`:

- `page == 0`
- `size <= 50`

Requests for deeper pages or large page sizes bypass the cache and go directly to the database. This bounds the cache memory footprint to the most frequently accessed result sets.

---

## Cache Eviction via Domain Events

Eviction is event-driven. Command services publish domain events after mutating operations. Listeners in `event/listeners/` subscribe to these events and call `CompositeCache.evict()`.

| Event | Cache evicted |
|---|---|
| `UserUpdatedEvent` | User cache entries for the affected user |
| `CourseUpdatedEvent` | Course cache entries for the affected course |
| `EnrollmentChangedEvent` | Course cache entries (seat count changed) |

This keeps cache invalidation decoupled from business logic — the command service does not call eviction methods directly.

---

## Distributed L1 Invalidation (Redis Pub/Sub)

L2 (Redis) is inherently consistent — all instances share the same store, so a write on one node is immediately visible to all. The problem is L1: each instance holds its own Caffeine heap cache. Evicting a key on one instance does nothing for the other instances.

This is resolved via Redis Pub/Sub on the `cache-evict` channel:

1. A mutating operation calls `CompositeCache.evict()` on the local instance — this clears the key from both L1 and L2 immediately.
2. A `CacheEvictEvent` (carrying `cacheName`, `key`, and an `evictAll` flag for full-cache clears) is published to the `cache-evict` Redis channel.
3. All other instances have a `RedisCacheListener` subscribed to that channel. On receipt, each subscriber evicts the same key (or clears the entire cache if `evictAll = true`) from its local Caffeine L1 only — L2 is already the shared source of truth and requires no cross-instance notification.

The cross-instance invalidation is asynchronous and zero-cost on the critical write path. No instance serves a stale in-process entry after a mutation propagates.

---

## Configuration

### L1 (Caffeine)

| Parameter | Value |
|---|---|
| TTL | 10 minutes (write-based expiry) |
| Max entries | 10,000 (shared across all cache names) |

### L2 (Redis)

| Parameter | Value |
|---|---|
| TTL | 10 minutes (matched to L1) |
| Serializer | `GenericJacksonJsonRedisSerializer` with default typing enabled |

Default typing on the Redis serializer allows polymorphic types to round-trip correctly without requiring explicit type hints at each cache call site. Values are stored as typed JSON objects and deserialized back to their original Java types on retrieval.

---

## Design Rationale

For the full analysis of why two levels are used rather than Redis-only or Caffeine-only, see [design-decisions.md](design-decisions.md). In summary: Caffeine alone does not survive horizontal scaling (each instance has its own heap); Redis alone is correct but adds a network round-trip to every cache hit. The composite approach gets sub-millisecond L1 latency for warm caches and shared correctness guarantees via L2, with cross-instance L1 coherence maintained by the Pub/Sub eviction channel.
