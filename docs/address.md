# Address API Spec

Spesifikasi API untuk modul **Address** pada project **Contact Management**.

Dokumen ini melanjutkan konvensi di `user.md` dan `contact.md` (format response, header autentikasi, dan kode status). Address merupakan sub-resource dari Contact, sehingga semua endpoint berada di bawah `/api/contacts/{contactId}/addresses` dan **membutuhkan autentikasi**.

## Daftar Isi

1. [Konvensi Umum](#konvensi-umum)
2. [Create Address](#1-create-address)
3. [Get Address](#2-get-address)
4. [Update Address](#3-update-address)
5. [Remove Address](#4-remove-address)
6. [List Address](#5-list-address)
7. [Ringkasan Endpoint](#ringkasan-endpoint)
8. [Catatan Implementasi](#catatan-implementasi)

---

## Konvensi Umum

### Base URL

```
/api/contacts/{contactId}/addresses
```

### Format Data

- Semua request dan response menggunakan `application/json`.
- Semua field bertipe `string`.

### Data Address

| Field        | Tipe   | Constraint                                   |
|--------------|--------|----------------------------------------------|
| `id`         | string | Dibuat server (UUID), tidak dapat diubah     |
| `street`     | string | Opsional, maksimal 200 karakter              |
| `city`       | string | Opsional, maksimal 100 karakter              |
| `province`   | string | Opsional, maksimal 100 karakter              |
| `country`    | string | Wajib, 1–100 karakter                        |
| `postalCode` | string | Wajib, 1–10 karakter                         |

> `postalCode` bertipe `string`, bukan angka, agar nol di depan (mis. `01234`) dan format non-numerik di negara lain tidak hilang.

### Autentikasi

Semua endpoint wajib menyertakan header:

| Header        | Nilai                         |
|---------------|-------------------------------|
| `X-API-TOKEN` | Token yang didapat dari login |

### Kepemilikan Data

Address terikat ke satu contact, dan contact terikat ke satu user. Akses hanya diizinkan jika contact milik user yang sedang login.

- Contact tidak ada **atau** bukan milik user ini → `404` dengan pesan `Contact not found`.
- Address tidak ada pada contact tersebut → `404` dengan pesan `Address not found`.

Server tidak boleh membedakan "tidak ada" dan "milik user lain" agar keberadaan data user lain tidak bocor. Address juga harus dipastikan milik `contactId` pada URL, bukan sekadar ada di database.

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
    "country": "must not be blank",
    "postalCode": "must not be blank"
  }
}
```

### Kode Status HTTP

| Status | Arti                                                      |
|--------|-----------------------------------------------------------|
| `200`  | Request berhasil                                          |
| `201`  | Resource berhasil dibuat                                  |
| `400`  | Request tidak valid (validasi gagal / body tidak terbaca) |
| `401`  | Tidak terautentikasi (token tidak ada, salah, atau kedaluwarsa) |
| `404`  | Contact atau address tidak ditemukan                      |
| `500`  | Kesalahan internal server                                 |

---

## 1. Create Address

Membuat address baru untuk sebuah contact.

- **Method:** `POST`
- **URL:** `/api/contacts/{contactId}/addresses`
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
  "street": "Jalan Merdeka No. 10",
  "city": "Bandung",
  "province": "Jawa Barat",
  "country": "Indonesia",
  "postalCode": "40111"
}
```

### Validasi

| Field        | Aturan                              |
|--------------|-------------------------------------|
| `street`     | Opsional, maksimal 200 karakter     |
| `city`       | Opsional, maksimal 100 karakter     |
| `province`   | Opsional, maksimal 100 karakter     |
| `country`    | Wajib diisi, maksimal 100 karakter  |
| `postalCode` | Wajib diisi, maksimal 10 karakter   |

### Response Sukses — `201 Created`

```json
{
  "data": {
    "id": "b7e2a9d4-5c13-4f08-9a6e-1d3f8c2b7a45",
    "street": "Jalan Merdeka No. 10",
    "city": "Bandung",
    "province": "Jawa Barat",
    "country": "Indonesia",
    "postalCode": "40111"
  }
}
```

### Response Error

**`400 Bad Request`** — validasi gagal

```json
{
  "errors": {
    "country": "must not be blank",
    "postalCode": "must not be blank"
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

## 2. Get Address

Mengambil detail satu address berdasarkan ID.

- **Method:** `GET`
- **URL:** `/api/contacts/{contactId}/addresses/{addressId}`
- **Autentikasi:** Diperlukan

### Path Parameter

| Parameter   | Tipe   | Keterangan         |
|-------------|--------|--------------------|
| `contactId` | string | ID contact (UUID)  |
| `addressId` | string | ID address (UUID)  |

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
    "id": "b7e2a9d4-5c13-4f08-9a6e-1d3f8c2b7a45",
    "street": "Jalan Merdeka No. 10",
    "city": "Bandung",
    "province": "Jawa Barat",
    "country": "Indonesia",
    "postalCode": "40111"
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

**`404 Not Found`** — address tidak ada pada contact tersebut

```json
{
  "errors": "Address not found"
}
```

---

## 3. Update Address

Memperbarui seluruh data address (full update). Field opsional yang tidak dikirim akan dianggap kosong.

- **Method:** `PUT`
- **URL:** `/api/contacts/{contactId}/addresses/{addressId}`
- **Autentikasi:** Diperlukan

### Path Parameter

| Parameter   | Tipe   | Keterangan         |
|-------------|--------|--------------------|
| `contactId` | string | ID contact (UUID)  |
| `addressId` | string | ID address (UUID)  |

### Request Header

| Header         | Nilai              |
|----------------|--------------------|
| `Content-Type` | `application/json` |
| `Accept`       | `application/json` |
| `X-API-TOKEN`  | Token hasil login  |

### Request Body

```json
{
  "street": "Jalan Asia Afrika No. 25",
  "city": "Bandung",
  "province": "Jawa Barat",
  "country": "Indonesia",
  "postalCode": "40112"
}
```

### Validasi

Sama dengan [Create Address](#validasi).

### Response Sukses — `200 OK`

```json
{
  "data": {
    "id": "b7e2a9d4-5c13-4f08-9a6e-1d3f8c2b7a45",
    "street": "Jalan Asia Afrika No. 25",
    "city": "Bandung",
    "province": "Jawa Barat",
    "country": "Indonesia",
    "postalCode": "40112"
  }
}
```

### Response Error

**`400 Bad Request`** — validasi gagal

```json
{
  "errors": {
    "country": "must not be blank"
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

**`404 Not Found`** — address tidak ada pada contact tersebut

```json
{
  "errors": "Address not found"
}
```

---

## 4. Remove Address

Menghapus address berdasarkan ID.

- **Method:** `DELETE`
- **URL:** `/api/contacts/{contactId}/addresses/{addressId}`
- **Autentikasi:** Diperlukan

### Path Parameter

| Parameter   | Tipe   | Keterangan         |
|-------------|--------|--------------------|
| `contactId` | string | ID contact (UUID)  |
| `addressId` | string | ID address (UUID)  |

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

**`404 Not Found`** — address tidak ada pada contact tersebut

```json
{
  "errors": "Address not found"
}
```

---

## 5. List Address

Mengambil seluruh address milik satu contact. Endpoint ini **tidak terpaginasi** karena jumlah address per contact diasumsikan kecil.

- **Method:** `GET`
- **URL:** `/api/contacts/{contactId}/addresses`
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
  "data": [
    {
      "id": "b7e2a9d4-5c13-4f08-9a6e-1d3f8c2b7a45",
      "street": "Jalan Merdeka No. 10",
      "city": "Bandung",
      "province": "Jawa Barat",
      "country": "Indonesia",
      "postalCode": "40111"
    },
    {
      "id": "c4d8f1a2-7e39-4b50-a2c6-9f0e3d5b8a17",
      "street": "Jalan Sudirman No. 5",
      "city": "Jakarta Selatan",
      "province": "DKI Jakarta",
      "country": "Indonesia",
      "postalCode": "12190"
    }
  ]
}
```

Jika contact belum memiliki address, response tetap `200` dengan array kosong:

```json
{
  "data": []
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