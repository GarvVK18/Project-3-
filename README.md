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

## Build & Run

```bash
# Build Java sources using Gradle
./gradlew compileJava

# Build full project jar
./gradlew build
```