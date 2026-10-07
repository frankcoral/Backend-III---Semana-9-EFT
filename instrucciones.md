# Instrucciones para ejecutar y probar — Banco XYZ EFT

Este documento explica cómo preparar, ejecutar y validar la solución final de **Desarrollo Backend III (PBY2203)**.

Los comandos locales están pensados para **Windows PowerShell** y deben ejecutarse desde la raíz del proyecto, salvo indicación contraria.

## 1. Requisitos

- Windows 10/11.
- PowerShell.
- Java 21 o superior.
- Docker Desktop con Docker Compose.
- Git.

Puertos principales:

```text
8761 Eureka
8888 Config Server
9000 Authorization Server
8081 BFF Web
8082 BFF Mobile
8083 BFF ATM
9092 Kafka
3307 MySQL local
```

## 2. Variables de entorno

```powershell
Copy-Item .env.example .env
```

Los valores de ejemplo son únicamente para demostración académica. En producción deben utilizarse secretos seguros.

## 3. Certificados HTTPS

Si es necesario regenerar los certificados locales:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\generar-certificados.ps1
```

## 4. Validar compilación

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\validar-builds.ps1
```

Resultado esperado:

```text
VALIDACION EFT COMPLETA: TODOS LOS MODULOS -> BUILD SUCCESS
```

## 5. Levantar el stack

Ambiente limpio:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\preparar-entorno-demo.ps1
```

O manualmente:

```powershell
docker compose up -d --build
```

Estado:

```powershell
docker compose ps
```

Validación:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\verificar-stack.ps1
```

Resultado esperado:

```text
STACK EFT SALUDABLE
```

## 6. Datos demo

```text
RUT: 11111111-1
Cuenta: CTA-DEMO-000001
ID esperado: 1
Saldo inicial: 100000 CLP
```

## 7. Prueba E2E

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\probar-e2e.ps1
```

Resultado esperado:

```text
BFF Mobile        -> HTTP 200
Saldo ATM         -> HTTP 200
Retiro ATM        -> HTTP 201
ATM contra Mobile -> HTTP 403
```

## 8. OAuth2 Mobile

Obtener token:

```powershell
$pair = "banco-mobile:banco-mobile-secret"
$encoded = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes($pair))

$mobile = Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:9000/oauth2/token" `
  -Headers @{ Authorization = "Basic $encoded" } `
  -ContentType "application/x-www-form-urlencoded" `
  -Body "grant_type=client_credentials&scope=banco.read%20canal.mobile"
```

Consultar:

```powershell
curl.exe -k -sS `
  -H "Authorization: Bearer $($mobile.access_token)" `
  -w "`nHTTP_STATUS:%{http_code}`n" `
  https://localhost:8082/api/mobile/clientes/11111111-1/resumen
```

Debe responder HTTP 200 y como máximo 3 transacciones recientes.

## 9. OAuth2 ATM

```powershell
$pair = "banco-atm:banco-atm-secret"
$encoded = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes($pair))

$atm = Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:9000/oauth2/token" `
  -Headers @{ Authorization = "Basic $encoded" } `
  -ContentType "application/x-www-form-urlencoded" `
  -Body "grant_type=client_credentials&scope=banco.read%20banco.write%20canal.atm"
```

Saldo:

```powershell
curl.exe -k -sS `
  -H "Authorization: Bearer $($atm.access_token)" `
  -w "`nHTTP_STATUS:%{http_code}`n" `
  https://localhost:8083/api/atm/cuentas/1/saldo
```

Retiro:

```powershell
'{"monto":1000}' | Set-Content -Encoding ASCII .\retiro-atm.json

curl.exe -k -sS `
  -H "Authorization: Bearer $($atm.access_token)" `
  -H "Content-Type: application/json" `
  --data-binary "@retiro-atm.json" `
  -w "`nHTTP_STATUS:%{http_code}`n" `
  https://localhost:8083/api/atm/cuentas/1/retiros

Remove-Item .\retiro-atm.json
```

Resultado esperado: HTTP 201, estado `APROBADA` y `eventoPublicado=true`.

## 10. Aislamiento entre canales

Con token ATM:

```powershell
curl.exe -k -sS `
  -H "Authorization: Bearer $($atm.access_token)" `
  -o NUL `
  -w "HTTP_STATUS:%{http_code}`n" `
  https://localhost:8082/api/mobile/cloud-config
```

Resultado esperado:

```text
HTTP_STATUS:403
```

## 11. OAuth2 Web con Authorization Code + PKCE

El BFF Web utiliza un flujo **interactivo** de OAuth2 con Authorization Code + PKCE y un token JWT con el scope `canal.web`. Esta prueba se ejecuta en **Windows PowerShell**, con `auth-server` (puerto 9000) y `bff-web` (HTTPS 8081) encendidos.

**Importante:** mantén abierta la **misma ventana de PowerShell** durante los pasos 11.1 al 11.4. Allí permanecerán las variables `$verifier`, `$redirectUri` y `$webToken`. Los ejemplos siguientes son para el entorno local; para AWS, consulta `despliegue.md` y configura previamente las URL registradas para ese entorno.

### 11.1. Generar PKCE y abrir la autorización

Generar un `code_verifier` aleatorio y su `code_challenge` (S256):

```powershell
$verifierBytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($verifierBytes)
$rng.Dispose()

$verifier = ([Convert]::ToBase64String($verifierBytes)).TrimEnd('=').Replace('+', '-').Replace('/', '_')

$sha256 = [System.Security.Cryptography.SHA256]::Create()
$challengeBytes = $sha256.ComputeHash([Text.Encoding]::ASCII.GetBytes($verifier))
$sha256.Dispose()

$challenge = ([Convert]::ToBase64String($challengeBytes)).TrimEnd('=').Replace('+', '-').Replace('/', '_')
```

Armar la solicitud al Authorization Server. La URI de retorno debe coincidir **exactamente** con la propiedad `OAUTH2_WEB_REDIRECT_URI` del archivo `.env` usado al levantar los contenedores:

```powershell
$redirectUri = 'https://localhost:8081/login/oauth2/code/banco-web'
$requestedScopes = 'openid profile banco.read banco.write canal.web'

$encodedRedirectUri = [uri]::EscapeDataString($redirectUri)
$encodedScopes = [uri]::EscapeDataString($requestedScopes)

$authorizeUrl = "http://localhost:9000/oauth2/authorize?response_type=code&client_id=banco-web&redirect_uri=$encodedRedirectUri&scope=$encodedScopes&code_challenge=$challenge&code_challenge_method=S256"

Start-Process $authorizeUrl
```

### 11.2. Iniciar sesión y obtener el `code`

En el navegador, iniciar sesión con el usuario de demostración (si se conservan los valores de `.env.example`):

```text
Usuario: banco_user
Contraseña: banco123
```

Aceptar la pantalla de consentimiento con los permisos solicitados. Luego el Authorization Server redirige a la URL `https://localhost:8081/login/oauth2/code/banco-web?code=...` (pueden existir parámetros adicionales).

**Copia solamente el valor del parámetro `code` de la barra de direcciones.** Es posible que el navegador muestre HTTP 403 en esa URL, porque el BFF de esta demostración no implementa una página de callback para iniciar sesión de forma interactiva. Eso **no impide** canjear el código si el parámetro `code` está presente. El código es de un solo uso y tiene una vigencia breve (5 minutos en la configuración demo).

### 11.3. Canjear el código por el token Web

Regresar a la **misma ventana de PowerShell** del paso 11.1 y pegar el código recibido:

```powershell
$code = 'PEGAR_AQUI_SOLO_EL_VALOR_DE_CODE'

$pairWeb = 'banco-web:banco-web-secret'
$encodedWeb = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes($pairWeb))

$webTokenResponse = Invoke-RestMethod `
  -Method Post `
  -Uri 'http://localhost:9000/oauth2/token' `
  -Headers @{ Authorization = "Basic $encodedWeb" } `
  -ContentType 'application/x-www-form-urlencoded' `
  -Body @{
    grant_type    = 'authorization_code'
    code          = $code
    redirect_uri  = $redirectUri
    code_verifier = $verifier
  }

$webToken = $webTokenResponse.access_token
$webTokenResponse | Select-Object token_type, expires_in, scope
```

El secreto `banco-web-secret` es exclusivamente el valor demo de `.env.example`; si cambiaste `OAUTH2_WEB_CLIENT_SECRET`, utiliza el secreto configurado en tu entorno. El resultado debe indicar `Bearer` e incluir los scopes:

```text
openid
profile
banco.read
banco.write
canal.web
```

**No muestres ni publiques el contenido completo de `$webToken`.**

### 11.4. Consultar el endpoint protegido de Web

En la misma terminal, usando el token obtenido:

```powershell
curl.exe -k -sS `
  -H "Authorization: Bearer $webToken" `
  -w "`nHTTP_STATUS:%{http_code}`n" `
  https://localhost:8081/api/web/clientes/11111111-1/detalle
```

Resultado esperado:

```text
HTTP_STATUS:200
```

La respuesta agrega información del cliente, sus cuentas y transacciones procedentes de `clientes-service`, `cuentas-service` y `pagos-service`. El parámetro `-k` permite trabajar con el certificado HTTPS autofirmado del entorno de demostración.

### 11.5. Solución de problemas habituales

- **`invalid_grant` al canjear:** el código ya se usó, expiró o no corresponde al `code_verifier`. Vuelve a ejecutar 11.1 y genera una autorización nueva, sin cambiar de terminal.
- **`invalid_redirect_uri`:** confirma que `$redirectUri` coincide exactamente con `OAUTH2_WEB_REDIRECT_URI` en `.env` y con la URI registrada por `auth-server`.
- **HTTP 403 en la URL de callback del navegador:** si aparece `?code=...`, copia ese código y continúa con 11.3; la verificación definitiva es el HTTP 200 del endpoint de negocio.
- **HTTP 401/403 al consultar Web:** revisa que el token sea válido, no esté expirado y contenga `canal.web` y `banco.read`.

## 12. Kafka

Describir el tópico principal:

```powershell
docker exec eft-kafka /opt/kafka/bin/kafka-topics.sh `
  --bootstrap-server localhost:9092 `
  --describe `
  --topic banco.transacciones.completadas
```

Debe mostrar 3 particiones.

Ver consumidor ATM:

```powershell
docker compose logs --since=10m bff-atm |
  Select-String "ATM_TRANSACCION_RECIBIDA"
```

## 13. Escalabilidad horizontal

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\escalar-microservicios.ps1 -Replicas 2
```

Resultado esperado:

```text
CLIENTES-SERVICE -> instancias UP: 2
CUENTAS-SERVICE  -> instancias UP: 2
PAGOS-SERVICE    -> instancias UP: 2
```

## 14. Failover

Con dos réplicas de Cuentas:

```powershell
docker stop banco-xyz-bff-cuentas-service-1
Start-Sleep -Seconds 40
powershell -ExecutionPolicy Bypass -File .\scripts\probar-e2e.ps1
```

El sistema debe continuar funcionando mediante la segunda réplica.

Restaurar:

```powershell
docker start banco-xyz-bff-cuentas-service-1
```

## 15. Resilience4j

Detener Pagos:

```powershell
docker stop banco-xyz-bff-pagos-service-1 banco-xyz-bff-pagos-service-2
Start-Sleep -Seconds 40
```

Un retiro ATM debe responder:

```text
HTTP 503
resilience = FALLBACK_ACTIVO
```

Restaurar:

```powershell
docker start banco-xyz-bff-pagos-service-1 banco-xyz-bff-pagos-service-2
```

## 16. Spring Batch

```powershell
cd .\batch-migration
```

Transacciones diarias:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--spring.batch.job.name=reporteTransaccionesDiariasJob"
```

Intereses mensuales:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--spring.batch.job.name=calculoInteresesMensualesJob"
```

Estados anuales:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--spring.batch.job.name=estadosCuentaAnualesJob"
```

Retry recuperado:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--spring.batch.job.name=retryEvidenceJob"
```

Retry agotado y rollback:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--spring.batch.job.name=retryExhaustionEvidenceJob"
```

Reejecución automática:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--spring.batch.job.name=criticalAutoRerunEvidenceJob"
```

## 17. Rendimiento Batch validado

Resultados finales observados sobre el mismo conjunto de datos:

```text
1x1 -> 1472 ms
2x2 -> 1057 ms
3x3 -> 878 ms
4x4 -> 775 ms
```

La reducción de 1x1 a 4x4 fue aproximadamente **47,35 %**.

Los tiempos absolutos pueden variar entre equipos; la comparación debe realizarse sobre el mismo conjunto de datos.

## 18. Despliegue en AWS

El procedimiento cloud completo se encuentra en:

```text
despliegue.md
```

La validación final se realizó en AWS EC2, con el stack completo y dos réplicas de Clientes, Cuentas y Pagos registradas en Eureka.

## 19. Apagar entorno local

```powershell
docker compose down
```

Para eliminar también el volumen MySQL:

```powershell
docker compose down -v
```
