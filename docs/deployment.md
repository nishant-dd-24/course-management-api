# Deployment

> Related: [architecture.md](architecture.md) | [cicd.md](cicd.md)

---

## Infrastructure

| Component | Detail |
|---|---|
| Host | DigitalOcean Droplet (Linux VPS) |
| Domain | `api.nishantdd.dev` → Droplet public IP (DNS A record) |
| TLS | Let's Encrypt certificates, terminated at Nginx |
| Public ports | 80 (redirected to 443), 443 |
| Container runtime | Docker + Docker Compose |

---

## Production Topology

```
Client
  → DNS (api.nishantdd.dev)
  → DigitalOcean Droplet (Linux VPS)
  → Nginx container (ports 80/443)
  → Active app container (app-blue or app-green)
  → Spring Boot API (:8080)
```

All containers are attached to `app-network` (Docker bridge network). Inter-service communication uses Docker DNS service names: `postgres`, `redis`, `app-blue`, `app-green`. External traffic only reaches Nginx on ports 80 and 443.

---

## Docker Services

Production services are defined in `docker-compose.yml`:

| Service | Role |
|---|---|
| `postgres` | PostgreSQL database |
| `redis` | Redis — rate limit state, cache L2, token store, eviction Pub/Sub |
| `nginx` | Reverse proxy, TLS terminator, traffic router |
| `app-blue` | Blue app container (default active on initial deploy) |
| `app-green` | Green app container (profile-gated; activated during blue-green cutover) |

Image selection for each color is controlled by `IMAGE_TAG_BLUE` and `IMAGE_TAG_GREEN` environment variables.

---

## Environment Variables

Both `docker-compose.yml` and `docker-compose.dev.yml` read from a `.env` file in the project root.
In `docker-compose.yml` (production), service-to-service hosts are intentionally fixed to Docker DNS names (`postgres`, `redis`) and only secrets/image tags are interpolated from `.env`.

```env
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/course_db
SPRING_DATASOURCE_USERNAME=course_user
SPRING_DATASOURCE_PASSWORD=your_db_password

# Redis
SPRING_DATA_REDIS_HOST=redis
SPRING_DATA_REDIS_PORT=6379

# JWT
JWT_SECRET=your_base64_encoded_secret_min_32_chars
JWT_EXPIRATION_SECONDS=3600
JWT_REFRESH_EXPIRATION_SECONDS=604800

# Admin bootstrap
APP_ADMIN_EMAIL=admin@example.com
APP_ADMIN_PASSWORD=12345678
APP_ADMIN_NAME=Admin
```

> Within the Docker network, the database and Redis hosts are the compose service names (`postgres`, `redis`) rather than `localhost`.

`JWT_SECRET` must be a Base64-encoded string representing a secret of at least 32 raw bytes.

### Full property reference

| Property | Default | Environment variable override |
|---|---|---|
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/course_db` | `SPRING_DATASOURCE_URL` |
| `spring.datasource.username` | `course_user` | `SPRING_DATASOURCE_USERNAME` |
| `spring.datasource.password` | _(required)_ | `SPRING_DATASOURCE_PASSWORD` |
| `spring.data.redis.host` | `localhost` | `SPRING_DATA_REDIS_HOST` |
| `spring.data.redis.port` | `6379` | `SPRING_DATA_REDIS_PORT` |
| `app.jwt.secret` | _(required)_ | `JWT_SECRET` |
| `app.jwt.expiration-seconds` | `3600` | `JWT_EXPIRATION_SECONDS` |
| `app.jwt.refresh-expiration-seconds` | `604800` | `JWT_REFRESH_EXPIRATION_SECONDS` |
| `app.admin.email` | `admin@example.com` | `APP_ADMIN_EMAIL` |
| `app.admin.password` | `12345678` | `APP_ADMIN_PASSWORD` |
| `app.admin.name` | `Admin` | `APP_ADMIN_NAME` |

---

## Blue-Green Deployment

Two app containers are defined in the compose file (`app-blue`, `app-green`). At any time, exactly one is the active traffic target.

**Active environment detection:** The active color is determined by reading the `upstream backend` target in `nginx.conf` — either `server app-blue:8080;` or `server app-green:8080;`.

**Cutover sequence** (executed by `scripts/deploy.sh`, triggered by CI/CD):

1. Detect the currently active color from `nginx.conf`.
2. Start the inactive color with the new image tag.
3. Wait until `/actuator/health/readiness` returns healthy from inside the Nginx container.
4. Update `nginx.conf` upstream to point to the new color.
5. Validate config with `nginx -t`, then reload with `nginx -s reload`.
6. Verify the live endpoint responds healthy at `https://api.nishantdd.dev/actuator/health/readiness`.
7. On success: remove the old container.
8. On failure before traffic switch (readiness timeout): remove the failed target container and exit with a non-zero status.
9. On failure after traffic switch (post-switch verification): restore previous upstream, reload Nginx, remove failed target container, and exit with a non-zero status.

Traffic stays on the old container until the new one is confirmed healthy. The cutover is instantaneous (Nginx reload is non-disruptive) and fully automated. See [cicd.md](cicd.md) for how the deploy script is triggered.

---

## Nginx Configuration

- HTTP (port 80) → permanent redirect to HTTPS (`301`).
- HTTPS (port 443) → proxied to `upstream backend` (active app container, port 8080).
- Preserved proxy headers: `Host`, `X-Real-IP`, `X-Forwarded-For`, `X-Forwarded-Proto`.
- Docker DNS resolver configured as `127.0.0.11` for runtime service-name resolution.

---

## HTTPS / TLS

Let's Encrypt certificates are bind-mounted from the host into the Nginx container:

| Host path | Container path |
|---|---|
| `/etc/letsencrypt` | `/etc/nginx/ssl` |

Nginx uses:
- `/etc/nginx/ssl/live/api.nishantdd.dev/fullchain.pem`
- `/etc/nginx/ssl/live/api.nishantdd.dev/privkey.pem`

SSL termination is valid for `api.nishantdd.dev`. This compose configuration requires the host to have Let's Encrypt certificates already provisioned — it is not suitable for bare `localhost` usage without modification.

---

## Local Development (Docker)

Uses a separate compose file optimized for the development feedback loop:

```bash
docker compose -f docker-compose.dev.yml up --build
```

- Mounts local source directory into the container — code changes are reflected without rebuilding the image.
- Enables **Spring DevTools** for automatic application restart on classpath changes.
- Publishes `8080:8080`.
- No app healthcheck configured (intentional — reduces startup gate during development).
- No Nginx, no TLS.

### Stop containers

```bash
docker compose down
```

Remove volumes (wipes the database):

```bash
docker compose down -v
```
