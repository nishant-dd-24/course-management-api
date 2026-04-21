# Session Management

> Related: [request-flow.md](request-flow.md) | [api.md](api.md) | [design-decisions.md](design-decisions.md)

---

## Token Model

The API uses a two-token model: a short-lived access token for authorizing individual requests and a long-lived refresh token for obtaining new access tokens without re-authentication.

| Property | Access Token | Refresh Token |
|---|---|---|
| **Purpose** | Authorizes individual API requests | Obtains a new access token |
| **Lifetime** | Short-lived (configurable, default 1h) | Long-lived (configurable, default 7 days) |
| **Subject** | `userId` (immutable primary key) | Same `userId` |
| **Storage on logout** | Blacklisted at `blacklist:<token>` in Redis | Deleted from `refresh:<token>` in Redis when `X-Refresh-Token` is supplied |

JWT subject is `userId` rather than email. Email is mutable and an unreliable long-lived identity claim. See [design-decisions.md](design-decisions.md) for the full rationale.

---

## Redis Key Layout

| Key pattern | Written | Deleted | Purpose |
|---|---|---|---|
| `refresh:<refreshToken>` | On login | On logout (if `X-Refresh-Token` provided) | Server-side refresh token registry |
| `blacklist:<accessToken>` | On logout | Self-expiring via TTL | Access token revocation store |

**`refresh:<refreshToken>`** — Only tokens present in this Redis key are accepted by the refresh endpoint. This provides a server-side revocation point: a refresh token that has been deleted from Redis is rejected even if its JWT signature is still cryptographically valid. Token rotation is not implemented — the refresh endpoint issues a new access token and returns the same refresh token.

**`blacklist:<accessToken>`** — Written on logout with a TTL equal to the token's remaining lifetime. The JWT filter checks this key on every authenticated request; a match results in `401 Unauthorized` regardless of signature validity. The TTL-based self-cleanup means no background job is needed to purge stale entries.

---

## Session Lifecycle

```
Login
  └─► issue accessToken + store refreshToken in Redis (refresh:<token>)
              │
              ▼
  Authenticated requests (accessToken in Authorization header)
              │
              └─► JWT filter: validate signature → check blacklist → load SecurityContext
              │
              ▼
  Token expiry / proactive refresh
              │
              └─► POST /users/refresh with refreshToken
                    └─► validate refreshToken exists in Redis
                          └─► issue new accessToken (same refreshToken returned)
                                │
                                ▼
                          Repeat authenticated request cycle

Logout
  └─► blacklist accessToken in Redis (TTL = remaining token lifetime)
  └─► delete refreshToken from Redis (if X-Refresh-Token header provided)
        └─► all subsequent requests with either token are rejected
```

---

## Logout Invalidation

`POST /users/logout` performs two operations in sequence:

1. Writes `blacklist:<accessToken>` to Redis with TTL equal to the token's remaining validity window.
2. If `X-Refresh-Token` is present in the request header, deletes `refresh:<refreshToken>` from Redis.

After logout, any request bearing the old access token is rejected at the blacklist check in the JWT filter — before it reaches the controller layer. The refresh token, if provided, is immediately invalid for the refresh endpoint.

---

## Admin Bootstrap

`AdminInitializer` runs on startup and creates an `ADMIN` user if one does not already exist for the configured email. The operation is `@Transactional`. If a user with `app.admin.email` already exists, bootstrap is skipped silently.

Configuration properties:

```properties
app.admin.email=admin@example.com
app.admin.password=12345678
app.admin.name=Admin
```

These can be overridden via environment variables using Spring's relaxed binding:

```bash
export APP_ADMIN_EMAIL=admin@example.com
export APP_ADMIN_PASSWORD=changeme
export APP_ADMIN_NAME="Default Admin"
```

---

## Design Rationale

For the full reasoning behind the access + refresh token model with Redis backing (versus pure stateless JWT), see [design-decisions.md](design-decisions.md). In summary: pure stateless JWTs cannot be revoked before expiry; the Redis blacklist provides true revocation on logout at the cost of one sub-millisecond Redis read per authenticated request.
