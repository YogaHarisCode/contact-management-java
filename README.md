# Contact Management API

A REST API backend for managing users, contacts, and addresses, built with Java and Spring Boot.

This is a portfolio project for learning and demonstrating Java backend development. It is under active development: user registration, login, get current user, update current user, and logout are implemented, and the Contact and Address modules are planned (see [API Documentation](#api-documentation)).

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

`UserArgumentResolverTest` is a unit test for the component that validates the `X-API-TOKEN` header. The same component is also exercised over HTTP by the get current user and update user tests in `UserControllerWebMvcTest` and `UserControllerTest`, and by the logout tests in `AuthControllerWebMvcTest` and `AuthControllerTest`.

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

Endpoints that accept a JSON body return `415 Unsupported Media Type` when `Content-Type` is not JSON.

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

### Update Current User

Updates the name and/or password of the user that owns the token. Only the fields that are sent are changed.

- **Method:** `PATCH`
- **URL:** `/api/users/current`
- **Headers:** `X-API-TOKEN: <token>`, `Content-Type: application/json`, `Accept: application/json`

**Request body**

```json
{
  "name": "John Doe Updated",
  "password": "newsecretpassword"
}
```

| Field | Required | Rules |
|---|---|---|
| `name` | No | Must not be blank when provided. Max 100 characters |
| `password` | No | Must not be blank when provided. At least 8 characters. Max 72 bytes in UTF-8 (BCrypt limit) |

- A field that is left out, or sent as `null`, is not changed.
- An empty JSON object (`{}`) is valid: nothing changes and the response holds the current data.
- Authentication is checked before the body is validated, so an invalid token returns `401` even if the body is also invalid.
- Updating the password does not replace or invalidate the current token.

**Responses**

`200 OK`

```json
{
  "data": {
    "username": "johndoe",
    "name": "John Doe Updated"
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
        "field": "name",
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

`401 Unauthorized` (header missing or blank, token unknown or expired, or the user no longer exists)

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
curl -X PATCH http://localhost:8080/api/users/current \
  -H "X-API-TOKEN: <token>" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{"name":"John Doe Updated","password":"newsecretpassword"}'
```

### Logout User

Revokes the token of the user that owns it. The request has no body.

- **Method:** `DELETE`
- **URL:** `/api/auth/logout`
- **Headers:** `X-API-TOKEN: <token>`, `Accept: application/json`

- The token and its expiry are cleared, so the token stops working immediately.
- Only the user who owns the token is affected.
- The user can log in again to get a new token.
- Calling logout again with the same token returns `401`, because the token is no longer known.
- A token that was already replaced by a newer login is rejected with `401` and never revokes the newer token.

**Responses**

`200 OK`

```json
{
  "data": "OK"
}
```

`401 Unauthorized` (header missing or blank, token unknown, expired or without expiry, the user no longer exists, or the token was replaced by a newer login)

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
curl -X DELETE http://localhost:8080/api/auth/logout \
  -H "X-API-TOKEN: <token>" \
  -H "Accept: application/json"
```

### Authentication token

Login issues a token that clients send in the `X-API-TOKEN` header. A request is accepted only if the token belongs to a user and has not expired; otherwise it is rejected with `401 Unauthorized`. This check is implemented in `UserArgumentResolver`: any controller method that declares a `User` parameter is authenticated automatically. Currently `GET /api/users/current`, `PATCH /api/users/current`, and `DELETE /api/auth/logout` use it. Logout clears the token, so it is rejected from then on and the user must log in again to get a new one. Full Spring Security integration is planned.

### Planned endpoints

These endpoints are not implemented yet.

| Module | Method | URL |
|---|---|---|
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
    │   │   │   ├── UpdateUserRequest.java
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