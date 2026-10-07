param(
    [switch]$ConservarDatos
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
Push-Location $Root
try {
    if (-not $ConservarDatos) {
        Write-Host "Recreando entorno limpio (se elimina el volumen MySQL local)..." -ForegroundColor Yellow
        docker compose down -v --remove-orphans
    }
    else {
        Write-Host "Recreando contenedores conservando datos existentes..." -ForegroundColor Yellow
        docker compose down --remove-orphans
    }

    Write-Host "Construyendo y levantando stack EFT..." -ForegroundColor Cyan
    docker compose up -d --build

    Write-Host "Esperando inicializacion..." -ForegroundColor Cyan
    Start-Sleep -Seconds 20

    Write-Host "`n=== ESTADO ===" -ForegroundColor Green
    docker compose ps

    Write-Host "`nEntorno listo. En una instalacion limpia se crean RUT 11111111-1 y cuenta CTA-DEMO-000001." -ForegroundColor Green
}
finally {
    Pop-Location
}
