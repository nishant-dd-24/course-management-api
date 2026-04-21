# Concurrency Control

> Related: [api.md](api.md) | [testing.md](testing.md) | [design-decisions.md](design-decisions.md)

---

## Problem

Multiple students can attempt to enroll in a course simultaneously. Without coordination, two concurrent transactions can both read the same `enrolledStudents` count, both conclude a seat is available, both increment the count, and both succeed — resulting in more enrollments than the course's `maxSeats` allows.

---

## Implementation: Pessimistic Write Locking

Enrollment operations use **pessimistic write locking** (`PESSIMISTIC_WRITE`) via JPA. The course row is locked at the database level for the duration of the transaction, making the read-compare-increment sequence atomic.

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT c FROM Course c WHERE c.id = :courseId AND c.isActive = true")
Optional<Course> findByIdForUpdate(@Param("courseId") Long courseId);
```

The sequence within a single enrollment transaction:

1. Acquire `PESSIMISTIC_WRITE` lock on the course row.
2. Read current `enrolledStudents` and `maxSeats`.
3. Validate that `enrolledStudents < maxSeats`.
4. Increment `enrolledStudents` and persist the enrollment record.
5. Release the lock on transaction commit.

Any concurrent transaction attempting to lock the same row waits until the first transaction completes. This guarantees that seat-count reads are always consistent with the state after the preceding write.

**Inactive course behavior:** The `isActive = true` filter in the lock query means that attempting to enroll in an inactive course returns `404 Not Found` rather than a seat-availability error. The lock is never acquired, so no row-level contention occurs for inactive courses.

**`DataIntegrityViolationException` safety net:** The global exception handler catches `DataIntegrityViolationException` as a fallback for any duplicate enrollment attempts that reach the database under a race condition. This is a secondary safeguard; the pessimistic lock is the primary mechanism.

---

## Trade-off

Slightly reduced throughput under high concurrent enrollment on the same course, in exchange for strict seat-count correctness. Under realistic workloads — concurrent enrollment bursts on the same specific course — this trade-off is favorable: seat-count integrity is a hard correctness requirement, and lock contention only manifests when multiple users target the same course simultaneously.

For the full comparison against optimistic locking, see [design-decisions.md](design-decisions.md).

---

## Concurrency Validation

The correctness guarantee is verified by integration tests in `EnrollmentFlowIT` under the `ConcurrencyTests` nested class. Each test uses a real PostgreSQL Testcontainer with actual JPA pessimistic locking in effect.

**Test setup:**
- A course is created with `maxSeats = 2`.
- 5 threads are spawned via `ExecutorService`.
- All threads are held behind a `CountDownLatch` until each is ready, then released simultaneously.
- Each thread sends an enrollment request for the same course.

**Test 1 — `shouldAllowOnlyLimitedEnrollments_whenConcurrentRequests`:**
- Asserts that exactly 2 requests return `2xx`.
- Asserts that the active enrollment count in the database equals 2.
- Asserts that `enrolledStudents` on the course entity reflects the correct value of 2.

**Test 2 — `shouldRejectExcessEnrollments_whenConcurrentRequests`:**
- Asserts that exactly 2 requests return `2xx`.
- Asserts that all remaining requests return `4xx` (seat exhausted or conflict).

Both tests run end-to-end through the full HTTP stack (MockMvc → filter chain → service → repository → PostgreSQL), providing confidence that the seat-boundary guarantee holds under actual concurrent load rather than being a theoretical property of the locking strategy.

See [testing.md](testing.md) for the full integration test structure.
