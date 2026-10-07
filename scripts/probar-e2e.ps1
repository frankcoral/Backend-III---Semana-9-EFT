param(
    [long]$CuentaId = 1,
    [string]$RutDemo = "11111111-1",
    [decimal]$MontoRetiro = 1000
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
$OutputEncoding = [System.Text.UTF8Encoding]::new($false)
$Root = Split-Path -Parent $PSScriptRoot
$EnvFile = Join-Path $Root ".env"

function Leer-EnvFile([string]$Name) {
    if (-not (Test-Path $EnvFile)) { return $null }
    $line = Get-Content $EnvFile | Where-Object { $_ -match "^$([regex]::Escape($Name))=" } | Select-Object -Last 1
    if (-not $line) { return $null }
    return ($line -split "=", 2)[1].Trim()
}

function Valor-O-Default([string]$EnvName, [string]$Default) {
    $value = [Environment]::GetEnvironmentVariable($EnvName)
    if (-not [string]::IsNullOrWhiteSpace($value)) { return $value }
    $fromFile = Leer-EnvFile $EnvName
    if (-not [string]::IsNullOrWhiteSpace($fromFile)) { return $fromFile }
    return $Default
}

function Obtener-Token([string]$ClientId, [string]$Secret, [string]$Scope) {
    $pair = "$ClientId`:$Secret"
    $encoded = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes($pair))
    return Invoke-RestMethod `
        -Method Post `
        -Uri "http://localhost:9000/oauth2/token" `
        -Headers @{ Authorization = "Basic $encoded" } `
        -ContentType "application/x-www-form-urlencoded" `
        -Body "grant_type=client_credentials&scope=$([uri]::EscapeDataString($Scope))"
}

function Curl-Status([string[]]$CurlArgs) {
    $output = & curl.exe @CurlArgs
    if ($LASTEXITCODE -ne 0) { throw "curl fallo con codigo $LASTEXITCODE" }
    return $output
}

$mobileSecret = Valor-O-Default "OAUTH2_MOBILE_CLIENT_SECRET" "banco-mobile-secret"
$atmSecret = Valor-O-Default "OAUTH2_ATM_CLIENT_SECRET" "banco-atm-secret"

Write-Host "=== 1. Token Mobile ===" -ForegroundColor Cyan
$mobile = Obtener-Token "banco-mobile" $mobileSecret "banco.read canal.mobile"
Write-Host "Scopes: $($mobile.scope)"

Write-Host "`n=== 2. BFF Mobile optimizado ===" -ForegroundColor Cyan
Curl-Status @(
    "-k", "-s", "--max-time", "30", "-w", "`nHTTP_STATUS:%{http_code}`n",
    "-H", "Authorization: Bearer $($mobile.access_token)",
    "https://localhost:8082/api/mobile/clientes/$RutDemo/resumen"
)

Write-Host "`n=== 3. Token ATM ===" -ForegroundColor Cyan
$atm = Obtener-Token "banco-atm" $atmSecret "banco.read banco.write canal.atm"
Write-Host "Scopes: $($atm.scope)"

Write-Host "`n=== 4. Saldo ATM ===" -ForegroundColor Cyan
Curl-Status @(
    "-k", "-s", "--max-time", "30", "-w", "`nHTTP_STATUS:%{http_code}`n",
    "-H", "Authorization: Bearer $($atm.access_token)",
    "https://localhost:8083/api/atm/cuentas/$CuentaId/saldo"
)

Write-Host "`n=== 5. Retiro ATM ===" -ForegroundColor Cyan
$body = @{ monto = $MontoRetiro } | ConvertTo-Json -Compress
$tempBody = Join-Path ([System.IO.Path]::GetTempPath()) ("banco-xyz-retiro-" + [guid]::NewGuid().ToString("N") + ".json")
try {
    [System.IO.File]::WriteAllText($tempBody, $body, [System.Text.UTF8Encoding]::new($false))
    Curl-Status @(
        "-k", "-s", "--max-time", "30", "-w", "`nHTTP_STATUS:%{http_code}`n",
        "-H", "Authorization: Bearer $($atm.access_token)",
        "-H", "Content-Type: application/json",
        "--data-binary", "@$tempBody",
        "https://localhost:8083/api/atm/cuentas/$CuentaId/retiros"
    )
}
finally {
    Remove-Item $tempBody -Force -ErrorAction SilentlyContinue
}

Write-Host "`n=== 6. Aislamiento por canal: token ATM contra Mobile ===" -ForegroundColor Cyan
Curl-Status @(
    "-k", "-s", "--max-time", "30", "-o", "NUL", "-w", "HTTP_STATUS:%{http_code}`n",
    "-H", "Authorization: Bearer $($atm.access_token)",
    "https://localhost:8082/api/mobile/cloud-config"
)

Write-Host "`nEsperado: Mobile 200, ATM saldo 200, retiro 201 y aislamiento 403." -ForegroundColor Green
