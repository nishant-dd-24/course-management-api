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
* 📊 Structured logging for debugging and monitoring

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
controller   → REST endpoints  
service      → business logic  
repository   → database layer  
dto          → request/response models  
entity       → database entities  
mapper       → entity ↔ DTO conversion  
exception    → custom exceptions & handlers  
security     → JWT & authentication  
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

## 📊 Logging Example

```
ACTION=CREATE_USER email=user@example.com  
ACTION=LOGIN_SUCCESS userId=22  
ACTION=CREATE_USER_DUPLICATE email=user@example.com  
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

* Request tracing with correlation IDs
* Metrics & monitoring (Spring Actuator / Prometheus)

### ⚡ Performance Optimization

* Caching (e.g., Redis) for frequently accessed data
* Database indexing & query optimization
* Pagination tuning for large datasets

### 🔐 Security & Reliability

* Rate limiting to prevent abuse
* Handling concurrency & race conditions (e.g., duplicate enrollments)
* Improved validation & edge case handling

### 🧪 Quality & Deployment

* Unit & integration testing
* Dockerization
* API documentation (Swagger/OpenAPI)


---

## 👨‍💻 Author

**Nishantkumar Dwivedi**

---
