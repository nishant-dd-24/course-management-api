# Course Management Frontend

React single-page application for the [Course Management API](../README.md). Provides login, registration, a role-aware dashboard, course management, enrollment views, and admin user management.

| Environment | URL |
|---|---|
| **Production** | [`https://app.nishantdd.dev`](https://app.nishantdd.dev) |
| **Local dev** | `http://localhost:5173` (Vite default) |

The frontend talks to the backend API configured via `VITE_API_BASE_URL`.

---

## Tech Stack

| Layer | Technology |
|---|---|
| UI | React 19 |
| Build | Vite 8 |
| Styling | Tailwind CSS 4 (`@tailwindcss/vite`) |
| Routing | History API (`pushState` / `popstate`) — no React Router |
| HTTP | Native `fetch` with automatic token refresh |

---

## Project Structure

```
frontend/
├── src/
│   ├── api/              # API client and domain fetch helpers
│   │   ├── client.js     # apiFetch, token refresh, ApiError
│   │   ├── auth.js       # Login, register, refresh, logout requests
│   │   ├── courses.js    # Course CRUD and listing
│   │   └── enrollments.js
│   ├── auth/
│   │   ├── AuthContext.jsx   # Session state, login/logout, refresh on load
│   │   └── tokenStore.js     # localStorage for refresh token + user snapshot
│   ├── components/
│   │   ├── auth/         # AuthShell layout
│   │   ├── courses/      # CourseForm, CourseList, CourseCard, EnrollButton
│   │   ├── dashboard/    # DashboardHeader, DashboardContent
│   │   ├── enrollments/  # EnrollmentList
│   │   └── ui/           # Button, Card, Table, Badge, Navigation, etc.
│   ├── pages/
│   │   ├── Login.jsx
│   │   ├── Register.jsx
│   │   ├── Dashboard.jsx
│   │   ├── CoursesPage.jsx
│   │   ├── EnrollmentsPage.jsx
│   │   └── Users.jsx         # ADMIN only
│   ├── services/
│   │   └── authService.js    # Auth orchestration above api/auth
│   ├── lib/
│   │   └── cn.js             # Tailwind class merge helper
│   ├── App.jsx               # Route guard + page rendering
│   ├── main.jsx
│   └── index.css
├── .env.development      # VITE_API_BASE_URL for local dev
├── .env.production       # VITE_API_BASE_URL for production builds
├── vite.config.js
└── package.json
```

---

## Prerequisites

- Node.js 22+ and npm (matches the CI pipeline)
- Backend API running and reachable at the URL configured in `.env.development`

Start the API locally with Docker Compose or `./mvnw spring-boot:run`. See the [root README](../README.md).

---

## Setup

```bash
cd frontend
npm install
```

---

## Development

```bash
npm run dev
```

Open `http://localhost:5173`.

### Environment variables

| File | Variable | Default / value |
|---|---|---|
| `.env.development` | `VITE_API_BASE_URL` | `http://localhost:8080` |
| `.env.production` | `VITE_API_BASE_URL` | `https://api.nishantdd.dev` |

Vite exposes only variables prefixed with `VITE_`. To point at a different API during development, edit `.env.development` and restart the dev server.

### CORS

The backend allows browser requests from:

- `http://localhost:5173` (local dev)
- `https://app.nishantdd.dev` (production)

If you change the Vite dev port, update `SecurityConfig.corsConfigurationSource()` in the backend accordingly.

---

## Authentication Flow

1. **Login** — `POST /users/login` returns `accessToken`, `refreshToken`, and `user`.
2. **Storage** — The refresh token and user snapshot are stored in `localStorage`. The access token is kept in React state only.
3. **Session restore** — On page load, if a refresh token exists, the app calls `POST /users/refresh` to obtain a new access token.
4. **API requests** — `apiFetch` attaches `Authorization: Bearer <accessToken>`. On `401`, it retries once after refreshing.
5. **Logout** — Clears local storage immediately, then calls `POST /users/logout` with the access token and, if available, the `X-Refresh-Token` header to also invalidate the refresh token server-side.

Protected routes redirect unauthenticated users to `/login?redirect=<path>`.

---

## Pages and Role Access

Routing is handled in `App.jsx` using the History API.

| Path | Access | Description |
|---|---|---|
| `/login` | Public | Sign in |
| `/register` | Public | Create a student account |
| `/dashboard` | Authenticated | Role-aware quick actions |
| `/courses` | Authenticated | Role-specific course views (browse, manage, or admin list) |
| `/enrollments` | Authenticated | Student enrollments or instructor course enrollments |
| `/users` | ADMIN | Paginated user list with deactivate action |

### Role-specific behavior

| Role | Courses page | Enrollments page |
|---|---|---|
| **STUDENT** | Browse active courses; enroll/unenroll | View and manage own enrollments |
| **INSTRUCTOR** | Manage own courses (create, edit, activate/deactivate) | View enrollments per owned course |
| **ADMIN** | List all courses | User management via `/users`; course browse via `/courses` |

---

## Build

```bash
npm run build
```

Output is written to `frontend/dist/`. In production, Nginx serves this directory at `app.nishantdd.dev` (see [docs/deployment.md](../docs/deployment.md)).

Preview the production build locally:

```bash
npm run preview
```

Note: `preview` serves the built assets but still calls the API URL baked in at build time (`VITE_API_BASE_URL` from `.env.production`).

---

## Lint

```bash
npm run lint
```

---

## Deployment

The GitHub Actions pipeline builds the frontend on every push to `main`:

1. `npm ci` in `frontend/`
2. `npm run build` (uses `.env.production`)
3. `frontend/dist` is copied to the droplet alongside `nginx.conf` and `docker-compose.yml`

Nginx serves the static files from `/usr/share/nginx/html` (bind-mounted from `frontend/dist` on the host). See [docs/cicd.md](../docs/cicd.md).

---

## API Integration

All authenticated requests go through `src/api/client.js`:

```javascript
import { apiFetch } from "./client";

const users = await apiFetch("/users?page=0&size=10");
```

Domain helpers in `src/api/courses.js` and `src/api/enrollments.js` wrap common operations. Errors throw `ApiError` with `status` and parsed response `data`.

For the full backend API reference, see [docs/api.md](../docs/api.md).

---

## Troubleshooting

| Symptom | Fix |
|---|---|
| Blank page after login | Check browser console; confirm API is running and `VITE_API_BASE_URL` is correct |
| CORS error | Ensure the API is running and your dev origin is allowed in backend CORS config |
| Session lost on refresh | Confirm `localStorage` is not blocked; check that `/users/refresh` succeeds |
| 403 on admin pages | Log in with an `ADMIN` account (bootstrapped on first API start) |
