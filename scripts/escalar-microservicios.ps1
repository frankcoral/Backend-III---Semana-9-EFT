param(
    [ValidateRange(1,10)]
    [int]$Replicas = 2,
    [ValidateRange(15,180)]
    [int]$TimeoutSegundos = 90
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot

function Obtener-InstanciasUp([string]$App) {
    try {
        $r = Invoke-RestMethod `
            -Uri "http://localhost:8761/eureka/apps/$App" `
            -Headers @{ Accept = "application/json" } `
            -TimeoutSec 10

        return @($r.application.instance | Where-Object { $_.status -eq "UP" })
    }
    catch {
        return @()
    }
}

Push-Location $Root
try {
    Write-Host "Escalando Clientes, Cuentas y Pagos a $Replicas replica(s)..." -ForegroundColor Cyan

    docker compose up -d `
        --scale clientes-service=$Replicas `
        --scale cuentas-service=$Replicas `
        --scale pagos-service=$Replicas `
        clientes-service cuentas-service pagos-service

    if ($LASTEXITCODE -ne 0) {
        throw "Docker Compose no pudo escalar los microservicios."
    }

    $apps = @("CLIENTES-SERVICE", "CUENTAS-SERVICE", "PAGOS-SERVICE")
    $limite = (Get-Date).AddSeconds($TimeoutSegundos)

    Write-Host "Esperando registro de Eureka (máximo $TimeoutSegundos s)..." -ForegroundColor Yellow

    do {
        $listo = $true
        foreach ($app in $apps) {
            if ((Obtener-InstanciasUp $app).Count -lt $Replicas) {
                $listo = $false
                break
            }
        }

        if (-not $listo) {
            Start-Sleep -Seconds 5
        }
    } while (-not $listo -and (Get-Date) -lt $limite)

    Write-Host "`n=== CONTENEDORES ===" -ForegroundColor Yellow
    docker compose ps clientes-service cuentas-service pagos-service

    Write-Host "`n=== EUREKA ===" -ForegroundColor Yellow
    $faltantes = @()

    foreach ($app in $apps) {
        $up = @(Obtener-InstanciasUp $app)
        Write-Host "$app -> instancias UP: $($up.Count)" -ForegroundColor $(if ($up.Count -ge $Replicas) { "Green" } else { "Red" })
        $up | Select-Object instanceId, status, ipAddr | Format-Table -AutoSize

        if ($up.Count -lt $Replicas) {
            $faltantes += "$app ($($up.Count)/$Replicas)"
        }
    }

    if ($faltantes.Count -gt 0) {
        throw "Eureka no registro todas las replicas esperadas: $($faltantes -join ', ')"
    }

    Write-Host "ESCALABILIDAD VALIDADA: $Replicas replica(s) UP por microservicio." -ForegroundColor Green
}
finally {
    Pop-Location
}
