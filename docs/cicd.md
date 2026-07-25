# CI/CD Pipeline

> Related: [deployment.md](deployment.md) | [testing.md](testing.md)

> **Deployment Status:** The CI/CD pipeline and deployment infrastructure are fully implemented. However, the production deployment is currently inactive and not hosted.

---

## Overview

Deployment is fully automated via **GitHub Actions** (`.github/workflows/deploy.yml`) and the remote `scripts/deploy.sh` script. Every push to `main` triggers the pipeline.

The pipeline builds and deploys both the **backend Docker image** and the **frontend static assets**.

---

## Pipeline Stages

### 1. `test` job

```
./mvnw clean verify -Preal
```

Runs the full test suite using the `real` Maven profile, which activates the `test` Spring profile and uses PostgreSQL + Redis Testcontainers for integration tests (while unit tests still use mocks by design). See [testing.md](testing.md) for test coverage and profile details.

The `deploy` job only runs if `test` succeeds.

### 2. `deploy` job

**Frontend build:**

```bash
cd frontend
npm ci
npm run build
```

Uses Node.js 22 (matching local development recommendation). The build reads `VITE_API_BASE_URL=https://api.nishantdd.dev` from `frontend/.env.production`.

**Backend image build and push:**

- Docker image is built from the root `Dockerfile` and tagged as `nishantdd/course-management-api:${{ github.sha }}`.
- Versioned image is pushed to Docker Hub.

**Transfer artifacts to host server:**

The following are copied to `/root/course-management-api` on the host server via SCP:

| Artifact | Purpose |
|---|---|
| `docker-compose.yml` | Production service definitions |
| `nginx.conf` | TLS, API proxy, static frontend routing |
| `scripts/*` | Blue-green deploy script |
| `frontend/dist` | Built React SPA served by Nginx |

Before copying, the pipeline removes the previous `frontend/dist` on the host server to avoid stale assets.

**Remote execution:**

- SSH executes `./scripts/deploy.sh ${{ github.sha }}` on the host server.

---

## Deploy Script (`scripts/deploy.sh`)

The deploy script performs the full blue-green cutover sequence on the host server. It receives the new image SHA as its only argument.

```
deploy.sh <image-sha>
```

**Sequence:**

1. **Detect active color** — reads `nginx.conf` to determine whether `app-blue` or `app-green` is the current upstream target.
2. **Start inactive container** — pulls and starts the new image on the inactive color (`app-blue` or `app-green`).
3. **Health check** — polls `/actuator/health/readiness` from inside the Nginx container until the new app container responds healthy. Fails the deployment if the timeout is exceeded.
4. **Update Nginx upstream** — rewrites the `upstream backend` block in `nginx.conf` to point to the new color.
5. **Recreate Nginx container** — runs `docker compose up -d --force-recreate --no-deps nginx` to pick up the updated `nginx.conf` bind-mount.
6. **Validate and reload Nginx** — runs `nginx -t` to validate the updated config, then `nginx -s reload` to apply it with zero-downtime (Nginx reload is non-disruptive to active connections).
7. **Post-switch verification** — sends a health check to `https://api.nishantdd.dev/actuator/health/readiness` to confirm that external traffic is reaching the new container.
8. **Cleanup** — removes the old container after successful verification.

The Nginx container is recreated on every cutover (`docker compose up -d --force-recreate --no-deps nginx`), which ensures it picks up the latest `nginx.conf` and any updated `frontend/dist` static assets that were copied by the SCP step. No manual Nginx restart is needed after a deploy.

**Rollback on failure:**

- If readiness fails before traffic switch, the script removes the failed target container and exits with a non-zero status code (traffic never left the old container).
- If post-switch verification fails, the script restores the previous upstream in `nginx.conf`, reloads Nginx to revert traffic to the old container, removes the failed new container, and exits with a non-zero status code.
- GitHub Actions marks the deployment step as failed, and the previous deployment remains live.

---

## Secrets and Configuration

The following secrets must be configured in the GitHub repository (`Settings → Secrets and variables → Actions`):

| Secret | Purpose |
|---|---|
| `DOCKER_USERNAME` | Docker Hub login username |
| `DOCKER_PASSWORD` | Docker Hub password/token for push |
| `SERVER_IP` | Host server IP or hostname |
| `SSH_PRIVATE_KEY` | Private SSH key for server access |

Environment variables for the running application (database credentials, JWT secret, admin bootstrap) are sourced from the `.env` file on the host server at `/root/course-management-api/.env`. This file is not managed by the pipeline — it must be provisioned on the server manually. Use `.env.example` in the repository as a template.

---

## Artifact Versioning

Every successful deployment produces a Docker image tagged with the full Git commit SHA:

```
nishantdd/course-management-api:abc1234def5678...
```

This makes every deployed backend version traceable to a specific commit. The `IMAGE_TAG_BLUE` or `IMAGE_TAG_GREEN` variable in the deploy script is set to the incoming SHA, which is passed to Docker Compose for the new container.

Frontend assets are not separately versioned — they are rebuilt on every deploy and copied as `frontend/dist` to the host server, tied to the same commit that triggered the pipeline.

---

## What Is Not Automated

| Item | Managed by |
|---|---|
| Server provisioning | Manual (e.g., VPS provider) |
| DNS records (`api.nishantdd.dev`, `app.nishantdd.dev`) | Manual |
| Let's Encrypt certificate issuance/renewal | Manual (certbot on host) |
| `.env` on the host | Manual |

See [deployment.md](deployment.md) for infrastructure and TLS setup details.
