# Request Flow

> Related: [session-management.md](session-management.md) | [rate-limiting.md](rate-limiting.md) | [observability.md](observability.md)

---

## Filter Chain Overview

Every HTTP request passes through a three-stage `OncePerRequestFilter` chain before reaching a controller. Filters execute in the following order:

```
Client Request
      │
      ▼
┌─────────────────┐
│  Trace Filter   │  Generates/propagates X-Trace-Id; stores in MDC
└─────────────────┘
         │
         ▼
┌──────────────────────┐
│  Rate Limit Filter   │  Resolves identity; checks distributed token bucket (Redis)
└──────────────────────┘
         │
         ▼
┌─────────────────┐
│   JWT Filter    │  Validates token signature and expiry; checks Redis blacklist;
│                 │  loads SecurityContext
│                 │  (skipped for: /users/login, /users/register, /users/refresh,
│                 │   /actuator/health/**, /swagger-ui/**, /v3/api-docs/**)
└─────────────────┘
         │
         ▼
    Controller
```

---

## Trace Filter

`TraceFilter` is the outermost filter and runs unconditionally on every request.

**Responsibilities:**
- Reads `X-Trace-Id` from the incoming request header, or generates a new UUID v4 if the header is absent.
- Stores the trace ID in MDC under the key `traceId`.
- Propagates the trace ID back to the client in the response via `X-Trace-Id`.
- Calls `MDC.clear()` in its own `finally` block at the very end of the request lifecycle. This is the authoritative MDC cleanup point — all other MDC participants (`LogUtil`, `LoggingAspect`) only remove the keys they individually added, leaving the trace ID intact for the full request duration.

Because `logstash-logback-encoder` is configured with `includeMdc=true`, the trace ID appears in every JSON log entry for the duration of the request without any manual propagation. See [observability.md](observability.md).

---

## Rate Limit Filter

`RateLimitFilter` runs after trace context is established but before JWT validation. This ordering ensures that rate limit decisions are logged with the trace ID and that unauthenticated requests (which cannot pass JWT validation) are still subject to rate limiting.

**Responsibilities:**
- Determines request identity: attempts to extract `userId`/role from the presented JWT; if parsing fails (or no token is present), falls back to client IP identity.
- Resolves the appropriate bucket limits by combining role-based and endpoint-specific limits.
- Consults the distributed token bucket in Redis (via Bucket4j `LettuceBasedProxyManager`).
- Returns `429 Too Many Requests` with `Retry-After` and rate limit headers if the bucket is exhausted.
- Sets `X-RateLimit-Limit` and `X-RateLimit-Remaining` headers on all responses.

The filter is active for all Spring profiles except `mock-redis`. See [rate-limiting.md](rate-limiting.md) for bucket configuration, limit tables, and key design.

---

## JWT Filter

`JwtFilter` runs last in the chain, after rate limiting is applied.

**Responsibilities:**
- Skips processing for public endpoints: `/users/login`, `/users/register`, `/users/refresh`, `/actuator/health/**`, `/swagger-ui/**`, and `/v3/api-docs/**`.
- Extracts and validates the JWT from the `Authorization: Bearer <token>` header — validates signature and expiry.
- Checks the Redis blacklist (`blacklist:<token>`) to reject tokens that were invalidated by logout, regardless of signature validity.
- Loads the `SecurityContext` with the authenticated principal so downstream Spring Security role checks can proceed.

**Failure modes:**
- Missing or malformed `Authorization` header → request proceeds without a `SecurityContext`; Spring Security will return `401` if the endpoint requires authentication.
- Expired or invalid token signature → `401 Unauthorized`.
- Token present in Redis blacklist → `401 Unauthorized`.
- Valid token, insufficient role for endpoint → `403 Forbidden` (handled by Spring Security after filter chain).

The `401` and `403` responses that originate from Spring Security (before reaching the controller) are handled by `CustomAuthenticationEntryPoint` and `CustomAccessDeniedHandler` respectively, ensuring the same JSON error schema is returned. See [error-handling.md](error-handling.md) and [session-management.md](session-management.md).
