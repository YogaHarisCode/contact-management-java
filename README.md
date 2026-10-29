# Contact Management API

A RESTful API for managing users, contacts, and addresses, built with Spring Boot. It follows a
layered architecture (controller, service, repository) with a single response envelope for every
endpoint and centralized error handling.

## Features

| Module  | Feature                             | Status  |
|---------|-------------------------------------|---------|
| User    | Register user                       | Done    |
| User    | Login (token authentication)        | Done    |
| User    | Get current user profile            | Done    |
| User    | Update current user profile         | Done    |
| User    | Logout                              | Done    |
| Contact | Create, update, get, search, delete | Planned |
| Address | Create, update, get, list, delete   | Planned |

## Tech Stack

| Area        | Choice                                                         |
|-------------|----------------------------------------------------------------|
| Language    | Java 25                                                        |
| Framework   | Spring Boot 4.1.1 (Spring Web MVC)                             |
| Persistence | Spring Data JPA (Hibernate), HikariCP, MySQL                   |
| Validation  | Jakarta Bean Validation                                        |
| Security    | `spring-security-crypto` (BCrypt hashing, no filter chain)     |
| Utilities   | Lombok, SLF4J                                                  |
| Testing     | JUnit Jupiter, Mockito, Spring Boot Test (`RestTestClient`)    |
| Build       | Maven                                                          |

## Prerequisites

- JDK 25
- Apache Maven
- MySQL (8.x recommended), running locally or reachable from your machine

Docker is not required, and the repository does not include a Docker setup.

## Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/YogaHarisCode/contact-management-java.git
cd contact-management-java
```

### 2. Create the database

Hibernate does not manage the schema (`spring.jpa.hibernate.ddl-auto` is not set), so create it
manually:

```sql
CREATE DATABASE IF NOT EXISTS contact_management CHARACTER SET utf8mb4;
USE contact_management;

CREATE TABLE users (
    username         VARCHAR(100) NOT NULL,
    password         VARCHAR(100) NOT NULL,
    name             VARCHAR(100) NOT NULL,
    token            VARCHAR(100),
    token_expired_at BIGINT,
    PRIMARY KEY (username),
    CONSTRAINT unique_token UNIQUE (token)
) ENGINE = InnoDB;
```

### 3. Configure the application

Configuration is read from environment variables, with local-development defaults defined in
`src/main/resources/application.properties`:

| Variable            | Default                                          | Description                                  |
|---------------------|--------------------------------------------------|----------------------------------------------|
| `DATABASE_URL`      | `jdbc:mysql://localhost:3306/contact_management` | JDBC URL                                     |
| `DATABASE_USERNAME` | `root`                                           | Database user                                |
| `DATABASE_PASSWORD` | `1234`                                           | Database password                            |
| `LOG_LEVEL`         | `INFO`                                           | Log level of the `contactmanagement` package |

Set your own credentials before running (the defaults are for local development only).

Linux/macOS:

```bash
export DATABASE_USERNAME=myuser
export DATABASE_PASSWORD=mypassword
```

Windows PowerShell:

```powershell
$env:DATABASE_USERNAME = "myuser"
$env:DATABASE_PASSWORD = "mypassword"
```

### 4. Run the application

```bash
mvn spring-boot:run
```

The API is served at `http://localhost:8080`.

### 5. Try it

```bash
# Register
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" -H "Accept: application/json" \
  -d '{"username":"johndoe","password":"secretpassword","name":"John Doe"}'

# Login (copy the token from the response)
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" -H "Accept: application/json" \
  -d '{"username":"johndoe","password":"secretpassword"}'

# Get the current user
curl http://localhost:8080/api/users/current \
  -H "Accept: application/json" -H "X-API-TOKEN: <token>"

# Update the current user
curl -X PATCH http://localhost:8080/api/users/current \
  -H "Content-Type: application/json" -H "Accept: application/json" \
  -H "X-API-TOKEN: <token>" \
  -d '{"name":"John Doe Updated"}'
```

## API Documentation

### Conventions

Every response uses the same envelope. A successful response carries `data`, a failed one carries
`errors` (a string). Fields that are `null` are omitted.

```json
{ "data": { } }
```

```json
{ "errors": "message" }
```

Protected endpoints require the `X-API-TOKEN` header with the token returned by login.

### Endpoint summary

| Method  | URL                  | Auth  | Description                  |
|---------|----------------------|-------|------------------------------|
| `POST`  | `/api/users`         | No    | Register a new user          |
| `POST`  | `/api/auth/login`    | No    | Log in and receive a token   |
| `GET`   | `/api/users/current` | Token | Get the current user profile |
| `PATCH` | `/api/users/current` | Token | Update the current user      |

### Register User

`POST /api/users` (public)

Request body:

| Field      | Type   | Rules                    |
|------------|--------|--------------------------|
| `username` | string | Required, max 100 chars  |
| `password` | string | Required, 8 to 100 chars |
| `name`     | string | Required, max 100 chars  |

```json
{
  "username": "johndoe",
  "password": "secretpassword",
  "name": "John Doe"
}
```

Responses:

| Status | Condition                      | Body                                                                               |
|--------|--------------------------------|------------------------------------------------------------------------------------|
| 200    | User created                   | `{"data":{"username":"johndoe","name":"John Doe"}}`                                |
| 400    | Validation failed              | `{"errors":"password size must be between 8 and 100; username must not be blank"}` |
| 400    | Username already registered    | `{"errors":"Username already exists"}`                                             |
| 400    | Missing or malformed JSON body | `{"errors":"Malformed or missing request body"}`                                   |

The password is hashed with BCrypt before it is stored and is never returned.

### Login User

`POST /api/auth/login` (public)

Request body:

| Field      | Type   | Rules                   |
|------------|--------|-------------------------|
| `username` | string | Required, max 100 chars |
| `password` | string | Required, max 100 chars |

Responses:

| Status | Condition                          | Body                                                                                       |
|--------|------------------------------------|--------------------------------------------------------------------------------------------|
| 200    | Credentials valid                  | `{"data":{"token":"3c829e12-4217-48f5-a169-715a31bc0d6d","tokenExpiredAt":1900000000000}}` |
| 400    | Validation failed                  | `{"errors":"password must not be blank"}`                                                  |
| 400    | Missing or malformed JSON body     | `{"errors":"Malformed or missing request body"}`                                           |
| 401    | Unknown username or wrong password | `{"errors":"Username or password wrong"}`                                                  |

The token is a random UUID. `tokenExpiredAt` is epoch milliseconds (UTC) and the token is valid
for 30 days. A new login replaces the previous token.

### Get Current User

`GET /api/users/current` (requires `X-API-TOKEN`)

Headers:

| Header        | Description                          |
|---------------|--------------------------------------|
| `X-API-TOKEN` | Token returned by the login endpoint |

Responses:

| Status | Condition                                                | Body                                                |
|--------|----------------------------------------------------------|-----------------------------------------------------|
| 200    | Token valid                                              | `{"data":{"username":"johndoe","name":"John Doe"}}` |
| 401    | Header missing or blank, token unknown, or token expired | `{"errors":"Unauthorized"}`                         |

### Update Current User

`PATCH /api/users/current` (requires `X-API-TOKEN`)

Headers:

| Header         | Description                          |
|----------------|--------------------------------------|
| `X-API-TOKEN`  | Token returned by the login endpoint |
| `Content-Type` | `application/json`                   |

Request body. Every field is optional, and an omitted field keeps its current value:

| Field      | Type   | Rules                                                      |
|------------|--------|------------------------------------------------------------|
| `name`     | string | Max 100 chars, must not be blank                           |
| `password` | string | 8 to 100 chars, must not be blank; stored as a BCrypt hash |

```json
{
  "name": "John Doe Updated",
  "password": "newsecretpassword"
}
```

Responses:

| Status | Condition                                      | Body                                                      |
|--------|------------------------------------------------|-----------------------------------------------------------|
| 200    | Profile updated                                | `{"data":{"username":"johndoe","name":"John Doe Updated"}}` |
| 400    | Validation failed (for example short password) | `{"errors":"password size must be between 8 and 100"}`    |
| 400    | Missing or malformed JSON body                 | `{"errors":"Malformed or missing request body"}`          |
| 401    | Header missing or blank, token unknown, or token expired | `{"errors":"Unauthorized"}`                     |

Authentication is checked before the body is validated, so a request without a valid token
always returns 401, even when the body is also invalid. The username and the token are never
changed by this endpoint, and the password is never returned.

## Testing

```bash
mvn test
```

| Test class                 | Type                                                                  | Needs MySQL |
|----------------------------|-----------------------------------------------------------------------|-------------|
| `UserControllerTest`       | `@SpringBootTest` integration (register, get and update current user) | Yes         |
| `AuthControllerTest`       | `@SpringBootTest` integration (login)                                 | Yes         |
| `UserControllerWebMvcTest` | `@WebMvcTest`, service and repository mocked (get and update)         | No          |
| `AuthControllerWebMvcTest` | `@WebMvcTest`, `AuthService` mocked                                   | No          |
| `UserServiceImplTest`      | Mockito unit test (get and update current user)                       | No          |
| `AuthServiceImplTest`      | Mockito unit test                                                     | No          |
| `UserArgumentResolverTest` | Mockito unit test                                                     | No          |

The integration tests use the datasource from `application.properties` and delete all rows in
`users`. Point them to a dedicated test database. To run only the tests that need no database:

```bash
mvn test -Dtest='UserControllerWebMvcTest*,AuthControllerWebMvcTest,AuthServiceImplTest,UserServiceImplTest,UserArgumentResolverTest'
```

## Project Structure

```text
contact-management-java
├── pom.xml
└── src
    ├── main