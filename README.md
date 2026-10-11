# 🏦 SpringBank — Banking System REST API

<p align="center">
  <strong>A secure, transaction-safe banking backend built with Java and Spring Boot.</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk" alt="Java 21" />
  <img src="https://img.shields.io/badge/Spring_Boot-4.1.1-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" alt="Spring Boot" />
  <img src="https://img.shields.io/badge/PostgreSQL-Database-4169E1?style=for-the-badge&logo=postgresql&logoColor=white" alt="PostgreSQL" />
  <img src="https://img.shields.io/badge/JWT-Authentication-black?style=for-the-badge&logo=jsonwebtokens" alt="JWT" />
</p>

SpringBank is a REST API for banking operations, including authentication, account management, deposits, withdrawals, fund transfers, and transaction history.

The project focuses on backend engineering fundamentals such as **transaction management, concurrency control, idempotency, and ownership-based authorization**.

---

## ✨ Features

| Feature | Description |
|---|---|
| 🔐 Authentication | JWT-based registration and login with BCrypt password hashing |
| 🏦 Account Management | Create, list, and view savings or current accounts |
| 💰 Deposits & Withdrawals | Validated balance operations with pessimistic locking |
| 🔄 Fund Transfers | Atomic debit, credit, and ledger updates |
| 🛡️ Idempotency | Safe retries for supported money operations |
| 📜 Transaction History | Paginated history with type and date filters |
| 🔒 Authorization | Users can access only their own accounts |
| ⚠️ Error Handling | Consistent JSON errors and validation messages |
| 📘 API Documentation | Interactive Swagger UI with JWT authorization |

## 🧰 Tech Stack

<p>
  <img src="https://img.shields.io/badge/Java-21-orange?logo=openjdk" alt="Java" />
  <img src="https://img.shields.io/badge/Spring_Boot-4.1.1-6DB33F?logo=springboot" alt="Spring Boot" />
  <img src="https://img.shields.io/badge/PostgreSQL-4169E1?logo=postgresql&logoColor=white" alt="PostgreSQL" />
  <img src="https://img.shields.io/badge/Spring_Security-6DB33F?logo=springsecurity&logoColor=white" alt="Spring Security" />
  <img src="https://img.shields.io/badge/Maven-C71A36?logo=apachemaven&logoColor=white" alt="Maven" />
  <img src="https://img.shields.io/badge/JUnit_5-25A162?logo=junit5&logoColor=white" alt="JUnit 5" />
</p>

- **Language:** Java 21
- **Framework:** Spring Boot
- **Database:** PostgreSQL with Spring Data JPA
- **Security:** Spring Security, JWT, BCrypt
- **API Documentation:** Springdoc OpenAPI / Swagger UI
- **Testing:** JUnit 5, Mockito, MockMvc, H2
- **Build Tool:** Maven Wrapper
- **Containerization:** Docker

## 🏗️ Architecture

SpringBank follows a layered architecture that separates HTTP handling, business logic, persistence, and security.

```text
Client
  │
  ▼
REST Controllers
  │
  ▼
Service Layer
  │
  ▼
Spring Data JPA Repositories
  │
  ▼
PostgreSQL Database
```

### Project Structure

```text
src/main/java/com/example/banking/
├── config/        # Security and OpenAPI configuration
├── controller/    # REST API endpoints
├── dto/           # Request and response objects
├── exception/     # Custom exceptions and error handling
├── model/         # JPA entities
├── repository/    # Database access
├── security/      # JWT authentication
├── service/       # Banking business logic
└── util/          # Account number and idempotency utilities
```

### Engineering Highlights

- **Atomic transfers:** Sender debit, receiver credit, and ledger entries execute in one database transaction.
- **Concurrency safety:** Pessimistic write locks protect balance-changing operations.
- **Idempotent operations:** Request fingerprints help distinguish legitimate retries from reused keys with different parameters.
- **Ownership checks:** Account access is restricted to the authenticated owner.

---

## ⚙️ Getting Started

### Prerequisites

- Java Development Kit (JDK) 21
- PostgreSQL 14 or later
- Git
- Docker Desktop (optional)

Maven does not need to be installed separately because the project includes the Maven Wrapper.

### 1. Clone the Repository

```bash
git clone https://github.com/ankitpasvan/banking-system.git
cd banking-system
```

> Replace the repository URL above if your actual GitHub repository uses a different URL.

### 2. Create the Database

Open PostgreSQL and run:

```sql
CREATE DATABASE banking;
```

### 3. Configure Environment Variables

In Windows PowerShell, set the values for your current terminal session:

```powershell
$env:DATABASE_URL = "jdbc:postgresql://localhost:5432/banking"
$env:DATABASE_USERNAME = "your_db_username"
$env:DATABASE_PASSWORD = "your_db_password"
$env:JWT_SECRET = "your_base64_encoded_secret"
$env:JWT_EXPIRATION_MS = "3600000"
```

Generate a Base64-encoded secret containing 32 random bytes:

```powershell
$bytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
[Convert]::ToBase64String($bytes)
$rng.Dispose()
```

Copy the generated value into `JWT_SECRET`. Keep it private and never commit it to Git.

### 4. Run Tests

```powershell
.\mvnw.cmd clean test
```

### 5. Start the Application

```powershell
.\mvnw.cmd spring-boot:run
```

The API runs at:

**http://localhost:8080**

---

## 🐳 Run with Docker

Build the application first:

```powershell
.\mvnw.cmd clean package -DskipTests
docker build -t springbank .
```

Start the container:

```powershell
docker run -p 8080:8080 `
  -e DATABASE_URL="jdbc:postgresql://host.docker.internal:5432/banking" `
  -e DATABASE_USERNAME="your_db_username" `
  -e DATABASE_PASSWORD="your_db_password" `
  -e JWT_SECRET="your_base64_encoded_secret" `
  -e JWT_EXPIRATION_MS="3600000" `
  springbank
```

This example assumes PostgreSQL is running on the Windows host and is reachable from Docker Desktop.

---

## 📘 API Documentation

Once the application is running, open:

| Resource | URL |
|---|---|
| Swagger UI | [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) |
| OpenAPI JSON | [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs) |

Use the Swagger **Authorize** button to test authenticated endpoints with a JWT obtained from the login endpoint.

## 🔌 API Endpoints

All endpoints except registration and login require a valid bearer token.

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/auth/register` | Register a user |
| `POST` | `/api/auth/login` | Authenticate and receive a JWT |
| `GET` | `/api/users/me` | View the current user profile |
| `PUT` | `/api/users/me` | Update the profile name |
| `POST` | `/api/accounts` | Create an account |
| `GET` | `/api/accounts` | List your accounts |
| `GET` | `/api/accounts/{id}` | View an account |
| `POST` | `/api/accounts/{accountId}/deposit` | Deposit funds |
| `POST` | `/api/accounts/{accountId}/withdraw` | Withdraw funds |
| `POST` | `/api/accounts/{accountId}/transfer` | Transfer funds |
| `GET` | `/api/accounts/{accountId}/transactions` | View transaction history |

### Example: Create an Account

**Request**

```http
POST /api/accounts
Authorization: Bearer <your-jwt>
Content-Type: application/json
```

```json
{
  "accountType": "SAVINGS"
}
```

**Example response**

```json
{
  "accountType": "SAVINGS"
}
```

> The response fields shown above are illustrative. Refer to the actual DTO and Swagger schema for the complete response structure.

### Example: Transfer Funds

**Request**

```http
POST /api/accounts/acc-1/transfer
Authorization: Bearer <your-jwt>
Content-Type: application/json
Idempotency-Key: transfer-unique-001
```

```json
{
  "toAccountId": "acc-2",
  "amount": 100.00,
  "description": "Shared expenses"
}
```

Use a unique idempotency key for each new operation. Reuse the same key when retrying the same request.

---

## 🔐 Security

- Passwords are hashed using BCrypt.
- JWTs use HS256 signing and have a configurable expiration.
- Authenticated users can access only their own accounts.
- Invalid or expired tokens are rejected.
- Database credentials and JWT secrets should be supplied through environment variables.

## 🔁 Idempotency

Deposit, withdrawal, and transfer operations support an optional `Idempotency-Key` header.

| Scenario | Expected behavior |
|---|---|
| First request with a new key | Operation executes |
| Retry with the same key and same parameters | Previous result is replayed |
| Same key with different parameters | `409 Conflict` |
| Same key while an operation is in progress | `409 Conflict` |
| Failed operation that safely releases its claim | Key can be retried |

Keys are scoped per user and retained for up to 24 hours, subject to the application's scheduled cleanup.

## 🧪 Testing

Run the full test suite:

```powershell
.\mvnw.cmd clean test
```

**Latest reported verification — October 11, 2026**

| Result | Count |
|---|---:|
| Tests run | 188 |
| Failures | 0 |
| Errors | 0 |
| Skipped | 0 |
| Build status | `BUILD SUCCESS` |

The test suite includes authentication, account operations, transfers, transaction history, authorization, and idempotency scenarios.

## 🚧 Limitations & Future Improvements

- Introduce Flyway or Liquibase for database schema migrations.
- Use a distributed lock for scheduled cleanup if deploying multiple application instances.
- Add refresh-token support.
- Consider pagination for account listings as data grows.
- Configure and verify production deployment, monitoring, and secret management.

---

## 👨‍💻 Author

**Ankit Pasvan**

Full Stack Developer | Java & Spring Boot | MERN Stack

- GitHub: [@ankitpasvan](https://github.com/ankitpasvan)

---

<p align="center">
  <strong>Built to demonstrate secure API design, transaction safety, and backend engineering fundamentals.</strong>
</p>
