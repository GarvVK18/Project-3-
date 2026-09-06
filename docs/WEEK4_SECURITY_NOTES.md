# Week 4 Security Hardening Notes

Before production release, verify the following security controls:

- Keep database, Redis, mail/SMS, signing-key, and client credentials outside source control.
- Restrict CORS origins instead of allowing broad wildcard origins with credentials.
- Protect MFA setup/enable/disable operations with authenticated-user authorization.
- Ensure token issuance uses the same signing/validation strategy as the configured resource server.
- Persist signing keys appropriately for multi-instance deployments instead of generating new keys on every restart.
- Confirm revoked tokens are rejected consistently across all protected resources.
- Apply rate limiting to sensitive authentication and recovery operations.
- Keep audit records free of passwords, tokens, and other secrets.
