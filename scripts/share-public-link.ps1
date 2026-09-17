# Script to create an instant public HTTPS link for your IAM server
Write-Host "Creating secure public link for IAM Server (port 9000)..." -ForegroundColor Cyan

$cloudflared = "$HOME\cloudflared.exe"
if (-not (Test-Path $cloudflared)) {
    Write-Host "Downloading cloudflared..." -ForegroundColor Yellow
    Invoke-WebRequest -Uri "https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-windows-amd64.exe" -OutFile $cloudflared -UseBasicParsing
}

Write-Host "Starting tunnel with reliable TCP http2 protocol..." -ForegroundColor Green
& $cloudflared tunnel --url http://localhost:9000 --protocol http2
