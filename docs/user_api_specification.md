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

#### Unsupported Media Type (415)

Returned when `Content-Type` is not JSON.

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

Update current authenticated user information (name and/or password). Only the fields that are sent are changed.

- **URL**: `/api/users/current`
- **Method**: `PATCH`
- **Headers**:
  - `X-API-TOKEN: {token}`
  - `Content-Type: application/json`
  - `Accept: application/json`

The user is identified by the token in `X-API-TOKEN`.

### Request Body
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

Behavior:
- A field that is left out, or sent as `null`, is not changed.
- An empty JSON object (`{}`) is valid. Nothing changes and the response holds the current data.
- Fields other than `name` and `password` in the body are ignored.
- Authentication is checked before the body is validated. A request with an invalid token returns `401` even if its body is also invalid.
- The token is not replaced or invalidated by this request, including when the password changes. It stays valid until it expires or the next login replaces it.

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

The response never contains the password, token, or token expiry.

#### Bad Request (400 Bad Request)

Validation failed:
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

Returned when the `X-API-TOKEN` header is missing or blank, the token is unknown or expired, or the user no longer exists:
```json
{
  "errors": {
    "code": "401 UNAUTHORIZED",
    "message": "Unauthorized"
  }
}
```

#### Unsupported Media Type (415)

Returned when `Content-Type` is not JSON.

---

## 5. Logout User

Invalidate the current authentication token.

- **URL**: `/api/auth/logout`
- **Method**: `DELETE`
- **Headers**:
  - `X-API-TOKEN: {token}`
  - `Accept: application/json`

The user is identified by the token in `X-API-TOKEN`. No request body or query parameter is used.

Behavior:
- The token and its expiry stored for the user are cleared. The token stops working immediately.
- Only the user who owns the token is affected.
- The user can log in again to get a new token.
- Calling logout again with the same token returns `401`, because the token is no longer known.
- A token that was already replaced by a newer login is rejected with `401`. It never revokes the newer token.

### Response

#### Success (200 OK)
```json
{
  "data": "OK"
}
```

#### Unauthorized (401 Unauthorized)

Returned when the `X-API-TOKEN` header is missing or blank, the token is unknown, expired or has no expiry, the user no longer exists, or the token was replaced by a newer login:
```json
{
  "errors": {
    "code": "401 UNAUTHORIZED",
    "message": "Unauthorized"
  }
}
```