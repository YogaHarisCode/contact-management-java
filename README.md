# Contact Management API

A RESTful API for managing users, contacts, and addresses, built with Spring Boot.
The project follows a layered architecture (controller, service, repository) with consistent
JSON responses and centralized error handling.

## Status

| Module  | Feature                                      | Status      |
|---------|----------------------------------------------|-------------|
| User    | Register user                                | Implemented |
| User    | Login, current profile, update, logout       | Planned     |
| Contact | Create, update, get, search, remove          | Planned     |
| Address | Create, update, get, list, remove            | Planned     |

## Tech Stack

- **Language:** Java 25
- **Framework:** Spring Boot 4.1.1 (Spring Web MVC)
- **Persistence:** Spring Data JPA (Hibernate), HikariCP connection pool
- **Database:** MySQL
- **Validation:** Jakarta Bean Validation
- **Security:** `spring-security-crypto` (BCrypt password hashing only)
- **Utilities:** Lombok, SLF4J logging
- **Testing:** JUnit 5, Spring Boot Test, `RestTestClient`
- **Build tool:** Maven

## Prerequisites

- JDK 25
- Maven 3.9+ (or the Maven wrapper, if present in the repository)
- MySQL 8.x
- Docker (optional, only as a quick way to start a local MySQL instance)

## Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/YogaHarisCode/contact-management-java.git
cd contact-management-java
```

### 2. Prepare the database

The application does not create the schema automatically
(`spring.jpa.hibernate.ddl-auto` is not set, so Hibernate leaves the schema untouched).
Create the database and the `users` table with any MySQL client (MySQL Workbench, DBeaver,
or the `mysql` CLI):

```sql
CREATE DATABASE IF NOT EXISTS contact_management CHARACTER SET utf8mb4;
USE contact_management;

CREATE TABLE users (
    username         VARCHAR(100) NOT NULL,
    password         VARCHAR(100) NOT NULL,
    name             VARCHAR(100) NOT NULL,
    token            VARCHAR(100) NULL,
    token_expired_at BIGINT       NULL,
    PRIMARY KEY (username)
) ENGINE = InnoDB;
```

Optional: start MySQL with Docker (replace the password placeholder):

```bash
docker run --name contact-mysql \
  -e MYSQL_ROOT_PASSWORD=<your-password> \
  -e MYSQL_DATABASE=contact_management \
  -p 3306:3306 -d mysql:8
```

> With MySQL's default collations, string comparison is case-insensitive, so the usernames
> `John` and `john` are treated as the same username.

### 3. Configure the application

Settings are read from `src/main/resources/application.properties` and can be overridden with
environment variables:

| Variable            | Description                          | Default                                           |
|---------------------|--------------------------------------|---------------------------------------------------|
| `DATABASE_URL`      | JDBC URL of the database             | `jdbc:mysql://localhost:3306/contact_management`  |
| `DATABASE_USERNAME` | Database user                        | `root`                                            |
| `DATABASE_PASSWORD` | Database password                    | Local development default, always override it     |
| `LOG_LEVEL`         | Log level of the `contactmanagement` package | `INFO`                                    |

Bash / macOS / Linux:

```bash
export DATABASE_PASSWORD='<your-password>'
export LOG_LEVEL=DEBUG
```

Windows PowerShell:

```powershell
$env:DATABASE_PASSWORD = "<your-password>"
$env:LOG_LEVEL = "DEBUG"
```

### 4. Run the application

```bash
mvn spring-boot:run
```

The API is available at `http://localhost:8080`.

To build and run a standalone jar instead:

```bash
mvn clean package -DskipTests
java -jar target/contact-management-java-0.0.1-SNAPSHOT.jar
```

### 5. Run the tests

```bash
mvn test
```

The integration tests use the configured datasource and delete all rows from `users` before
each test. Point `DATABASE_URL` to a dedicated test schema, never to a database holding data
you want to keep.

## API Documentation

### Conventions

- Base URL: `http://localhost:8080`
- Requests and responses use `application/json`.
- Every response uses the same envelope. Fields that are `null` are omitted.

Success:

```json
{
  "data": { }
}
```

Error:

```json
{
  "errors": "Human readable message"
}
```

### Register User

Creates a new user account. This endpoint is public.

- **URL:** `/api/users`
- **Method:** `POST`
- **Headers:** `Content-Type: application/json`, `Accept: application/json`

**Request body**

| Field      | Type   | Required | Rules                    |
|------------|--------|----------|--------------------------|
| `username` | string | Yes      | Not blank, max 100 chars |
| `password` | string | Yes      | 8 to 100 characters      |
| `name`     | string | Yes      | Not blank, max 100 chars |

```json
{
  "username": "johndoe",
  "password": "secretpassword",
  "name": "John Doe"
}
```

The password is hashed with BCrypt before it is stored and is never returned.

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

`400 Bad Request`, validation failed. Violations are listed as `<field> <message>`, sorted
alphabetically and separated by `; ` (messages come from the default Bean Validation
messages):

```json
{
  "errors": "password size must be between 8 and 100; username must not be blank"
}
```

`400 Bad Request`, username already registered:

```json
{
  "errors": "Username already exists"
}
```

`400 Bad Request`, missing or malformed JSON body:

```json
{
  "errors": "Malformed or missing request body"
}
```

**Example**

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{"username":"johndoe","password":"secretpassword","name":"John Doe"}'
```

> Errors raised by the framework itself (for example an unsupported `Content-Type` or an
> unknown URL) currently use Spring Boot's default error format instead of the envelope above.

### Planned Endpoints

These endpoints are specified but not implemented yet.

| Module  | Method   | Endpoint                                          | Purpose                      |
|---------|----------|---------------------------------------------------|------------------------------|
| User    | `POST`   | `/api/auth/login`                                 | Log in and receive a token   |
| User    | `GET`    | `/api/users/current`                              | Get the current profile      |
| User    | `PATCH`  | `/api/users/current`                              | Update name and/or password  |
| User    | `DELETE` | `/api/auth/logout`                                | Invalidate the token         |
| Contact | `POST`   | `/api/contacts`                                   | Create a contact             |
| Contact | `PUT`    | `/api/contacts/{contactId}`                       | Update a contact             |
| Contact | `GET`    | `/api/contacts/{contactId}`                       | Get a contact                |
| Contact | `GET`    | `/api/contacts`                                   | Search contacts (paginated)  |
| Contact | `DELETE` | `/api/contacts/{contactId}`                       | Remove a contact             |
| Address | `POST`   | `/api/contacts/{contactId}/addresses`             | Create an address            |
| Address | `PUT`    | `/api/contacts/{contactId}/addresses/{addressId}` | Update an address            |
| Address | `GET`    | `/api/contacts/{contactId}/addresses/{addressId}` | Get an address               |
| Address | `GET`    | `/api/contacts/{contactId}/addresses`             | List addresses of a contact  |
| Address | `DELETE` | `/api/contacts/{contactId}/addresses/{addressId}` | Remove an address            |

Authenticated endpoints will expect the token in the `X-API-TOKEN` header.

## Project Structure

```text
contact-management-java
├── pom.xml
├── README.md
└── src
    ├── main
    │   ├── java/contactmanagement          # root package, also holds the @SpringBootApplication class
    │   │   ├── config
    │   │   │   └── PasswordEncoderConfig.java
    │   │   ├── controller
    │   │   │   └── UserController.java
    │   │   ├── entity
    │   │   │   └── User.java
    │   │   ├── exception
    │   │   │   └── GlobalExceptionHandler.java
    │   │   ├── model                       # request/response DTOs and the response envelope
    │   │   │   ├── RegisterUserRequest.java
    │   │   │   ├── UserResponse.java
    │   │   │   └── WebResponse.java
    │   │   ├── repository
    │   │   │   └── UserRepository.java
    │   │   └── service
    │   │       ├── UserService.java
    │   │       └── UserServiceImpl.java
    │   └── resources
    │       └── application.properties
    └── test/java/contactmanagement/controller
        └── UserControllerTest.java
```

## Design Notes

- **Layering:** controllers handle HTTP only, services hold business rules, repositories
  handle persistence, and DTOs keep entities out of API responses.
- **Registration safety:** the username check gives a friendly error, while the database
  primary key guards against concurrent duplicate registrations.
- **Sensitive data:** passwords are stored as BCrypt hashes and are excluded from responses
  and logs.
- **Error handling:** `GlobalExceptionHandler` maps exceptions to the standard error envelope.