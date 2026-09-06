# PowerShell script to quickly bootstrap local development environment
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host " Starting IAM Server Local Dev Services  " -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan

# Start PostgreSQL and Redis via Docker Compose
docker compose up -d postgres redis

# Check status
Write-Host "Waiting for services to become healthy..." -ForegroundColor Yellow
Start-Sleep -Seconds 3

docker compose ps

Write-Host "`nEnvironment ready! You can now run: gradle bootRun" -ForegroundColor Green
