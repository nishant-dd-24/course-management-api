# 📚 Course Management API

A backend system built using Spring Boot for managing users, courses, and enrollments with role-based access and JWT authentication.

---

## 🚀 Features

* 🔐 JWT-based authentication & authorization
* 👥 Role-based access (ADMIN, INSTRUCTOR, STUDENT)
* 📚 Course management (create, update, delete, search, pagination)
* 🎓 Enrollment system with validation
* 🔍 Search, filtering, and pagination support
* 🛡️ Global exception handling
* 🧹 Input sanitization & validation

### 🔍 Observability

* Request tracing using correlation IDs
* Trace ID propagation via `X-Trace-Id` header
* MDC-based logging for request-level debugging

### ⚙️ Concurrency Handling

* Pessimistic locking using JPA (`PESSIMISTIC_WRITE`)
* Prevents race conditions during concurrent updates
* Ensures data consistency in critical operations (e.g., enrollments)

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
filter       → request filters (JWT filter, Trace filter)  
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

* `POST /auth/login` → Login and get JWT token

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

## 📊 Logging Example

```
[traceId=abc123] ACTION=CREATE_USER email=user@example.com  
[traceId=abc123] ACTION=LOGIN_SUCCESS userId=22  
[traceId=xyz789] ACTION=ENROLL_USER userId=14 courseId=3    
```

---

## 🎯 Key Highlights

* Clean layered architecture
* Proper separation of concerns
* Production-style logging format
* Handles edge cases like duplicate users, invalid access, etc.

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

* Rate limiting to prevent abuse
* Improved validation & edge case handling

### 🧪 Quality & Deployment

* Unit & integration testing
* Dockerization
* API documentation (Swagger/OpenAPI)


---

## 👨‍💻 Author

**Nishantkumar Dwivedi**

---
