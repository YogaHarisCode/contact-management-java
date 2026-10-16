# User API Spec

Spesifikasi API untuk modul **User** pada project **Contact Management**.

## Daftar Isi

1. [Konvensi Umum](#konvensi-umum)
2. [Register User](#1-register-user)
3. [Login User](#2-login-user)
4. [Get User](#3-get-user)
5. [Update User](#4-update-user)
6. [Logout User](#5-logout-user)
7. [Ringkasan Endpoint](#ringkasan-endpoint)
8. [Catatan Implementasi](#catatan-implementasi)

---

## Konvensi Umum

### Base URL

```
/api
```

### Format Data

- Semua request dan response menggunakan `application/json`.
- Semua field bertipe `string` kecuali disebutkan lain.
- Waktu menggunakan **epoch time dalam milidetik** (`number`).

### Data User

| Field      | Tipe   | Constraint                                      |
|------------|--------|-------------------------------------------------|
| `username` | string | Wajib, unik, 1–100 karakter, tanpa spasi        |
| `password` | string | Wajib saat register/login, 8–100 karakter       |
| `name`     | string | Wajib saat register, 1–100 karakter             |

> `password` **tidak pernah** dikembalikan di response mana pun.

### Autentikasi

Endpoint yang membutuhkan autentikasi harus menyertakan header:

| Header        | Nilai                       |
|---------------|-----------------------------|
| `X-API-TOKEN` | Token yang didapat dari login |

### Struktur Response

**Sukses**

```json
{
  "data": { }
}
```

**Error**

```json
{
  "errors": "Pesan error"
}
```

Untuk error validasi, `errors` berisi daftar pesan per field:

```json
{
  "errors": {
    "username": "must not be blank",
    "password": "size must be between 8 and 100"
  }
}
```

### Kode Status HTTP

| Status | Arti                                                        |
|--------|-------------------------------------------------------------|
| `200`  | Request berhasil                                            |
| `201`  | Resource berhasil dibuat                                    |
| `400`  | Request tidak valid (validasi gagal / body tidak terbaca)   |
| `401`  | Tidak terautentikasi (token tidak ada, salah, atau kedaluwarsa) atau kredensial salah |
| `409`  | Konflik data (username sudah dipakai)                       |
| `500`  | Kesalahan internal server                                   |

---

## 1. Register User

Mendaftarkan user baru.

- **Method:** `POST`
- **URL:** `/api/users`
- **Autentikasi:** Tidak diperlukan

### Request Header

| Header         | Nilai              |
|----------------|--------------------|
| `Content-Type` | `application/json` |
| `Accept`       | `application/json` |

### Request Body

```json
{
  "username": "yoga",
  "password": "rahasia123",
  "name": "Yoga Haris"
}
```

### Validasi

| Field      | Aturan                           |
|------------|----------------------------------|
| `username` | Wajib diisi, maksimal 100 karakter, belum terdaftar |
| `password` | Wajib diisi, 8–100 karakter      |
| `name`     | Wajib diisi, maksimal 100 karakter |

### Response Sukses — `201 Created`

```json
{
  "data": {
    "username": "yoga",
    "name": "Yoga Haris"
  }
}
```

### Response Error

**`400 Bad Request`** — validasi gagal

```json
{
  "errors": {
    "username": "must not be blank",
    "password": "size must be between 8 and 100",
    "name": "must not be blank"
  }
}
```

**`409 Conflict`** — username sudah terdaftar

```json
{
  "errors": "Username already registered"
}
```

---

## 2. Login User

Autentikasi user dan menerbitkan token API.

- **Method:** `POST`
- **URL:** `/api/auth/login`
- **Autentikasi:** Tidak diperlukan

### Request Header

| Header         | Nilai              |
|----------------|--------------------|
| `Content-Type` | `application/json` |
| `Accept`       | `application/json` |

### Request Body

```json
{
  "username": "yoga",
  "password": "rahasia123"
}
```

### Validasi

| Field      | Aturan                |
|------------|-----------------------|
| `username` | Wajib diisi           |
| `password` | Wajib diisi           |

### Response Sukses — `200 OK`

```json
{
  "data": {
    "token": "7f3c9d2e-4b1a-4e8f-9c6d-2a5b8e1f0c34",
    "expiredAt": 1767225600000
  }
}
```

| Field       | Keterangan                                   |
|-------------|----------------------------------------------|
| `token`     | Token acak untuk header `X-API-TOKEN`        |
| `expiredAt` | Waktu token kedaluwarsa (epoch milidetik)    |

### Response Error

**`400 Bad Request`** — validasi gagal

```json
{
  "errors": {
    "username": "must not be blank",
    "password": "must not be blank"
  }
}
```

**`401 Unauthorized`** — username atau password salah

```json
{
  "errors": "Username or password wrong"
}
```

> Pesan error sengaja **tidak membedakan** antara username tidak ditemukan dan password salah, untuk mencegah user enumeration.

---

## 3. Get User

Mengambil data user yang sedang login.

- **Method:** `GET`
- **URL:** `/api/users/current`
- **Autentikasi:** Diperlukan

### Request Header

| Header        | Nilai                |
|---------------|----------------------|
| `Accept`      | `application/json`   |
| `X-API-TOKEN` | Token hasil login    |

### Request Body

Tidak ada.

### Response Sukses — `200 OK`

```json
{
  "data": {
    "username": "yoga",
    "name": "Yoga Haris"
  }
}
```

### Response Error

**`401 Unauthorized`** — token tidak ada, tidak valid, atau kedaluwarsa

```json
{
  "errors": "Unauthorized"
}
```

---

## 4. Update User

Memperbarui data user yang sedang login. Semua field bersifat opsional, tetapi **minimal satu field harus dikirim**. Field yang tidak dikirim tidak akan diubah.

- **Method:** `PATCH`
- **URL:** `/api/users/current`
- **Autentikasi:** Diperlukan

### Request Header

| Header         | Nilai              |
|----------------|--------------------|
| `Content-Type` | `application/json` |
| `Accept`       | `application/json` |
| `X-API-TOKEN`  | Token hasil login  |

### Request Body

```json
{
  "name": "Yoga Haris Setiawan",
  "password": "passwordBaru456"
}
```

### Validasi

| Field      | Aturan                                               |
|------------|------------------------------------------------------|
| `name`     | Opsional; jika dikirim: tidak boleh kosong, maks 100 karakter |
| `password` | Opsional; jika dikirim: 8–100 karakter               |

> `username` tidak dapat diubah.

### Response Sukses — `200 OK`

```json
{
  "data": {
    "username": "yoga",
    "name": "Yoga Haris Setiawan"
  }
}
```

### Response Error

**`400 Bad Request`** — validasi gagal atau tidak ada field yang dikirim

```json
{
  "errors": {
    "password": "size must be between 8 and 100"
  }
}
```

**`401 Unauthorized`** — token tidak ada, tidak valid, atau kedaluwarsa

```json
{
  "errors": "Unauthorized"
}
```

---

## 5. Logout User

Menghapus token user sehingga sesi berakhir.

- **Method:** `DELETE`
- **URL:** `/api/auth/logout`
- **Autentikasi:** Diperlukan

### Request Header

| Header        | Nilai              |
|---------------|--------------------|
| `Accept`      | `application/json` |
| `X-API-TOKEN` | Token hasil login  |

### Request Body

Tidak ada.

### Response Sukses — `200 OK`

```json
{
  "data": "OK"
}
```

### Response Error

**`401 Unauthorized`** — token tidak ada, tidak valid, atau kedaluwarsa

```json
{
  "errors": "Unauthorized"
}
```

