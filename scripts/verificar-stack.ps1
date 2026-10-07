$ErrorActionPreference = "Stop"

function Assert-Http([string]$Nombre, [string]$Url) {
    try {
        $r = Invoke-WebRequest -UseBasicParsing -Uri $Url -TimeoutSec 10
        if ($r.StatusCode -lt 200 -or $r.StatusCode -ge 300) {
            throw "$Nombre respondio HTTP $($r.StatusCode)"
        }
        Write-Host "OK  $Nombre -> $Url" -ForegroundColor Green
    }
    catch {
        Write-Host "FAIL $Nombre -> $Url :: $($_.Exception.Message)" -ForegroundColor Red
        throw
    }
}

Assert-Http "Config Server" "http://localhost:8888/bff-web/default"
Assert-Http "Eureka" "http://localhost:8761/eureka/apps"
Assert-Http "Auth Server JWK" "http://localhost:9000/oauth2/jwks"

Write-Host "`n=== Servicios de negocio registrados en Eureka ===" -ForegroundColor Cyan
foreach ($App in @("CLIENTES-SERVICE", "CUENTAS-SERVICE", "PAGOS-SERVICE")) {
    $r = Invoke-RestMethod -Uri "http://localhost:8761/eureka/apps/$App" -Headers @{Accept="application/json"}
    $instances = @($r.application.instance)
    $up = @($instances | Where-Object { $_.status -eq "UP" })
    if ($up.Count -lt 1) {
        throw "$App no tiene instancias UP"
    }
    Write-Host "$App -> $($up.Count) instancia(s) UP" -ForegroundColor Green
}

Write-Host "`nSTACK EFT SALUDABLE" -ForegroundColor Green
