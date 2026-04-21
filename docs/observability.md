# Observability

> Related: [request-flow.md](request-flow.md) | [architecture.md](architecture.md)

---

## Request Tracing

`TraceFilter` runs first in the filter chain and establishes the request trace context:

- Reads `X-Trace-Id` from the incoming request header, or generates a new UUID v4 if the header is absent.
- Stores the trace ID in MDC under the key `traceId`.
- Propagates the trace ID to the client in the `X-Trace-Id` response header.
- Calls `MDC.clear()` in its own `finally` block at the end of every request — this is the authoritative cleanup point for all MDC state.

Because `logstash-logback-encoder` is configured with `includeMdc = true`, the trace ID is present in every JSON log line for the full duration of the request without manual passing or thread-local management.

---

## Structured JSON Logging

Logging is configured via `logback-spring.xml` using `LogstashEncoder`. Every log line is emitted as a flat JSON object. The `app` field is injected globally via `customFields` in `logback-spring.xml`.

**Real examples from a login request:**

```json
{
  "timestamp": "2026-04-06T12:29:54.909189587+05:30",
  "@version": "1",
  "message": "Login successful",
  "logger": "com.nishant.coursemanagement.service.user.UserServiceImpl",
  "thread": "http-nio-8080-exec-1",
  "level": "INFO",
  "level_value": 20000,
  "traceId": "0eaaf0b2-9128-4872-a3a7-a3a6ec9b7459",
  "path": "/users/login",
  "action": "LOGIN_SUCCESS",
  "method": "POST",
  "userId": "22",
  "clientIp": "0:0:0:0:0:0:0:1",
  "app": "course-management"
}
```

```json
{
  "timestamp": "2026-04-06T12:29:54.924532492+05:30",
  "@version": "1",
  "message": "LOGIN_ATTEMPT",
  "logger": "com.nishant.coursemanagement.log.aspect.LoggingAspect",
  "thread": "http-nio-8080-exec-1",
  "level": "INFO",
  "level_value": 20000,
  "traceId": "0eaaf0b2-9128-4872-a3a7-a3a6ec9b7459",
  "path": "/users/login",
  "method": "POST",
  "durationMs": "143",
  "clientIp": "0:0:0:0:0:0:0:1",
  "status": "SUCCESS",
  "app": "course-management"
}
```

**Key structural points:**

- `action` is the machine-readable key intended for log querying and alerting; `message` is the human-readable description.
- `status` (`SUCCESS` / `FAILED`) and `durationMs` are added automatically by `LoggingAspect` after every `@Loggable`-annotated method completes.
- `traceId`, `method`, `path`, and `clientIp` are set once by `TraceFilter` and are present on all log entries for the full request lifetime.
- MDC fields appear as flat top-level keys in the JSON output alongside standard Logback fields.

---

## Annotation-Driven Log Instrumentation

Log instrumentation is handled through a dedicated `log` package with three coordinated layers.

### `@Loggable` (annotation)

Placed on any method to declare its logging intent. Supported fields:

| Field | Description |
|---|---|
| `action` | Machine-readable action key written to MDC (e.g. `"DELETE_USER"`) |
| `message` | Human-readable log message; falls back to `action` if blank |
| `level` | Log level — `DEBUG`, `INFO`, `WARN`, `ERROR` (default: `INFO`) |
| `extras` | SpEL expressions evaluated against method parameters (e.g. `"#request.email"`) |
| `extraKeys` | MDC key names corresponding to each `extras` expression by index |
| `includeCurrentUser` | When `true`, resolves the authenticated user and writes `currentUserId` to MDC |

### `LoggingAspect` (AOP)

Intercepts all `@Loggable`-annotated methods. On each invocation:

1. Populates MDC from the annotation metadata (`action`, `extras`, `extraKeys`, `currentUserId` if requested).
2. Invokes the target method.
3. Adds `status` (`SUCCESS` / `FAILED`) and `durationMs` to MDC after the method returns.
4. Emits the log line at the declared level.
5. Clears all keys it added from MDC in a `finally` block (does not call `MDC.clear()` — the trace ID and other request-scoped keys set by `TraceFilter` remain intact).

### `LogUtil.log()` (inline helper)

Used directly inside method bodies for **branch-level log points** — cases where the `action`, message, or context differ depending on the execution path (e.g. duplicate email detected, password mismatch, role change on update). These cannot be represented as a single method-entry annotation.

`LogUtil.log()` accepts action, message, and variadic key-value pairs, populates MDC, emits the log line, and clears its own MDC additions in a `finally` block. It does not call `MDC.clear()`, so the trace ID remains in context.

---

## Usage Example

```java
// Method-entry log — declared via annotation
@Loggable(
    action = "DELETE_USER",
    level = WARN,
    includeCurrentUser = true,
    extras = {"#id"},
    extraKeys = {"deletedUserId"}
)
public void deleteUser(Long id) { ... }

// Branch-level log — emitted inline
LogUtil.log(log, WARN, "LOGIN_FAILED", "Login failed",
    "reason", "INVALID_PASSWORD",
    "userId", user.getId()
);
```

The two mechanisms are complementary. `@Loggable` handles the unconditional method-entry log point with zero boilerplate. `LogUtil.log()` handles conditional, branch-specific log points within the same method body. Neither requires a `try/finally` at the call site. See [design-decisions.md](design-decisions.md) for the rationale behind maintaining both mechanisms.

---

## MDC Lifecycle

```
TraceFilter.doFilter() start
  └─► MDC.put("traceId", ...)
  └─► MDC.put("path", ...), MDC.put("method", ...), MDC.put("clientIp", ...)
          │
          ▼ (request processing)
          │
          ├─► LoggingAspect adds/removes its own keys per method invocation
          ├─► LogUtil.log() adds/removes its own keys per call site
          │
          ▼ (response written)
          │
TraceFilter.doFilter() finally
  └─► MDC.clear()   ← sole call to MDC.clear() for the request
```

This design ensures that all MDC participants can rely on `traceId` and other request-scoped fields being present throughout the request lifetime, while still cleaning up after themselves to prevent key leakage across method boundaries.
