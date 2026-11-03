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
| Contact | Create contact                      | Done    |
| Contact | Get contact                         | Done    |
| Contact | Update, search, delete              | Planned |
| Address | Create, update, get, list, delete   | Planned |

## Tech Stack

| Area        | Choice                                                         |
|-------------|----------------------------------------------------------------|
| Language    | Java 25                                                        |
| Framework   | Spring Boot 4.1.1 (Spring Web MVC)                             |
| Persistence | Spring Data JPA (Hibernate), HikariCP, MySQL                   |
| Migration   | Flyway (`spring-boot-starter-flyway`, `flyway-mysql`)          |
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

Flyway creates the tables on startup, but it cannot create the database itself. Create an empty
schema once:

```sql
CREATE DATABASE IF NOT EXISTS contact_management CHARACTER SET utf8mb4;
```

The tables (`users`, `contacts`) are created by the migration
`src/main/resources/db/migration/V1__create_users_and_contacts.sql`. Hibernate only validates the
schema (`spring.jpa.hibernate.ddl-auto=validate`) and never changes it.

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

# Create a contact (copy the id from the response)
curl -X POST http://localhost:8080/api/contacts \
  -H "Content-Type: application/json" -H "Accept: application/json" \
  -H "X-API-TOKEN: <token>" \
  -d '{"firstName":"Eko","lastName":"Khannedy","email":"eko@example.com","phone":"081234567890"}'

# Get a contact
curl http://localhost:8080/api/contacts/<contactId> \
  -H "Accept: application/json" -H "X-API-TOKEN: <token>"

# Logout (invalidates the token)
curl -X DELETE http://localhost:8080/api/auth/logout \
  -H "Accept: application/json" -H "X-API-TOKEN: <token>"
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

| Method   | URL                         | Auth  | Description                              |
|----------|-----------------------------|-------|------------------------------------------|
| `POST`   | `/api/users`                | No    | Register a new user                      |
| `POST`   | `/api/auth/login`           | No    | Log in and receive a token               |
| `GET`    | `/api/users/current`        | Token | Get the current user profile             |
| `PATCH`  | `/api/users/current`        | Token | Update the current user                  |
| `DELETE` | `/api/auth/logout`          | Token | Log out and invalidate the current token |
| `POST`   | `/api/contacts`             | Token | Create a contact for the current user    |
| `GET`    | `/api/contacts/{contactId}` | Token | Get one contact of the current user      |

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

| Status | Condition                                                | Body                                                        |
|--------|----------------------------------------------------------|-------------------------------------------------------------|
| 200    | Profile updated                                          | `{"data":{"username":"johndoe","name":"John Doe Updated"}}` |
| 400    | Validation failed (for example short password)           | `{"errors":"password size must be between 8 and 100"}`      |
| 400    | Missing or malformed JSON body                           | `{"errors":"Malformed or missing request body"}`            |
| 401    | Header missing or blank, token unknown, or token expired | `{"errors":"Unauthorized"}`                                 |

Authentication is checked before the body is validated, so a request without a valid token
always returns 401, even when the body is also invalid. The username and the token are never
changed by this endpoint, and the password is never returned.

### Logout User

`DELETE /api/auth/logout` (requires `X-API-TOKEN`)

Headers:

| Header        | Description                          |
|---------------|--------------------------------------|
| `X-API-TOKEN` | Token returned by the login endpoint |

No request body.

Responses:

| Status | Condition                                                | Body                        |
|--------|----------------------------------------------------------|-----------------------------|
| 200    | Token valid, session ended                               | `{"data":"OK"}`             |
| 401    | Header missing or blank, token unknown, or token expired | `{"errors":"Unauthorized"}` |

Logout clears the token and its expiry time of the current user. The old token is rejected
from then on, so a second logout with the same token returns 401. Other users are not
affected, and the user can log in again to receive a new token.

### Create Contact

`POST /api/contacts` (requires `X-API-TOKEN`)

Headers:

| Header         | Description                          |
|----------------|--------------------------------------|
| `X-API-TOKEN`  | Token returned by the login endpoint |
| `Content-Type` | `application/json`                   |
| `Accept`       | `application/json`                   |

Request body:

| Field       | Type   | Rules                                                                                   |
|-------------|--------|-----------------------------------------------------------------------------------------|
| `firstName` | string | Required, not blank, max 100 chars                                                      |
| `lastName`  | string | Optional, max 100 chars                                                                 |
| `email`     | string | Optional, valid email format, max 200 chars                                             |
| `phone`     | string | Optional, Indonesian mobile number (`+62`, `62`, or `0` prefix, then `8`), no separators |

```json
{
  "firstName": "Eko",
  "lastName": "Khannedy",
  "email": "eko@example.com",
  "phone": "081234567890"
}
```

Responses:

| Status | Condition                                                | Body                                                                                                                                      |
|--------|----------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------|
| 200    | Contact created                                          | `{"data":{"id":"1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d","firstName":"Eko","lastName":"Khannedy","email":"eko@example.com","phone":"081234567890"}}` |
| 400    | Validation failed                                        | `{"errors":"firstName must not be blank"}`                                                                                                |
| 400    | Invalid phone number                                     | `{"errors":"phone must be a valid Indonesian mobile number"}`                                                                             |
| 400    | Missing or malformed JSON body                           | `{"errors":"Malformed or missing request body"}`                                                                                          |
| 401    | Header missing or blank, token unknown, or token expired | `{"errors":"Unauthorized"}`                                                                                                               |

The contact always belongs to the owner of the token; the owner cannot be set in the body. The
`id` is a UUID generated by the server. Authentication is checked before the body is validated,
so an invalid token returns 401 even when the body is also invalid.

### Get Contact

`GET /api/contacts/{contactId}` (requires `X-API-TOKEN`)

Headers:

| Header        | Description                          |
|---------------|--------------------------------------|
| `X-API-TOKEN` | Token returned by the login endpoint |
| `Accept`      | `application/json`                   |

Path parameter:

| Parameter   | Type   | Description                                          |
|-------------|--------|------------------------------------------------------|
| `contactId` | string | Id (UUID) of the contact, as returned by create contact |

No request body.

Responses:

| Status | Condition                                                         | Body                                                                                                                                      |
|--------|-------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------|
| 200    | Contact found and owned by the current user                       | `{"data":{"id":"1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d","firstName":"Eko","lastName":"Khannedy","email":"eko@example.com","phone":"081234567890"}}` |
| 401    | Header missing or blank, token unknown, or token expired          | `{"errors":"Unauthorized"}`                                                                                                               |
| 404    | Contact does not exist, or belongs to another user                | `{"errors":"Contact is not found"}`                                                                                                       |

Only contacts owned by the token's user can be read. A contact that belongs to another user
returns the same 404 as an id that does not exist, so the response never reveals whether an id
is in use. Optional fields that were not provided when the contact was created are left out of
the response.

## Testing

```bash
mvn test
```

| Test class                    | Type                                                                  | Needs MySQL |
|-------------------------------|-----------------------------------------------------------------------|-------------|
| `UserControllerTest`          | `@SpringBootTest` integration (register, get and update current user) | Yes         |
| `AuthControllerTest`          | `@SpringBootTest` integration (login and logout)                      | Yes         |
| `ContactControllerTest`       | `@SpringBootTest` integration (create and get contact)                | Yes         |
| `UserControllerWebMvcTest`    | `@WebMvcTest`, service and repository mocked (get and update)         | No          |
| `AuthControllerWebMvcTest`    | `@WebMvcTest`, `AuthService` and `UserRepository` mocked              | No          |
| `ContactControllerWebMvcTest` | `@WebMvcTest`, `ContactService` and `UserRepository` mocked           | No          |
| `UserServiceImplTest`         | Mockito unit test (get and update current user)                       | No          |
| `AuthServiceImplTest`         | Mockito unit test (login and logout)                                  | No          |
| `ContactServiceImplTest`      | Mockito unit test (create and get contact)                            | No          |
| `UserArgumentResolverTest`    | Mockito unit test                                                     | No          |

Tests use their own configuration in `src/test/resources/application.properties`, which points
to a separate schema, `contact_management_test` (created automatically and migrated by Flyway).
The integration tests delete all rows in `contacts` and `users`, but never touch the main
schema. The test datasource can be changed with `TEST_DATABASE_URL`, `TEST_DATABASE_USERNAME`,
and `TEST_DATABASE_PASSWORD`.

To run only the tests that need no database:

```bash
mvn test -Dtest='*WebMvcTest,*ServiceImplTest,UserArgumentResolverTest'
```

## Project Structure

```text
contact-management-java
├── pom.xml
└── src
    ├── main
    │   ├── java/contactmanagement
    │   │   ├── ContactManagementJavaApplication.java
    │   │   ├── config
    │   │   │   ├── PasswordEncoderConfig.java
    │   │   │   └── WebConfiguration.java
    │   │   ├── controller
    │   │   │   ├── AuthController.java
    │   │   │   ├── ContactController.java
    │   │   │   └── UserController.java
    │   │   ├── entity
    │   │   │   ├── Contact.java
    │   │   │   └── User.java
    │   │   ├── exception
    │   │   │   └── GlobalExceptionHandler.java
    │   │   ├── model
    │   │   │   ├── ContactResponse.java
    │   │   │   ├── CreateContactRequest.java
    │   │   │   ├── LoginResponse.java
    │   │   │   ├── LoginUserRequest.java
    │   │   │   ├── RegisterUserRequest.java
    │   │   │   ├── UpdateUserRequest.java
    │   │   │   ├── UserResponse.java
    │   │   │   └── WebResponse.java
    │   │   ├── repository
    │   │   │   ├── ContactRepository.java
    │   │   │   └── UserRepository.java
    │   │   ├── resolver
    │   │   │   └── UserArgumentResolver.java
    │   │   └── service
    │   │       ├── AuthService.java
    │   │       ├── AuthServiceImpl.java
    │   │       ├── ContactService.java
    │   │       ├── ContactServiceImpl.java
    │   │       ├── UserService.java
    │   │       └── UserServiceImpl.java
    │   └── resources
    │       ├── application.properties
    │       └── db/migration
    │           └── V1__create_users_and_contacts.sql
    └── test
        ├── java/contactmanagement
        │   ├── controller
        │   │   ├── AuthControllerTest.java
        │   │   ├── AuthControllerWebMvcTest.java
        │   │   ├── ContactControllerTest.java
        │   │   ├── ContactControllerWebMvcTest.java
        │   │   ├── UserControllerTest.java
        │   │   └── UserControllerWebMvcTest.java
        │   ├── resolver
        │   │   └── UserArgumentResolverTest.java
        │   └── service
        │       ├── AuthServiceImplTest.java
        │       ├── ContactServiceImplTest.java
        │       └── UserServiceImplTest.java
        └── resources
            └── application.properties
```

## Architecture and Conventions

- **Layers:** `controller` handles HTTP only, `service` holds business rules, `repository` is
  Spring Data, `model` holds request/response DTOs and the envelope, `entity` holds JPA
  entities, `resolver` authenticates requests, `exception` holds the global handler, and
  `config` holds beans and MVC configuration.
- **Authentication:** `UserArgumentResolver` reads the `X-API-TOKEN` header and injects the
  current `User` into controller methods that declare a `User` parameter. An endpoint is
  protected only if it declares that parameter.
- **Ownership:** a contact's owner always comes from the authenticated user, never from the
  request body. Contacts are loaded by owner and id together, so one user can never read
  another user's contact.
- **Errors:** services and the resolver throw `ResponseStatusException`;
  `GlobalExceptionHandler` converts every error into the standard envelope.
- **Validation:** `@Valid @RequestBody` on controllers, Bean Validation annotations on DTOs.
- **Schema:** managed by Flyway migrations in `src/main/resources/db/migration`; Hibernate only
  validates it.
- **Logging:** SLF4J with `key=value` parameters. Passwords, hashes, tokens, and contact
  personal data (name, email, phone) are never logged.

## Current Limitations

- No rate limiting or brute-force protection on registration and login.
- Tokens are stored in plain text in `users.token`.
- Changing the password does not revoke the active token.
- `application.properties` contains local-development fallbacks for the database credentials;
  remove them before any real deployment.
- Only create and get contact are implemented; update, search, and delete contact, and all
  address endpoints, are specified but not implemented yet.