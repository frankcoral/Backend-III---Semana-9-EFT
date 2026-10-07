# Arquitectura técnica — Banco XYZ EFT

## 1. Objetivo

Modernizar la plataforma legacy del Banco XYZ mediante una arquitectura distribuida que reduzca acoplamiento, mejore escalabilidad, resiliencia, seguridad y capacidad de integración.

## 2. Procesos críticos identificados

La propuesta identifica los cinco procesos exigidos por la EFT:

1. **Migración Batch a Spring Batch:** reemplaza scripts legacy por Jobs controlados, trazables y reejecutables.
2. **División del monolito en microservicios:** separa Clientes, Cuentas y Pagos.
3. **Patrón BFF:** crea interfaces independientes para Web, Mobile y ATM.
4. **Seguridad distribuida:** OAuth2/JWT y scopes específicos por canal.
5. **Mensajería asíncrona:** Kafka desacopla publicación y consumo de eventos.

## 3. Tres requerimientos clave del negocio

### 3.1 Escalabilidad y continuidad operacional

**Necesidad:** el sistema bancario debe soportar variaciones de carga y evitar que una única instancia se convierta en punto de falla.

**Decisión:** microservicios stateless, Eureka, Spring Cloud LoadBalancer y réplicas Docker.

**Justificación:** cada servicio puede escalar independientemente y las llamadas se resuelven contra instancias disponibles. Se validó continuidad con una réplica de Cuentas detenida y escalabilidad con dos instancias de los tres servicios de negocio.

### 3.2 Seguridad diferenciada por canal

**Necesidad:** Web, Mobile y ATM tienen perfiles de riesgo y permisos distintos.

**Decisión:** tres BFF independientes y OAuth2/JWT con scopes `canal.web`, `canal.mobile` y `canal.atm`.

**Justificación:** evita reutilizar credenciales de un canal en otro y aplica autorización mínima. Se comprobó HTTP 403 al utilizar un token ATM contra Mobile.

### 3.3 Resiliencia y consistencia de operaciones críticas

**Necesidad:** un retiro no debe confirmarse si la dependencia encargada de procesarlo no está disponible.

**Decisión:** Resilience4j, fallbacks explícitos, fuente autoritativa en `cuentas-service`, mensajería Kafka y control de concurrencia optimista.

**Justificación:** ante caída de Pagos, ATM responde 503 `FALLBACK_ACTIVO` en lugar de confirmar una operación no procesada.

## 4. Componentes

### BFF

- `bff-web`: respuesta completa; Authorization Code + PKCE.
- `bff-mobile`: respuesta ligera; máximo 3 transacciones recientes.
- `bff-atm`: saldo y retiros; consumidor Kafka de transacciones completadas.

### Microservicios

- `clientes-service`
- `cuentas-service`
- `pagos-service`

### Infraestructura

- `config-server`
- `discovery-server`
- `auth-server`
- Kafka
- MySQL

## 5. Spring Cloud

Spring Cloud Config centraliza configuración.

Eureka registra dinámicamente los servicios.

Spring Cloud LoadBalancer permite que BFF y servicios dependientes utilicen nombres lógicos en lugar de URLs fijas.

## 6. Seguridad

Los BFF exponen HTTPS para la demostración.

Scopes:

```text
canal.web
canal.mobile
canal.atm
banco.read
banco.write
```

Los endpoints validan JWT y aplican autorización según operación.

## 7. Kafka

Tópicos:

```text
banco.clientes.eventos
banco.cuentas.eventos
banco.transacciones.completadas
```

El tópico principal de transacciones utiliza 3 particiones. Pagos publica operaciones completadas y ATM consume eventos con concurrencia 3.

## 8. Resilience4j

Se utiliza Circuit Breaker/fallback para controlar fallos de dependencias. La evidencia principal detiene ambas réplicas de Pagos y demuestra una respuesta controlada HTTP 503.

## 9. Spring Batch

Procesos:

```text
reporteTransaccionesDiariasJob
calculoInteresesMensualesJob
estadosCuentaAnualesJob
```

Mecanismos:

- chunks;
- Manager/Worker;
- particionamiento;
- threads;
- Skip;
- Retry/backoff;
- rollback;
- métricas;
- auto-restart controlado.

Resultados de rendimiento:

| Configuración | Tiempo |
|---|---:|
| 1x1 | 1472 ms |
| 2x2 | 1057 ms |
| 3x3 | 878 ms |
| 4x4 | 775 ms |

Mejora 1x1→4x4: aproximadamente **47,35 %**.

## 10. Despliegue y escalabilidad

Docker Compose orquesta el stack.

Clientes, Cuentas y Pagos no dependen de puertos de host fijos para sus réplicas.

La solución se desplegó en AWS EC2 y Eureka confirmó:

```text
CLIENTES-SERVICE UP (2)
CUENTAS-SERVICE  UP (2)
PAGOS-SERVICE    UP (2)
```

## 11. Comparación con el sistema legacy

| Aspecto | Legacy | Solución modernizada |
|---|---|---|
| Procesamiento masivo | Scripts/Jobs rígidos | Spring Batch con Retry, particiones y métricas |
| Arquitectura | Monolítica | Microservicios independientes |
| Canales | Backend común | BFF por canal |
| Seguridad | Centralizada y poco granular | OAuth2/JWT y scopes |
| Integración | Acoplada | Kafka asíncrono |
| Resiliencia | Falla con impacto amplio | Circuit Breaker y fallback |
| Escalabilidad | Principalmente vertical | Réplicas horizontales |
| Despliegue | Infraestructura legacy | Docker + AWS EC2 |

## 12. Limitaciones y próximos pasos

La demostración cloud se ejecuta en una única instancia EC2, suficiente para evidenciar despliegue y múltiples réplicas de contenedores, pero no representa alta disponibilidad multi-host.

Evolución recomendada:

- ECS/EKS o Auto Scaling;
- ALB;
- RDS;
- MSK;
- Secrets Manager;
- certificados administrados;
- observabilidad centralizada;
- subredes privadas y políticas IAM mínimas.

## 13. Conclusión

La arquitectura final cubre los procesos críticos de modernización solicitados y demuestra integración efectiva entre Spring Batch, BFF, microservicios, Spring Cloud, seguridad distribuida, Kafka, Resilience4j, Docker y AWS.
