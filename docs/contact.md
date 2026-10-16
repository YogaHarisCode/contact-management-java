# Contact API Spec

Spesifikasi API untuk modul **Contact** pada project **Contact Management**.

Dokumen ini melanjutkan konvensi di `user.md` (format response, header autentikasi, dan kode status). Semua endpoint Contact **membutuhkan autentikasi**, dan setiap contact hanya dapat diakses oleh user pemiliknya.

## Daftar Isi

1. [Konvensi Umum](#konvensi-umum)
2. [Create Contact](#1-create-contact)
3. [Get Contact](#2-get-contact)
4. [Update Contact](#3-update-contact)
5. [Remove Contact](#4-remove-contact)
6. [Search Contact](#5-search-contact)
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

### Data Contact

| Field       | Tipe   | Constraint                                   |
|-------------|--------|----------------------------------------------|
| `id`        | string | Dibuat server (UUID), tidak dapat diubah     |
| `firstName` | string | Wajib, 1–100 karakter                        |
| `lastName`  | string | Opsional, maksimal 100 karakter              |
| `email`     | string | Opsional, format email valid, maksimal 100 karakter |
| `phone`     | string | Opsional, maksimal 20 karakter, hanya angka dengan awalan `+` opsional |

### Autentikasi

Semua endpoint wajib menyertakan header:

| Header        | Nilai                         |
|---------------|-------------------------------|
| `X-API-TOKEN` | Token yang didapat dari login |

### Kepemilikan Data

Contact terikat ke user yang membuatnya. Mengakses contact milik user lain dianggap sama seperti contact yang tidak ada, yaitu `404 Not Found`. Server tidak boleh membedakan keduanya, agar keberadaan data milik user lain tidak bocor.

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

Untuk error validasi, `errors` berisi pesan per field:

```json
{
  "errors": {
    "firstName": "must not be blank",
    "email": "must be a well-formed email address"
  }
}
```

### Kode Status HTTP

| Status | Arti                                                      |
|--------|-----------------------------------------------------------|
| `200`  | Request berhasil                                          |
| `201`  | Resource berhasil dibuat                                  |
| `400`  | Request tidak valid (validasi gagal / parameter salah)    |
| `401`  | Tidak terautentikasi (token tidak ada, salah, atau kedaluwarsa) |
| `404`  | Contact tidak ditemukan                                   |
| `500`  | Kesalahan internal server                                 |

---

## 1. Create Contact

Membuat contact baru untuk user yang sedang login.

- **Method:** `POST`
- **URL:** `/api/contacts`
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
  "firstName": "Budi",
  "lastName": "Santoso",
  "email": "budi@example.com",
  "phone": "081234567890"
}
```

### Validasi

| Field       | Aturan                                    |
|-------------|-------------------------------------------|
| `firstName` | Wajib diisi, maksimal 100 karakter        |
| `lastName`  | Opsional, maksimal 100 karakter           |
| `email`     | Opsional, format email valid, maks 100 karakter |
| `phone`     | Opsional, maksimal 20 karakter, angka dengan awalan `+` opsional |

### Response Sukses — `201 Created`

```json
{
  "data": {
    "id": "3f1b7c52-8d4e-4a96-b1c3-5e2f9a0d7c18",
    "firstName": "Budi",
    "lastName": "Santoso",
    "email": "budi@example.com",
    "phone": "081234567890"
  }
}
```

### Response Error

**`400 Bad Request`** — validasi gagal

```json
{
  "errors": {
    "firstName": "must not be blank",
    "email": "must be a well-formed email address"
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

## 2. Get Contact

Mengambil detail satu contact berdasarkan ID.

- **Method:** `GET`
- **URL:** `/api/contacts/{contactId}`
- **Autentikasi:** Diperlukan

### Path Parameter

| Parameter   | Tipe   | Keterangan         |
|-------------|--------|--------------------|
| `contactId` | string | ID contact (UUID)  |

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
  "data": {
    "id": "3f1b7c52-8d4e-4a96-b1c3-5e2f9a0d7c18",
    "firstName": "Budi",
    "lastName": "Santoso",
    "email": "budi@example.com",
    "phone": "081234567890"
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

**`404 Not Found`** — contact tidak ada atau bukan milik user ini

```json
{
  "errors": "Contact not found"
}
```

---

## 3. Update Contact

Memperbarui seluruh data contact (full update). Field yang tidak dikirim akan dianggap kosong, kecuali `firstName` yang wajib.

- **Method:** `PUT`
- **URL:** `/api/contacts/{contactId}`
- **Autentikasi:** Diperlukan

### Path Parameter

| Parameter   | Tipe   | Keterangan         |
|-------------|--------|--------------------|
| `contactId` | string | ID contact (UUID)  |

### Request Header

| Header         | Nilai              |
|----------------|--------------------|
| `Content-Type` | `application/json` |
| `Accept`       | `application/json` |
| `X-API-TOKEN`  | Token hasil login  |

### Request Body

```json
{
  "firstName": "Budi",
  "lastName": "Santoso Putra",
  "email": "budi.putra@example.com",
  "phone": "+6281234567890"
}
```

### Validasi

Sama dengan [Create Contact](#validasi).

### Response Sukses — `200 OK`

```json
{
  "data": {
    "id": "3f1b7c52-8d4e-4a96-b1c3-5e2f9a0d7c18",
    "firstName": "Budi",
    "lastName": "Santoso Putra",
    "email": "budi.putra@example.com",
    "phone": "+6281234567890"
  }
}
```

### Response Error

**`400 Bad Request`** — validasi gagal

```json
{
  "errors": {
    "firstName": "must not be blank"
  }
}
```

**`401 Unauthorized`** — token tidak ada, tidak valid, atau kedaluwarsa

```json
{
  "errors": "Unauthorized"
}
```

**`404 Not Found`** — contact tidak ada atau bukan milik user ini

```json
{
  "errors": "Contact not found"
}
```

---

## 4. Remove Contact

Menghapus contact berdasarkan ID.

- **Method:** `DELETE`
- **URL:** `/api/contacts/{contactId}`
- **Autentikasi:** Diperlukan

### Path Parameter

| Parameter   | Tipe   | Keterangan         |
|-------------|--------|--------------------|
| `contactId` | string | ID contact (UUID)  |

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

**`404 Not Found`** — contact tidak ada atau bukan milik user ini

```json
{
  "errors": "Contact not found"
}
```

---

## 5. Search Contact

Mencari contact milik user yang sedang login dengan filter opsional dan paginasi. Tanpa filter, endpoint ini mengembalikan seluruh contact milik user secara terpaginasi.

- **Method:** `GET`
- **URL:** `/api/contacts`
- **Autentikasi:** Diperlukan

### Query Parameter

| Parameter | Tipe    | Wajib | Default | Keterangan                                                        |
|-----------|---------|-------|---------|-------------------------------------------------------------------|
| `name`    | string  | Tidak | -       | Pencarian parsial (case-insensitive) pada `firstName` **atau** `lastName` |
| `email`   | string  | Tidak | -       | Pencarian parsial (case-insensitive) pada `email`                 |
| `phone`   | string  | Tidak | -       | Pencarian parsial pada `phone`                                    |
| `page`    | integer | Tidak | `0`     | Nomor halaman, dimulai dari `0`. Harus `>= 0`                     |
| `size`    | integer | Tidak | `10`    | Jumlah data per halaman. Harus `1`–`100`                          |

Jika beberapa filter dikirim bersamaan, semuanya digabung dengan operator **AND**.

Contoh:

```
GET /api/contacts?name=budi&email=example.com&page=0&size=10
```

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
  "data": [
    {
      "id": "3f1b7c52-8d4e-4a96-b1c3-5e2f9a0d7c18",
      "firstName": "Budi",
      "lastName": "Santoso",
      "email": "budi@example.com",
      "phone": "081234567890"
    },
    {
      "id": "9a2d4e61-0c7b-4f35-8e1a-6b3c5d7f2a90",
      "firstName": "Budiman",
      "lastName": "Hakim",
      "email": "budiman@example.com",
      "phone": "081298765432"
    }
  ],
  "paging": {
    "currentPage": 0,
    "totalPage": 1,
    "size": 10
  }
}
```

| Field                 | Keterangan                        |
|-----------------------|-----------------------------------|
| `data`                | Daftar contact (kosong jika tidak ada hasil) |
| `paging.currentPage`  | Halaman saat ini (dimulai dari `0`) |
| `paging.totalPage`    | Total jumlah halaman              |
| `paging.size`         | Jumlah data per halaman           |

Jika tidak ada hasil, response tetap `200` dengan `data` berupa array kosong:

```json
{
  "data": [],
  "paging": {
    "currentPage": 0,
    "totalPage": 0,
    "size": 10
  }
}
```

### Response Error

**`400 Bad Request`** — parameter paginasi tidak valid

```json
{
  "errors": "size must be between 1 and 100"
}
```

**`401 Unauthorized`** — token tidak ada, tidak valid, atau kedaluwarsa

```json
{
  "errors": "Unauthorized"
}
```