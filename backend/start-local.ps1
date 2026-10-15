$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

# Load environment variables from .env
if (Test-Path (Join-Path $PSScriptRoot '.env')) {
    Get-Content -LiteralPath (Join-Path $PSScriptRoot '.env') | ForEach-Object {
        if ($_ -match '^\s*([A-Za-z0-9_]+)\s*=\s*(.*)\s*$') {
            [Environment]::SetEnvironmentVariable($matches[1], $matches[2].Trim(), 'Process')
        }
    }
}

# Free port 8080 if already in use
$listeners = @(Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue)
if ($listeners.Count -gt 0) {
    Write-Host "Cong 8080 dang duoc su dung boi Process ID: $($listeners[0].OwningProcess). Dang giai phong..." -ForegroundColor Yellow
    Stop-Process -Id $listeners[0].OwningProcess -Force -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 1
}

# Check if packaged jar exists, if not build it
$jarPath = ".\target\smart-rental-backend-0.0.1-SNAPSHOT.jar"
if (-not (Test-Path $jarPath)) {
    Write-Host "Dang bien dich va dong goi backend..." -ForegroundColor Cyan
    cmd /c ".\mvnw.cmd package -DskipTests"
}

Write-Host "Khoi dong PhongTroBackendApplication..." -ForegroundColor Green
& java -jar ".\target\smart-rental-backend-0.0.1-SNAPSHOT.jar"
