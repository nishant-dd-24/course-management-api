# Deployment

> Related: [architecture.md](architecture.md) | [cicd.md](cicd.md)

> **Deployment Status:** The project contains a complete production-ready deployment pipeline. The infrastructure can be deployed to a fresh server using the provided deployment tooling. However, the production deployment is currently **inactive** and not hosted. Any domain names referenced below are configuration examples reflecting the implemented architecture.

---

## Infrastructure

| Component | Detail |
|---|---|
| Host | Linux VPS (previously a DigitalOcean Droplet) |
| API domain | `api.nishantdd.dev` → VPS public IP (DNS A record) |
| Web app domain | `app.nishantdd.dev` → VPS public IP (DNS A record) |
| TLS | Let's Encrypt certificates, terminated at Nginx |
| Public ports | 80 (redirected to 443), 443 |
| Container runtime | Docker + Docker Compose |

---

## Production Topology

```
Browser → app.nishantdd.dev → Nginx → frontend/dist (static React SPA)

Client  → api.nishantdd.dev  → Nginx → Active app container (app-blue or app-green)
                                              → Spring Boot API (:8080)
                                              → PostgreSQL
                                              → Redis
```

All containers are attached to `app-network` (Docker bridge network). Inter-service communication uses Docker DNS service names: `postgres`, `redis`, `app-blue`, `app-green`. External traffic only reaches Nginx on ports 80 and 443 — app containers are not published directly to the host.

---

## Docker Services

Production services are defined in `docker-compose.yml`:

| Service | Role |
|---|---|
| `postgres` | PostgreSQL 15 database |
| `redis` | Redis 7 — rate limit state, cache L2, token store, eviction Pub/Sub |
| `nginx` | Reverse proxy, TLS terminator, static frontend server, traffic router |
| `certbot` | Certificate provisioning helper (manual/one-off use) |
| `app-blue` | Blue app container (active color is determined at runtime by `nginx.conf`; the committed config currently targets `app-green`) |
| `app-green` | Green app container (profile-gated; activated during blue-green cutover) |

Image selection for each color is controlled by `IMAGE_TAG_BLUE` and `IMAGE_TAG_GREEN` environment variables.

The Nginx container bind-mounts:

| Host path | Container path | Purpose |
|---|---|---|
| `./nginx.conf` | `/etc/nginx/nginx.conf` | Routing and TLS config |
| `/etc/letsencrypt` | `/etc/nginx/ssl` | TLS certificates |
| `./certbot-www` | `/var/www/certbot` | ACME HTTP-01 challenges |
| `./frontend/dist` | `/usr/share/nginx/html` | Built React SPA |

---

## Environment Variables

Both `docker-compose.yml` and `docker-compose.dev.yml` read from a `.env` file in the project root.
Copy `.env.example` to `.env` and fill in required values before starting either stack.

In `docker-compose.yml` (production), service-to-service hosts are intentionally fixed to Docker DNS names (`postgres`, `redis`) and only secrets/image tags are interpolated from `.env`.

```env
# Database (required)
SPRING_DATASOURCE_PASSWORD=your_db_password

# JWT (required) — Base64-encoded, minimum 32 raw bytes
JWT_SECRET=your_base64_encoded_secret

# JWT lifetimes (optional)
JWT_EXPIRATION_SECONDS=3600
JWT_REFRESH_EXPIRATION_SECONDS=604800

# Admin bootstrap (optional — defaults shown; override for any non-local deployment)
APP_ADMIN_EMAIL=admin@example.com
APP_ADMIN_PASSWORD=changeme
APP_ADMIN_NAME=Admin

# Docker image tags (Compose interpolation — set by scripts/deploy.sh at deploy time)
IMAGE_TAG_BLUE=latest
IMAGE_TAG_GREEN=latest
```

> Within the Docker network, the database and Redis hosts are the compose service names (`postgres`, `redis`) rather than `localhost`.

Generate a JWT secret:

```bash
openssl rand -base64 32
```

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

Environment variables override `application.properties` values via Spring Boot relaxed binding (e.g. `APP_ADMIN_EMAIL` → `app.admin.email`).

> **`IMAGE_TAG_BLUE` / `IMAGE_TAG_GREEN`** are Docker Compose interpolation variables, not Spring properties. They select which Docker image tag each color container pulls and default to `latest` when unset. In production, `scripts/deploy.sh` sets the active color's tag to the incoming Git commit SHA — the `:latest` fallback is a safety default only and may reference a stale image on a fresh host.

---

## Admin Bootstrap

`AdminInitializer` runs on startup and creates an `ADMIN` user if one does not already exist for the configured email. The operation is `@Transactional`. If a user with `app.admin.email` already exists, bootstrap is skipped silently.

These properties can be overridden via environment variables using Spring's relaxed binding (see table above).

---

## Blue-Green Deployment

Two app containers are defined in the compose file (`app-blue`, `app-green`). At any time, exactly one is the active traffic target.

**Active environment detection:** The active color is determined by reading the `upstream backend` target in `nginx.conf` — either `server app-blue:8080;` or `server app-green:8080;`.

**Cutover sequence** (executed by `scripts/deploy.sh`, triggered by CI/CD):

1. Detect the currently active color from `nginx.conf`.
2. Start the inactive color with the new image tag.
3. Wait until `/actuator/health/readiness` returns healthy from inside the Nginx container. The script polls up to 30 times with 5-second intervals (150 seconds total) before declaring a timeout.
4. Update `nginx.conf` upstream to point to the new color.
5. Recreate the Nginx container (`docker compose up -d --force-recreate --no-deps nginx`) so the updated `nginx.conf` bind-mount is picked up by the new container process.
6. Validate the running config with `nginx -t` and apply it with `nginx -s reload` inside the container.
7. Verify the live endpoint responds healthy at `https://api.nishantdd.dev/actuator/health/readiness`.
8. On success: remove the old container.
9. On failure before traffic switch (readiness timeout): remove the failed target container and exit with a non-zero status.
10. On failure after traffic switch (post-switch verification): restore previous upstream, reload Nginx, remove failed target container, and exit with a non-zero status.

Traffic stays on the old container until the new one is confirmed healthy. The cutover is instantaneous (Nginx reload is non-disruptive) and fully automated. See [cicd.md](cicd.md) for how the deploy script is triggered.

---

## Nginx Configuration

`nginx.conf` defines two HTTPS virtual hosts on the same Nginx container:

| Server name | Behavior |
|---|---|
| `api.nishantdd.dev` | Reverse proxy to `upstream backend` (active app container, port 8080) |
| `app.nishantdd.dev` | Serves `frontend/dist` as a static SPA (`try_files` → `index.html`) |

Common settings:

- HTTP (port 80) → permanent redirect to HTTPS (`301`) for both domains.
- ACME HTTP-01 challenges served from `/var/www/certbot` at `/.well-known/acme-challenge/`.
- Preserved proxy headers for the API: `Host`, `X-Real-IP`, `X-Forwarded-For`, `X-Forwarded-Proto`.
- Docker DNS resolver configured as `127.0.0.11` for runtime service-name resolution.
- Static assets under `/assets/` cached for one year with immutable headers.

---

## HTTPS / TLS

Let's Encrypt certificates are bind-mounted from the host into the Nginx container:

| Host path | Container path |
|---|---|
| `/etc/letsencrypt` | `/etc/nginx/ssl` |

Nginx uses separate certificate pairs per domain:

| Domain | Certificate paths (inside container) |
|---|---|
| `api.nishantdd.dev` | `/etc/nginx/ssl/live/api.nishantdd.dev/fullchain.pem`, `privkey.pem` |
| `app.nishantdd.dev` | `/etc/nginx/ssl/live/app.nishantdd.dev/fullchain.pem`, `privkey.pem` |

SSL termination is valid for both production domains. The production compose configuration requires Let's Encrypt certificates already provisioned on the host — it is not suitable for bare `localhost` usage without modification.

---

## Frontend Deployment

The React SPA is built to static files and served by Nginx — it does not run as a separate container.

**Local build:**

```bash
cd frontend
npm ci
npm run build
```

Output: `frontend/dist/`. Production builds use `VITE_API_BASE_URL=https://api.nishantdd.dev` from `frontend/.env.production`.

**CI/CD:** The GitHub Actions pipeline runs `npm ci && npm run build` in `frontend/`, then SCPs `frontend/dist` to the host server alongside `nginx.conf` and `docker-compose.yml`. Nginx bind-mounts the directory read-only.

See [frontend/README.md](../frontend/README.md) for local development setup.

---

## Local Development (Docker)

Uses a separate compose file optimized for the development feedback loop:

```bash
docker compose -f docker-compose.dev.yml up --build
```

- Mounts local source directory into the container — code changes are reflected without rebuilding the image.
- Includes **Spring DevTools** on the classpath; the local source directory is mounted into the container, allowing DevTools to detect file changes and trigger an application restart.
- Publishes `8080:8080` for direct API access.
- No Nginx, no TLS, no frontend container (run the frontend separately with `npm run dev`).

### Stop containers

Development stack:

```bash
docker compose -f docker-compose.dev.yml down
```

Remove volumes (wipes the database):

```bash
docker compose -f docker-compose.dev.yml down -v
```

Production stack:

```bash
docker compose down
```

---

## Local Frontend + API

Typical local workflow:

1. Start the API stack: `docker compose -f docker-compose.dev.yml up --build`
2. In a second terminal:

```bash
cd frontend
npm install
npm run dev
```

3. Open `http://localhost:5173` — the dev server calls `http://localhost:8080` (configured in `frontend/.env.development`).

The backend CORS configuration allows `http://localhost:5173`.
