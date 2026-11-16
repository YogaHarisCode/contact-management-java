# User API Specification

API Specification for User Management module in Contact Management Application.

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

### Response

#### Success (200 OK)
```json
{
  "data": {
    "token": "3c829e12-4217-48f5-a169-715a31bc0d6d"
  }
}
```

#### Unauthorized (401 Unauthorized)
```json
{
  "errors": {
    "code": "401 UNAUTHORIZED",
    "message": "Username or password wrong"
  }
}
```

---

## 3. Get User (Current Profile)

Get detail profile of current authenticated user.

- **URL**: `/api/users/current`
- **Method**: `GET`
- **Headers**:
  - `X-API-TOKEN: {token}`
  - `Accept: application/json`

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

## 4. Update User

Update current authenticated user information (Name and/or Password).

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