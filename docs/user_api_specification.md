# User API Specification

API Specification for User Management module in Contact Management Application.

## Error Response Format

Every error response uses the same envelope. `errors` is an object:

```json
{
  "errors": {
    "code": "401 UNAUTHORIZED",
    "message": "Unauthorized"
  }
}
```

| Field | Description |
|---|---|
| `code` | HTTP status (for example `409 CONFLICT`, `401 UNAUTHORIZED`), or `VALIDATION_ERROR` for validation failures |
| `message` | Human-readable message |
| `details` | List of `{field, message}`. Only present for validation errors |

---

## 1. Register User

Register a new user to the system.

- **URL**: `/api/users`
- **Method**: `POST`
- **Headers**:
  - `Content-Type: application/json`
  - `Accept: application/json`

### Request Body
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

### Response

#### Success (200 OK)
```json
{
  "data": {
    "username": "johndoe",
    "name": "John Doe"
  }
}
```

#### Bad Request (400 Bad Request)

Validation failed:
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

Missing or malformed JSON body:
```json
{
  "errors": {
    "code": "400 BAD_REQUEST",
    "message": "Request body is missing or malformed"
  }
}
```

#### Conflict (409 Conflict)
```json
{
  "errors": {
    "code": "409 CONFLICT",
    "message": "Username already exists"
  }
}
```

---

## 2. Login User

Authenticate user and receive an authentication token.

- **URL**: `/api/auth/login`
- **Method**: `POST`
- **Headers**:
  - `Content-Type: application/json`
  - `Accept: application/json`

### Request Body
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

### Response

#### Success (200 OK)
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

#### Bad Request (400 Bad Request)

Validation failed:
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

Missing or malformed JSON body:
```json
{
  "errors": {
    "code": "400 BAD_REQUEST",
    "message": "Request body is missing or malformed"
  }
}
```

#### Unauthorized (401 Unauthorized)

Unknown username or wrong password (same response for both):
```json
{
  "errors": {
    "code": "401 UNAUTHORIZED",
    "message": "Username or password wrong"
  }
}
```

#### Unsupported Media Type (415)

Returned when `Content-Type` is not JSON.

---

## 3. Get User (Current Profile)

Get detail profile of current authenticated user.

- **URL**: `/api/users/current`
- **Method**: `GET`
- **Headers**:
  - `X-API-TOKEN: {token}`
  - `Accept: application/json`

The user is identified by the token in `X-API-TOKEN`. No request body or query parameter is used.

### Response

#### Success (200 OK)
```json
{
  "data": {
    "username": "johndoe",
    "name": "John Doe"
  }
}
```

The response never contains the password, token, or token expiry.

#### Unauthorized (401 Unauthorized)

Returned when the `X-API-TOKEN` header is missing or blank, the token is unknown, or the token has expired:
```json
{
  "errors": {
    "code": "401 UNAUTHORIZED",
    "message": "Unauthorized"
  }
}
```

---

## 4. Update User

Update current authenticated user information (Name and/or Password).

**Status**: Not implemented yet.

- **URL**: `/api/users/current`
- **Method**: `PATCH`
- **Headers**:
  - `X-API-TOKEN: {token}`
  - `Content-Type: application/json`
  - `Accept: application/json`

### Request Body
```json
{
  "name": "John Doe Updated",
  "password": "newsecretpassword"
}
```
*(Note: All fields are optional)*

### Response

#### Success (200 OK)
```json
{
  "data": {
    "username": "johndoe",
    "name": "John Doe Updated"
  }
}
```

#### Unauthorized (401 Unauthorized)
```json
{
  "errors": {
    "code": "401 UNAUTHORIZED",
    "message": "Unauthorized"
  }
}
```

---

## 5. Logout User

Invalidate the current authentication token.

**Status**: Not implemented yet.

- **URL**: `/api/auth/logout`
- **Method**: `DELETE`
- **Headers**:
  - `X-API-TOKEN: {token}`
  - `Accept: application/json`

### Response

#### Success (200 OK)
```json
{
  "data": "OK"
}
```

#### Unauthorized (401 Unauthorized)
```json
{
  "errors": {
    "code": "401 UNAUTHORIZED",
    "message": "Unauthorized"
  }
}
```