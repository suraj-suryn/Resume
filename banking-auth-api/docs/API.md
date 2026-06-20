# API Documentation — Banking Auth API

Base URL: `http://localhost:8080`  
All protected endpoints require: `Authorization: Bearer <access_token>`

---

## Authentication Endpoints

### POST `/api/auth/register`

Register a new user account. No authentication required.

**Request Body**
```json
{
  "username": "suraj123",
  "email": "suraj@example.com",
  "password": "secret123"
}
```

| Field | Type | Constraints |
|---|---|---|
| `username` | String | Required, 3–50 chars, unique |
| `email` | String | Required, valid email format, unique |
| `password` | String | Required, min 6 chars |

**Response `201 Created`**
```json
{
  "success": true,
  "message": "User registered successfully",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600000,
    "username": "suraj123",
    "role": "USER"
  }
}
```

**Error Responses**

| Status | Condition |
|---|---|
| `400 Bad Request` | Validation failure (missing/invalid fields) |
| `409 Conflict` | Username or email already exists |

---

### POST `/api/auth/login`

Authenticate with username and password.

**Request Body**
```json
{
  "username": "suraj123",
  "password": "secret123"
}
```

**Response `200 OK`**
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600000,
    "username": "suraj123",
    "role": "USER"
  }
}
```

**Error Responses**

| Status | Condition |
|---|---|
| `400 Bad Request` | Missing username or password |
| `401 Unauthorized` | Invalid credentials |

---

### POST `/api/auth/refresh`

Exchange a refresh token for a new access token.

**Request Header**
```
Refresh-Token: eyJhbGciOiJIUzI1NiJ9...
```

**Response `200 OK`**
```json
{
  "success": true,
  "message": "Token refreshed successfully",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600000,
    "username": "suraj123",
    "role": "USER"
  }
}
```

**Error Responses**

| Status | Condition |
|---|---|
| `400 Bad Request` | Missing Refresh-Token header |
| `401 Unauthorized` | Token expired or invalid |

---

## Account Endpoints

All account endpoints require a valid Bearer JWT token.

---

### POST `/api/accounts`

Open a new bank account for the authenticated user.

**Request Header**
```
Authorization: Bearer <access_token>
```

**Request Body**
```json
{
  "accountType": "SAVINGS"
}
```

| Field | Type | Values |
|---|---|---|
| `accountType` | Enum | `SAVINGS`, `CURRENT`, `FIXED_DEPOSIT` |

**Response `201 Created`**
```json
{
  "success": true,
  "message": "Account created successfully",
  "data": {
    "id": 1,
    "accountNumber": "ACC1718900000001-a3f2",
    "balance": 0.00,
    "accountType": "SAVINGS",
    "createdAt": "2026-06-20T10:00:00",
    "active": true
  }
}
```

---

### GET `/api/accounts`

List all active accounts belonging to the authenticated user.

**Response `200 OK`**
```json
{
  "success": true,
  "message": "Accounts retrieved successfully",
  "data": [
    {
      "id": 1,
      "accountNumber": "ACC1718900000001-a3f2",
      "balance": 1500.00,
      "accountType": "SAVINGS",
      "createdAt": "2026-06-20T10:00:00",
      "active": true
    }
  ]
}
```

---

### GET `/api/accounts/{id}`

Get details of a specific account. The account must belong to the authenticated user.

**Path Parameter**

| Parameter | Type | Description |
|---|---|---|
| `id` | Long | Account ID |

**Response `200 OK`**
```json
{
  "success": true,
  "message": "Account retrieved successfully",
  "data": {
    "id": 1,
    "accountNumber": "ACC1718900000001-a3f2",
    "balance": 1500.00,
    "accountType": "SAVINGS",
    "createdAt": "2026-06-20T10:00:00",
    "active": true
  }
}
```

**Error Responses**

| Status | Condition |
|---|---|
| `404 Not Found` | Account ID does not exist |
| `403 Forbidden` | Account belongs to a different user |

---

### DELETE `/api/accounts/{id}`

Soft-close an account (marks as inactive, does not delete data).

**Response `200 OK`**
```json
{
  "success": true,
  "message": "Account closed successfully"
}
```

**Error Responses**

| Status | Condition |
|---|---|
| `404 Not Found` | Account ID does not exist |
| `403 Forbidden` | Account belongs to a different user |

---

## Transaction Endpoints

---

### POST `/api/transactions/transfer/{fromAccountId}`

Transfer funds from one account to another.  
The source account must belong to the authenticated user.  
The entire operation is atomic (`@Transactional`) — either both debit and credit succeed or both roll back.

**Path Parameter**

| Parameter | Type | Description |
|---|---|---|
| `fromAccountId` | Long | Source account ID |

**Request Body**
```json
{
  "targetAccountNumber": "ACC1718900000002-b7c1",
  "amount": 250.00,
  "description": "Rent payment"
}
```

| Field | Type | Constraints |
|---|---|---|
| `targetAccountNumber` | String | Required, must exist and be active |
| `amount` | Decimal | Required, minimum 0.01 |
| `description` | String | Optional |

**Response `200 OK`**
```json
{
  "success": true,
  "message": "Transfer completed successfully",
  "data": {
    "id": 10,
    "referenceNumber": "TXN-550e8400-e29b-41d4-a716",
    "amount": 250.00,
    "transactionType": "DEBIT",
    "description": "Rent payment",
    "targetAccountNumber": "ACC1718900000002-b7c1",
    "createdAt": "2026-06-20T14:30:00"
  }
}
```

**Error Responses**

| Status | Condition |
|---|---|
| `400 Bad Request` | Amount ≤ 0, or missing required fields |
| `403 Forbidden` | Source account not owned by authenticated user |
| `404 Not Found` | Source or target account not found |
| `422 Unprocessable Entity` | Insufficient funds in source account |

---

### GET `/api/transactions/account/{accountId}`

Retrieve paginated transaction history for an account.  
The account must belong to the authenticated user.

**Path Parameter**

| Parameter | Type | Description |
|---|---|---|
| `accountId` | Long | Account ID |

**Query Parameters**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `page` | Integer | `0` | Page number (0-based) |
| `size` | Integer | `10` | Records per page |

**Example Request**
```
GET /api/transactions/account/1?page=0&size=10
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**Response `200 OK`**
```json
{
  "success": true,
  "message": "Transactions retrieved successfully",
  "data": [
    {
      "id": 10,
      "referenceNumber": "TXN-550e8400-e29b-41d4-a716",
      "amount": 250.00,
      "transactionType": "DEBIT",
      "description": "Rent payment",
      "targetAccountNumber": "ACC1718900000002-b7c1",
      "createdAt": "2026-06-20T14:30:00"
    },
    {
      "id": 9,
      "referenceNumber": "TXN-3f6a9c11-71e3-4b28-9f01",
      "amount": 1000.00,
      "transactionType": "CREDIT",
      "description": "Salary",
      "targetAccountNumber": null,
      "createdAt": "2026-06-19T09:00:00"
    }
  ]
}
```

---

## Common Response Format

All endpoints return a consistent `ApiResponse<T>` wrapper:

```json
{
  "success": true | false,
  "message": "Human-readable message",
  "data": { ... }
}
```

The `data` field is omitted (`null`) when there is no body (e.g., close account).

---

## Error Response Format

```json
{
  "success": false,
  "message": "Detailed error description"
}
```

For validation errors, `data` contains a field-level map:

```json
{
  "success": false,
  "message": "Validation failed",
  "data": {
    "username": "must not be blank",
    "password": "size must be between 6 and 2147483647"
  }
}
```

---

## HTTP Status Code Reference

| Code | Meaning |
|---|---|
| `200 OK` | Request succeeded |
| `201 Created` | Resource created successfully |
| `400 Bad Request` | Validation failure or malformed request |
| `401 Unauthorized` | Missing or invalid JWT |
| `403 Forbidden` | Authenticated but not authorized |
| `404 Not Found` | Resource does not exist |
| `409 Conflict` | Duplicate resource (username/email) |
| `422 Unprocessable Entity` | Business rule violation (insufficient funds) |
| `500 Internal Server Error` | Unexpected server error |

---

## JWT Token Details

| Property | Value |
|---|---|
| Algorithm | HS256 (HMAC-SHA256) |
| Access token expiry | 1 hour (configurable) |
| Refresh token expiry | 24 hours (configurable) |
| Custom claims | `role` |

**Decoded access token payload example:**
```json
{
  "sub": "suraj123",
  "role": "USER",
  "iat": 1718875200,
  "exp": 1718878800
}
```

---

## Interactive API Explorer

Start the application and open Swagger UI:  
`http://localhost:8080/swagger-ui.html`

1. Click **Authorize** (top right)
2. Enter `Bearer <your_access_token>`
3. All protected endpoints will include the token automatically
