$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot

$Projects = @(
    "config-server",
    "discovery-server",
    "auth-server",
    "clientes-service",
    "cuentas-service",
    "pagos-service",
    "bff-web",
    "bff-mobile",
    "bff-atm",
    "batch-migration"
)

foreach ($Project in $Projects) {
    Write-Host "`n=== VALIDANDO $Project ===" -ForegroundColor Cyan
    Push-Location (Join-Path $Root $Project)
    try {
        & .\mvnw.cmd clean test
        if ($LASTEXITCODE -ne 0) {
            throw "Fallo Maven en $Project"
        }
    }
    finally {
        Pop-Location
    }
}

Write-Host "`nVALIDACION EFT COMPLETA: TODOS LOS MODULOS -> BUILD SUCCESS" -ForegroundColor Green
