SpringBank — Banking System REST API
A production-style Spring Boot REST API for a banking system: JWT authentication, account
management, deposits, withdrawals, atomic transfers, transaction history, and idempotent
money operations — backed by PostgreSQL with JPA.
Built to demonstrate real backend engineering concerns: transaction safety, concurrency
control, idempotent retries, and ownership-based authorization.
Key features
JWT authentication — register/login with BCrypt-hashed passwords; stateless HS256
  tokens (1-hour default lifetime).
Account management — create SAVINGS/CURRENT accounts, list and view your accounts.
Deposits & withdrawals — balance-guarded, pessimistic-locked single-account operations.
Atomic transfers — debit + credit + both ledger entries in one database transaction;
  a failure anywhere rolls everything back.
Idempotency keys — optional Idempotency-Key header on deposit, withdraw and
  transfer: safe retries never move money twice; key reuse with different parameters is
  rejected with 409.
Transaction history — paginated, filterable ledger (by type and date range).
Ownership-based authorization — you can only touch your own accounts; anything else
  looks like 404 (no account-id probing).
Consistent errors — one JSON error shape for every failure, plus field-level
  validation messages.
Swagger UI — interactive API docs with a JWT "Authorize" button.
Technology stack
Area
Choice
Language
Java 21
Framework
Spring Boot 4.1.1 — Web, Data JPA, Validation, Security
Database
PostgreSQL (runtime) · H2 in-memory (tests)
Security
Spring Security, JJWT (HS256), BCrypt
API docs
springdoc-openapi 3.1.1 (Swagger UI)
Money
BigDecimal, scale 2, persisted as NUMERIC(19,2)
Build
Maven — wrapper (mvnw) included, no install needed
Tests
JUnit 5, Mockito, MockMvc, @DataJpaTest
Architecture and project structure
Layered design — controller → service → repository — with DTO records at the API
boundary (JPA entities never leak to clients).
src/main/java/com/example/banking/
├── BankingSystemApplication.java      # entry point; @EnableScheduling (idempotency cleanup)
├── config/
│   ├── SecurityConfig.java            # stateless JWT filter chain; /api/auth/** + Swagger public
│   └── OpenApiConfig.java             # Swagger "Authorize" button (JWT bearer scheme)
├── controller/                        # REST endpoints, @Valid DTOs, no business logic
│   ├── AuthController.java            # POST /api/auth/register, /login
│   ├── UserController.java            # GET / PUT /api/users/me
│   ├── AccountController.java         # POST / GET /api/accounts, GET /api/accounts/{id}
│   ├── TransactionController.java     # POST /api/accounts/{accountId}/deposit, /withdraw
│   ├── TransferController.java        # POST /api/accounts/{accountId}/transfer
│   └── TransactionHistoryController.java  # GET /api/accounts/{accountId}/transactions
├── dto/                               # request/response records + validation annotations
├── exception/                         # domain exceptions, GlobalExceptionHandler
├── model/                             # JPA entities: User, Account, Transaction, IdempotencyRecord
├── repository/                        # Spring Data JPA + locked balance operations
├── security/                          # JwtService, JwtAuthenticationFilter, entry point
├── service/                           # UserService, AccountService, TransactionService,
│                                      # TransferService, TransactionHistoryService, IdempotencyService
└── util/                              # AccountNumberGenerator, IdempotencyFingerprint (SHA-256)
Notable engineering decisions:
Transfer atomicity — TransferService is @Transactional: sender debit, receiver
  credit and both ledger rows commit or roll back together. Plain PostgreSQL handles this
  on any installation (no replica set required).
No overdrafts under concurrency — balance updates load the account row with
  PESSIMISTIC_WRITE and re-check ownership + funds while the lock is held.
Idempotency without races — a unique constraint on (userId, idempotencyKey) lets
  the database serialize duplicate requests; the claim is committed in its own
  transaction so a concurrent retry gets 409 immediately instead of blocking. A SHA-256
  fingerprint of the normalized request tells genuine retries apart from key reuse with
  different parameters.
Prerequisites
JDK 21
PostgreSQL 14+ (local or reachable)
No Maven install needed — the project ships the Maven Wrapper.
  These docs use .\mvnw.cmd (Windows); on macOS/Linux use ./mvnw.
Environment variables
All secrets come from environment variables — never commit real values.
Variable
Default
Purpose
DATABASE_URL
jdbc:postgresql://localhost:5432/banking
JDBC URL of the PostgreSQL database
DATABASE_USERNAME
postgres
Database user
DATABASE_PASSWORD
postgres
Database password (set your own)
DDL_AUTO
update
Hibernate schema mode (update = dev)
JWT_SECRET
(empty — app fails fast with a clear error)
Base64 secret, decodes to ≥ 32 bytes
JWT_EXPIRATION_MS
3600000 (1 hour)
Token lifetime in milliseconds
Generate a signing secret (run once, keep it private):
[Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Maximum 256 }))
Database setup and configuration
Create the database once:
-- in psql, or: createdb -U postgres banking
CREATE DATABASE banking;
Hibernate creates/updates the schema automatically on startup (DDL_AUTO=update) —
convenient for development. For production, set DDL_AUTO=validate and manage the
schema with a migration tool such as Flyway or Liquibase.
Local installation and startup (Windows)
cd E:\banking-system

# 1. Set secrets for this session
$env:DATABASE_PASSWORD = "<your-postgres-password>"
$env:JWT_SECRET = "<output-of-the-generator-above>"

# 2. Verify everything first (recommended)
.\mvnw.cmd clean test

# 3. Start the API
.\mvnw.cmd spring-boot:run
The API listens on http://localhost:8080.
Docker setup
The Dockerfile packages the pre-built jar on a Java 21 JRE:
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/banking-system-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
# 1. Build the jar (the Dockerfile copies it from target\)
.\mvnw.cmd clean package -DskipTests

# 2. Build the image
docker build -t springbank .

# 3. Run it (host.docker.internal reaches host PostgreSQL on Docker Desktop)
docker run -p 8080:8080 `
  -e DATABASE_URL=jdbc:postgresql://host.docker.internal:5432/banking `
  -e DATABASE_USERNAME=postgres `
  -e DATABASE_PASSWORD="<your-postgres-password>" `
  -e JWT_SECRET="<your-jwt-secret>" `
  springbank
Notes:
The repo has no compose file or .dockerignore; the commands above are the complete
  flow the current Dockerfile supports.
The container needs a reachable PostgreSQL: on the host (as above) or a sibling
  container on the same Docker network (jdbc:postgresql://<name>:5432/banking).
Swagger / OpenAPI documentation
Swagger UI: http://localhost:8080/swagger-ui.html (public)
OpenAPI JSON: http://localhost:8080/v3/api-docs (public)
Trying authenticated endpoints in the UI: call POST /api/auth/login, copy token,
click Authorize, enter Bearer <token>.
API endpoints
Base URL http://localhost:8080. Everything except POST /api/auth/** requires
Authorization: Bearer <token>.
Method
Path
Description
POST
/api/auth/register
Register (public) → 201
POST
/api/auth/login
Login (public) → 200 + JWT
GET
/api/users/me
Current user profile
PUT
/api/users/me
Update profile name
POST
/api/accounts
Create account → 201
GET
/api/accounts
List my accounts
GET
/api/accounts/{id}
One account (404 if not mine)
POST
/api/accounts/{accountId}/deposit
Deposit → 201
POST
/api/accounts/{accountId}/withdraw
Withdraw → 201 (422 if short)
POST
/api/accounts/{accountId}/transfer
Transfer → 201
GET
/api/accounts/{accountId}/transactions
Paginated history (filters below)
History query params: type (DEPOSIT/WITHDRAWAL/TRANSFER_IN/TRANSFER_OUT),
from/to (YYYY-MM-DD), page (default 0), size (default 20, max 100).
Example requests and responses
Register — POST /api/auth/register → 201
// request
{ "name": "Ankit", "email": "ankit@example.com", "password": "secret123" }

// response (no token here — log in next)
{ "id": "u1", "name": "Ankit", "email": "ankit@example.com", "createdAt": "2026-10-11T10:00:00Z" }
Login — POST /api/auth/login → 200
// request
{ "email": "ankit@example.com", "password": "secret123" }

// response
{
  "token": "<jwt>",
  "user": { "id": "u1", "name": "Ankit", "email": "ankit@example.com", "createdAt": "2026-10-11T10:00:00Z" }
}
Create account — POST /api/accounts → 201
// request
{ "accountType": "SAVINGS" }

// response
{
  "id": "acc-1", "accountNumber": "123456789012", "userId": "u1",
  "accountType": "SAVINGS", "balance": 0.00, "createdAt": "2026-10-11T10:05:00Z"
}
Deposit — POST /api/accounts/acc-1/deposit → 201
// request
{ "amount": 500.00, "description": "salary" }

// response
{
  "id": "tx-9", "accountId": "acc-1", "type": "DEPOSIT",
  "amount": 500.00, "balanceAfter": 1500.00,
  "description": "salary", "createdAt": "2026-10-11T10:06:00Z"
}
Transfer — POST /api/accounts/acc-1/transfer → 201
// request
{ "toAccountId": "acc-2", "amount": 100.00, "description": "rent split" }

// response
{
  "transactionId": "tx-10", "fromAccountId": "acc-1", "toAccountId": "acc-2",
  "amount": 100.00, "senderBalanceAfter": 1400.00,
  "description": "rent split", "createdAt": "2026-10-11T10:07:00Z"
}
History — GET /api/accounts/acc-1/transactions?type=DEPOSIT&page=0&size=20 → 200
{
  "content": [ { "id": "tx-9", "accountId": "acc-1", "type": "DEPOSIT",
                 "amount": 500.00, "balanceAfter": 1500.00, "description": "salary",
                 "createdAt": "2026-10-11T10:06:00Z" } ],
  "page": 0, "size": 20, "totalElements": 1, "totalPages": 1
}
Authentication, JWT and authorization
POST /api/auth/register and POST /api/auth/login are public; every other endpoint
  needs Authorization: Bearer <jwt>.
Passwords are BCrypt-hashed; tokens are HS256-signed with JWT_SECRET, valid for
  JWT_EXPIRATION_MS (default 1 hour).
Login never reveals whether an email is registered — unknown email and wrong password
  produce the identical 401.
Authorization is ownership-based: account endpoints verify the account belongs to the
  caller. Someone else's account returns 404, not 403, so account ids can't be probed.
Error handling and validation
Every error uses the same JSON shape (fieldErrors only for validation failures):
{
  "timestamp": "2026-10-11T10:15:30Z",
  "status": 409,
  "error": "Conflict",
  "message": "Idempotency key was already used with different request parameters",
  "path": "/api/accounts/acc-1/transfer",
  "fieldErrors": {}
}
Status
When
400
Validation failed — e.g. { "fieldErrors": { "amount": "Amount must be at least 0.01" } }
401
Missing, invalid or expired JWT
404
Account/user/transaction not found — including "not yours"
409
Idempotency conflict, or key already in use by a running request
422
Insufficient funds
500
Unexpected server error
Validation rules (enforced by Jakarta annotations on the DTOs): amount 0.01–1000000.00
with at most 2 decimals; required fields non-blank; description ≤ 100 chars; transfers
can't target the same account.
Idempotency
POST .../deposit, .../withdraw and .../transfer accept an optional
Idempotency-Key header. Without it, behavior is unchanged.
Scenario
Result
First request with key K
Executes normally (201)
Retry: same key K, identical parameters
Replays the first result — money never moves twice
Same key K, different parameters
409 Conflict
Same key K while the first request is still running
409 Conflict
Keys are scoped per user and expire after 24 hours (hourly cleanup). A failed operation
releases its key immediately, so the same key can be retried straight away.
Testing
.\mvnw.cmd clean test
Verified result (2026-10-11): Tests run: 188, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS.
Tests run on an in-memory H2 database; no PostgreSQL needed for the suite.
Coverage: registration/login, profile, accounts, deposits/withdrawals, transfers,
  history filtering + pagination, ownership rejection, JWT rejection, pessimistic-lock
  balance safety, and idempotency (replay, conflict, in-progress, release-after-failure —
  including a real transaction-behavior test proving the release survives a rollback).
Current limitations and future improvements
Schema management — DDL_AUTO=update suits development; production should use
  validate with a migration tool (Flyway/Liquibase).
Single-instance scheduler — the hourly idempotency cleanup assumes one app
  instance; multiple replicas need a distributed lock (e.g. ShedLock).
No refresh tokens — access tokens expire after an hour; clients re-authenticate.
Account listing isn't paginated — fine at portfolio scale.
Historical note — a fully commented-out MongoConfig.java remains in config/
  as a reference to the pre-PostgreSQL implementation; it is not active code.
Secrets hygiene — keep credentials in environment variables, never in the repo,
  and rotate any credential that was ever committed.
