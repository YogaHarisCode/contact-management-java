# Address API Specification

API Specification for Address Management module in Contact Management Application.

## 1. Create Address

Create a new address for a specific contact.

* **URL**: `/api/contacts/{contactId}/addresses`
* **Method**: `POST`
* **Headers**:
  * `X-API-TOKEN: {token}`
  * `Content-Type: application/json`
  * `Accept: application/json`

### Request Body

```json
{
  "street": "Jalan Belakang No. 123",
  "city": "Jakarta Selatan",
  "province": "DKI Jakarta",
  "country": "Indonesia",
  "postalCode": "12345"
}
```

*(Note: `country` is mandatory; `street`, `city`, `province`, and `postalCode` are optional)*

### Response

#### Success (200 OK)

```json
{
  "data": {
    "id": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
    "street": "Jalan Belakang No. 123",
    "city": "Jakarta Selatan",
    "province": "DKI Jakarta",
    "country": "Indonesia",
    "postalCode": "12345"
  }
}
```

#### Bad Request (400 Bad Request)

```json
{
  "errors": "Country is required"
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

## 2. Update Address

Update an existing address for a contact.

* **URL**: `/api/contacts/{contactId}/addresses/{addressId}`
* **Method**: `PUT`
* **Headers**:
  * `X-API-TOKEN: {token}`
  * `Content-Type: application/json`
  * `Accept: application/json`

### Request Body

```json
{
  "street": "Jalan Kebon Jeruk No. 456",
  "city": "Jakarta Barat",
  "province": "DKI Jakarta",
  "country": "Indonesia",
  "postalCode": "54321"
}
```

### Response

#### Success (200 OK)

```json
{
  "data": {
    "id": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
    "street": "Jalan Kebon Jeruk No. 456",
    "city": "Jakarta Barat",
    "province": "DKI Jakarta",
    "country": "Indonesia",
    "postalCode": "54321"
  }
}
```

#### Not Found (404 Not Found)

```json
{
  "errors": "Contact or address is not found"
}
```

#### Unauthorized (401 Unauthorized)

```json
{
  "errors": "Unauthorized"
}
```

---

## 3. Get Address

Get detail of a specific address by ID.

* **URL**: `/api/contacts/{contactId}/addresses/{addressId}`
* **Method**: `GET`
* **Headers**:
  * `X-API-TOKEN: {token}`
  * `Accept: application/json`

### Response

#### Success (200 OK)

```json
{
  "data": {
    "id": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
    "street": "Jalan Kebon Jeruk No. 456",
    "city": "Jakarta Barat",
    "province": "DKI Jakarta",
    "country": "Indonesia",
    "postalCode": "54321"
  }
}
```

#### Not Found (404 Not Found)

```json
{
  "errors": "Contact or address is not found"
}
```

#### Unauthorized (401 Unauthorized)

```json
{
  "errors": "Unauthorized"
}
```

---

## 4. List Address

Get all addresses for a specific contact.

* **URL**: `/api/contacts/{contactId}/addresses`
* **Method**: `GET`
* **Headers**:
  * `X-API-TOKEN: {token}`
  * `Accept: application/json`

### Response

#### Success (200 OK)

```json
{
  "data": [
    {
      "id": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
      "street": "Jalan Kebon Jeruk No. 456",
      "city": "Jakarta Barat",
      "province": "DKI Jakarta",
      "country": "Indonesia",
      "postalCode": "54321"
    }
  ]
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

## 5. Remove Address

Delete an address by ID.

* **URL**: `/api/contacts/{contactId}/addresses/{addressId}`
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
  "errors": "Contact or address is not found"
}
```

#### Unauthorized (401 Unauthorized)

```json
{
  "errors": "Unauthorized"
}
```