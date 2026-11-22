# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A monorepo of five **independent** Spring Boot 4.1 / Java 25 microservices for a generic
store ("Tienda Generica"), built for a distributed-systems course. Domain language,
identifiers, and messages are all in Spanish — keep new code consistent with that.

| Module        | Port | Schema        | Responsibility                                              |
|---------------|------|---------------|------------------------------------------------------------|
| `ms-identidad`| 8081 | `db_identidad`| Login + user (`usuario`) CRUD. **Only service that mints JWTs.** |
| `ms-clientes` | 8082 | `db_clientes` | Customer (`cliente`) CRUD.                                  |
| `ms-catalogo` | 8083 | `db_catalogo` | Supplier (`proveedor`) + product (`producto`) CRUD, CSV bulk load. |
| `ms-ventas`   | 8084 | `db_ventas`   | Sale (`venta` + `detalle_venta`) registration and lookup, IVA/total calculation; validates `cedula_cliente` over HTTP against ms-clientes (`cedula_usuario` is trusted from the JWT, not validated against ms-identidad — see "Persistencia entre microservicios"). Also serves `GET /api/ventas/resumen` (ventas de hoy / del mes en curso by `fecha_venta`) for the dashboard. |
| `ms-reportes` | 8085 | *(none — no DB of its own)* | Módulo 7 read-only queries (usuarios, clientes, ventas-por-cliente): pure HTTP aggregator over ms-identidad, ms-clientes, ms-catalogo and ms-ventas. Also serves `GET /api/reportes/resumen`, the single call the frontend dashboard makes (conteos de productos/clientes/proveedores/usuarios activos + resumen de ventas de ms-ventas). |

There is **no aggregator/parent `pom.xml`** at the repo root. Each module is a standalone
Maven project with its own wrapper. All Maven commands must be run from inside the module directory.

## Commands

Run from a module directory (e.g. `ms-identidad/`). Use `./mvnw.cmd` on Windows, `./mvnw` elsewhere.

```
./mvnw.cmd spring-boot:run                      # run the service
./mvnw.cmd clean package                        # build jar (runs tests)
./mvnw.cmd test                                 # all tests
./mvnw.cmd test -Dtest=UsuarioServiceTest       # one test class
./mvnw.cmd test -Dtest=UsuarioServiceTest#crearUsuario   # one test method
```

None of the tests need MySQL or `application-local.properties`. Three kinds:
- `*ServiceTest` — plain JUnit 5 + Mockito unit tests on the `service` layer; they mock
  repositories/gateways and do **not** load a Spring context. Most tests are these.
- `Ms*ApplicationTests` — context-load tests with the `test` profile
  (`src/test/resources/application-test.properties`): H2 in memory in the four modules with a DB;
  `ms-reportes` has no DB, only fake URLs and credentials.
- `VentaEndToEndTest`, `ReporteEndToEndTest` — full context; outbound HTTP to other services is
  intercepted by `MockRestServiceServer`, so nothing leaves the JVM.

Whole-deployment verification lives in `docker/pruebas/` (Docker; `e2e.sh` also runs against
Kubernetes with `MODO=k8s`) and `k8s/fallos.sh` — see "Distributed deployment" and "Cloud deployment".

## Local setup

- The database is a **local MySQL 8.4 in Docker** (container `tienda-mysql`, root
  `docker-compose.yml`), published on `127.0.0.1:3307` (3306 is left free for a host MySQL).
  Each developer has their own. (It used to be a shared Railway instance; that one is gone.)
  Services run with `ddl-auto=validate` and never create or alter tables.
- `docker compose up -d mysql` reads the two passwords from the git-ignored root `.env`
  (copy `.env.example`). On first start (empty volume) the container runs, in order,
  `db/schema.sql` (creates the 4 schemas + all tables — `ms-reportes` has none), `db/seed.sql`
  (seeds the 5 suppliers referenced by `ms-catalogo/productos.csv`) and `db/usuario-app.sql`
  (grants the app user `tienda` only SELECT/INSERT/UPDATE/DELETE on the 4 schemas).
  To rebuild from the scripts: `docker compose down -v && docker compose up -d mysql`.
- Each module reads `spring.datasource.*` and `app.jwt.secret` from a **git-ignored**
  `src/main/resources/application-local.properties` (copy it from the committed
  `application-local.properties.example`, drop the `.example` suffix, fill in real values).
  `application.properties` pulls it in via `spring.config.import=optional:classpath:application-local.properties`
  — no profile to activate; the service will not start if the file is missing (tests use H2 instead).
  Datasource: `jdbc:mysql://localhost:3307/<schema>?useSSL=false&...`, user `tienda`, password =
  `MYSQL_PASSWORD` from `.env` (`useSSL=false` only because the connection never leaves the
  machine — a remote DB must go back to `useSSL=true`). `app.jwt.secret` must be byte-identical
  across all five modules (`ms-reportes` has no datasource, but it still needs `app.jwt.secret`
  to verify tokens).
- `POST /api/productos/carga` wipes the whole `productos` table.

### Distributed deployment (Docker) — "Grafico explicativo segundo corte.jpeg"
- `docker compose up -d --build` runs the course's target architecture, extended with
  ms-identidad and ms-reportes: **every** service has 2 replicas behind its own NGINX
  round-robin balancer, plus 2 frontends behind an entry NGINX on port 80 (19 containers).
  Flow: `navegador -> nginx-front:80 -> front-1|2 -> nginx-<servicio> -> ms-<servicio>-1|2 -> mysql`.
- Network `tienda-red` 172.28.0.0/24 (.1 = host). Per service a block of 10: `.X0` its NGINX,
  `.X1`/`.X2` its replicas — identidad .1x, clientes .2x, catalogo .3x, ventas .4x, reportes .5x,
  front .6x, mysql .100. Host ports and the full table are in `README.md`; all bound to 127.0.0.1.
- Inside containers every Spring Boot listens on 8080; config comes from env vars set in the
  compose file from `.env` (`APP_JWT_SECRET`, `SPRING_DATASOURCE_*`, `MS_*_URL`, ...). No secret is
  baked into an image (`application-local.properties` is in each `.dockerignore`).
  Service-to-service calls always go through the balancer (`MS_*_URL=http://nginx-...`).
- The 6 NGINX share one template, `docker/nginx/balanceo.conf.template` (`zone` so the round robin
  is shared across worker processes; `client_max_body_size 2m` for the CSV upload; response header
  `X-Atendido-Por` shows which replica answered).
- `REPLICA_1/2` of each NGINX are the replicas' **fixed IPs**, not container names. Keep it that
  way: NGINX resolves names only at startup and Docker's DNS does not resolve a stopped
  container, so with names an NGINX (re)started while one replica was down crash-looped with
  `host not found in upstream` and the whole service returned 502.
- `TZ: America/Bogota` is set for all 10 Spring Boot replicas (`x-spring-env`); the image defaults
  to UTC. `ms-ventas` writes `fecha_venta` with the JVM clock but computes "today / this month"
  for the dashboard in `America/Bogota`; under UTC a sale after 7 p.m. landed on the next day.
- Each "FR NodeJS" is `frontend/servidor.mjs` (Node, no dependencies): serves the `ng build
  --configuration docker` output and proxies `/api/*` to the balancers. Targets come from
  `DESTINO_IDENTIDAD|CLIENTES|CATALOGO|VENTAS|REPORTES` (default: the `nginx-*` names, so the
  compose sets nothing; `k8s/` sets the Service names). Every response carries
  `X-Atendido-Por-Front: <hostname>` (pod name in k8s). It proxies with `agent: false` (a new
  connection per request) on purpose: a k8s Service balances per *connection*, so keep-alive
  would pin almost everything to one replica. `environment.docker.ts`
  has empty URLs so the browser calls `/api/...` same-origin (no CORS). **Do not go back to
  `ng serve` here**: each dev-server copy hashes its optimized deps differently, so two copies
  behind a balancer return 504 "Outdated Optimize Dep" and the page stays blank.
- `ms-identidad-2` waits for `ms-identidad-1` to be healthy (+ `restart: on-failure`): both seed
  `admininicial` and the system user on startup and would race on an empty DB.
- `ms-identidad` seeds an initial admin (`admininicial` / `admin123456`) via `DataInitializer`
  on startup. It is **auto-deactivated** the moment the first real user is created.
- Verification scripts (`bash` + `curl` + `jq`), details in `README.md`:
  - `docker/pruebas/todo.sh` — mvn tests, build, cold start as compose project `tienda-prueba`
    (own empty volume, so the normal volume is never touched), `e2e.sh`, `fallos.sh`, cleanup.
  - `docker/pruebas/e2e.sh` — functional checks through `http://localhost`. **Needs a freshly
    created DB** (logs in as `admininicial`, and the CSV load wipes `productos`); it aborts if
    `admininicial` can't log in. Never point it at a DB with real data.
  - `docker/pruebas/fallos.sh` — read-only failover checks (stops replicas, restarts an NGINX
    with a replica down); safe on the normal stack.
- Images build for `linux/amd64` and `linux/arm64`: every Dockerfile's build stage is
  `FROM --platform=$BUILDPLATFORM ...` (jar/JS are arch-independent, so no emulation) and the
  final stage only copies. `docker/publicar-imagenes.sh <version>` pushes all six to GHCR
  (`ghcr.io/bonex-ishmov/tienda-<module>:<version>`, public) and refuses to overwrite a tag.

### Cloud deployment (Kubernetes on Oracle OKE) — full guide in `README.md`
- Oracle Cloud Free Tier account (tenancy `bonexishmov`, home region `us-sanjose-1`). **Not**
  Pay As You Go. `infra/oci/` (Terraform, provider `oracle/oci`, auth from `~/.oci/config`)
  creates the VCN (subnets: `api` public but only `mi_ip`, `nodos` private behind a NAT,
  `balanceo` public port 80 only), an OKE **BASIC_CLUSTER** (free control plane, flannel), a node
  pool and a USD 50/month budget alert. `terraform.tfvars` and `*.tfstate` are git-ignored.
- Nodes: 2 × `VM.Standard.E5.Flex` (x86, 1 OCPU / 8 GB), paid from the trial credit (USD 300,
  **expires 2026-12-18**) because there was no A1 (ARM, Always Free) capacity. Migrating = set
  `forma_nodo`/`memoria_nodo` in tfvars + `terraform apply` (images are already multi-arch);
  `infra/oci/buscar-capacidad-a1.sh` polls for capacity. Node count is owned by
  `infra/oci/nodos.sh encender|apagar|estado` (Terraform ignores `size`). Turn nodes off when
  not in use — the user cares about the credit.
- `k8s/` = the app: namespace `tienda`; one Service (ClusterIP :80→8080) + Deployment ×2 per
  microservice (these Services **replace the NGINX balancers** — user's decision); `front` ×2
  behind a `LoadBalancer` Service (OCI flexible 10 Mbps, `security-rule-management-mode: None`
  because Terraform owns the security lists); MySQL as a StatefulSet with a 50 GB `oci-bv` PVC
  (`subPath: mysql` because of `lost+found`) and a headless Service. `k8s/desplegar.sh` creates
  the Secret `tienda-secretos` from `.env` and the ConfigMap `mysql-init` from `db/*.sql`
  (single copy of the SQL), then applies everything. The 5 microservice manifests share one
  structure — keep them in sync when changing one.
- Keep: fully-qualified image names (`docker.io/library/...` — OKE's CRI-O rejects short
  names); `topologySpreadConstraints` with `DoNotSchedule` + `matchLabelKeys:
  [pod-template-hash]` + `nodeTaintsPolicy: Honor` (one replica per node, version-aware);
  `JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=60` with a 1 GiB limit. A StatefulSet won't replace
  a pod that never became ready: delete `mysql-0` by hand after fixing its manifest.
- `kubectl` only works from the IP in `mi_ip`; a timeout usually means the user's public IP
  changed (update tfvars + `terraform apply`). Tear down in order: `kubectl delete namespace
  tienda` (removes the OCI load balancer and the block volume) **then** `terraform destroy`.
- The cloud DB contains the e2e test user `ana`/`ana12345` (and `admininicial` is deactivated)
  unless the user already replaced it. Never run `e2e.sh` against a deployment with real data.

## Architecture

### Request flow (every module)
`Controller` (`@RestController`, `/api/...`) → `Service` (`@Service`, `@Transactional`, all
business rules and validation) → `Repository` (Spring Data JPA).

`ms-reportes` is the deliberate exception: it has no database, so there is no `Repository`
layer. It goes `Controller` → `Service` → `Gateway` (HTTP adapters that call ms-identidad,
ms-clientes, ms-catalogo and ms-ventas) instead — it persists nothing of its own, so a
repository would have nothing to talk to. (ms-catalogo was added as a gateway for the
`GET /api/reportes/resumen` dashboard endpoint, which needs product and supplier counts.)

- **DTOs are Java `record`s** (`*Request`, `*Update Request`, `*Response`). Entities are never
  exposed directly.
- **Mappers are static utility classes** (`UsuarioMapper.aResponse(...)`), not Spring beans.
- **`GlobalExceptionHandler`** (`@RestControllerAdvice`) is the single place that turns
  exceptions into the uniform `ErrorResponse` JSON. Throw the domain exceptions and let it map:
  - `RecursoNoEncontradoException` → 404
  - `ReglaNegocioException` → 409 (duplicate keys, "cannot delete initial user", etc.)
  - bean-validation failure → 400
  - `CredencialesInvalidasException` → 401 (`ms-identidad` only)
  - `ms-catalogo` adds `ArchivoInvalidoException` and `DatosInvalidosException` for CSV errors.
  - `ms-ventas` and `ms-reportes` add `DependenciaNoDisponibleException` → 503 (an outbound HTTP
    call to another service failed).

### Authentication — shared secret, single issuer
- All five services share the **same HS256 secret** in `app.jwt.secret`
  (`application.properties`). It **must be byte-identical** across modules or token
  verification fails.
- Only `ms-identidad` creates tokens (`JwtService.generarToken` in `AuthService.login`).
  Subject = `usuario`; claims = `cedula`, `nombre`. Default TTL 1h (`app.jwt.expiracion-ms`).
- `JwtAuthenticationFilter` + `JwtService` are **copied almost verbatim into every module**
  (only the package differs). The copies **only verify the signature** — no DB lookup, no
  roles. When changing auth logic, apply the same change to all five copies (tracked as
  deliberate, unresolved debt — see `docs/diagnostico.md`).
- `SecurityConfig`: stateless session, CSRF disabled. `ms-identidad` permits `/api/auth/**`
  and authenticates everything else; `ms-clientes`, `ms-catalogo`, `ms-ventas` and
  `ms-reportes` authenticate every request — none of them has a public endpoint.

### Frontend (Angular)
- SPA en `frontend/` (Angular CLI 22, standalone components, sin NgModules), puerto
  4200 en dev (`ng serve` / `npm start`).
- **Zoneless**: el proyecto se creó con `--zoneless`, sin `zone.js`. Angular detecta
  cambios por señales (`signal`/`computed`), no por el parcheo automático que
  `zone.js` hace sobre promesas, `setTimeout` y eventos del DOM. Es un cambio de
  comportamiento real frente a la mayoría de tutoriales y proyectos Angular < 18
  (que sí asumen `zone.js`): mutar estado por fuera de una señal, o por fuera de un
  evento que Angular ya maneja, no dispara detección de cambios sola. Guardar en
  `signal` todo estado que una plantilla lee; los observables de `HttpClient` no
  necesitan tratamiento especial (el scheduler zoneless los cubre).
- **Token JWT en `localStorage`** (`SesionService`, clave `tienda.token`). Decisión
  consciente con un trade-off conocido, no un descuido:
  - Vulnerable a XSS: cualquier script inyectado en la página puede leer
    `localStorage` desde JS. Con una cookie `httpOnly` ese vector se cierra, porque
    el JS del navegador ni siquiera puede leerla.
  - Pero una cookie `httpOnly` exige rehacer la seguridad de los 5 backends: hoy son
    stateless (`SessionCreationPolicy.STATELESS`) con CSRF deshabilitado porque el
    JWT viaja a mano en el header `Authorization`, que un atacante no puede forzar a
    mandar; una cookie la manda el navegador solo en cada petición, así que
    reintroduce el problema que CSRF protege contra, y tocaría reactivar y coordinar
    esa protección en los 5 `SecurityConfig`. Para un proyecto académico se aceptó
    el riesgo de XSS a cambio de no reabrir esa superficie en los 5 módulos.
  - Consecuencia práctica: no meter HTML de terceros sin sanitizar en el frontend
    (Angular ya escapa interpolaciones `{{ }}` por defecto; el riesgo aparece si se
    usa `[innerHTML]` con contenido no confiable) — si eso cambia, el trade-off deja
    de sostenerse y hay que reconsiderar cookies `httpOnly`.

### Cross-service conventions
- Services never share a database or use cross-schema foreign keys. Within `ms-catalogo`,
  `Producto` → `Proveedor` **is** a real FK (`fk_producto_proveedor`) because they live in
  the same schema.
- `POST /api/{recurso}/lote` endpoints take a list of IDs and return many records in one
  call — used for internal service-to-service reads: `ms-ventas` calls the `ms-clientes` and
  `ms-catalogo` ones to validate a sale, `ms-reportes` calls the `ms-clientes` one to resolve names.
- For `GET /api/reportes/resumen`, `ms-reportes` also does plain list reads against
  `ms-catalogo` (`GET /api/productos`, `GET /api/proveedores`) and counts the rows — ms-catalogo
  has no count endpoint. All service-to-service calls still carry the system-user JWT.

### CSV bulk load (`ms-catalogo`)
`POST /api/productos/carga`, `multipart/form-data`, part name `archivo`. The service reads the
whole file, validates **every** row accumulating all errors, and **aborts without touching the
DB** if any row is invalid. On success it **replaces the entire product table** inside one
transaction (delete-all then insert). Header row is auto-detected (first cell not numeric).

## Documento de requisitos

La fuente funcional del proyecto es `docs/requisitos-tienda-generica.pdf`
(Ing. Ricardo D. Camargo, UEB / MinTIC Ciclo 3). Léelo cuando necesites saber
QUÉ debe hacer el sistema.

Qué se usa y qué no:
- Parte 1 (Especificación Funcional): fuente principal de requisitos.
- Parte 2 (Equipo Scrum) y Parte 3 (Sprints, HU, mockups, casos de prueba): NO son obligatorias.
- Parte 4 (Especificaciones Técnicas): solo lo compatible con la arquitectura actual.
- NO se usan: JSP, Tomcat como arquitectura principal, AWS/EC2/Elastic Beanstalk,
  Eclipse, ni arquitectura monolítica. El stack real es el descrito arriba en este archivo.

El documento define el QUÉ, no el CÓMO.

## Decisiones ya tomadas (no volver a proponerlas ni a decidirlas de otro modo)

Estas decisiones ya fueron discutidas y aprobadas. Varias contradicen el PDF a propósito;
cuando el PDF diga otra cosa, gana lo de aquí:

- Dinero: `DECIMAL` en BD y `BigDecimal` en Java. El PDF dice DOUBLE — no se usa.
- IVA: `precio_venta` es la base gravable y NO incluye IVA; el IVA se calcula
  sobre `precio_venta`. Razón: la pantalla de ventas del PDF define tres campos
  separados (Total Venta, Total IVA, Total con IVA); si `precio_venta` ya
  incluyera el IVA, "Total Venta" y "Total con IVA" serían el mismo valor y el
  formulario no tendría sentido. La relación `precio_venta ≈ precio_compra × 1.19`
  que aparece en `productos.csv` es un artefacto de datos de muestra generados
  sin margen comercial, no una regla de negocio.
- Venta: sin límite de productos en el backend. El "máximo 3 productos" del PDF es
  solo la maqueta; el frontend muestra 3 filas por defecto pero puede agregar más.
- Contraseñas: hasheadas siempre. Ningún endpoint devuelve la contraseña ni el hash,
  aunque el mockup del PDF muestre una columna Password.
- Rutas: convención REST estándar (`GET /api/clientes/{id}`), no las del PDF
  (`/clientes/guardar`, `/clientes/eliminar/{id}`).
- Usuario inicial `admininicial` se desactiva automáticamente al crear el primer
  usuario real (columna `activo`).
- Frontend: una sola aplicación en Angular + TypeScript que consume los servicios
  REST. Los mockups del PDF son referencia visual del QUÉ, no del CÓMO. No se usa
  JSP (ver "Documento de requisitos").
- Roles: NO se implementan. El PDF usa las palabras "administrador" y "usuario" en
  las historias, pero no define tabla de roles, ni gestión de roles, ni permisos
  por módulo; la tabla `usuarios` que especifica solo tiene cédula, nombre, email,
  usuario y contraseña. Implementar roles sería inventar un requisito.
- Autenticación servicio-a-servicio: sin API Gateway. `ms-ventas` y `ms-reportes`
  hacen login contra `ms-identidad` con un usuario de sistema propio y usan ese
  JWT para llamar a los endpoints `/lote`, que siguen exigiendo JWT. El usuario de
  sistema se excluye del listado de usuarios del módulo de reportes.
- Carga CSV de productos: modo reemplazo total de la tabla, en una transacción.
- Versiones: se usa Java y Spring Boot actuales. Las versiones del PDF (Java 11,
  Spring Boot 2.4.5) no obligan.
- Nube: Kubernetes gestionado en **Oracle Cloud (OKE)**, cuenta Free Tier, sin pasar a
  Pay As You Go; infraestructura con Terraform. AWS sigue excluido (ver "Documento de
  requisitos"). En Kubernetes los balanceadores NGINX se reemplazan por **Services** (y el
  NGINX de entrada por un Service `LoadBalancer`); MySQL corre **dentro del clúster**
  (StatefulSet + disco persistente); imágenes **públicas en GHCR**; secretos en **Secrets de
  Kubernetes** creados desde `.env`. El balanceo se muestra con la cabecera
  `X-Atendido-Por-Front` y con `kubectl`; las 2 réplicas de cada servicio van en nodos
  distintos. El despliegue en Docker con NGINX se conserva tal cual para correr en local.

## Persistencia entre microservicios

El PDF diseña UNA sola base de datos con FKs entre `ventas`, `clientes`, `usuarios`
y `productos`. Esta arquitectura NO hace eso: un esquema por servicio, sin FKs entre
esquemas. Consecuencia para `ms-ventas`: guarda `cedula_cliente` y `cedula_usuario`
como valores sin FK. `cedula_cliente` sí se valida llamando por HTTP a `ms-clientes`
(el endpoint `/lote` existe para eso), autenticado con el JWT del usuario de sistema
(ver "Autenticación servicio-a-servicio"). `cedula_usuario` NO se valida contra
`ms-identidad`: sale del claim `cedula` del JWT del usuario que hace la venta y se usa
tal cual, porque el token ya prueba que `ms-identidad` autenticó a ese usuario.
Decisión consciente para evitar un round-trip HTTP extra por cada venta; se asume la
ventana de vigencia del token (1h, `app.jwt.expiracion-ms`).

## Cómo trabajar conmigo

Proyecto académico. Debo poder entender y explicar cada línea ante mi profesor.

- Avanza por etapas. No pases a la siguiente fase hasta que yo lo indique.
- Antes de implementar, explica: qué se construye, en qué microservicio, qué archivos
  se crean o modifican, dónde van, qué dependencias y qué configuración de MySQL hacen
  falta, cómo se ejecuta y cómo se prueba.
- Analiza antes de modificar. No regeneres archivos existentes; ante un error, corrige
  lo que hay en vez de rehacer. Respeta el código que ya funciona.
- Si hay contradicción o ambigüedad entre el PDF y lo que te pido, señálala y
  pregúntame. No decidas en silencio.
- Diferencia siempre: lo que exige el documento / lo que es decisión técnica / lo que
  es recomendación tuya.
- No inventes requisitos. No agregues patrones ni tecnologías para complejizar.
- No hagas commit ni push a menos que te lo pida.
- No afirmes que algo funciona si no lo comprobaste.
- No modifiques este archivo (CLAUDE.md) sin pedirme permiso primero.