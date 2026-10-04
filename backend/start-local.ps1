$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
# Load payment settings without printing secrets. Existing database/IDE settings remain usable.
Get-Content -LiteralPath (Join-Path $PSScriptRoot '.env') | ForEach-Object {
    if ($_ -match '^(VNPAY_[A-Z_]+)=(.*)$') {
        [Environment]::SetEnvironmentVariable($matches[1], $matches[2].Trim(), 'Process')
    }
}
if ([string]::IsNullOrWhiteSpace($env:VNPAY_HASH_SECRET)) {
    throw 'Chua dien VNPAY_HASH_SECRET trong backend/.env.'
}
$paymentListeners = @(Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue)
if ($paymentListeners.Count -gt 0) {
    throw 'Cong 8080 dang duoc su dung. Dung backend cu trong IntelliJ/terminal truoc khi chay lai.'
}
$paymentMavenRepo = Join-Path $env:USERPROFILE '.m2\repository'
& "$PSScriptRoot\mvn.ps1" -MavenArgs @("-Dmaven.repo.local=$paymentMavenRepo", 'spring-boot:run')
