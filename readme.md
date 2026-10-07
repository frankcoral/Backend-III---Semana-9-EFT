# Banco XYZ — EFT Desarrollo Backend III (PBY2203)

Proyecto final transversal para modernizar el backend legacy del **Banco XYZ** mediante Spring Batch, microservicios, patrón Backend for Frontend, seguridad distribuida, mensajería asíncrona, resiliencia, Docker y despliegue en AWS.

## Repositorio

Repositorio oficial:

```text
https://github.com/frankcoral/Backend-III---Semana-9-EFT.git
```

## Cinco procesos críticos de modernización

La solución aborda los cinco procesos exigidos por la pauta:

1. Migración de procesos legacy a **Spring Batch**.
2. División del sistema monolítico en **microservicios**.
3. Implementación de **Backend for Frontend (BFF)** para Web, Mobile y ATM.
4. Implementación de **seguridad distribuida** con Spring Security y OAuth2/JWT.
5. Integración de **mensajería asíncrona con Apache Kafka**.

## Arquitectura final

```text
                           auth-server
                         OAuth2 / JWT / JWK
                                |
            +-------------------+-------------------+
            |                   |                   |
        BFF WEB             BFF MOBILE           BFF ATM
        HTTPS               HTTPS                HTTPS
      canal.web           canal.mobile          canal.atm
            |                   |                   |
            +-------------------+-------------------+
                                |
                    Eureka + LoadBalancer
                                |
            +-------------------+-------------------+
            |                   |                   |
     clientes-service     cuentas-service      pagos-service
            |                   |                   |
            +------------- MySQL / Kafka -----------+
                                                |
                           banco.transacciones.completadas
                                                |
                                             BFF ATM
                                             consumer
```

Servicios de infraestructura:

- `config-server`: configuración centralizada con Spring Cloud Config.
- `discovery-server`: descubrimiento dinámico con Eureka.
- `auth-server`: emisión y validación de tokens OAuth2/JWT.
- Apache Kafka: mensajería asíncrona.
- MySQL: persistencia.

`batch-migration` funciona como módulo independiente para los procesos masivos.

## Spring Batch

Se implementaron los tres procesos solicitados:

- Reporte de transacciones diarias.
- Cálculo de intereses mensuales.
- Generación de estados de cuenta anuales.

La implementación incluye:

- Jobs y Steps separados;
- Reader, Processor y Writer;
- procesamiento por chunks;
- Manager/Worker;
- particionamiento;
- ejecución paralela;
- SkipPolicy y SkipListener;
- Retry con backoff;
- rollback ante fallo persistente;
- métricas;
- reejecución automática controlada ante fallo crítico.

El archivo `intereses_trimestrales.csv` conserva su nombre histórico por compatibilidad, aunque el Job implementado corresponde al cálculo de **intereses mensuales**.

### Comparación de rendimiento

Pruebas realizadas sobre el mismo conjunto de datos:

| Configuración | Duración |
|---|---:|
| 1 partición / 1 hilo | 1472 ms |
| 2 particiones / 2 hilos | 1057 ms |
| 3 particiones / 3 hilos | 878 ms |
| 4 particiones / 4 hilos | 775 ms |

La configuración 4x4 redujo el tiempo aproximadamente un **47,35 %** respecto de 1x1.

## BFF por canal

### BFF Web

Entrega una vista completa para navegador e integra Cliente, Cuentas y Pagos. Utiliza OAuth2 **Authorization Code + PKCE**, HTTPS y el scope `canal.web`.

### BFF Mobile

Entrega un payload reducido con información esencial y solo las **3 transacciones más recientes**, disminuyendo datos innecesarios para dispositivos móviles. Utiliza `canal.mobile`.

### BFF ATM

Permite consultar saldo y realizar retiros. Utiliza `canal.atm`, HTTPS y tolerancia a fallos. El saldo no se mantiene como estado autoritativo dentro del BFF: la fuente de verdad es `cuentas-service`.

## Seguridad

Scopes principales:

```text
Web:    banco.read banco.write canal.web
Mobile: banco.read canal.mobile
ATM:    banco.read banco.write canal.atm
```

Se validó que un token ATM no pueda acceder al BFF Mobile, obteniendo HTTP 403.

## Microservicios

### clientes-service
Gestiona información y perfiles de clientes.

### cuentas-service
Gestiona cuentas, estados y saldos. La entidad de cuenta utiliza control de concurrencia optimista.

### pagos-service
Procesa depósitos, pagos, transferencias y retiros, y publica eventos de operaciones completadas.

## Kafka

Tópicos principales:

```text
banco.clientes.eventos
banco.cuentas.eventos
banco.transacciones.completadas
```

`banco.transacciones.completadas` utiliza **3 particiones**. `pagos-service` actúa como productor y `bff-atm` consume eventos con concurrencia 3.

## Resilience4j

Ante indisponibilidad total de `pagos-service`, el BFF ATM evita confirmar una operación no procesada y responde de forma controlada:

```text
HTTP 503
resilience = FALLBACK_ACTIVO
```

## Escalabilidad y failover

Clientes, Cuentas y Pagos admiten múltiples réplicas sin puertos de host fijos.

Se validó localmente:

```text
CLIENTES-SERVICE -> 2 UP
CUENTAS-SERVICE  -> 2 UP
PAGOS-SERVICE    -> 2 UP
```

También se validó continuidad operacional al detener una réplica de Cuentas mientras la segunda permanecía disponible.

## Despliegue real en AWS

La solución final fue desplegada en **AWS EC2** mediante Docker Compose.

En la instancia cloud se levantó el stack completo y se escaló horizontalmente:

```text
CLIENTES-SERVICE -> 2 instancias UP
CUENTAS-SERVICE  -> 2 instancias UP
PAGOS-SERVICE    -> 2 instancias UP
```

Eureka confirmó las seis instancias de negocio registradas en la nube. Las evidencias se encuentran en:

```text
evidencias/15-aws-ec2-stack.png
evidencias/16-aws-eureka-escalabilidad.png
```

Los pasos reproducibles se encuentran en `despliegue.md`.

## Datos de demostración

```text
RUT: 11111111-1
Cuenta: CTA-DEMO-000001
Saldo inicial: 100000 CLP
```

La carga automática se controla con:

```text
DEMO_DATA_ENABLED=true
```

## Ejecución rápida local

Crear `.env`:

```powershell
Copy-Item .env.example .env
```

Levantar:

```powershell
docker compose up -d --build
```

Verificar:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\verificar-stack.ps1
```

Prueba E2E:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\probar-e2e.ps1
```

Escalabilidad:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\escalar-microservicios.ps1 -Replicas 2
```

## Documentación entregada

- `readme.md`: descripción general, arquitectura y resultados.
- `instrucciones.md`: ejecución y pruebas paso a paso.
- `despliegue.md`: despliegue reproducible en AWS.
- `docs/arquitectura-tecnica.md`: propuesta técnica, requerimientos y decisiones.
- `evidencias/README.md`: trazabilidad de las 24 evidencias finales.
- Informe técnico final en PDF usando la plantilla oficial de la EFT.

## Estado final

Se validaron:

- compilación de los módulos;
- stack Docker saludable;
- tres BFF independientes;
- seguridad OAuth2/JWT separada por canal;
- tres microservicios con Spring Cloud;
- Eureka y LoadBalancer;
- Kafka productor/consumidor;
- tres particiones en el tópico principal;
- Resilience4j y fallback;
- escalabilidad horizontal;
- failover;
- tres procesos Spring Batch;
- Retry, rollback y reejecución automática;
- comparación 1x1, 2x2, 3x3 y 4x4;
- despliegue y escalabilidad real en AWS EC2.

El video MP4 se prepara como último entregable independiente.
