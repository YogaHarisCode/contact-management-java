# Contact Management API

A REST API backend for managing users, contacts, and addresses, built with Java and Spring Boot.

This is a portfolio project for learning and demonstrating Java backend development. It is under active development: user registration is implemented, and the remaining modules are planned (see [API Documentation](#api-documentation)).

## Tech Stack

| Component | Technology |
|---|---|
| Language | Java 25 |
| Framework | Spring Boot 4.1.2 |
| Web | Spring Web MVC |
| Persistence | Spring Data JPA (Hibernate) |
| Database | MySQL 8.0 |
| Migrations | Flyway |
| Validation | Bean Validation (`spring-boot-starter-validation`) |
| Password hashing | `spring-security-crypto` (BCrypt) |
| Boilerplate reduction | Lombok |
| Build tool | Maven |
| Testing | JUnit, Mockito, `RestTestClient` |

## Prerequisites

- JDK 25
- Maven 3.9+ (or the Maven Wrapper, if it is present in the repository)
- MySQL 8.0 running locally

Docker is not used in this project yet.

## Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/YogaHarisCode/contact-management-java.git
cd contact-management-java
```

### 2. Set up the database

Create the application and test databases, plus a dedicated user:

```sql
CREATE DATABASE contact_management;
CREATE DATABASE contact_management_test;

CREATE USER 'contact_app'@'localhost' IDENTIFIED BY '<your-password>';
GRANT ALL PRIVILEGES ON contact_management.* TO 'contact_app'@'localhost';
GRANT ALL PRIVILEGES ON contact_management_test.* TO 'contact_app'@'localhost';
```

Tables are created automatically by Flyway when the application starts.

### 3. Configure the application

Database settings are read from environment variables:

| Variable | Description | Default |
|---|---|---|
| `DATABASE_URL` | JDBC URL | `jdbc:mysql://localhost:3306/contact_management` |
| `DATABASE_USERNAME` | Database user | none, must be set |
| `DATABASE_PASSWORD` | Database password | none, must be set |

Linux / macOS:

```bash
export DATABASE_USERNAME=contact_app
export DATABASE_PASSWORD='<your-password>'
```

Windows (PowerShell):

```powershell
$env:DATABASE_USERNAME = "contact_app"
$env:DATABASE_PASSWORD = "<your-password>"
```

In IntelliJ IDEA, set them under **Run Configuration > Environment variables**.

### 4. Run the application

```bash
./mvnw spring-boot:run
```

On Windows use `mvnw.cmd spring-boot:run`, or `mvn spring-boot:run` if Maven is installed. The API is available at `http://localhost:8080`.

## Running Tests

```bash
./mvnw test
```

The project uses three kinds of tests for every feature:

| Type | Class naming | What it covers |
|---|---|---|
| Unit test | `UserServiceTest` | Service logic in isolation (repository and password encoder mocked) |
| Controller slice test | `UserControllerWebMvcTest` | Controller layer, validation, and error responses (`@WebMvcTest`, service mocked) |
| Integration test | `UserControllerTest` | Full flow from controller to the `contact_management_test` database |

Integration tests use the `test` profile. Make sure the `contact_management_test` database exists and the credentials used by the test profile are valid.

## API Documentation

### Response format

Every response is wrapped in the same envelope.

Success:

```json
{
  "data": {}
}
```

Error:

```json
{
  "errors": {
    "code": "409 CONFLICT",
    "message": "Username already exists",
    "details": []
  }
}
```

| Field | Description |
|---|---|
| `errors.code` | HTTP status (for example `409 CONFLICT`), or `VALIDATION_ERROR` for validation failures |
| `errors.message` | Human-readable message |
| `errors.details` | Per-field problems. Only present for validation errors |

### Register User

Registers a new user.

- **Method:** `POST`
- **URL:** `/api/users`
- **Headers:** `Content-Type: application/json`, `Accept: application/json`

**Request body**

```json
{
  "username": "johndoe",
  "password": "secretpassword",
  "name": "John Doe"
}
```

| Field | Required | Rules |
|---|---|---|
| `username` | Yes | Max 100 characters. Letters, digits, `.`, `_`, `-` only |
| `password` | Yes | Max 72 bytes in UTF-8 (BCrypt limit) |
| `name` | Yes | Max 100 characters |

**Responses**

`200 OK`

```json
{
  "data": {
    "username": "johndoe",
    "name": "John Doe"
  }
}
```

`400 Bad Request` (validation failed)

```json
{
  "errors": {
    "code": "VALIDATION_ERROR",
    "message": "Request validation failed",
    "details": [
      {
        "field": "username",
        "message": "must not be blank"
      }
    ]
  }
}
```

`400 Bad Request` (missing or malformed JSON body)

```json
{
  "errors": {
    "code": "400 BAD_REQUEST",
    "message": "Request body is missing or malformed"
  }
}
```

`409 Conflict` (username already exists)

```json
{
  "errors": {
    "code": "409 CONFLICT",
    "message": "Username already exists"
  }
}
```

**Example**

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{"username":"johndoe","password":"secretpassword","name":"John Doe"}'
```

### Planned endpoints

These endpoints are not implemented yet.

| Module | Method | URL |
|---|---|---|
| Auth | `POST` | `/api/auth/login` |
| Auth | `DELETE` | `/api/auth/logout` |
| User | `GET` | `/api/users/current` |
| User | `PATCH` | `/api/users/current` |
| Contact | `POST` | `/api/contacts` |
| Contact | `GET` | `/api/contacts` (search with pagination) |
| Contact | `GET` | `/api/contacts/{contactId}` |
| Contact | `PUT` | `/api/contacts/{contactId}` |
| Contact | `DELETE` | `/api/contacts/{contactId}` |
| Address | `POST` | `/api/contacts/{contactId}/addresses` |
| Address | `GET` | `/api/contacts/{contactId}/addresses` |
| Address | `GET` | `/api/contacts/{contactId}/addresses/{addressId}` |
| Address | `PUT` | `/api/contacts/{contactId}/addresses/{addressId}` |
| Address | `DELETE` | `/api/contacts/{contactId}/addresses/{addressId}` |

Authentication will use token-based access (`X-API-TOKEN` header). Full Spring Security integration is planned.

## Project Structure

The code is organized by layer.

```
contact-management-java/
├── pom.xml
├── README.md
└── src/
    ├── main/
    │   ├── java/com/yogaharis/contactmanagement/
    │   │   ├── ContactManagementJavaApplication.java
    │   │   ├── config/
    │   │   │   └── PasswordEncoderConfig.java
    │   │   ├── constraint/
    │   │   │   ├── MaxBytes.java
    │   │   │   └── MaxBytesValidator.java
    │   │   ├── controller/
    │   │   │   └── UserController.java
    │   │   ├── dto/
    │   │   │   ├── ErrorDetail.java
    │   │   │   ├── ErrorResponse.java
    │   │   │   ├── RegisterUserRequest.java
    │   │   │   ├── UserResponse.java
    │   │   │   └── WebResponse.java
    │   │   ├── entity/
    │   │   │   └── User.java
    │   │   ├── exception/
    │   │   │   └── GlobalExceptionHandler.java
    │   │   ├── repository/
    │   │   │   └── UserRepository.java
    │   │   └── service/
    │   │       └── UserService.java
    │   └── resources/
    │       ├── application.properties
    │       └── db/migration/
    │           └── V1__create_user_table.sql
    └── test/
        ├── java/com/yogaharis/contactmanagement/
        │   ├── controller/
        │   │   ├── UserControllerTest.java
        │   │   └── UserControllerWebMvcTest.java
        │   └── service/
        │       └── UserServiceTest.java
        └── resources/
            └── application-test.properties
```

## Author

Yoga Harisistianugroho ([@YogaHarisCode](https://github.com/YogaHarisCode))