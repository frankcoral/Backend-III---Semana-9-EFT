# Evidencias — Desarrollo Backend III — EFT Semana 9

Esta carpeta contiene las **24 evidencias finales** utilizadas para validar la solución Banco XYZ de la Evaluación Final Transversal. Las capturas fueron renumeradas en una secuencia única y ordenadas por arquitectura, seguridad, mensajería, resiliencia, escalabilidad, nube y Spring Batch.

## Índice de evidencias

| N° | Archivo | Qué demuestra | Criterio principal |
|---:|---|---|---|
| 01 | `01-docker-stack-final.png` | Stack Docker final sin `core-banco`, con infraestructura, 3 BFF y 3 microservicios. | Docker / Arquitectura |
| 02 | `02-stack-saludable-eureka.png` | Config Server, Eureka, Auth Server y servicios de negocio registrados correctamente. | Spring Cloud |
| 03 | `03-bff-web-final-200.png` | BFF Web responde HTTP 200 con agregación de Cliente, Cuentas y Pagos. | BFF Web |
| 04 | `04-e2e-mobile-atm-seguridad.png` | Mobile HTTP 200, ATM saldo 200, retiro 201 y aislamiento ATM→Mobile 403. | BFF / OAuth2 |
| 05 | `05-oauth2-web-consentimiento-scopes.png` | Consentimiento OAuth2 y scopes específicos del canal Web. | Seguridad distribuida |
| 06 | `06-oauth2-web-token-scopes.png` | Token Web con `banco.read`, `banco.write` y `canal.web`, sin exponer el token completo. | Seguridad distribuida |
| 07 | `07-oauth2-atm-rechazado-en-mobile.png` | Un token ATM no puede utilizarse contra el BFF Mobile. | Seguridad por canal |
| 08 | `08-deposito-pagos-aprobado.png` | Operación de depósito aprobada por `pagos-service`. | Microservicios |
| 09 | `09-kafka-transaccion-completada.png` | Publicación de una transacción completada en Kafka. | Kafka |
| 10 | `10-kafka-consumidor-atm.png` | `bff-atm` consume `ATM_TRANSACCION_RECIBIDA` con partición, offset y estado aprobado. | Kafka |
| 11 | `11-kafka-topic-3-particiones.png` | `banco.transacciones.completadas` configurado con 3 particiones. | Kafka / Escalabilidad |
| 12 | `12-resilience4j-fallback-pagos.png` | Fallback HTTP 503 `FALLBACK_ACTIVO` cuando Pagos no está disponible. | Resilience4j |
| 13 | `13-escalabilidad-horizontal-local.png` | Clientes, Cuentas y Pagos ejecutándose con 2 réplicas cada uno en el entorno Docker local. | Escalabilidad |
| 14 | `14-loadbalancer-failover.png` | Continuidad del E2E con una réplica de Cuentas detenida. | Eureka / LoadBalancer |
| 15 | `15-aws-ec2-stack.png` | Stack completo ejecutándose en una instancia EC2. | Nube / Docker |
| 16 | `16-aws-eureka-escalabilidad.png` | Eureka en AWS mostrando 2 instancias UP de Clientes, Cuentas y Pagos. | Nube / Escalabilidad |
| 17 | `17-batch-transacciones-diarias.png` | Job de transacciones diarias con procesamiento particionado y métricas. | Spring Batch |
| 18 | `18-batch-intereses-mensuales.png` | Job de intereses mensuales con paralelismo y métricas. | Spring Batch |
| 19 | `19-batch-estados-anuales-resumen.png` | Resumen del proceso anual y archivo de estados generado. | Spring Batch |
| 20 | `20-batch-estados-anuales-completed.png` | `estadosCuentaAnualesJob` finaliza en `COMPLETED`. | Spring Batch |
| 21 | `21-batch-retry-recuperado.png` | Fallo transitorio, Retry y recuperación exitosa en `COMPLETED`. | Spring Batch / Errores |
| 22 | `22-batch-retry-agotado-rollback.png` | Reintentos agotados y rollback controlado ante fallo persistente. | Spring Batch / Errores |
| 23 | `23-batch-auto-restart.png` | Fallo crítico seguido de reejecución automática y recuperación exitosa. | Spring Batch / Reejecución |
| 24 | `24-batch-rendimiento-1x1-4x4.png` | Comparación 1x1, 2x2, 3x3 y 4x4 sobre el mismo conjunto de datos. | Spring Batch / Rendimiento |

## Relación con la pauta de evaluación

### Criterio 1 — Cinco procesos críticos
La identificación de los cinco procesos de modernización se documenta en `readme.md` y `docs/arquitectura-tecnica.md`: Spring Batch, microservicios, BFF, seguridad distribuida y mensajería asíncrona con Kafka.

### Criterio 2 — Arquitectura y requerimientos de negocio
La propuesta y los tres requerimientos justificados se documentan en `docs/arquitectura-tecnica.md`. Las evidencias 01, 02, 13, 14, 15 y 16 muestran la arquitectura funcionando.

### Criterio 3 — Spring Batch
Evidencias principales: **17 a 24**.

Se demuestran los tres procesos obligatorios, paralelismo, particionamiento, Retry, rollback, reejecución automática y comparación de rendimiento.

### Criterio 4 — BFF Web, Mobile y ATM
Evidencias principales: **03 a 07**.

Se demuestra separación por canal, respuestas diferenciadas, OAuth2/JWT, scopes y aislamiento de autorización.

### Criterio 5 — Microservicios, Spring Cloud, Resilience4j y Kafka
Evidencias principales: **02, 08 a 14**.

Se demuestra integración de Clientes, Cuentas y Pagos, descubrimiento por Eureka, LoadBalancer, Kafka y fallback con Resilience4j.

### Criterio 6 — Docker, escalabilidad horizontal y nube
Evidencias principales: **15 y 16**.

La solución fue desplegada realmente en AWS EC2. Eureka muestra **2 instancias UP** de `CLIENTES-SERVICE`, `CUENTAS-SERVICE` y `PAGOS-SERVICE`.

### Criterio 7 — Documentación
La entrega incluye:

```text
readme.md
instrucciones.md
despliegue.md
docs/arquitectura-tecnica.md
evidencias/README.md
```

El informe técnico final se entrega además en PDF utilizando la plantilla oficial de la EFT.

### Criterio 8 — Video
El video MP4 de 5 a 7 minutos se realiza como entregable final separado, con webcam y los cuatro puntos exigidos en la pauta.

## Secuencia recomendada de revisión

Una revisión rápida de la solución puede seguir esta secuencia:

```text
01 -> 02 -> 03 -> 04 -> 10 -> 11 -> 12 -> 13 -> 14 -> 15 -> 16 -> 17 -> 21 -> 22 -> 23 -> 24
```

Con esa secuencia se observa el stack, seguridad por canal, mensajería, resiliencia, escalabilidad local y en nube, y los mecanismos avanzados de Spring Batch.

> Las evidencias corresponden a un entorno académico de demostración. No se incluyen tokens completos ni secretos de producción.
