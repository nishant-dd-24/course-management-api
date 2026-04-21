# Error Handling

> Related: [api.md](api.md) | [request-flow.md](request-flow.md)

---

## Overview

All errors are handled by a global `@RestControllerAdvice` and return a consistent JSON structure. Spring Security failures (which occur before the request reaches the controller) are handled by dedicated entry point and access denied handlers that share the same response writer, ensuring format consistency regardless of which layer catches the error.

---

## Error Response Schema

```json
{
  "traceId": "2cf5dd29-3bb0-44b3-921a-2ac07aa09f4a",
  "path": "/users/abc",
  "method": "GET",
  "status": 400,
  "message": "Invalid value 'abc' for parameter 'id'. Expected type: Long",
  "errorCode": "BAD_REQUEST",
  "timestamp": "2026-04-03T08:52:47.856580784",
  "errors": {}
}
```

The `errors` map is populated **only** for `@Valid` validation failures (`MethodArgumentNotValidException`), where it contains field-level messages keyed by field name. For all other error cases it is an empty `{}`.

The `traceId` field matches the `X-Trace-Id` request/response header, linking the error response to the structured log entry for that request.

---

## HTTP Status Code Reference

| Scenario | Status |
|---|---|
| `@Valid` validation failure | `400 Bad Request` |
| Invalid enum value / type mismatch / bad argument | `400 Bad Request` |
| Authentication required (missing/invalid/blacklisted token) | `401 Unauthorized` |
| Access denied (insufficient role) | `403 Forbidden` |
| Resource not found | `404 Not Found` |
| Method not allowed | `405 Method Not Allowed` |
| Duplicate resource (e.g. email already registered) | `409 Conflict` |
| Rate limit exceeded | `429 Too Many Requests` |
| Unexpected server error | `500 Internal Server Error` |

---

## Dual 401/403 Coverage

Spring Security intercepts some auth failures before the request reaches the controller layer. Two dedicated handlers cover this:

**`CustomAuthenticationEntryPoint`** handles `401 Unauthorized` when Spring Security rejects a request due to a missing, expired, or invalid JWT — before the request reaches the controller.

**`CustomAccessDeniedHandler`** handles `403 Forbidden` when Spring Security blocks an authenticated user due to insufficient role — before the request reaches the controller.

The `GlobalExceptionHandler` covers the equivalent cases that do reach the controller (`UnauthorizedException`, `AuthorizationDeniedException`). Both paths use the same `ErrorResponseWriter`, so the JSON response shape is identical regardless of which layer catches the error.

---

## Validation

Bean validation is applied to all request DTOs via `@Valid`. Validation failures produce a `400 Bad Request` with the `errors` map populated:

```json
{
  "status": 400,
  "errorCode": "VALIDATION_FAILED",
  "message": "Validation Failed",
  "errors": {
    "email": "must not be blank",
    "password": "size must be between 8 and 100"
  }
}
```

Write flows (user and course create/update) also apply payload normalization and sanitization in the service layer before persistence/business-rule checks, handled by utilities in the `util` package (`Sanitizer`, `StringUtil`).

---

## `DataIntegrityViolationException` Handling

`DataIntegrityViolationException` (from the JPA layer) is mapped to `409 Conflict` by the global handler. This serves as a safety net for duplicate enrollment attempts that reach the database under race conditions — the primary guard is the pessimistic write lock, but the exception handler provides a consistent error response if the constraint violation still occurs. See [concurrency.md](concurrency.md).
