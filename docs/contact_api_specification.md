# Contact API Specification

API Specification for Contact Management module in Contact Management Application.

## 1. Create Contact

Create a new contact for the authenticated user.

* **URL**: `/api/contacts`
* **Method**: `POST`
* **Headers**:
  * `X-API-TOKEN: {token}`
  * `Content-Type: application/json`
  * `Accept: application/json`

### Request Body

```json
{
  "firstName": "Eko",
  "lastName": "Khannedy",
  "email": "eko@example.com",
  "phone": "081234567890"
}
```

*(Note: `firstName` is mandatory; `lastName`, `email`, and `phone` are optional)*

### Response

#### Success (200 OK)

```json
{
  "data": {
    "id": "1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d",
    "firstName": "Eko",
    "lastName": "Khannedy",
    "email": "eko@example.com",
    "phone": "081234567890"
  }
}
```

#### Bad Request (400 Bad Request)

```json
{
  "errors": "First name is required"
}
```

#### Unauthorized (401 Unauthorized)

```json
{
  "errors": "Unauthorized"
}
```

---

## 2. Update Contact

Update an existing contact details.

* **URL**: `/api/contacts/{contactId}`
* **Method**: `PUT`
* **Headers**:
  * `X-API-TOKEN: {token}`
  * `Content-Type: application/json`
  * `Accept: application/json`

### Request Body

```json
{
  "firstName": "Eko",
  "lastName": "Kurniawan",
  "email": "ekokurniawan@example.com",
  "phone": "089876543210"
}
```

### Response

#### Success (200 OK)

```json
{
  "data": {
    "id": "1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d",
    "firstName": "Eko",
    "lastName": "Kurniawan",
    "email": "ekokurniawan@example.com",
    "phone": "089876543210"
  }
}
```

#### Not Found (404 Not Found)

```json
{
  "errors": "Contact is not found"
}
```

#### Unauthorized (401 Unauthorized)

```json
{
  "errors": "Unauthorized"
}
```

---

## 3. Get Contact

Get a specific contact detail by ID.

* **URL**: `/api/contacts/{contactId}`
* **Method**: `GET`
* **Headers**:
  * `X-API-TOKEN: {token}`
  * `Accept: application/json`

### Response

#### Success (200 OK)

```json
{
  "data": {
    "id": "1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d",
    "firstName": "Eko",
    "lastName": "Khannedy",
    "email": "eko@example.com",
    "phone": "081234567890"
  }
}
```

#### Not Found (404 Not Found)

```json
{
  "errors": "Contact is not found"
}
```

#### Unauthorized (401 Unauthorized)

```json
{
  "errors": "Unauthorized"
}
```

---

## 4. Search Contact

Search contacts with filter options and pagination.

* **URL**: `/api/contacts`
* **Method**: `GET`
* **Headers**:
  * `X-API-TOKEN: {token}`
  * `Accept: application/json`
* **Query Parameters**:
  * `name`: (Optional) Search string for `firstName` or `lastName`
  * `email`: (Optional) Search string for `email`
  * `phone`: (Optional) Search string for `phone`
  * `page`: (Optional, default: 0) Page number
  * `size`: (Optional, default: 10) Number of items per page

### Response

#### Success (200 OK)

```json
{
  "data": [
    {
      "id": "1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d",
      "firstName": "Eko",
      "lastName": "Khannedy",
      "email": "eko@example.com",
      "phone": "081234567890"
    }
  ],
  "paging": {
    "currentPage": 0,
    "totalPage": 1,
    "size": 10
  }
}
```

#### Unauthorized (401 Unauthorized)

```json
{
  "errors": "Unauthorized"
}
```

---

## 5. Remove Contact

Delete a contact by ID.

* **URL**: `/api/contacts/{contactId}`
* **Method**: `DELETE`
* **Headers**:
  * `X-API-TOKEN: {token}`
  * `Accept: application/json`

### Response

#### Success (200 OK)

```json
{
  "data": "OK"
}
```

#### Not Found (404 Not Found)

```json
{
  "errors": "Contact is not found"
}
```

#### Unauthorized (401 Unauthorized)

```json
{
  "errors": "Unauthorized"
}
```