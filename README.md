# Tiny Ledger API

A simple REST ledger API.

## Features

- Create accounts
- Record money movements (deposits and withdrawals) within an account
- View current balance for a given account
- View transaction history for a given account

## Assumptions and Design Decisions

1. **In-memory storage**: Data is stored in a `ConcurrentHashMap` structures and will be lost when the application restarts.

2. **Account IDs**: Generated as UUIDs to ensure uniqueness.

3. **No negative balance**: Withdrawals that exceed the current balance are rejected.

4. **Atomic operations**: Basic thread safety is provided via a `ConcurrentHashMap`, but complex atomic operations (like simultaneous deposits/withdrawals) are not guaranteed to be atomic and thread safe.

5. **No specified currency**: For the sake of simplicity, there is no mention of currency in the ledger app. 

## Getting Started

### Prerequisites

- Java 21 or higher
- Maven 3.6+

### Running the Application

```bash
# Using Maven
mvn spring-boot:run
```

The application will start on `http://localhost:8080`.

## API Documentation

### Base URL
```
http://localhost:8080/api/v1/ledger
```

### Endpoints

#### 1. Create Account
Creates a new account with a unique ID and zero balance.

```http
POST /api/v1/ledger/accounts
```

**Response:** `201 Created`
```json
{
  "accountId": "xxxxx (UUID)"
}
```

#### 2. Get Account Balance
Retrieves the current balance for an account.

```http
GET /api/v1/ledger/accounts/{accountId}/balance
```

**Response:** `200 OK` and current balance

**Error Response:** `404 Not Found` if account doesn't exist.

#### 3. Get Transaction History
Retrieves all transactions for an account, sorted by timestamp (most recent first).

```http
GET /api/v1/ledger/accounts/{accountId}/transactions
```

**Response:** `200 OK`
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440001",
    "accountId": "550e8400-e29b-41d4-a716-446655440000",
    "amount": 50.00,
    "type": "WITHDRAWAL",
    "timestamp": "2025-12-03T10:30:00Z"
  },
  {
    "id": "550e8400-e29b-41d4-a716-446655440002",
    "accountId": "550e8400-e29b-41d4-a716-446655440000",
    "amount": 200.00,
    "type": "DEPOSIT",
    "timestamp": "2025-12-03T10:00:00Z"
  }
]
```

**Error Response:** `404 Not Found` if account doesn't exist.

#### 4. Create Transaction
Performs a deposit or withdrawal for an account.

```http
POST /api/v1/ledger/accounts/{accountId}/transactions

{
  "amount": 100.00,
  "type": "DEPOSIT"
}
```

**Transaction Types:**
- `DEPOSIT` - Adds money to the account
- `WITHDRAWAL` - Removes money from the account

**Response:** `201 Created`
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440003",
  "accountId": "550e8400-e29b-41d4-a716-446655440000",
  "amount": 100.00,
  "type": "DEPOSIT",
  "timestamp": "2025-12-03T11:00:00Z"
}
```

**Error Responses:**
- `404 Not Found` - Account doesn't exist
- `400 Bad Request` - Invalid amount (zero, negative, or null)
- `400 Bad Request` - Insufficient balance for withdrawal

## Testing with cURL

Step-by-step commands to test the API:

### 1. Create an account

```bash
curl -X POST http://localhost:8080/api/v1/ledger/accounts
```

Response:
```json
{"accountId":"a]1b2c3d4-e5f6-7890-abcd-ef1234567890"}
```

Save the account ID for the next steps:
```bash
ACCOUNT_ID="your-account-id-here"
```

### 2. Check initial balance (should be 0)

```bash
curl http://localhost:8080/api/v1/ledger/accounts/$ACCOUNT_ID/balance
```

Response:
```json
0
```

### 3. Make a deposit of 100.00

```bash
curl -X POST http://localhost:8080/api/v1/ledger/accounts/$ACCOUNT_ID/transactions \
  -H "Content-Type: application/json" \
  -d '{"amount": 100.00, "type": "DEPOSIT"}'
```

Response:
```json
{"id":"...","accountId":"...","amount":100.00,"type":"DEPOSIT","timestamp":"..."}
```

### 4. Make another deposit of 50.00

```bash
curl -X POST http://localhost:8080/api/v1/ledger/accounts/$ACCOUNT_ID/transactions \
  -H "Content-Type: application/json" \
  -d '{"amount": 50.00, "type": "DEPOSIT"}'
```

### 5. Check balance (should be 150.00)

```bash
curl http://localhost:8080/api/v1/ledger/accounts/$ACCOUNT_ID/balance
```

Response:
```json
150.00
```

### 6. Make a withdrawal of 30.00

```bash
curl -X POST http://localhost:8080/api/v1/ledger/accounts/$ACCOUNT_ID/transactions \
  -H "Content-Type: application/json" \
  -d '{"amount": 30.00, "type": "WITHDRAWAL"}'
```

### 7. Check balance (should be 120.00)

```bash
curl http://localhost:8080/api/v1/ledger/accounts/$ACCOUNT_ID/balance
```

Response:
```json
120.00
```

### 8. View transaction history

```bash
curl http://localhost:8080/api/v1/ledger/accounts/$ACCOUNT_ID/transactions
```

Response (most recent first):
```json
[
  {"id":"...","accountId":"...","amount":30.00,"type":"WITHDRAWAL","timestamp":"..."},
  {"id":"...","accountId":"...","amount":50.00,"type":"DEPOSIT","timestamp":"..."},
  {"id":"...","accountId":"...","amount":100.00,"type":"DEPOSIT","timestamp":"..."}
]
```

### 9. Try to withdraw more than the balance (should fail)

```bash
curl -X POST http://localhost:8080/api/v1/ledger/accounts/$ACCOUNT_ID/transactions \
  -H "Content-Type: application/json" \
  -d '{"amount": 500.00, "type": "WITHDRAWAL"}'
```

Response (400 Bad Request):
```json
{"status":400,"message":"Account has insufficient balance for this transaction","timestamp":"..."}
```

### 10. Try to access a non-existent account (should fail)

```bash
curl http://localhost:8080/api/v1/ledger/accounts/non-existent-id/balance
```

Response (404 Not Found):
```json
{"status":404,"message":"Account not found with id: non-existent-id","timestamp":"..."}
```
