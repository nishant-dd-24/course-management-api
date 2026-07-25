# Troubleshooting

> Related: [deployment.md](deployment.md) | [local-development.md](local-development.md)

This document provides solutions to common issues encountered when running or deploying the Course Management API.

## Common Issues

| Symptom | Likely cause | Fix |
|---|---|---|
| **App fails to start with JWT error** | `JWT_SECRET` not set | Add a Base64 secret to `.env` (`openssl rand -base64 32`) |
| **Frontend cannot reach API** | API not running or wrong base URL | Confirm API is running on `:8080`; check `frontend/.env.development` |
| **CORS errors in browser** | Origin not allowed | Backend allows `http://localhost:5173` and `https://app.nishantdd.dev` only |
| **`docker compose up -d` fails on TLS** | Missing Let's Encrypt certs | The `scripts/bootstrap.sh` script must be run for the initial provisioning. Do not run the production docker-compose file directly on a fresh server without the bootstrap process. |
| **Integration tests hang or fail** | Docker not available for Testcontainers | Ensure Docker daemon is running; use `-Preal` for full Redis coverage |
| **API returns `401 Unauthorized` for everything** | Expired or invalid token | Authenticate via `/users/login` and pass the returned token as a Bearer token |

## Logs

If you encounter issues during deployment, check the container logs:

```bash
# Backend logs
docker compose logs -f app-blue
# or app-green, depending on which is active

# Database logs
docker compose logs -f postgres

# Nginx logs
docker compose logs -f nginx
```

## Need Help?

If you encounter an issue not covered here, please open an issue on the GitHub repository.
