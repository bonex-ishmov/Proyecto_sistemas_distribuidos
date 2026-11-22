# Tienda Genérica — monorepo de microservicios

Cinco microservicios Spring Boot independientes (Java 25, Spring Boot 4.1.1) y un
frontend Angular para la gestión de una tienda genérica. Arquitectura y decisiones
de diseño en `CLAUDE.md`; estado del proyecto en `docs/diagnostico.md`.

Se puede correr de tres formas:

| Forma | Para qué | Sección |
|-------|----------|---------|
| `./mvnw spring-boot:run` por módulo + MySQL en Docker | Desarrollar | [Configurar un entorno nuevo](#configurar-un-entorno-nuevo) |
| `docker compose up -d --build` (19 contenedores, NGINX) | La arquitectura del curso, en tu PC | [Arquitectura distribuida en Docker](#arquitectura-distribuida-en-docker) |
| Kubernetes en Oracle Cloud (OKE), con Terraform | Mostrarla funcionando en la nube | [Despliegue en la nube](#despliegue-en-la-nube-kubernetes-en-oracle-cloud) |

| Módulo        | Puerto | Esquema      |
|---------------|--------|--------------|
| ms-identidad  | 8081   | db_identidad |
| ms-clientes   | 8082   | db_clientes  |
| ms-catalogo   | 8083   | db_catalogo  |
| ms-ventas     | 8084   | db_ventas    |
| ms-reportes   | 8085   | — (sin esquema ni BD propia) |

> `ms-reportes` es un agregador de solo lectura (módulo 7: consultas y reportes):
> no persiste nada, así que su `application-local.properties` no lleva
> `spring.datasource.*`. En su lugar necesita `app.jwt.secret` (igual que los
> demás módulos) y `ms.identidad.usuario-sistema` / `ms.identidad.password-sistema`:
> las credenciales del **mismo usuario de sistema que usa `ms-ventas`**
> (`ventas-sistema`) para autenticarse contra ms-identidad, ms-clientes y
> ms-ventas al armar cada reporte.

## Base de datos

MySQL **8.4 local, en un contenedor Docker** (`tienda-mysql`, definido en
`docker-compose.yml` de la raíz). Escucha en `127.0.0.1:3307` del host: no en
3306, para no chocar con un MySQL instalado en la máquina, y solo en `127.0.0.1`,
para no exponer la base a la red local. Cada integrante tiene su propia base.

Los servicios arrancan con `spring.jpa.hibernate.ddl-auto=validate`: **no crean ni
modifican tablas**. El esquema se administra con scripts versionados, que el
contenedor ejecuta solo, en este orden, **la primera vez que arranca** (con el
volumen vacío):

| Script               | Qué hace                                                                            |
|----------------------|-----------------------------------------------------------------------------------|
| `db/schema.sql`      | Crea los 4 esquemas (`ms-reportes` no tiene) y todas sus tablas, índices y FKs. Idempotente. |
| `db/seed.sql`        | Inserta los 5 proveedores que referencia `ms-catalogo/productos.csv`. Idempotente. |
| `db/usuario-app.sql` | Da al usuario `tienda` (el de los servicios) `SELECT/INSERT/UPDATE/DELETE` sobre los 4 esquemas; nada más. |

> El usuario inicial `admininicial` / `admin123456` lo crea `DataInitializer` de
> `ms-identidad` al arrancar (hashea la clave con BCrypt; no hace nada si ya existe).

### ⚠️ Cuidados

- **`POST /api/productos/carga` borra la tabla `productos` completa** y la recarga
  desde el CSV.
- No cambiar `ddl-auto` a `update` "para probar rápido". Si `validate` falla es
  porque una entidad y `db/schema.sql` divergieron: hay que actualizar el script
  (y recrear la base, ver abajo).

## Configurar un entorno nuevo

### 1. Requisitos
- JDK 25
- Docker con el plugin `docker compose`, y el usuario dentro del grupo `docker`

### 2. Levantar la base
```
cp .env.example .env          # y poner las dos claves (MYSQL_ROOT_PASSWORD, MYSQL_PASSWORD)
docker compose up -d mysql
docker compose ps             # esperar a que diga "healthy"
```
`.env` está en el `.gitignore`: las claves nunca van al repo.

Para borrar la base y recrearla desde los scripts (por ejemplo, tras cambiar
`db/schema.sql`):
```
docker compose down -v && docker compose up -d mysql
```

### 3. Configurar cada módulo
Cada módulo lee sus credenciales de un archivo **no versionado**
`src/main/resources/application-local.properties`. En cada módulo:

```
cp src/main/resources/application-local.properties.example \
   src/main/resources/application-local.properties
```

y editá el archivo copiado con los valores reales. Aplica a `ms-identidad`,
`ms-clientes`, `ms-catalogo` y `ms-ventas`; `ms-reportes` no tiene base de datos
propia y usa credenciales distintas (ver la nota más arriba):

| Propiedad                    | Valor                                                                                        |
|------------------------------|--------------------------------------------------------------------------------------------|
| `spring.datasource.url`      | `jdbc:mysql://localhost:3307/<esquema>?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true` — `<esquema>` según el módulo |
| `spring.datasource.username` | `tienda`                                                                                  |
| `spring.datasource.password` | el valor de `MYSQL_PASSWORD` en `.env`                                                    |
| `app.jwt.secret`             | cadena de 64 caracteres, **idéntica en los cinco módulos** (incluido `ms-reportes`)         |

`application.properties` lo importa con
`spring.config.import=optional:classpath:application-local.properties`, así que se
lee automáticamente: **no hay que activar ningún perfil**. Si el archivo no existe,
el servicio no arranca (los tests son la excepción: usan H2).

`useSSL=false` porque la conexión no sale de la máquina. Si la base vuelve a estar
en un servidor remoto, hay que usar `useSSL=true`: sin SSL la contraseña y los
datos viajarían en texto plano por internet.

### 4. Ejecutar
```
cd ms-identidad
./mvnw.cmd spring-boot:run
```
Repetir por módulo.

## Arquitectura distribuida en Docker

`docker-compose.yml` levanta la arquitectura del "Grafico explicativo segundo
corte" (raíz del repo), extendida con `ms-identidad` y `ms-reportes`: cada
microservicio con **2 réplicas** detrás de un **NGINX balanceador** (round robin),
y 2 frontends ("FR NodeJS") detrás de un NGINX de entrada.

```
navegador -> nginx-front:80 -> front-1|2 (node servidor.mjs) -> nginx-<servicio> -> ms-<servicio>-1|2 -> mysql
```

Cada front es `frontend/servidor.mjs`: un servidor Node sin dependencias que sirve
la app ya compilada (`ng build --configuration docker`) y reenvía `/api/*` al
balanceador del microservicio correspondiente. No se usa `ng serve`: el servidor de
desarrollo genera sus módulos con una huella distinta en cada copia, y con dos
copias detrás de un balanceador el navegador recibía 504 y la página quedaba en blanco.

```
docker compose up -d --build      # construye las imágenes y levanta los 19 contenedores
docker compose ps                 # las 10 réplicas y mysql deben decir "healthy"
```
Entrada: **http://localhost**. Requiere `.env` completo (ver `.env.example`).

Red `tienda-red` (172.28.0.0/24); el `.1` es el host. Cada servicio tiene un bloque
de 10 direcciones: `.X0` su NGINX, `.X1`/`.X2` sus réplicas. Puertos solo en `127.0.0.1`.

| Servicio  | NGINX (IP / puerto host) | Réplica 1        | Réplica 2        |
|-----------|--------------------------|------------------|------------------|
| identidad | .10 / 8093               | .11 / 8086       | .12 / 8087       |
| clientes  | .20 / 8091               | .21 / 8082       | .22 / 8083       |
| catálogo  | .30 / 8092               | .31 / 8084       | .32 / 8085       |
| ventas    | .40 / 8090               | .41 / 8080       | .42 / 8081       |
| reportes  | .50 / 8094               | .51 / 8088       | .52 / 8089       |
| frontend  | .60 / **80**             | .61 / 9000       | .62 / 9001       |
| mysql     | .100 / 3307              |                  |                  |

Los puertos de host de las réplicas chocan con los de `./mvnw spring-boot:run`
(8081–8085): no correr las dos formas a la vez.

- **Ver el balanceo:** cada NGINX agrega la cabecera `X-Atendido-Por` con la IP de
  la réplica que respondió (`curl -i http://localhost/api/...`).
- **Ver la tolerancia a fallos:** `docker stop ms-clientes-1` y repetir peticiones;
  NGINX las pasa a la réplica que sigue viva. `docker start ms-clientes-1` la reincorpora.
- Los servicios se llaman entre sí **por el balanceador** (`MS_*_URL=http://nginx-...`),
  no a una réplica fija.

### Apagar todo
```
docker compose down       # apaga y borra los contenedores; los datos de MySQL quedan en el volumen
docker compose down -v    # además borra el volumen: la base se recrea desde db/ al volver a subir
```

### Verificar el despliegue completo

Hay tres scripts en `docker/pruebas/` (requieren `bash`, `curl` y `jq`):

| Script | Qué hace | ¿Toca datos? |
|--------|----------|--------------|
| `todo.sh` | Lo corre todo: `./mvnw test` en los 5 módulos, `docker compose build`, arranque en frío, revisión de lo que crearon los scripts de `db/`, `e2e.sh`, `fallos.sh` y limpieza. Con `--sin-tests` se salta los tests de Maven. | No: usa una pila aparte con su propio volumen. |
| `e2e.sh` | Prueba funcional de punta a punta, todo entrando por `http://localhost`. | **Sí**: necesita una base recién creada (crea datos y la carga CSV borra los productos). Si `admininicial` ya no puede entrar, se detiene sin tocar nada. |
| `fallos.sh` | Tolerancia a fallos: apaga réplicas, reinicia un NGINX con una réplica caída y comprueba que todo se recupera. | No: solo hace lecturas, con el usuario de sistema de `.env`. |

```
docker/pruebas/todo.sh            # la verificación completa (~5 min)
docker/pruebas/fallos.sh          # solo tolerancia a fallos, sobre la pila que esté arriba
```

**Cómo hace `todo.sh` el arranque en frío sin tocar tus datos.** Levanta la misma
pila con otro nombre de proyecto (`COMPOSE_PROJECT_NAME=tienda-prueba`), así que
Docker le crea un volumen nuevo y vacío (`tienda-prueba_mysql-datos`) y MySQL ejecuta
`db/*.sql` desde cero. Los nombres de contenedor y los puertos son fijos, así que
mientras tanto apaga la pila normal (sin `-v`: su volumen queda intacto). Al terminar,
aunque algo falle, borra la pila de prueba y su volumen, y vuelve a encender la pila
normal si estaba encendida. A mano sería:

```
docker compose down
docker compose -p tienda-prueba up -d --wait   # espera a que los 19 estén sanos
docker/pruebas/e2e.sh && docker/pruebas/fallos.sh
docker compose -p tienda-prueba down -v        # borra SOLO el volumen de prueba
docker compose up -d --wait                    # vuelve la pila normal, con sus datos
```

Qué se comprueba:

| Área | Comprobación |
|------|-------------|
| Base | Se crean las 6 tablas, los 5 proveedores sembrados y los permisos del usuario `tienda` (solo SELECT/INSERT/UPDATE/DELETE) en los 4 esquemas. |
| Frontend | `GET /` y una ruta del router (`/clientes`) devuelven `index.html`; `X-Atendido-Por` alterna entre `front-1` y `front-2`. |
| Autenticación | Sin token o con token falso → 401; clave mala → 401; `admininicial` entra y queda desactivado al crear el primer usuario real. |
| CRUD | Alta, duplicado (409), datos inválidos (400) e inexistente (404) en usuarios y clientes; ninguna respuesta trae la contraseña. |
| Catálogo | Un CSV con NIT inexistente se rechaza; el CSV válido carga los 18 productos. |
| Ventas | Total = Σ `precio_venta` × cantidad; IVA 19 % sobre ese total; total con IVA = suma de los dos; `cedula_usuario` sale del JWT; cliente o producto inexistente → 404; cantidad 0 → 400. |
| Zona horaria | `fecha_venta` queda en hora de Bogotá y la venta cuenta en "ventas de hoy". |
| Reportes | Los 4 endpoints responden; el usuario de sistema no aparece en el listado de usuarios; los conteos del dashboard cuadran. |
| Balanceo | Cada NGINX de servicio (puertos 8090–8094) reparte 3 y 3 entre sus dos réplicas. |
| Tolerancia a fallos | Con la réplica 1 de **todos** los servicios y `front-1` apagadas, la app sigue respondiendo (incluido el dashboard, que cruza 4 servicios); un NGINX reiniciado con una réplica caída arranca y atiende; al encender las réplicas, vuelven a recibir peticiones. |

Última verificación: **2026-11-19**, `todo.sh` en verde: 88 tests, 46 comprobaciones
de `e2e.sh` y 30 de `fallos.sh`. Detalle en `docs/diagnostico.md`.

### Dos detalles del compose que no hay que deshacer

- **NGINX apunta a las réplicas por IP fija, no por nombre** (`REPLICA_1: "172.28.0.21:8080"`).
  NGINX resuelve los nombres una sola vez, al arrancar, y el DNS de Docker no resuelve
  un contenedor apagado. Con nombres, un NGINX que (re)arrancaba con una réplica caída
  fallaba con `host not found in upstream`, quedaba reiniciándose y el servicio entero
  respondía 502 aunque la otra réplica estuviera sana. Con IP arranca igual y marca
  como caída la réplica que no responde.
- **`TZ=America/Bogota` en las réplicas Spring Boot.** La imagen trae UTC. `ms-ventas`
  guarda `fecha_venta` con la hora del JVM y calcula "ventas de hoy / del mes" en
  `America/Bogota`. En UTC, una venta hecha después de las 7 p. m. quedaba con fecha
  del día siguiente y no contaba en el dashboard. Con `./mvnw` esto no pasa: el JVM
  toma la zona del equipo.

## Despliegue en la nube: Kubernetes en Oracle Cloud

La misma tienda corre en un clúster de Kubernetes gestionado por Oracle (OKE), en la
cuenta Free Tier del proyecto (región `us-sanjose-1`). La infraestructura se crea con
**Terraform** (`infra/oci/`) y la aplicación con **manifiestos de Kubernetes** (`k8s/`).

```
navegador -> Balanceador de Oracle (Service "front", IP pública, :80) -> front ×2
          -> Service ms-<servicio> -> 2 pods (uno en cada nodo) -> Service mysql -> mysql-0 + disco de 50 GB
```

### Qué cambió respecto de Docker

| Docker Compose | Kubernetes / Oracle |
|----------------|---------------------|
| `nginx-front` (puerto 80) | Service `front` tipo **LoadBalancer**: Oracle crea un balanceador flexible de 10 Mbps con IP pública |
| `nginx-identidad`, `-clientes`, `-catalogo`, `-ventas`, `-reportes` | Un **Service** (ClusterIP, puerto 80 → 8080) por microservicio: reparte entre los pods que están listos |
| Contenedores `ms-*-1` / `-2` con IP fija | **Deployment** con `replicas: 2`, una réplica en cada nodo |
| `tienda-mysql` + volumen `mysql-datos` | **StatefulSet** `mysql` + disco persistente (PVC, Block Volume de 50 GB) + Service headless |
| `.env` leído por el compose | **Secret** `tienda-secretos`, creado desde el mismo `.env` |
| `db/*.sql` montados en `initdb.d` | **ConfigMap** `mysql-init` con los mismos tres scripts |
| `healthcheck` + `restart: on-failure` | Sondas `startupProbe` / `readinessProbe` / `livenessProbe`; Kubernetes recrea los pods |
| `depends_on` (esperar a MySQL) | initContainer `esperar-mysql` |
| Cabecera `X-Atendido-Por` de NGINX | Cabecera `X-Atendido-Por-Front` de `servidor.mjs` + `kubectl get pods -o wide` |
| Imágenes construidas en el PC | Imágenes públicas en GHCR: `ghcr.io/bonex-ishmov/tienda-<módulo>:1.0.0` (x86 y ARM) |

El código Java no cambió: todo llega por variables de entorno. Los NGINX se
reemplazaron porque en Kubernetes el **Service** ya hace ese trabajo: es un nombre y
una IP estables que reparten entre las réplicas listas, y si una réplica muere
Kubernetes la saca del reparto y crea otra.

### Cuánto cuesta

| Recurso | Costo |
|---------|-------|
| Clúster OKE **Basic**, red, balanceador de 10 Mbps, disco de MySQL | $0 (Always Free) |
| 2 nodos **E5.Flex** x86 de 1 OCPU / 8 GB | ~USD 2,90 por día **encendidos**, pagados con el crédito de prueba (USD 300, vence el **2026-12-18**) |

Los nodos gratuitos son los ARM **A1.Flex**, pero Oracle no tenía capacidad A1 en
San José al montar el clúster. Por eso los nodos se **apagan cuando no se usan** (ver
el interruptor más abajo). Al vencer la prueba, Oracle detiene lo que no es Always
Free: para seguir después hay que migrar a A1 (ver "Pasar a nodos ARM").
Una alerta de presupuesto (USD 50/mes) avisa por correo al gastar el 50 % y el 100 %.

### Requisitos (una sola vez)

- `terraform`, `kubectl` y el CLI `oci` (instalados en `~/.local/bin`).
- `~/.oci/config` con una clave de API de la cuenta de Oracle (la clave privada vive en
  `~/.oci/`, nunca en el repo).
- Sesión en GHCR para publicar imágenes: `docker login ghcr.io -u <usuario>` con un
  token *classic* con permiso `write:packages`.

### Pasos

```
# 1. Imágenes (x86 + ARM) a GHCR. Versión nueva por cada cambio; nunca se sobrescribe.
docker/publicar-imagenes.sh 1.0.0

# 2. Infraestructura: red, clúster, nodos y alerta de presupuesto (18 recursos).
cd infra/oci
cp terraform.tfvars.example terraform.tfvars     # tenancy, región, tu IP, correo
terraform init
terraform plan                                   # revisar antes de crear
terraform apply
eval "$(terraform output -raw configurar_kubectl)"   # deja kubectl apuntando al clúster
cd ../..

# 3. La tienda: Secret (desde .env), ConfigMap (desde db/) y manifiestos.
k8s/desplegar.sh                                 # imprime la URL pública al final
```

`infra/oci/terraform.tfvars` y el estado de Terraform (`*.tfstate`) **no se versionan**.
Sin el estado, Terraform no sabe qué creó: no borrarlo.

### Operación diaria

| Qué | Comando |
|-----|---------|
| Apagar los nodos (deja de gastar crédito; los datos de MySQL quedan en su disco) | `infra/oci/nodos.sh apagar` |
| Encenderlos (~3 min; Kubernetes vuelve a poner los pods solo) | `infra/oci/nodos.sh encender` |
| Ver cuántos hay | `infra/oci/nodos.sh estado` |
| Ver los pods y en qué nodo está cada uno | `kubectl -n tienda get pods -o wide` |
| Ver la IP pública | `kubectl -n tienda get service front` |
| Ver qué front atendió | `curl -sI http://<IP>/ \| grep -i atendido` |

**Si `kubectl` dice `i/o timeout`**, casi siempre es porque cambió tu IP pública: la
API del clúster solo acepta la IP de `mi_ip` en `terraform.tfvars`. Ponla al día
(`curl -s https://checkip.amazonaws.com`) y corre `terraform apply` en `infra/oci`:
solo cambia esa regla. La tienda pública no se ve afectada, y `nodos.sh` tampoco
(usa el CLI de Oracle, no `kubectl`).

**Pasar a nodos ARM (gratis).** `infra/oci/buscar-capacidad-a1.sh` le pregunta a Oracle
cada 10 minutos si hay capacidad A1 y avisa cuando la haya. Entonces, en
`terraform.tfvars`: `forma_nodo = "VM.Standard.A1.Flex"` y `memoria_nodo = 6`, y
`terraform apply`. Las imágenes ya existen para ARM.

**Borrar todo** (en este orden: el balanceador y el disco los crea Kubernetes, así que
hay que borrarlos con Kubernetes antes de destruir el clúster):
```
kubectl delete namespace tienda      # borra pods, Services, el balanceador de Oracle y el disco de MySQL
cd infra/oci && terraform destroy
```

### Pruebas en la nube

```
MODO=k8s URL_BASE=http://<IP> docker/pruebas/e2e.sh   # SOLO sobre un despliegue nuevo
URL_BASE=http://<IP> k8s/fallos.sh                    # se puede correr cuando sea
```

- `e2e.sh` en modo `k8s` hace las mismas comprobaciones funcionales que en Docker y
  cambia las de balanceo: las peticiones llegan a los 2 pods del front, y cada Service
  tiene 2 pods listos en nodos distintos. **Necesita la base recién creada**: crea el
  usuario de prueba `ana` / `ana12345` (que desactiva a `admininicial`) y la carga CSV
  borra los productos.
- `k8s/fallos.sh` borra a la vez un pod de cada servicio mientras se le hacen peticiones,
  comprueba que Kubernetes los recrea, y borra el pod de MySQL para comprobar que los
  datos sobreviven en el disco. Solo hace lecturas.

Última verificación: **2026-11-19**, `e2e.sh` 53/53 y `k8s/fallos.sh` 17/17.
Mientras se borraban los pods respondieron 43 de 45 peticiones; las 2 que fallaron
estaban en curso en un pod justo cuando murió (en Docker NGINX las reintentaba en la
otra réplica).

> **El usuario `ana` / `ana12345` que crea `e2e.sh` existe en la base de la nube**, y la
> tienda es pública. Hay que crear el usuario real para la demo y eliminar `ana`.

### Detalles de los manifiestos que no hay que deshacer

- **Nombres de imagen completos** (`docker.io/library/mysql:8.4`, no `mysql:8.4`). Los
  nodos de OKE usan CRI-O, que rechaza los nombres cortos por ambiguos y deja el pod en
  `ImageInspectError`.
- **Reparto de réplicas** (`topologySpreadConstraints` con `DoNotSchedule`,
  `matchLabelKeys: [pod-template-hash]` y `nodeTaintsPolicy: Honor`): una réplica en cada
  nodo; solo cuentan los pods de la misma versión (si no, en una actualización las dos
  réplicas nuevas podían quedar en el mismo nodo), y un nodo caído deja de contar.
- **`subPath: mysql`** en el disco de MySQL: un disco nuevo trae `lost+found`, y MySQL se
  niega a inicializarse en una carpeta que no esté vacía.
- **Un StatefulSet no reemplaza un pod que nunca estuvo listo.** Si `mysql-0` queda con un
  error y se corrige el manifiesto, hay que borrarlo a mano (`kubectl -n tienda delete pod
  mysql-0`); el disco no se toca.
- **`agent: false` en `servidor.mjs`**: una conexión nueva por petición. Un Service reparte
  por conexión; con conexiones reutilizadas casi todo iría a la misma réplica.

## Pruebas
```
cd <módulo> && ./mvnw.cmd test
```
No tocan MySQL y no necesitan `application-local.properties`. Hay tres tipos:

- **Unitarias de la capa `service`** (`*ServiceTest`): JUnit 5 + Mockito, sin
  contexto Spring; la mayoría de los tests.
- **De carga de contexto** (`Ms*ApplicationTests`): levantan Spring con el perfil
  `test` (`src/test/resources/application-test.properties`): H2 en memoria en los
  cuatro módulos con base; `ms-reportes` no tiene base, solo URLs y credenciales ficticias.
- **De punta a punta dentro del servicio** (`VentaEndToEndTest`,
  `ReporteEndToEndTest`): contexto completo (con H2 en `ms-ventas`); las llamadas
  HTTP a otros microservicios las intercepta `MockRestServiceServer`, así que nunca
  salen de la JVM.

## Comandos de build
Ver `CLAUDE.md`.
