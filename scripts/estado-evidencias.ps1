$ErrorActionPreference = "Continue"

Write-Host "=== DOCKER: microservicios de negocio ===" -ForegroundColor Cyan
docker compose ps clientes-service cuentas-service pagos-service

Write-Host "`n=== EUREKA: instancias UP ===" -ForegroundColor Cyan
foreach ($App in @("CLIENTES-SERVICE", "CUENTAS-SERVICE", "PAGOS-SERVICE", "BFF-WEB", "BFF-MOBILE", "BFF-ATM")) {
    try {
        $r = Invoke-RestMethod -Uri "http://localhost:8761/eureka/apps/$App" -Headers @{Accept="application/json"}
        $instances = @($r.application.instance)
        Write-Host "`n$App"
        $instances | Select-Object instanceId,status,ipAddr | Format-Table -AutoSize
    }
    catch {
        Write-Warning "$App no disponible en Eureka"
    }
}

Write-Host "`n=== KAFKA: topic transacciones ===" -ForegroundColor Cyan
docker exec eft-kafka /opt/kafka/bin/kafka-topics.sh `
    --bootstrap-server localhost:9092 `
    --describe --topic banco.transacciones.completadas

Write-Host "`n=== ATM: ultimos eventos consumidos ===" -ForegroundColor Cyan
docker logs banco-bff-atm --tail 120 2>&1 | Select-String "ATM_TRANSACCION_RECIBIDA|Started BffAtmApplication"

Write-Host "`nUse esta salida como apoyo para capturas; nunca muestre tokens o secretos." -ForegroundColor Yellow
