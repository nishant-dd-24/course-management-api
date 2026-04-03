# 📚 Course Management API

A backend system built using Spring Boot for managing users, courses, and enrollments with role-based access and JWT authentication.

---

## 🚀 Features

* 🔐 JWT-based authentication & authorization
* 👥 Role-based access control (ADMIN, INSTRUCTOR, STUDENT)
* 📚 Course management (CRUD, pagination, filtering, search)
* 🎓 Enrollment system with validation & constraints
* 🛡️ Global exception handling with consistent JSON responses
* 🧹 Input validation & sanitization

### 🔍 Observability

* Request tracing using correlation IDs
* Trace ID propagation via `X-Trace-Id` header
* MDC-based structured logging for request-level debugging

### ⚙️ Concurrency Handling

* Pessimistic locking using JPA (`PESSIMISTIC_WRITE`)
* Prevents race conditions during concurrent updates
* Ensures strict data consistency in critical operations

### 🚦 Rate Limiting

* Role-based rate limiting (ADMIN, INSTRUCTOR, STUDENT, ANONYMOUS)
* Endpoint-specific rate limits (login, enrollments, read-heavy APIs)
* Hybrid identity resolution (JWT user or IP fallback)
* Token bucket algorithm using Bucket4j
* Caffeine-based cache with automatic eviction
* Path normalization (`/resource/{id}`) to prevent bucket explosion
* Standard rate limit headers:
    - `X-RateLimit-Limit`
    - `X-RateLimit-Remaining`
    - `Retry-After`

---

## 🛠️ Tech Stack

* Java 21
* Spring Boot
* Spring Security
* Spring Data JPA
* PostgreSQL
* Maven

---

## 📁 Project Structure

```
config       → application configuration (security, jackson, etc.)
controller   → REST endpoints  
service      → business logic  
repository   → database layer  
dto          → request/response models  
entity       → database entities  
mapper       → entity ↔ DTO conversion  
exception    → custom exceptions & handlers  
security     → JWT config & authentication  
filter       → request filters (Trace, JWT, Rate Limiting)
util         → helper utilities  
```

---

## ⚙️ Setup & Run

### 1. Clone the repository

```bash
git clone https://github.com/nishant-dd-24/course-management-api.git
cd course-management-api
```

### 2. Configure environment variables

```bash
export SPRING_DATASOURCE_PASSWORD=your_db_password
export JWT_SECRET=your_secret_key
export JWT_EXPIRATION_SECONDS=3600
```

### 3. Run the application

```bash
./mvnw spring-boot:run
```

---

## 📌 API Overview

### 🔐 Authentication

* `POST /users/login` → Login and get JWT token

---

### 👤 Users

* `POST /users/register` → Register new user
* `PATCH /users/{id}` → Update user (partial)
* `PUT /users/{id}` → Full update user
* `GET /users` → Get users (with pagination & search)
* `DELETE /users/{id}` → Delete user

---

### 📚 Courses

* `POST /courses` → Create course
* `PATCH /courses/{id}` → Update course (partial)
* `GET /courses` → Get all courses (pagination, filter, search)
* `GET /courses/{id}` → Get course by ID
* `DELETE /courses/{id}` → Delete course

---

### 🎓 Enrollments

* `POST /enrollments` → Enroll user in course
* `GET /enrollments` → Get enrollments (filter, pagination)
* `DELETE /enrollments/{id}` → Cancel enrollment

---

## 🔄 Request Flow

Client  
↓  
Trace Filter  
↓  
Rate Limit Filter  
↓  
JWT Authentication  
↓  
Controller

---

## 📐 API Behavior & Error Handling

This API follows RESTful standards for error handling and response consistency.

| Scenario                             | Status Code            |
|--------------------------------------|------------------------|
| Invalid request / Validation failure | 400 Bad Request        |
| Authentication required              | 401 Unauthorized       |
| Access denied                        | 403 Forbidden          |
| Resource not found                   | 404 Not Found          |
| Method not allowed                   | 405 Method Not Allowed |
| Rate limit exceeded                  | 429 Too Many Requests  |


### Example Error Response

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
---

## 🧠 Design Decisions

### Request Tracing

Implemented using a custom `OncePerRequestFilter`:

* Generates a unique trace ID per request (if not provided)
* Propagates trace ID via `X-Trace-Id` header
* Stores trace ID in MDC for structured logging

**Why?**
- Helps correlate logs across a single request
- Improves debugging in distributed or concurrent systems

---

### Pessimistic Locking for Concurrency Control

Used `@Lock(LockModeType.PESSIMISTIC_WRITE)` in critical queries.

**Why?**
- Prevents race conditions during concurrent updates
- Ensures strict consistency for operations like enrollment

**Trade-offs:**
- Reduced concurrency under high load
- Potential for lock contention

**Why not optimistic locking?**
- Avoids retry complexity
- Better suited for high-contention scenarios

---

### Rate Limiting Strategy

Implemented using a custom filter with Bucket4j and Caffeine.

**Key Design Choices:**
- Token bucket algorithm for smooth request throttling
- Role-based limits to prioritize privileged users
- Endpoint-specific limits for sensitive operations (e.g., login)
- Path normalization to prevent excessive bucket creation
- Caffeine cache to handle memory efficiently

**Why?**
- Prevent abuse (e.g., brute-force login attempts)
- Protect critical endpoints
- Ensure fair resource usage across users

**Trade-offs:**
- In-memory limits reset on application restart
- Not distributed (can be improved with Redis)

---

## 📊 Logging Example

```
[traceId=abc123] ACTION=CREATE_USER email=user@example.com  
[traceId=abc123] ACTION=LOGIN_SUCCESS userId=22  
[traceId=xyz789] ACTION=ENROLL_USER userId=14 courseId=3    
```

---

## 🎯 Key Highlights

* Clean layered architecture
* Production-oriented backend design (rate limiting, tracing, concurrency control)
* Consistent and RESTful error handling across all layers
* Multi-layer request processing pipeline (Trace → Rate Limit → Auth → Controller)
* Thread-safe and memory-efficient rate limiting implementation

---

## 🚀 Future Improvements

### 🔍 Observability & Monitoring

* Distributed tracing (e.g., OpenTelemetry)
* Metrics & monitoring (Spring Actuator / Prometheus)

### ⚡ Performance Optimization

* Caching (e.g., Redis)
* Database indexing & query optimization
* Pagination tuning for large datasets

### 🔐 Security & Reliability

* Distributed rate limiting using Redis
* Advanced validation & edge case handling
* API abuse detection & monitoring

### 🧪 Quality & Deployment

* Unit & integration testing
* Dockerization
* API documentation (Swagger/OpenAPI)


---

## 👨‍💻 Author

**Nishantkumar Dwivedi**

---
