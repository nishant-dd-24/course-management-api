# Rate Limiting

> Related: [request-flow.md](request-flow.md) | [design-decisions.md](design-decisions.md)

---

## Overview

Rate limiting is implemented using **Bucket4j** (token bucket algorithm) backed by a **Redis `ProxyManager<String>`** via Lettuce (`LettuceBasedProxyManager`). All bucket state is stored in Redis, making rate limit enforcement shared across all application instances and persistent across restarts.

The `RateLimitFilter` is active for all Spring profiles except `mock-redis`. It sits between the `TraceFilter` and the `JwtFilter` in the filter chain — see [request-flow.md](request-flow.md).

---

## Limit Resolution

Limits are resolved per-request on two orthogonal axes, and the **more restrictive** of the two applies:

### Role-based limits (per minute)

| Role | Requests/min |
|---|---|
| ADMIN | 100 |
| INSTRUCTOR | 50 |
| STUDENT | 20 |
| Anonymous (no valid JWT) | 10 |

### Endpoint-specific limits (per minute)

| Endpoint pattern | Limit |
|---|---|
| `POST /users/login` | 5 |
| `POST /enrollments/**` | 10 |
| `GET /courses/**` | 100 |
| All others | 20 |

---

## Identity Resolution and Bucket Keys

When a valid JWT is present, the bucket key is constructed as:

```
userId:ROLE:normalizedPath:method
```

For unauthenticated requests, the client IP address is used as the identity component:

```
<clientIp>:normalizedPath:method
```

This same IP-based format is also used when a token is present but cannot be parsed.

**Path normalization:** Numeric path segments are replaced with the placeholder `{id}` before constructing the key. For example, `/courses/42` normalizes to `/courses/{id}`. This prevents bucket explosion — without normalization, each unique resource ID would create a separate bucket entry per user, defeating the per-user per-endpoint intent and filling Redis with unbounded keys.

---

## Cross-Instance Consistency

Because buckets are stored in Redis, rate limit state is fully shared:

- A user who exhausts their quota on instance A cannot bypass the limit by routing subsequent requests to instance B.
- Application restarts do not reset any quota. The bucket state in Redis survives the JVM restart, eliminating the window where a restart acted as a de facto quota reset.

This is a hard requirement for any horizontally scaled deployment. See [design-decisions.md](design-decisions.md) for the trade-off analysis.

---

## Response Headers

All responses include rate limit headers:

```
X-RateLimit-Limit: 20
X-RateLimit-Remaining: 17
```

Responses that trigger the limit (`429 Too Many Requests`) additionally include:

```
Retry-After: 42
```

The `Retry-After` value is the number of seconds until the next token becomes available in the bucket.

---

## 429 Response

When the bucket is exhausted, the filter short-circuits the request and returns:

```json
{
  "traceId": "...",
  "path": "/users/login",
  "method": "POST",
  "status": 429,
  "message": "Too many requests. Please try again later.",
  "errorCode": "RATE_LIMIT_EXCEEDED",
  "timestamp": "..."
}
```

The response uses the same JSON error schema as all other error types. See [error-handling.md](error-handling.md).
