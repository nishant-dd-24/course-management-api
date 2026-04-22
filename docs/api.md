# API Reference

> Related: [session-management.md](session-management.md) | [request-flow.md](request-flow.md) | [error-handling.md](error-handling.md)

All endpoints except `/users/login`, `/users/register`, `/users/refresh`, `/actuator/health/**`, `/swagger-ui/**`, and `/v3/api-docs/**` require authentication via JWT.

All protected endpoints require the header:
```
Authorization: Bearer <access_token>
```

---

## Authentication

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/users/login` | Public | Authenticate and receive access + refresh tokens |
| `POST` | `/users/register` | Public | Register a new user |
| `POST` | `/users/refresh` | Public | Exchange a refresh token for a new access token |
| `POST` | `/users/logout` | Authenticated | Invalidate the current access and refresh tokens |

### POST /users/login

**Request:**
```json
{
  "email": "user@example.com",
  "password": "yourpassword"
}
```

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
  "expiresIn": 3600,
  "expiresAt": "2026-04-05T14:46:23.082550103Z",
  "user": {
    "id": 22,
    "name": "John Doe",
    "email": "john@example.com",
    "role": "STUDENT",
    "isActive": true
  }
}
```

Inactive users cannot log in; login returns `401 Unauthorized`.

### POST /users/register

**Request:**
```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "yourpassword"
}
```

Always creates a user with role `STUDENT`. If the email belongs to a soft-deleted user, registration reactivates that user instead of creating a second record.

### POST /users/refresh

**Request:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9..."
}
```

Responds with a new `accessToken`. The same `refreshToken` is returned and remains valid in Redis until it expires or is explicitly revoked via logout. See [session-management.md](session-management.md) for server-side token lifecycle.

### POST /users/logout

```
POST /users/logout
Authorization: Bearer <access_token>
X-Refresh-Token: <refresh_token>   ← optional
```

No request body. On success, the access token is blacklisted in Redis (`blacklist:<token>`) and the refresh token, if provided via `X-Refresh-Token`, is deleted from Redis (`refresh:<token>`).

---

## Operational Endpoints

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/actuator/health` | Public | Health and readiness information |
| `GET` | `/actuator/info` | Authenticated | Application info |
| `GET` | `/swagger-ui/index.html` | Public | Interactive API documentation (Swagger UI) |
| `GET` | `/v3/api-docs` | Public | Raw OpenAPI specification (JSON) |

`/actuator/health/readiness` is used by the blue-green deployment script to validate new containers before traffic is switched. See [deployment.md](deployment.md).

---

## API Documentation

Swagger UI is available at `/swagger-ui/index.html`. It provides an interactive interface for all endpoints, including:

- Enum dropdowns for `sortBy` and `direction` parameters on all list endpoints
- Validation constraints on all query and request body fields
- Authentication support via the **Authorize** button

The raw OpenAPI specification is available at `/v3/api-docs`.

**To authorize in Swagger UI:**
1. Log in via `POST /users/login` to obtain an `accessToken`.
2. Click **Authorize** at the top of the Swagger UI page.
3. Enter `Bearer <your_access_token>` in the value field.
4. All subsequent requests made through the UI will include the token.

---

## Users

| Method | Endpoint | Role | Description |
|---|---|---|---|
| `GET` | `/users` | ADMIN | List all users (paginated, filterable) |
| `GET` | `/users/{id}` | ADMIN | Get user by ID |
| `PUT` | `/users/{id}` | ADMIN | Full update of a user |
| `PATCH` | `/users/{id}` | ADMIN | Partial update of a user |
| `DELETE` | `/users/{id}` | ADMIN | Deactivate a user (soft delete) |
| `GET` | `/users/my` | Authenticated | Get own profile |
| `PUT` | `/users/my` | Authenticated | Update own profile |
| `PATCH` | `/users/my` | Authenticated | Partial update of own profile |
| `DELETE` | `/users/my` | Authenticated | Deactivate own account (soft delete) |
| `POST` | `/users/my/change-password` | Authenticated | Change password |

### Lifecycle notes

- `DELETE /users/{id}` and `DELETE /users/my` are soft deletes — they set `isActive = false` rather than removing the row.
- Registering an email that belongs to a soft-deleted user reactivates that user instead of inserting a new record.
- Inactive users cannot log in; `401 Unauthorized` is returned at the login endpoint.

### Query parameters for GET /users

List endpoints accept a `UserSearchRequest` DTO via query parameters. All fields are optional. Invalid values return `400 Bad Request` with a populated `errors` map.

| Param | Type | Constraints | Description |
|---|---|---|---|
| `name` | string | Max 50 characters | Filter by name (partial match) |
| `email` | string | Valid email format; max 100 characters | Filter by email |
| `isActive` | boolean | — | Filter by active status |
| `page` | integer | ≥ 0; default: `0` | Page number (0-indexed) |
| `size` | integer | 1–50; default: `5` | Page size |
| `sortBy` | `UserSortBy` | `ID`, `NAME`, `EMAIL`, `CREATED_AT` | Sort field (optional) |
| `direction` | `SortDirection` | `ASC`, `DESC`; defaults to `ASC` only when `sortBy` is provided and `direction` is omitted | Sort direction |

**Allowed `sortBy` values:**

| Value | Maps to |
|---|---|
| `ID` | `id` |
| `NAME` | `name` |
| `EMAIL` | `email` |
| `CREATED_AT` | `createdAt` |

**Example:**
```
GET /users?name=john&isActive=true&page=0&size=10&sortBy=NAME&direction=ASC
```

---

## Courses

| Method | Endpoint | Role | Description |
|---|---|---|---|
| `POST` | `/courses` | INSTRUCTOR | Create a new course |
| `GET` | `/courses` | ADMIN | List all courses (paginated, filterable) |
| `GET` | `/courses/{id}` | ADMIN | Get any course by ID |
| `GET` | `/courses/active/{id}` | Authenticated | Get an active course by ID |
| `GET` | `/courses/active` | Authenticated | Browse all active courses |
| `GET` | `/courses/my` | INSTRUCTOR | Get own courses |
| `PUT` | `/courses/{id}` | INSTRUCTOR | Full update; `maxSeats` retained if not provided |
| `PATCH` | `/courses/{id}` | INSTRUCTOR | Partial update — any of: title, description, maxSeats (min: 1) |
| `DELETE` | `/courses/{id}` | INSTRUCTOR | Deactivate a course (soft delete) |

### Course response fields

| Field | Description |
|---|---|
| `id` | Course ID |
| `title` | Course title |
| `description` | Course description |
| `instructorId` | ID of the owning instructor |
| `maxSeats` | Total seat capacity |
| `availableSeats` | Computed: `maxSeats - enrolledStudents` (not persisted) |
| `isActive` | Whether the course is active |

`maxSeats` defaults to `20` if not provided in `POST /courses`.

### Query parameters for GET /courses

List endpoints accept a `CourseSearchRequest` DTO via query parameters. All fields are optional. Invalid values return `400 Bad Request` with a populated `errors` map.

| Param | Type | Constraints | Description |
|---|---|---|---|
| `title` | string | Max 100 characters | Filter by title (partial match) |
| `isActive` | boolean | — | Filter by active status |
| `instructorId` | long | Must be a positive value | Filter by instructor |
| `page` | integer | ≥ 0; default: `0` | Page number (0-indexed) |
| `size` | integer | 1–50; default: `5` | Page size |
| `sortBy` | `CourseSortBy` | `ID`, `TITLE`, `CREATED_AT` | Sort field (optional) |
| `direction` | `SortDirection` | `ASC`, `DESC`; defaults to `ASC` only when `sortBy` is provided and `direction` is omitted | Sort direction |

**Allowed `sortBy` values:**

| Value | Maps to |
|---|---|
| `ID` | `id` |
| `TITLE` | `title` |
| `CREATED_AT` | `createdAt` |

**Example:**
```
GET /courses?title=java&isActive=true&page=0&size=5&sortBy=TITLE&direction=ASC
```

---

## Enrollments

| Method | Endpoint | Role | Description |
|---|---|---|---|
| `POST` | `/enrollments/{courseId}` | STUDENT | Enroll in a course |
| `DELETE` | `/enrollments/{courseId}` | STUDENT | Unenroll from a course |
| `GET` | `/enrollments/my` | STUDENT | Get own enrollments |
| `GET` | `/enrollments/{id}` | INSTRUCTOR | Get enrollments for a course |

### Lifecycle notes

- `DELETE /enrollments/{courseId}` is a soft delete — it sets `isActive = false` and decrements the course seat count.
- `POST /enrollments/{courseId}` reactivates a previously inactive enrollment for the same student/course pair instead of creating a duplicate row.
- An already active enrollment for the same student/course returns `409 Conflict`.
- Enrolling in an inactive course returns `404 Not Found` (the lock query filters on `isActive = true`). See [concurrency.md](concurrency.md).

### Enrollment response fields

| Field | Description |
|---|---|
| `id` | Enrollment ID |
| `studentId` | Enrolled student ID |
| `courseId` | Enrolled course ID |

### Query parameters for GET /enrollments/my and GET /enrollments/{id}

List endpoints accept an `EnrollmentSearchRequest` DTO via query parameters. All fields are optional. Invalid values return `400 Bad Request` with a populated `errors` map.

| Param | Type | Constraints | Description |
|---|---|---|---|
| `isActive` | boolean | — | Filter by active status |
| `page` | integer | ≥ 0; default: `0` | Page number (0-indexed) |
| `size` | integer | 1–50; default: `5` | Page size |
| `sortBy` | `EnrollmentSortBy` | `ID`, `CREATED_AT` | Sort field (optional) |
| `direction` | `SortDirection` | `ASC`, `DESC`; defaults to `ASC` only when `sortBy` is provided and `direction` is omitted | Sort direction |

**Allowed `sortBy` values:**

| Value | Maps to |
|---|---|
| `ID` | `id` |
| `CREATED_AT` | `createdAt` |

**Example:**
```
GET /enrollments/my?isActive=true&page=0&size=5&sortBy=CREATED_AT&direction=DESC
```

---

## Pagination Response Envelope

All list endpoints accept pagination and sort parameters via typed `SearchRequest` DTOs. The `page`, `size`, `sortBy`, and `direction` parameters are validated before the query executes — invalid values return `400 Bad Request`. Sorting is controlled by domain-specific enums (`UserSortBy`, `CourseSortBy`, `EnrollmentSortBy`) that map to internal database fields; the API surface never exposes raw column names or framework-internal sort syntax.

All paginated endpoints return a consistent `PageResponse<T>` wrapper:

```json
{
  "content": [...],
  "pageNumber": 0,
  "pageSize": 5,
  "totalElements": 18,
  "numberOfElements": 5,
  "totalPages": 4,
  "first": true,
  "last": false,
  "hasNext": true,
  "hasPrevious": false,
  "hasContent": true
}
```

A custom `PageResponse<T>` is used in preference to Spring's `Page<T>` because the Spring serialization is unstable between versions. See [design-decisions.md](design-decisions.md).

---

## Error Responses

All errors return a consistent JSON structure regardless of which layer (Spring Security or controller) handles the exception:

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

The `errors` map is populated only for `@Valid` validation failures — this includes both request body validation and `SearchRequest` DTO field violations (e.g. `page < 0`, `size > 50`, invalid email format). See [error-handling.md](error-handling.md) for the full status code reference and dual 401/403 handling.
