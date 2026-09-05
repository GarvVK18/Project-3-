#!/usr/bin/env bash
# Bash API verification and smoke-testing script
BASE_URL="http://localhost:9000"

echo "Verifying IAM Server at $BASE_URL..."

# 1. Health Check
HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/actuator/health" || echo "000")
if [ "$HTTP_STATUS" -eq 200 ]; then
    echo "[PASS] Health Endpoint: HTTP 200 OK"
else
    echo "[WARN] Server is not responding on $BASE_URL. Start with 'gradle bootRun' first."
fi

# 2. OIDC Discovery
OIDC_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/.well-known/openid-configuration" || echo "000")
if [ "$OIDC_STATUS" -eq 200 ]; then
    echo "[PASS] OIDC Discovery metadata available: HTTP 200 OK"
fi

echo "Smoke test script completed."
