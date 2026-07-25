# Local Development (Manual Setup)

> Related: [deployment.md](deployment.md) | [testing.md](testing.md)

This guide covers how to run the Course Management API locally without using Docker for the application itself.

## Prerequisites

- **Backend:** Java 21+, Maven 3.8+
- **Frontend:** Node.js 22+, npm
- **Infrastructure:** PostgreSQL 15 and Redis 7 (can be run locally or via Docker)

## 1. Provision Database

Connect to your local PostgreSQL instance and create the database and user:

```sql
CREATE USER course_user WITH PASSWORD 'your_db_password';
CREATE DATABASE course_db OWNER course_user;
```

Ensure PostgreSQL and Redis are running locally.

## 2. Configure Environment Variables

```bash
cp .env.example .env
```

Open `.env` and set the required values:

| Variable | Required | Notes |
|---|---|---|
| `SPRING_DATASOURCE_USERNAME` | No | PostgreSQL username; defaults to `course_user` |
| `SPRING_DATASOURCE_PASSWORD` | Yes | PostgreSQL password |
| `JWT_SECRET` | Yes | Base64-encoded secret, min 32 raw bytes (`openssl rand -base64 32`) |
| `APP_ADMIN_PASSWORD` | Optional | Password for the bootstrapped admin account |

Export the variables before running:

```bash
set -a && source .env && set +a
```

For a non-Docker run, also set connection hosts if needed:

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/course_db
export SPRING_DATASOURCE_USERNAME=course_user
export SPRING_DATA_REDIS_HOST=localhost
export SPRING_DATA_REDIS_PORT=6379
```

## 3. Start the API

```bash
./mvnw spring-boot:run
```

The API starts at `http://localhost:8080`. An `ADMIN` user is bootstrapped automatically on the first start using the configured admin credentials.

## 4. Run the Frontend

```bash
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173`. The dev server uses `VITE_API_BASE_URL=http://localhost:8080` from `frontend/.env.development`.
