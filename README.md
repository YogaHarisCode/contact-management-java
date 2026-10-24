# Contact Management API

A RESTful API for managing users, contacts, and addresses, built with Spring Boot. It follows
a layered architecture (controller, service, repository) with a single response envelope for
every endpoint and centralized error handling.

## Feature Status

| Module  | Feature                                | Status  |
|---------|----------------------------------------|---------|
| User    | Register user                          | Done    |
| User    | Login                                  | Done    |
| User    | Current profile, update, logout        | Planned |
| Contact | CRUD and search                        | Planned |
| Address | CRUD                                   | Planned |

## Tech Stack

| Area        | Choice                                                          |
|-------------|-----------------------------------------------------------------|
| Language    | Java 25                                                         |
| Framework   | Spring Boot 4.1.1 (Spring Web MVC)                              |
| Persistence | Spring Data JPA (Hibernate), HikariCP, MySQL                    |
| Validation  | Jakarta Bean Validation                                         |
| Security    | `spring-security-crypto` only (BCrypt hashing, no filter chain) |
| Utilities   | Lombok, SLF4J                                                   |
| Testing     | JUnit 5, Mockito, Spring Boot Test, `RestTestClient`            |
| Build       | Maven                                                           |

## Prerequisites

- JDK 25
- Maven 3.9+ (or the Maven wrapper, if the repository includes one)
- MySQL 8+ running locally or reachable over the network
- Git

Docker is not used by this project.

## Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/YogaHarisCode/contact-management-java.git
cd contact-management-java
```

### 2. Create the database

The schema is created manually; Hibernate does not manage it.

```sql
CREATE DATABASE IF NOT EXISTS contact_management CHARACTER SET utf8mb4;
USE contact_management;

CREATE TABLE users (
    username         VARCHAR(100) NOT NULL,
    password         VARCHAR(100) NOT NULL,
    name             VARCHAR(100) NOT NULL,
    token            VARCHAR(100) NULL,
    token_expired_at BIGINT       NULL,
    PRIMARY KEY (username),
    constraint unique_token unique (token)
) ENGINE = InnoDB;
```

### 3. Configure the application

Settings live in `src/main/resources/application.properties` and can be overridden with
environment variables:

| Variable            | Default                                          | Purpose                               |
|---------------------|--------------------------------------------------|---------------------------------------|
| `DATABASE_URL`      | `jdbc:mysql://localhost:3306/contact_management` | JDBC connection URL                   |
| `DATABASE_USERNAME` | `root`                                           | Database user                         |
| `DATABASE_PASSWORD` | `1234`                                           | Database password                     |
| `LOG_LEVEL`         | `INFO`                                           | Log level of the `contactmanagement` package |

The defaults are for local development only. Always set your own credentials and never
deploy with the default password.

### 4. Run the application

```bash
mvn spring-boot:run
```

The API is available at `http://localhost:8080`.

### 5. Run the tests

```bash
mvn test
```

`UserControllerTest` and `AuthControllerTest` run against the datasource configured in
`application.properties` and call `deleteAll()` on the `users` table. Point them to a
dedicated test database, never to data you want to keep.

## API Overview

All responses use the same envelope. Fields that are `null` are omitted.

```json
{ "data": { }, "errors": "message" }
```

A successful response contains `data`; a failed response contains `errors` (a string).

### Register User

Creates a new account. Public endpoint.

- **URL:** `POST /api/users`
- **Headers:** `Content-Type: application/json`, `Accept: application/json`

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

| Status | Condition                      | Body                                                |
|--------|--------------------------------|-----------------------------------------------------|
| 200    | User created                   | `{"data":{"username":"johndoe","name":"John Doe"}}` |
| 400    | Validation failed              | `{"errors":"password size must be between 8 and 100"}` |
| 400    | Username already registered    | `{"errors":"Username already exists"}`              |
| 400    | Missing or malformed JSON body | `{"errors":"Malformed or missing request body"}`    |

The password is hashed with BCrypt before storage and is never returned.

### Login User

Authenticates a user and issues a token. Public endpoint.

- **URL:** `POST /api/auth/login`
- **Headers:** `Content-Type: application/json`, `Accept: application/json`

Request body:

| Field      | Type   | Rules                   |
|------------|--------|-------------------------|
| `username` | string | Required, max 100 chars |
| `password` | string | Required, max 100 chars |

```json
{
  "username": "johndoe",
  "password": "secretpassword"
}
```

Responses:

| Status | Condition                         | Body                                                                   |
|--------|-----------------------------------|------------------------------------------------------------------------|
| 200    | Credentials valid                 | `{"data":{"token":"3c829e12-...","tokenExpiredAt":1900000000000}}`     |
| 400    | Validation failed                 | `{"errors":"password must not be blank"}`                              |
| 400    | Missing or malformed JSON body    | `{"errors":"Malformed or missing request body"}`                       |
| 401    | Unknown username or wrong password| `{"errors":"Username or password wrong"}`                              |

Notes:

- `tokenExpiredAt` is epoch milliseconds (UTC); a token is valid for 30 days.
- A new login replaces the previous token, so each user has one active token at a time.
- Unknown username and wrong password return the same message on purpose, so the API does
  not reveal which usernames exist.

### Planned Endpoints

| Method   | URL                                         | Description                |
|----------|---------------------------------------------|----------------------------|
| `GET`    | `/api/users/current`                        | Get current user profile   |
| `PATCH`  | `/api/users/current`                        | Update name and/or password|
| `DELETE` | `/api/auth/logout`                          | Invalidate current token   |
| `POST`   | `/api/contacts`                             | Create contact             |
| `GET`    | `/api/contacts`                             | Search contacts (paged)    |
| `GET`    | `/api/contacts/{contactId}`                 | Get contact                |
| `PUT`    | `/api/contacts/{contactId}`                 | Update contact             |
| `DELETE` | `/api/contacts/{contactId}`                 | Remove contact             |
| `POST`   | `/api/contacts/{contactId}/addresses`       | Create address             |
| `GET`    | `/api/contacts/{contactId}/addresses`       | List addresses             |
| `GET`    | `/api/contacts/{contactId}/addresses/{id}`  | Get address                |
| `PUT`    | `/api/contacts/{contactId}/addresses/{id}`  | Update address             |
| `DELETE` | `/api/contacts/{contactId}/addresses/{id}`  | Remove address             |

The planned endpoints are specified in the `*_api_specification.md` files and are not
implemented yet.

## Project Structure

```text
contact-management-java
├── pom.xml
└── src
    ├── main
    │   ├── java/contactmanagement
    │   │   ├── config
    │   │   │   └── PasswordEncoderConfig.java
    │   │   ├── controller
    │   │   │   ├── AuthController.java
    │   │   │   └── UserController.java
    │   │   ├── entity
    │   │   │   └── User.java
    │   │   ├── exception
    │   │   │   └── GlobalExceptionHandler.java
    │   │   ├── model
    │   │   │   ├── LoginResponse.java
    │   │   │   ├── LoginUserRequest.java
    │   │   │   ├── RegisterUserRequest.java
    │   │   │   ├── UserResponse.java
    │   │   │   └── WebResponse.java
    │   │   ├── repository
    │   │   │   └── UserRepository.java
    │   │   └── service
    │   │       ├── AuthService.java
    │   │       ├── AuthServiceImpl.java
    │   │       ├── UserService.java
    │   │       └── UserServiceImpl.java
    │   └── resources
    │       └── application.properties
    └── test/java/contactmanagement
        ├── controller
        │   ├── AuthControllerTest.java
        │   ├── AuthControllerWebMvcTest.java
        │   └── UserControllerTest.java
        └── service
            └── AuthServiceImplTest.java
```

The `@SpringBootApplication` entry-point class sits in the `contactmanagement` package.

## Architecture

- **controller:** HTTP concerns only (mapping, validation trigger, response envelope).
- **service:** business rules; an interface plus one implementation per service.
- **repository:** Spring Data JPA repositories.
- **entity:** JPA entities; they never leave the service layer.
- **model:** request and response DTOs and the `WebResponse` envelope.
- **exception:** `GlobalExceptionHandler` converts every error into the `WebResponse` format.
- **config:** Spring beans, such as the BCrypt `PasswordEncoder`.

## Logging

SLF4J with `key=value` parameters. Passwords, hashes, and tokens are never logged. Set
`LOG_LEVEL=DEBUG` to see method-entry logs.