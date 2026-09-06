# Identity and Access Management (IAM) Server

A centralized, enterprise-grade Authentication and Authorization server compliant with **OAuth 2.0** and **OpenID Connect (OIDC)** protocols. This system acts as the dedicated identity provider (IdP) for internal applications, featuring Single Sign-On (SSO), Multi-Factor Authentication (MFA), role- and permission-based access control (RBAC), distributed session management via Redis, programmatic token revocation, comprehensive audit logging, and token-bucket rate limiting.

---

## 🏗️ Architecture & Tech Stack

- **Language & Runtime**: Java 17+ (Temurin OpenJDK)
- **Framework**: Spring Boot 3.5, Spring Security 6, Spring Authorization Server
- **Data Persistence**: PostgreSQL (User, Role, Authority, Audit Logs)
- **Distributed Cache & Sessions**: Redis (Token caching, session invalidation, blacklist store)
- **Security & Concurrency**:
  - RFC 6238 Time-based One-Time Password (**TOTP / MFA**)
  - Token-bucket rate limiting via **Bucket4j**
  - BCrypt password hashing
  - Asymmetric RSA (2048-bit) JWT signing
- **Documentation & Deployment**:
  - **OpenAPI 3 / Swagger UI** (`/swagger-ui.html`)
  - **Docker & Docker Compose** (Multi-stage containerization)

---

## 📅 4-Week Implementation Roadmap

### Week 1: Core OAuth2 Setup & Database
- [x] Initialized Spring Boot project with Spring Authorization Server dependencies.
- [x] Defined relational database schemas for `User`, `Role`, and `Authority` entities with Many-to-Many mappings.
- [x] Configured Spring Authorization Server supporting standard OAuth2 grant types:
  - `authorization_code`
  - `client_credentials`
  - `refresh_token`
- [x] Implemented `CustomUserDetailsService` to authenticate credentials against PostgreSQL with BCrypt password hashing.

### Week 2: Custom Claims & OpenID Connect
- [x] Configured OpenID Connect (OIDC) endpoints (`openid`, `profile`, `read` scopes, `/.well-known/openid-configuration`).
- [x] Implemented `OAuth2TokenCustomizer<JwtEncodingContext>` to inject dynamic database roles (`roles: ["ADMIN", "USER"]`) and granular permissions (`permissions: [...]`) into JWT Access Tokens and ID Tokens.
- [x] Built REST endpoints for user registration (`/api/auth/register`), password reset request and confirmation (`/api/auth/password-reset/**`), and profile management (`/api/users/profile`).
- [x] Integrated a custom Thymeleaf OAuth2 consent page (`/oauth2/consent` -> `consent.html`).

### Week 3: Multi-Factor Authentication (MFA) & Caching
- [x] **Time-Based One-Time Password (TOTP)**: Implemented RFC 6238 TOTP algorithm compatible with Google Authenticator, Microsoft Authenticator, and Authy (`TotpService`).
- [x] **Two-Step Verification Endpoints** (`/api/mfa/**`):
  - `POST /api/mfa/setup`: Generates Base32 secret key and `otpauth://` QR URI.
  - `POST /api/mfa/enable`: Verifies 6-digit TOTP code and activates MFA on the account.
  - `POST /api/mfa/disable`: Disables MFA for user.
  - `POST /api/mfa/verify`: Validates TOTP code during 2FA login challenge.
  - `GET /api/mfa/status`: Checks active MFA state.
  - SMS/Email delivery simulation for multi-channel OTP delivery.
- [x] **Redis Distributed Cache & Session Management**:
  - Configured `StringRedisTemplate` with resilient in-memory fallback cache for distributed token storage.
- [x] **Programmatic Token Revocation & Forced Logout**:
  - `POST /api/auth/revoke`: Blacklists specific JWT access tokens.
  - `POST /api/auth/logout`: Revokes current token or terminates all active sessions across all devices (`?allDevices=true`).
  - `JwtRevocationFilter`: Servlet filter automatically blocking blacklisted/revoked tokens with HTTP 401.

### Week 4: Auditing, Rate Limiting & Productionization
- [x] **Comprehensive Audit Logging System**:
  - `AuditLog` entity capturing `timestamp`, `username`, `action`, `status`, `client_ip`, and `details`.
  - Spring Security `AuditSecurityEventListener` tracking `AuthenticationSuccessEvent` and `AuthenticationFailureEvent`.
  - Admin endpoint `GET /api/admin/audit-logs` protected by `@PreAuthorize("hasRole('ADMIN')")`.
- [x] **Strict Rate Limiting (Brute-Force Attack Mitigation)**:
  - `RateLimitingFilter` utilizing Bucket4j token bucket algorithm (10 requests/minute per client IP).
  - Applied to authentication and credential endpoints (`/api/auth/login`, `/api/auth/register`, `/api/auth/password-reset/**`, `/api/mfa/**`).
  - Returns HTTP `429 Too Many Requests` with `Retry-After: 60` header when threshold is breached.
- [x] **Interactive OpenAPI 3 / Swagger Documentation**:
  - Integrated SpringDoc OpenAPI UI available at `/swagger-ui.html` and `/v3/api-docs`.
- [x] **Production Containerization & Orchestration**:
  - Multi-stage `Dockerfile` with Eclipse Temurin 17 JRE.
  - `docker-compose.yml` orchestrating `iam-server`, `postgres`, and `redis` with automated health checks.
  - SSL/TLS termination profile configured in `application.properties`.

---

## 🚀 Running the Project

### Option A: Running with Docker Compose (Recommended)

To spin up the complete production stack (Spring Boot IAM Server, PostgreSQL 15, Redis 7):

```bash
docker compose up -d --build
```

- IAM Server: `http://localhost:9000`
- Swagger UI: `http://localhost:9000/swagger-ui.html`
- OpenAPI JSON: `http://localhost:9000/v3/api-docs`
- PostgreSQL: `localhost:5432`
- Redis: `localhost:6379`

To view logs or shut down:
```bash
docker compose logs -f iam-server
docker compose down
```

### Option B: Running Locally with Gradle

Ensure PostgreSQL is running locally on port 5432 (database: `iam_db`, user: `postgres`, password: `RAMGARU`), then run:

```bash
# Windows
.\gradlew.bat bootRun

# Linux / macOS
./gradlew bootRun
```

### Running Test Suite

```bash
# Windows
.\gradlew.bat test

# Linux / macOS
./gradlew test
```

---

## 📡 API Reference Overview

### 1. Authentication & MFA Endpoints
| Method | Endpoint | Description | Auth Required |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/register` | Register new user account | Public |
| `POST` | `/api/auth/login` | Authenticate user with BCrypt credentials | Public (Rate-limited) |
| `POST` | `/api/auth/password-reset/request` | Request password reset token | Public (Rate-limited) |
| `POST` | `/api/auth/password-reset/confirm` | Reset password using token | Public (Rate-limited) |
| `POST` | `/api/auth/revoke` | Programmatically revoke / blacklist token | Public |
| `POST` | `/api/auth/logout` | Revoke current token or all device sessions | Authenticated |
| `POST` | `/api/mfa/setup` | Generate TOTP secret and QR URI | Authenticated |
| `POST` | `/api/mfa/enable` | Verify initial code and enable MFA | Authenticated |
| `POST` | `/api/mfa/disable` | Disable MFA on account | Authenticated |
| `POST` | `/api/mfa/verify` | Validate 6-digit TOTP code during 2FA | Public (Rate-limited) |
| `GET` | `/api/mfa/status` | Get user MFA status | Authenticated |

### 2. User Profile Endpoints
| Method | Endpoint | Description | Auth Required |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/users/profile` | Get current authenticated user profile | Authenticated |
| `PUT` | `/api/users/profile` | Update username while preserving roles | Authenticated |

### 3. Admin Management & Auditing Endpoints
| Method | Endpoint | Description | Auth Required |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/admin/roles` | List all system roles | `ROLE_ADMIN` |
| `POST` | `/api/admin/roles/{roleName}` | Create a new role | `ROLE_ADMIN` |
| `POST` | `/api/admin/roles/{roleName}/authorities/{authorityName}` | Add authority to role | `ROLE_ADMIN` |
| `DELETE` | `/api/admin/roles/{roleName}/authorities/{authorityName}` | Remove authority from role | `ROLE_ADMIN` |
| `POST` | `/api/admin/users/{username}/roles/{roleName}` | Assign role to user | `ROLE_ADMIN` |
| `DELETE` | `/api/admin/users/{username}/roles/{roleName}` | Remove role from user | `ROLE_ADMIN` |
| `GET` | `/api/admin/audit-logs` | Retrieve chronological audit trail logs | `ROLE_ADMIN` |

### 4. OAuth2 & OpenID Connect Endpoints
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/.well-known/openid-configuration` | OpenID Connect discovery metadata |
| `GET` | `/oauth2/authorize` | Authorization endpoint |
| `POST` | `/oauth2/token` | Token issuance endpoint (Code, Client Credentials, Refresh) |
| `GET` | `/oauth2/consent` | Custom OAuth2 scope approval/denial consent UI |
| `GET` | `/userinfo` | OIDC UserInfo endpoint |

---

## 🔒 Security Specifications

- **Rate Limiting**: Monitored per IP address; triggers HTTP 429 when exceeding 10 requests/minute.
- **Audit Trails**: Automatically logs logins, invalid attempts, token revocations, and administrative role updates.
- **Token Blacklist**: Checks incoming bearer tokens against the blacklist store with instant revocation propagation.