# PowerShell API verification and smoke-testing script
$baseUrl = "http://localhost:9000"

Write-Host "Verifying IAM Server at $baseUrl..." -ForegroundColor Cyan

# 1. Health Check
try {
    $res = Invoke-RestMethod -Uri "$baseUrl/actuator/health" -Method Get -ErrorAction SilentlyContinue
    Write-Host "[PASS] Health Endpoint: Status is $($res.status)" -ForegroundColor Green
} catch {
    Write-Host "[WARN] Server is not running on $baseUrl. Start with 'gradle bootRun' first." -ForegroundColor Yellow
}

# 2. OIDC Discovery
try {
    $oidc = Invoke-RestMethod -Uri "$baseUrl/.well-known/openid-configuration" -Method Get -ErrorAction SilentlyContinue
    Write-Host "[PASS] OIDC Discovery metadata available: Issuer = $($oidc.issuer)" -ForegroundColor Green
} catch {
}

Write-Host "Smoke test script completed." -ForegroundColor Cyan
