# Despliegue en AWS — Banco XYZ EFT

Este documento describe el procedimiento utilizado para desplegar la solución Banco XYZ en **AWS EC2** con Docker Compose y validar escalabilidad horizontal.

## 1. Objetivo

Desplegar en nube:

- Config Server;
- Eureka;
- Authorization Server;
- BFF Web;
- BFF Mobile;
- BFF ATM;
- `clientes-service`;
- `cuentas-service`;
- `pagos-service`;
- Kafka;
- MySQL.

Luego escalar horizontalmente los tres microservicios de negocio.

## 2. Instancia EC2

Se utiliza una instancia Linux con recursos suficientes para construir y ejecutar las imágenes Java del proyecto.

Configuración recomendada para la demostración:

```text
Sistema: Ubuntu Server
Arquitectura: x86_64
Almacenamiento: 30 GiB
IP pública: habilitada
```

Un tamaño equivalente a 4 vCPU y 16 GiB de RAM facilita la construcción simultánea de los módulos.

## 3. Security Group

Para la demostración académica se habilitan:

```text
22    SSH
8081  BFF Web
8082  BFF Mobile
8083  BFF ATM
8761  Eureka
8888  Config Server
9000  Authorization Server
```

MySQL y Kafka no necesitan exponerse públicamente.

> En un entorno productivo, restringir orígenes, utilizar HTTPS administrado, balanceadores, subredes privadas y reglas mínimas de acceso.

## 4. Conexión

Con EC2 Instance Connect, usuario:

```text
ubuntu
```

Comprobar:

```bash
whoami
uname -a
```

## 5. Instalar Docker y Git

```bash
sudo apt-get update
sudo apt-get install -y ca-certificates curl git

sudo install -m 0755 -d /etc/apt/keyrings

curl -fsSL https://download.docker.com/linux/ubuntu/gpg | \
sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg

sudo chmod a+r /etc/apt/keyrings/docker.gpg

echo \
"deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
$(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

sudo apt-get update

sudo apt-get install -y \
docker-ce \
docker-ce-cli \
containerd.io \
docker-buildx-plugin \
docker-compose-plugin

sudo usermod -aG docker $USER

sudo docker --version
sudo docker compose version
git --version
```

## 6. Clonar el proyecto

```bash
git clone https://github.com/frankcoral/Backend-III---Semana-9-EFT.git banco-xyz-bff
cd banco-xyz-bff
git log --oneline -1
```

## 7. Preparar `.env`

```bash
cp .env.example .env
```

Reemplazar `<IP_PUBLICA_EC2>`:

```bash
sed -i 's|AUTH_ISSUER_URI=http://localhost:9000|AUTH_ISSUER_URI=http://<IP_PUBLICA_EC2>:9000|' .env

sed -i 's|OAUTH2_WEB_REDIRECT_URI=https://localhost:8081/login/oauth2/code/banco-web|OAUTH2_WEB_REDIRECT_URI=https://<IP_PUBLICA_EC2>:8081/login/oauth2/code/banco-web|' .env
```

Verificar:

```bash
grep -E "AUTH_ISSUER_URI|OAUTH2_WEB_REDIRECT_URI|DEMO_DATA_ENABLED|KAFKA_BOOTSTRAP_SERVERS" .env
```

## 8. Verificar almacenamiento

```bash
df -h /
lsblk -o NAME,SIZE,TYPE,MOUNTPOINTS
```

Para este proyecto se utilizaron **30 GiB**.

Si el volumen EBS se amplía después de crear la instancia y la raíz es `ext4` sobre `/dev/nvme0n1p1`:

```bash
sudo growpart /dev/nvme0n1 1
sudo resize2fs /dev/nvme0n1p1
df -h /
```

El nombre del dispositivo debe verificarse previamente con `lsblk`.

## 9. Construir y levantar

Desde la raíz:

```bash
sudo docker compose up -d --build
```

Comprobar:

```bash
sudo docker compose ps
```

El stack debe mostrar activos los tres BFF, los tres microservicios, Config, Eureka, Auth, Kafka y MySQL.

## 10. Escalar los microservicios

```bash
sudo docker compose up -d \
  --scale clientes-service=2 \
  --scale cuentas-service=2 \
  --scale pagos-service=2
```

Resultado esperado:

```text
clientes-service-1 healthy
clientes-service-2 healthy
cuentas-service-1 healthy
cuentas-service-2 healthy
pagos-service-1 healthy
pagos-service-2 healthy
```

## 11. Validar con Eureka

Abrir:

```text
http://<IP_PUBLICA_EC2>:8761
```

En `Instances currently registered with Eureka` debe aparecer:

```text
CLIENTES-SERVICE  UP (2)
CUENTAS-SERVICE   UP (2)
PAGOS-SERVICE     UP (2)
```

Esta validación demuestra escalabilidad horizontal real en nube.

## 12. Evidencias finales

Las capturas utilizadas en la EFT son:

```text
evidencias/15-aws-ec2-stack.png
evidencias/16-aws-eureka-escalabilidad.png
```

La primera muestra el stack ejecutándose dentro de EC2. La segunda muestra Eureka accesible mediante la IP pública y dos instancias de cada microservicio de negocio.

## 13. Consideraciones de seguridad

La configuración utilizada corresponde a una **demostración académica**.

Para producción:

- restringir SSH por origen;
- usar HTTPS con certificado administrado;
- no exponer Eureka, Config Server ni Authorization Server directamente a Internet;
- utilizar secretos administrados;
- ubicar bases de datos y brokers en red privada;
- usar ALB/NLB, Auto Scaling o ECS/EKS según el diseño;
- centralizar logs y métricas;
- utilizar RDS/MSK u otros servicios administrados cuando corresponda.

## 14. Detener recursos

Al finalizar la demostración, detener la instancia EC2 desde AWS para evitar consumo innecesario.

Si se desea apagar solo los contenedores:

```bash
sudo docker compose down
```

## 15. Resultado obtenido

La validación final confirmó en AWS:

```text
Stack completo ejecutándose en EC2
CLIENTES-SERVICE -> 2 instancias UP
CUENTAS-SERVICE  -> 2 instancias UP
PAGOS-SERVICE    -> 2 instancias UP
```

Por lo tanto, el proyecto demuestra Docker, despliegue real en nube y escalabilidad horizontal de los tres microservicios requeridos.
