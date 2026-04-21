# CI/CD Pipeline

> Related: [deployment.md](deployment.md) | [testing.md](testing.md)

---

## Overview

Deployment is fully automated via **GitHub Actions** (`.github/workflows/deploy.yml`) and the remote `scripts/deploy.sh` script. Every push to `main` triggers the pipeline.

---

## Pipeline Stages

### 1. `test` job

```
./mvnw clean verify -Preal
```

Runs the full test suite using the `real` Maven profile, which activates the `test` Spring profile and uses PostgreSQL + Redis Testcontainers for integration tests (while unit tests still use mocks by design). See [testing.md](testing.md) for test coverage and profile details.

The `deploy` job only runs if `test` succeeds.

### 2. `deploy` job

**Build and push:**
- Docker image is built and tagged as `nishantdd/course-management-api:${{ github.sha }}`.
- Versioned image is pushed to Docker Hub.

**Transfer artifacts to droplet:**
- `docker-compose.yml`, `nginx.conf`, and `scripts/*` are copied to `/root/course-management-api` on the droplet via SCP.

**Remote execution:**
- SSH executes `./scripts/deploy.sh ${{ github.sha }}` on the droplet.

---

## Deploy Script (`scripts/deploy.sh`)

The deploy script performs the full blue-green cutover sequence on the droplet. It receives the new image SHA as its only argument.

```
deploy.sh <image-sha>
```

**Sequence:**

1. **Detect active color** — reads `nginx.conf` to determine whether `app-blue` or `app-green` is the current upstream target.
2. **Start inactive container** — pulls and starts the new image on the inactive color (`app-blue` or `app-green`).
3. **Health check** — polls `/actuator/health/readiness` from inside the Nginx container until the new app container responds healthy. Fails the deployment if the timeout is exceeded.
4. **Update Nginx upstream** — rewrites the `upstream backend` block in `nginx.conf` to point to the new color.
5. **Validate and reload Nginx** — runs `nginx -t` to validate the updated config, then `nginx -s reload` to apply it with zero-downtime (Nginx reload is non-disruptive to active connections).
6. **Post-switch verification** — sends a health check to `https://api.nishantdd.dev/actuator/health/readiness` to confirm that external traffic is reaching the new container.
7. **Cleanup** — removes the old container after successful verification.

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
| `SERVER_IP` | DigitalOcean Droplet IP or hostname |
| `SSH_PRIVATE_KEY` | Private SSH key for droplet access |

Environment variables for the running application (database credentials, JWT secret, admin bootstrap) are sourced from the `.env` file on the droplet at `/root/course-management-api/.env`. This file is not managed by the pipeline — it must be provisioned on the droplet manually.

---

## Artifact Versioning

Every successful deployment produces a Docker image tagged with the full Git commit SHA:

```
nishantdd/course-management-api:abc1234def5678...
```

This makes every deployed version traceable to a specific commit. The `IMAGE_TAG_BLUE` or `IMAGE_TAG_GREEN` variable in the deploy script is set to the incoming SHA, which is passed to Docker Compose for the new container.
