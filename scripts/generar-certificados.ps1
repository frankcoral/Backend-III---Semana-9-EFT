$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$Tmp = Join-Path $Root "keystore.p12"

if (Test-Path $Tmp) { Remove-Item $Tmp -Force }

keytool -genkeypair `
  -alias bff-local `
  -keyalg RSA `
  -keysize 2048 `
  -storetype PKCS12 `
  -keystore $Tmp `
  -validity 365 `
  -storepass changeit `
  -keypass changeit `
  -dname "CN=localhost, OU=BackendIII, O=DuocUC, L=Santiago, ST=RegionMetropolitana, C=CL" `
  -ext "SAN=dns:localhost,ip:127.0.0.1"

foreach ($Service in @("bff-web", "bff-mobile", "bff-atm")) {
    Copy-Item $Tmp (Join-Path $Root "$Service/src/main/resources/keystore.p12") -Force
}

Remove-Item $Tmp -Force
Write-Host "Certificado local generado y copiado a WEB, MOBILE y ATM." -ForegroundColor Green
