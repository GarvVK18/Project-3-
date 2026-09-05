# IAM Server - Week 2 Implementation

Identity and Access Management (IAM) Server built with Spring Boot 3, Spring Security 6, and Spring Authorization Server.

## Features Implemented in Week 2

### 1. User Profile Management (`/api/users/profile`)
- **GET `/api/users/profile`**: Retrieves profile details for the currently authenticated user (id, username, enabled status, assigned roles). Never returns passwords or security secrets.
- **PUT `/api/users/profile`**: Allows authenticated users to update profile details (e.g., username) while preserving security fields (roles, enabled status, password).

### 2. Role and Authority/Permission Assignment (`/api/admin/**`)
- Extended `User`, `Role`, and `Authority` entities with Many-to-Many relationships.
- Updated `CustomUserDetailsService` to dynamically load roles (`ROLE_<name>`) and authorities (`<authority_name>`) from the database.
- Admin management endpoints protected with `@PreAuthorize("hasRole('ADMIN')")`:
  - `POST /api/admin/roles/{roleName}`: Create role
  - `GET /api/admin/roles`: List all roles
  - `POST /api/admin/roles/{roleName}/authorities/{authorityName}`: Assign authority to role
  - `DELETE /api/admin/roles/{roleName}/authorities/{authorityName}`: Remove authority from role
  - `POST /api/admin/users/{username}/roles/{roleName}`: Assign role to user
  - `DELETE /api/admin/users/{username}/roles/{roleName}`: Remove role from user

### 3. Custom JWT Claims
- Extended `AuthorizationServerConfig` using `OAuth2TokenCustomizer<JwtEncodingContext>`.
- Injects database-derived `roles` and `permissions` into JWT Access Tokens and ID Tokens.
- Example JWT Payload:
  ```json
  {
    "sub": "username",
    "roles": ["ADMIN", "USER"],
    "permissions": ["READ_PROFILE", "WRITE_PROFILE"]
  }
  ```

### 4. OpenID Connect (OIDC) Support
- Configured OIDC protocol support via Spring Authorization Server (`.oidc(Customizer.withDefaults())`).
- Supported scopes: `openid`, `profile`, `read`.
- Enabled OIDC Discovery metadata (`/.well-known/openid-configuration`), UserInfo endpoint, and ID Token generation.

### 5. Secure Password Reset Flow (`/api/auth/password-reset/**`)
- **POST `/api/auth/password-reset/request`**: Generates a secure, time-limited UUID reset token (30-minute expiration) associated with the user.
- **POST `/api/auth/password-reset/confirm`**: Validates the reset token and expiration, BCrypt encodes the new password, updates the user account, and invalidates the reset token.

### 6. Custom OAuth2 Consent Page (`/oauth2/consent`)
- Integrated custom consent flow into Spring Authorization Server using `ClientSettings.builder().requireAuthorizationConsent(true)`.
- Handled by `ConsentController` rendering a clean Thymeleaf template (`consent.html`).
- Displays application name, requested scope permissions, authenticated user name, and `ALLOW` / `DENY` controls.

---

## Features Implemented in Week 3: MFA, Redis Caching & Token Revocation

### 1. Multi-Factor Authentication (MFA) & Two-Step Verification
- **RFC 6238 / RFC 4226 TOTP:** Implemented `TotpService` generating 160-bit Base32 secret keys, standard `otpauth://totp/...` QR URIs compatible with Google Authenticator / Authy, and verifying 6-digit codes with ±1 time step tolerance (30-second window).
- **Out-of-Band (OOB) SMS & Email Delivery:** Implemented `OtpDeliveryService` with configurable SendGrid and Twilio integrations and resilient local caching with 5-minute TTL.
- **MFA Endpoints (`/api/auth/mfa/**`):**
  - `POST /api/auth/mfa/setup`: Generates TOTP secret and QR code URI or sends setup OTP.
  - `POST /api/auth/mfa/enable`: Verifies setup code and activates MFA for user account.
  - `POST /api/auth/mfa/disable`: Deactivates MFA for authenticated user.
  - `POST /api/auth/mfa/send-otp`: Resends OTP during two-step login challenge.
  - `POST /api/auth/mfa/verify-login`: Validates intermediate challenge token + OTP/TOTP code and issues JWT token.
- **Two-Step Login Flow:** Extended `/api/auth/login` to return HTTP `202 Accepted` with a short-lived temporary challenge token when `mfa_enabled` is active.

### 2. Redis Distributed Session & Authorization Caching
- **Redis Configuration:** Configured `LettuceConnectionFactory`, `RedisTemplate<String, Object>`, and `StringRedisTemplate` with Jackson JSON and String serializers.
- **OAuth2 Token Caching (`RedisOAuth2AuthorizationService`):**
  - Persists `OAuth2Authorization` records in Redis.
  - Stores short-lived authorization codes with 5-minute TTL (`oauth2:code:{code}`).
  - Stores refresh tokens with configured 30-day TTL (`oauth2:refresh:{token}`).
  - Stores access tokens and state mappings with TTL.
  - Built-in resilient in-memory fallback for local development and offline environments.

### 3. Programmatic Token Revocation & Multi-Device Forced Logout
- **Token Blacklisting:** `POST /api/auth/revoke` blacklists tokens in Redis (`blacklist:token:{token}`) with TTL matching remaining token validity.
- **Forced Logout Across All Devices:** `POST /api/auth/logout-all` registers a revocation timestamp in Redis (`revocation:user:{username}`) that invalidates all tokens issued prior to that timestamp.
- **Admin Force Logout:** `POST /api/admin/users/{username}/force-logout` allows administrators with `ROLE_ADMIN` to immediately invalidate all sessions for a specific user.
- **Security Interceptor Filter (`JwtRevocationFilter`):** Servlet filter registered in Spring Security's filter chain to inspect Bearer tokens against the Redis blacklist and user force-logout cutoff, returning HTTP `401 Unauthorized` for revoked tokens.

---

## Build & Run

```bash
# Build Java sources using Gradle
gradle compileJava

# Run all automated tests (85 tests covering Week 1, 2, and 3)
gradle test

# Build full project jar
gradle build
```