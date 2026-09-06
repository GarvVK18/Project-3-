# Week 4 Test Plan

## Scope
- Authentication and authorization smoke checks
- Rate-limit behavior on authentication endpoints
- Audit logging verification
- Actuator health verification
- Docker/Kubernetes deployment readiness

## Manual verification
1. Start the application with the configured PostgreSQL and Redis services.
2. Verify `/actuator/health` reports `UP`.
3. Verify unauthenticated access to protected resources is rejected.
4. Verify successful OAuth2 authorization-code flow and access-token issuance.
5. Verify rate limiting rejects requests after the configured threshold.
6. Verify authentication/security events create audit records.
7. Verify token revocation prevents subsequent use of revoked tokens.
8. Run the Gradle test suite with `./gradlew test`.

## Release criteria
- No authentication endpoint is unintentionally public.
- Secrets are supplied through environment/configuration rather than committed source.
- Health checks are available for deployment probes.
- Docker startup and application connectivity are verified before release.
