# IAM Server - API Contract Specification

Identity and Access Management (IAM) Server specification compliant with OAuth 2.0, OpenID Connect (OIDC), RFC 6238 Multi-Factor Authentication (MFA), and token lifecycle standards.

- **Base URL:** `http://localhost:9000`
- **Default Issuer:** `http://localhost:9000`
- **OIDC Discovery:** `http://localhost:9000/.well-known/openid-configuration`

---

## 1. OAuth 2.0 & OpenID Connect (OIDC) Endpoints

### 1.1 Authorization Endpoint
`GET /oauth2/authorize`
- **Parameters:**
  - `response_type`: `code`
  - `client_id`: e.g. `project3-client`
  - `redirect_uri`: Registered client redirect URI
  - `scope`: `openid profile read`
  - `state`: CSRF protection state

### 1.2 Consent Endpoint
`GET /oauth2/consent`
- Handled by Thymeleaf consent UI displaying application scopes and allowing `ALLOW` / `DENY` decision.

### 1.3 Token Issuance Endpoint
`POST /oauth2/token`
- **Headers:** `Authorization: Basic <base64(clientId:clientSecret)>`
- **Content-Type:** `application/x-www-form-urlencoded`
- **Grants Supported:**
  - `grant_type=authorization_code&code=...&redirect_uri=...`
  - `grant_type=client_credentials&scope=...`
  - `grant_type=refresh_token&refresh_token=...`
- **Response (200 OK):**
  ```json
  {
    "access_token": "eyJhbGciOi...",
    "token_type": "Bearer",
    "expires_in": 3600,
    "refresh_token": "eyJhbGciOi...",
    "scope": "openid profile read",
    "id_token": "eyJhbGciOi..."
  }
  ```

---

## 2. Authentication & MFA Endpoints

### 2.1 User Registration
`POST /api/auth/register`
- **Request:**
  ```json
  {
    "username": "alice",
    "password": "SecretPassword123"
  }
  ```
- **Response (200 OK):** User object with encrypted password omitted.

### 2.2 User Login (With 2-Step Challenge Detection)
`POST /api/auth/login`
- **Request:**
  ```json
  {
    "username": "alice",
    "password": "SecretPassword123"
  }
  ```
- **Response if MFA Disabled (200 OK):**
  ```
  Login successful
  ```
- **Response if MFA Enabled (202 Accepted):**
  ```json
  {
    "mfaRequired": true,
    "tempToken": "b48c08b6-...",
    "mfaType": "TOTP",
    "message": "Two-factor authentication challenge required. Please provide your TOTP code."
  }
  ```

### 2.3 MFA Setup
`POST /api/auth/mfa/setup?type=TOTP`
- **Headers:** `Authorization: Bearer <token>` or body `{"username": "alice"}`
- **Response (200 OK):**
  ```json
  {
    "secretKey": "JBSWY3DPEHPK3PXP",
    "qrCodeUri": "otpauth://totp/IAM-Server:alice?secret=JBSWY3DPEHPK3PXP&issuer=IAM-Server&digits=6&period=30",
    "mfaType": "TOTP",
    "message": "Scan the QR code in Google Authenticator or enter the secret key manually..."
  }
  ```

### 2.4 MFA Enable
`POST /api/auth/mfa/enable`
- **Request:**
  ```json
  {
    "username": "alice",
    "code": "123456",
    "mfaType": "TOTP",
    "email": "alice@example.com",
    "phoneNumber": "+1234567890"
  }
  ```
- **Response (200 OK):**
  ```json
  {
    "status": "SUCCESS",
    "message": "Multi-Factor Authentication enabled successfully",
    "mfaType": "TOTP"
  }
  ```

### 2.5 Verify Login Challenge
`POST /api/auth/mfa/verify-login`
- **Request:**
  ```json
  {
    "tempToken": "b48c08b6-...",
    "code": "123456"
  }
  ```
- **Response (200 OK):**
  ```json
  {
    "status": "SUCCESS",
    "message": "MFA verification successful",
    "token": "eyJhbGciOi...",
    "username": "alice"
  }
  ```

---

## 3. Token Revocation & Session Management

### 3.1 Revoke Single Token
`POST /api/auth/revoke`
- **Headers:** `Authorization: Bearer <token>` or Body `{"token": "..."}`
- **Response (200 OK):**
  ```json
  {
    "status": "SUCCESS",
    "message": "Token revoked successfully"
  }
  ```

### 3.2 Logout All Devices (User Force Logout)
`POST /api/auth/logout-all`
- Invalidate all active tokens for the authenticated user across all devices.

### 3.3 Admin Force Logout
`POST /api/admin/users/{username}/force-logout`
- **Access:** Requires `ROLE_ADMIN`
- Invalidate all active tokens and sessions for the specified target user.

---

## 4. Audit Logging & Monitoring (Admin Only)

### 4.1 Get Recent Security Audit Logs
`GET /api/admin/audit-logs`
- **Access:** Requires `ROLE_ADMIN`
- **Response (200 OK):**
  ```json
  [
    {
      "id": 1,
      "timestamp": "2026-09-05T22:30:00",
      "eventType": "LOGIN_SUCCESS",
      "username": "alice",
      "ipAddress": "192.168.1.5",
      "status": "SUCCESS",
      "details": "User authenticated successfully"
    }
  ]
  ```

### 4.2 Get Audit Logs by User
`GET /api/admin/audit-logs/user/{username}`

---

## 5. Rate Limiting & Brute-Force Defense

The following endpoints are guarded by Redis-backed sliding window rate limiting:
- `POST /api/auth/login`
- `POST /api/auth/mfa/verify-login`
- `POST /api/auth/password-reset/**`

- **Threshold:** Max 5 requests per 60 seconds per IP.
- **Headers Returned:**
  - `X-RateLimit-Limit: 5`
  - `X-RateLimit-Remaining: 0`
  - `X-RateLimit-Reset: 45`
  - `Retry-After: 45`
- **Exceeded Response (429 Too Many Requests):**
  ```json
  {
    "error": "Too many requests. Rate limit exceeded.",
    "retryAfterSeconds": 45
  }
  ```
