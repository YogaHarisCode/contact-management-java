# Contact Management API

A REST API backend for managing users, contacts, and addresses, built with Java and Spring Boot.

This is a portfolio project for learning and demonstrating Java backend development. It is under active development: user registration, login, and get current user are implemented, and the remaining modules are planned (see [API Documentation](#api-documentation)).

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
| Unit test | `UserServiceTest`, `AuthServiceTest` | Service logic in isolation (repository, password encoder and clock mocked) |
| Controller slice test | `UserControllerWebMvcTest`, `AuthControllerWebMvcTest` | Controller layer, validation, and error responses (`@WebMvcTest`, service mocked) |
| Integration test | `UserControllerTest`, `AuthControllerTest` | Full flow from controller to the `contact_management_test` database |

`UserArgumentResolverTest` is a unit test for the component that validates the `X-API-TOKEN` header. The same component is also exercised over HTTP by the get current user tests in `UserControllerWebMvcTest` and `UserControllerTest`.

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
| `errors.code` | HTTP status (for example `409 CONFLICT`, `401 UNAUTHORIZED`), or `VALIDATION_ERROR` for validation failures |
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
| `password` | Yes | At least 8 characters. Max 72 bytes in UTF-8 (BCrypt limit) |
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

### Login User

Authenticates a user and returns an API token.

- **Method:** `POST`
- **URL:** `/api/auth/login`
- **Headers:** `Content-Type: application/json`, `Accept: application/json`

**Request body**

```json
{
  "username": "johndoe",
  "password": "secretpassword"
}
```

| Field | Required | Rules |
|---|---|---|
| `username` | Yes | Must not be blank. Max 100 characters |
| `password` | Yes | Must not be blank. Max 72 bytes in UTF-8 (BCrypt limit) |

**Responses**

`200 OK`

```json
{
  "data": {
    "token": "7d3c5a9e-1b2f-4c6d-8e0a-9f1b2c3d4e5f",
    "tokenExpiredAt": 1893456000000
  }
}
```

| Field | Description |
|---|---|
| `token` | Random UUID. Valid for 30 days. Each login replaces the user's previous token |
| `tokenExpiredAt` | Expiry time of the token, in epoch milliseconds |

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

`401 Unauthorized` (unknown username or wrong password, same response for both)

```json
{
  "errors": {
    "code": "401 UNAUTHORIZED",
    "message": "Username or password wrong"
  }
}
```

**Example**

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{"username":"johndoe","password":"secretpassword"}'
```

### Get Current User

Returns the profile of the user that owns the token.

- **Method:** `GET`
- **URL:** `/api/users/current`
- **Headers:** `X-API-TOKEN: <token>`, `Accept: application/json`

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

`401 Unauthorized` (header missing or blank, token unknown, or token expired)

```json
{
  "errors": {
    "code": "401 UNAUTHORIZED",
    "message": "Unauthorized"
  }
}
```

**Example**

```bash
curl http://localhost:8080/api/users/current \
  -H "X-API-TOKEN: <token>" \
  -H "Accept: application/json"
```

### Authentication token

Login issues a token that clients send in the `X-API-TOKEN` header. A request is accepted only if the token belongs to a user and has not expired; otherwise it is rejected with `401 Unauthorized`. This check is implemented in `UserArgumentResolver`: any controller method that declares a `User` parameter is authenticated automatically. Currently only `GET /api/users/current` uses it. Full Spring Security integration is planned.

### Planned endpoints

These endpoints are not implemented yet.

| Module | Method | URL |
|---|---|---|
| Auth | `DELETE` | `/api/auth/logout` |
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
    │   │   │   ├── ClockConfig.java
    │   │   │   ├── PasswordEncoderConfig.java
    │   │   │   └── WebMvcConfig.java
    │   │   ├── constraint/
    │   │   │   ├── MaxBytes.java
    │   │   │   └── MaxBytesValidator.java
    │   │   ├── controller/
    │   │   │   ├── AuthController.java
    │   │   │   └── UserController.java
    │   │   ├── dto/
    │   │   │   ├── ErrorDetail.java
    │   │   │   ├── ErrorResponse.java
    │   │   │   ├── LoginUserRequest.java
    │   │   │   ├── RegisterUserRequest.java
    │   │   │   ├── TokenResponse.java
    │   │   │   ├── UserResponse.java
    │   │   │   └── WebResponse.java
    │   │   ├── entity/
    │   │   │   └── User.java
    │   │   ├── exception/
    │   │   │   └── GlobalExceptionHandler.java
    │   │   ├── repository/
    │   │   │   └── UserRepository.java
    │   │   ├── resolver/
    │   │   │   └── UserArgumentResolver.java
    │   │   └── service/
    │   │       ├── AuthService.java
    │   │       └── UserService.java
    │   └── resources/
    │       ├── application.properties
    │       └── db/migration/
    │           ├── V1__create_user_table.sql
    │           ├── V2__add_token_to_user.sql
    │           └── V3__rename_token_unique_index.sql
    └── test/
        ├── java/com/yogaharis/contactmanagement/
        │   ├── controller/
        │   │   ├── AuthControllerTest.java
        │   │   ├── AuthControllerWebMvcTest.java
        │   │   ├── UserControllerTest.java
        │   │   └── UserControllerWebMvcTest.java
        │   ├── resolver/
        │   │   └── UserArgumentResolverTest.java
        │   └── service/
        │       ├── AuthServiceTest.java
        │       └── UserServiceTest.java
        └── resources/
            └── application-test.properties
```

## Author

Yoga Harisistianugroho ([@YogaHarisCode](https://github.com/YogaHarisCode))