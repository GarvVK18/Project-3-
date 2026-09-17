$ErrorActionPreference = 'Stop'
$base = 'http://localhost:9000'

Write-Host 'Week 4 IAM smoke test'
Write-Host '1. Checking application health...'
try {
  $health = Invoke-RestMethod "$base/actuator/health"
  if ($health.status -ne 'UP') { throw "Health status is $($health.status)" }
  Write-Host 'PASS: actuator health is UP'
} catch {
  Write-Host "FAIL: health check - $($_.Exception.Message)"
  exit 1
}

Write-Host '2. Checking protected endpoint without token...'
try {
  Invoke-WebRequest "$base/user" -Method Get -UseBasicParsing | Out-Null
  Write-Host 'WARN: /user was accessible without a token; review endpoint security.'
} catch {
  $status = $_.Exception.Response.StatusCode.value__
  if ($status -eq 401 -or $status -eq 403) { Write-Host "PASS: /user rejected unauthenticated request ($status)" }
  else { Write-Host "WARN: /user returned HTTP $status" }
}

Write-Host '3. Checking OAuth2 authorization endpoint exists...'
try {
  $response = Invoke-WebRequest "$base/oauth2/authorize" -Method Get -MaximumRedirection 0 -ErrorAction Stop
  Write-Host "INFO: authorization endpoint returned HTTP $($response.StatusCode)"
} catch {
  $status = $_.Exception.Response.StatusCode.value__
  if ($status -eq 302 -or $status -eq 400 -or $status -eq 401) { Write-Host "PASS: authorization endpoint is registered (HTTP $status)" }
  else { Write-Host "WARN: authorization endpoint returned HTTP $status" }
}

Write-Host 'Smoke test completed. Review WARN messages before release.'
